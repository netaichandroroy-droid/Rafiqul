package com.example.ui

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AiVoiceResult
import com.example.ai.AiVoiceScope
import com.example.ai.AiVoiceStatus
import com.example.ai.BanglaAiVoiceAssistant
import com.example.ai.FemaleAiVoiceOption
import com.example.auth.AppLockCredentialStore
import com.example.auth.PasswordHasher
import com.example.data.BackupExporter
import com.example.data.CloudAutoSyncRepository
import com.example.data.CloudSyncPrefsStore
import com.example.data.CloudSyncUiState
import com.example.data.DriveUnauthorizedException
import com.example.data.GoogleDriveBackupClient
import com.example.data.GoogleDriveBackupStatus
import com.example.data.GoogleDriveBackupStore
import com.example.data.LocalTransactionStore
import com.example.data.MemberAppLinkStore
import com.example.data.MemberStore
import com.example.data.MemberTakaJomaStore
import com.example.data.MonthlySalesStore
import com.example.data.OnlinePaymentStore
import com.example.model.AccountType
import com.example.model.AreaManagerRoleFilter
import com.example.model.AsmNameReportScope
import com.example.model.BanglaFormatters
import com.example.model.BangladeshBankDirectory
import com.example.model.HisabTransaction
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.OnlinePaymentConfig
import com.example.model.PaymentChannel
import com.example.model.ReportCategory
import com.example.model.ReportPeriodScope
import com.example.model.TakaJomaStatus
import com.example.model.TxFilter
import com.example.model.TxType
import com.google.firebase.Timestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

enum class HisabTab(val label: String) {
    HOME("Home"),
    TRANSACTIONS("Ledger"),
    REPORTS("Reports"),
    ACCOUNTS("Accounts"),
    SETTINGS("Settings")
}

enum class SettingsSubPage {
    MAIN,
    MEMBER_LIST,
    ONLINE_PAYMENT,
    MEMBER_TAKA_JOMA,
    SALES_UPLOAD
}

data class HisabUiControls(
    val selectedTab: HisabTab = HisabTab.HOME,
    val settingsSubPage: SettingsSubPage = SettingsSubPage.MAIN,
    val selectedReportCategory: ReportCategory = ReportCategory.MONTHLY,
    val reportPeriodScope: ReportPeriodScope = ReportPeriodScope.SELECTED_MONTH,
    val asmNameScope: AsmNameReportScope = AsmNameReportScope.DEPOT_TEAM,
    val areaRoleFilter: AreaManagerRoleFilter = AreaManagerRoleFilter.ALL,
    val filter: TxFilter = TxFilter.ALL,
    val reportMonth: String = BanglaFormatters.currentYearMonth(),
    val salesMonth: String = BanglaFormatters.currentYearMonth(),
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null
)

data class DriveUiState(
    val isConnected: Boolean = false,
    val connectedEmail: String = "",
    val driveFileId: String = "",
    val lastBackupAtMs: Long = 0L,
    val lastBackupSummary: String = "",
    val autoBackupEnabled: Boolean = true,
    val isSyncing: Boolean = false,
    val oauthConnected: Boolean = false,
    val safDocumentUri: String = "",
    val safDisplayName: String = ""
)

data class AiVoiceUiState(
    val status: AiVoiceStatus = AiVoiceStatus.Idle,
    val selectedVoice: FemaleAiVoiceOption = FemaleAiVoiceOption.AOEDE,
    val selectedScope: AiVoiceScope = AiVoiceScope.ALL_MEMBERS,
    val selectedMemberId: String? = null,
    val lastScript: String = "",
    val hasCachedAudio: Boolean = false
) {
    val isBusy: Boolean
        get() = status is AiVoiceStatus.ComposingScript || status is AiVoiceStatus.GeneratingAudio

    val isPlaying: Boolean
        get() = status is AiVoiceStatus.Playing
}

class LocalHisabViewModel(
    private val localStore: LocalTransactionStore,
    private val memberStore: MemberStore,
    private val onlinePaymentStore: OnlinePaymentStore,
    private val takaJomaStore: MemberTakaJomaStore? = null,
    private val monthlySalesStore: MonthlySalesStore? = null,
    private val ownerUserId: String = DEFAULT_ADMIN_OWNER_ID,
    private val reservedAdminLoginId: String = AppLockCredentialStore.DEFAULT_LOGIN_ID,
    private val memberAppLinkStore: MemberAppLinkStore? = null,
    private val driveBackupStore: GoogleDriveBackupStore? = null,
    private val cloudSyncRepository: CloudAutoSyncRepository? = null,
    private val cloudSyncPrefs: CloudSyncPrefsStore? = null
) : ViewModel() {

    private var allTransactions: List<HisabTransaction> = localStore.loadAll()
    private val _transactions = MutableStateFlow(filterForOwner(allTransactions))
    val transactions: StateFlow<List<HisabTransaction>> = _transactions.asStateFlow()

    private val isAdminSession = ownerUserId == DEFAULT_ADMIN_OWNER_ID
    private val scopedMemberId: String? = if (isAdminSession) {
        null
    } else {
        ownerUserId.removePrefix("member_").takeIf { it.isNotBlank() }
    }
    private val _members = MutableStateFlow(if (isAdminSession) memberStore.loadAll() else emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _onlinePaymentConfig = MutableStateFlow(onlinePaymentStore.load())
    val onlinePaymentConfig: StateFlow<OnlinePaymentConfig> = _onlinePaymentConfig.asStateFlow()

    private val _takaJomaList = MutableStateFlow(loadScopedTakaJoma())
    val takaJomaList: StateFlow<List<MemberTakaJoma>> = _takaJomaList.asStateFlow()

    private val _monthlySales = MutableStateFlow(monthlySalesStore?.loadAll().orEmpty())
    val monthlySales: StateFlow<List<MonthlySalesRecord>> = _monthlySales.asStateFlow()

    private val _controls = MutableStateFlow(HisabUiControls())
    val controls: StateFlow<HisabUiControls> = _controls.asStateFlow()

    private val _driveState = MutableStateFlow(
        driveBackupStore?.loadStatus()?.let { status ->
            DriveUiState(
                isConnected = status.isConnected,
                connectedEmail = status.connectedEmail,
                driveFileId = status.driveFileId,
                lastBackupAtMs = status.lastBackupAtMs,
                lastBackupSummary = status.lastBackupSummary,
                autoBackupEnabled = status.autoBackupEnabled,
                oauthConnected = status.oauthConnected,
                safDocumentUri = status.safDocumentUri,
                safDisplayName = status.safDisplayName
            )
        } ?: DriveUiState()
    )
    val driveState: StateFlow<DriveUiState> = _driveState.asStateFlow()

    private val _autoBackupRevision = MutableStateFlow(0)
    val autoBackupRevision: StateFlow<Int> = _autoBackupRevision.asStateFlow()

    private val aiVoiceAssistant by lazy { BanglaAiVoiceAssistant() }
    private var aiVoiceJob: Job? = null
    private var cachedVoiceResult: AiVoiceResult? = null
    private val _aiVoiceState = MutableStateFlow(AiVoiceUiState())
    val aiVoiceState: StateFlow<AiVoiceUiState> = _aiVoiceState.asStateFlow()

    val cloudSyncState: StateFlow<CloudSyncUiState> = cloudSyncRepository?.syncState
        ?: MutableStateFlow(CloudSyncUiState()).asStateFlow()

    private var lastSyncedDataSignature: String = ""

    init {
        lastSyncedDataSignature = computeDomainSignature()
        cloudSyncRepository?.let { repo ->
            viewModelScope.launch {
                repo.remoteUpdates.collect {
                    val beforeSig = lastSyncedDataSignature
                    refreshSharedData()
                    val afterSig = computeDomainSignature()
                    if (afterSig != beforeSig) {
                        lastSyncedDataSignature = afterSig
                        notifyAdminDataChanged()
                    }
                }
            }
        }
    }

    private fun computeDomainSignature(): String {
        val txPart = allTransactions.joinToString(",") { "${it.id}:${it.updatedAt?.seconds ?: 0}:${it.amount}" }
        val memberPart = _members.value.joinToString(",") { "${it.id}:${it.isActive}:${it.code}" }
        val takaPart = takaJomaStore?.loadAll().orEmpty().joinToString(",") { "${it.id}:${it.approvalStatus}:${it.reviewedAtMs}" }
        val salesPart = _monthlySales.value.joinToString(",") { "${it.id}:${it.tpSales}:${it.tpPlusVat}" }
        return "${txPart.hashCode()}|${memberPart.hashCode()}|${takaPart.hashCode()}|${salesPart.hashCode()}"
    }

    private fun pushCloudMutation(block: suspend CloudAutoSyncRepository.() -> Unit) {
        val repo = cloudSyncRepository ?: return
        if (!repo.isAutoSyncActive()) return
        viewModelScope.launch {
            repo.block()
        }
    }

    fun setCloudAutoSyncEnabled(enabled: Boolean) {
        val repo = cloudSyncRepository ?: return
        repo.setAutoSyncEnabled(enabled)
        showMessage(
            if (enabled) "Auto cloud sync enabled"
            else "Auto cloud sync paused"
        )
    }

    fun syncCloudNow() {
        val repo = cloudSyncRepository ?: return
        viewModelScope.launch {
            val res = repo.syncAllNow()
            res.onSuccess {
                showMessage("All data synced to cloud successfully")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Cloud sync failed")
            }
        }
    }

    fun syncAdminCredentialsToCloud(
        loginId: String,
        passwordHash: String,
        isDefaultPassword: Boolean
    ) {
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_APP_CONFIG,
            CloudAutoSyncRepository.DOC_ADMIN_AUTH
        )
        pushCloudMutation {
            syncAdminCredentials(
                loginId = loginId,
                passwordHash = passwordHash,
                isDefaultPassword = isDefaultPassword
            )
        }
    }

    fun syncMemberToCloud(member: Member) {
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_MEMBERS,
            CloudAutoSyncRepository.sanitizeId(member.id)
        )
        pushCloudMutation {
            upsertMember(member)
        }
    }

    private fun notifyAdminDataChanged() {
        lastSyncedDataSignature = computeDomainSignature()
        if (isAdminSession && _driveState.value.isConnected && _driveState.value.autoBackupEnabled) {
            _autoBackupRevision.update { it + 1 }
        }
    }

    fun notifyMemberAppLinkUpdated() {
        notifyAdminDataChanged()
        val url = memberAppLinkStore?.getDownloadUrl().orEmpty()
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_APP_CONFIG,
            CloudAutoSyncRepository.DOC_MEMBER_APP_LINK
        )
        pushCloudMutation {
            syncMemberAppLink(url)
        }
    }

    private fun loadScopedTakaJoma(): List<MemberTakaJoma> {
        val store = takaJomaStore ?: return emptyList()
        return if (isAdminSession) {
            store.loadAll()
        } else {
            scopedMemberId?.let { store.loadForMember(it) } ?: emptyList()
        }
    }

    private fun filterForOwner(list: List<HisabTransaction>): List<HisabTransaction> {
        return list.filter { tx ->
            if (ownerUserId == DEFAULT_ADMIN_OWNER_ID) {
                tx.userId == DEFAULT_ADMIN_OWNER_ID || tx.userId.isBlank()
            } else {
                tx.userId == ownerUserId
            }
        }
    }

    fun refreshSharedData() {
        allTransactions = localStore.loadAll()
        _transactions.value = filterForOwner(allTransactions)
        _takaJomaList.value = loadScopedTakaJoma()
        _monthlySales.value = monthlySalesStore?.loadAll().orEmpty()
        _onlinePaymentConfig.value = onlinePaymentStore.load()
        if (isAdminSession) {
            _members.value = memberStore.loadAll()
            driveBackupStore?.loadStatus()?.let { persisted ->
                _driveState.update { current ->
                    current.copy(
                        isConnected = persisted.isConnected,
                        connectedEmail = persisted.connectedEmail,
                        driveFileId = persisted.driveFileId,
                        lastBackupAtMs = persisted.lastBackupAtMs,
                        lastBackupSummary = persisted.lastBackupSummary,
                        autoBackupEnabled = persisted.autoBackupEnabled,
                        oauthConnected = persisted.oauthConnected,
                        safDocumentUri = persisted.safDocumentUri,
                        safDisplayName = persisted.safDisplayName
                    )
                }
            }
        }
    }

    fun selectTab(tab: HisabTab) {
        refreshSharedData()
        _controls.update {
            it.copy(
                selectedTab = tab,
                settingsSubPage = if (tab == HisabTab.SETTINGS) it.settingsSubPage else SettingsSubPage.MAIN
            )
        }
    }

    fun openReportCategory(category: ReportCategory) {
        refreshSharedData()
        _controls.update {
            it.copy(
                selectedTab = HisabTab.REPORTS,
                settingsSubPage = SettingsSubPage.MAIN,
                selectedReportCategory = category
            )
        }
    }

    fun setReportPeriodScope(scope: ReportPeriodScope) {
        _controls.update { it.copy(reportPeriodScope = scope) }
    }

    fun setAsmNameScope(scope: AsmNameReportScope) {
        _controls.update { it.copy(asmNameScope = scope) }
    }

    fun setAreaRoleFilter(filter: AreaManagerRoleFilter) {
        _controls.update { it.copy(areaRoleFilter = filter) }
    }

    fun openSettingsSubPage(page: SettingsSubPage) {
        _controls.update { it.copy(settingsSubPage = page) }
    }

    fun setFilter(filter: TxFilter) {
        _controls.update { it.copy(filter = filter) }
    }

    fun shiftReportMonth(delta: Int) {
        _controls.update { state ->
            state.copy(reportMonth = BanglaFormatters.shiftYearMonth(state.reportMonth, delta))
        }
    }

    fun shiftSalesMonth(delta: Int) {
        _controls.update { state ->
            state.copy(salesMonth = BanglaFormatters.shiftYearMonth(state.salesMonth, delta))
        }
    }

    fun clearSnackbar() {
        _controls.update { it.copy(snackbarMessage = null) }
    }

    fun showMessage(message: String) {
        _controls.update { it.copy(snackbarMessage = message) }
    }

    fun saveTransaction(
        editingId: String?,
        type: TxType,
        amount: Double,
        category: String,
        note: String,
        date: String,
        account: AccountType,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isAdminSession && editingId != null) {
            showMessage("Members are not allowed to edit transactions")
            onComplete(false)
            return
        }
        val cleanCategory = category.trim()
        val cleanNote = note.trim().ifBlank { cleanCategory }
        val now = Timestamp.now()

        var savedTx: HisabTransaction? = null
        val updatedAll = if (editingId == null) {
            val created = HisabTransaction(
                id = UUID.randomUUID().toString(),
                userId = ownerUserId,
                type = type.wireValue,
                amount = amount,
                category = cleanCategory,
                note = cleanNote,
                date = date,
                account = account.wireValue,
                createdAt = now,
                updatedAt = now
            )
            savedTx = created
            listOf(created) + allTransactions
        } else {
            allTransactions.map { existing ->
                val isOwned = if (isAdminSession) {
                    existing.userId == DEFAULT_ADMIN_OWNER_ID || existing.userId.isBlank()
                } else {
                    existing.userId == ownerUserId
                }
                if (existing.id == editingId && isOwned) {
                    existing.copy(
                        type = type.wireValue,
                        amount = amount,
                        category = cleanCategory,
                        note = cleanNote,
                        date = date,
                        account = account.wireValue,
                        updatedAt = now
                    ).also { savedTx = it }
                } else {
                    existing
                }
            }
        }.sortedWith(
            compareByDescending<HisabTransaction> { it.date }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
        )

        localStore.saveAll(updatedAll)
        allTransactions = updatedAll
        _transactions.value = filterForOwner(updatedAll)
        savedTx?.let { tx ->
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_TRANSACTIONS,
                CloudAutoSyncRepository.sanitizeId(tx.id)
            )
            pushCloudMutation { upsertTransaction(tx) }
        }
        notifyAdminDataChanged()
        val submittedName = cleanNote.ifBlank { cleanCategory }.ifBlank { type.bnLabel }
        _controls.update {
            it.copy(
                snackbarMessage = if (editingId == null) {
                    "$submittedName submitted successfully"
                } else {
                    "$submittedName updated successfully"
                }
            )
        }
        onComplete(true)
    }

    fun deleteTransaction(transaction: HisabTransaction) {
        if (!isAdminSession) {
            showMessage("Members are not allowed to delete transactions")
            return
        }
        val isOwned = transaction.userId == DEFAULT_ADMIN_OWNER_ID || transaction.userId.isBlank()
        if (!isOwned) return
        val updatedAll = allTransactions.filterNot { it.id == transaction.id }
        localStore.saveAll(updatedAll)
        allTransactions = updatedAll
        _transactions.value = filterForOwner(updatedAll)
        cloudSyncPrefs?.markPendingDelete(
            CloudAutoSyncRepository.COL_TRANSACTIONS,
            CloudAutoSyncRepository.sanitizeId(transaction.id)
        )
        pushCloudMutation { deleteTransaction(transaction.id) }
        notifyAdminDataChanged()
        _controls.update { it.copy(snackbarMessage = "Transaction deleted") }
    }

    fun buildFullBackupJson(): String {
        val currentTakaJoma = takaJomaStore?.loadAll() ?: _takaJomaList.value
        val currentSales = monthlySalesStore?.loadAll() ?: _monthlySales.value
        val currentMembers = if (isAdminSession) memberStore.loadAll() else emptyList()
        return BackupExporter.buildFullAppBackupJson(
            transactions = allTransactions,
            members = currentMembers,
            memberTakaJoma = currentTakaJoma,
            monthlySales = currentSales,
            onlinePaymentConfig = _onlinePaymentConfig.value,
            memberAppDownloadUrl = memberAppLinkStore?.getDownloadUrl().orEmpty()
        )
    }

    fun restoreFromJson(jsonText: String): Boolean {
        if (!isAdminSession) return false
        val parsed = BackupExporter.parseFullAppBackupJson(jsonText)
        if (parsed.isFailure) {
            _controls.update {
                it.copy(snackbarMessage = parsed.exceptionOrNull()?.message ?: "Please select a valid JSON backup")
            }
            return false
        }
        val backup = parsed.getOrThrow()
        if (!backup.isFullSnapshot) {
            val now = Timestamp.now()
            val restored = backup.transactions.map { it.copy(userId = ownerUserId, createdAt = now, updatedAt = now) }
            val combinedAll = (restored + allTransactions).sortedWith(
                compareByDescending<HisabTransaction> { it.date }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
            localStore.saveAll(combinedAll)
            allTransactions = combinedAll
            _transactions.value = filterForOwner(combinedAll)
            restored.forEach { tx ->
                cloudSyncPrefs?.markPendingUpsert(
                    CloudAutoSyncRepository.COL_TRANSACTIONS,
                    CloudAutoSyncRepository.sanitizeId(tx.id)
                )
            }
            pushCloudMutation { restored.forEach { upsertTransaction(it) } }
            notifyAdminDataChanged()
            _controls.update {
                it.copy(snackbarMessage = "${restored.size} transactions restored")
            }
            return true
        }

        val sortedTx = backup.transactions.sortedWith(
            compareByDescending<HisabTransaction> { it.date }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
        )
        localStore.saveAll(sortedTx)
        allTransactions = sortedTx
        _transactions.value = filterForOwner(sortedTx)
        sortedTx.forEach {
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_TRANSACTIONS,
                CloudAutoSyncRepository.sanitizeId(it.id)
            )
        }

        memberStore.saveAll(backup.members)
        _members.value = memberStore.loadAll()
        backup.members.forEach {
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_MEMBERS,
                CloudAutoSyncRepository.sanitizeId(it.id)
            )
        }

        takaJomaStore?.let { store ->
            store.saveAll(backup.memberTakaJoma)
            _takaJomaList.value = loadScopedTakaJoma()
            backup.memberTakaJoma.forEach {
                cloudSyncPrefs?.markPendingUpsert(
                    CloudAutoSyncRepository.COL_TAKA_JOMA,
                    CloudAutoSyncRepository.sanitizeId(it.id)
                )
            }
        }

        monthlySalesStore?.let { store ->
            store.saveAll(backup.monthlySales)
            _monthlySales.value = store.loadAll()
            backup.monthlySales.forEach {
                cloudSyncPrefs?.markPendingUpsert(
                    CloudAutoSyncRepository.COL_MONTHLY_SALES,
                    CloudAutoSyncRepository.sanitizeId(it.id)
                )
            }
        }

        backup.onlinePaymentConfig?.let { cfg ->
            onlinePaymentStore.save(cfg)
            _onlinePaymentConfig.value = cfg
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_APP_CONFIG,
                CloudAutoSyncRepository.DOC_ONLINE_PAYMENT
            )
        }

        backup.memberAppDownloadUrl?.let { url ->
            memberAppLinkStore?.saveDownloadUrl(url)
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_APP_CONFIG,
                CloudAutoSyncRepository.DOC_MEMBER_APP_LINK
            )
        }

        pushCloudMutation { syncAllNow() }
        notifyAdminDataChanged()
        _controls.update {
            it.copy(snackbarMessage = "All data restored (${backup.buildBanglaSummary()})")
        }
        return true
    }

    fun completeDriveConnect(
        accessToken: String,
        hintEmail: String = "",
        onUnauthorized: suspend () -> Unit = {}
    ) {
        if (!isAdminSession) return
        viewModelScope.launch {
            _driveState.update { it.copy(isSyncing = true) }
            val result = withContext(Dispatchers.IO) {
                val fetchedEmail = GoogleDriveBackupClient.fetchUserEmail(accessToken).orEmpty()
                val resolvedEmail = fetchedEmail.ifBlank { hintEmail.trim() }
                val findRes = GoogleDriveBackupClient.findBackupFile(accessToken)
                Triple(resolvedEmail, findRes.getOrNull(), findRes.exceptionOrNull())
            }
            val (email, existingMeta, findErr) = result
            if (findErr is DriveUnauthorizedException) {
                onUnauthorized()
                _driveState.update { it.copy(isSyncing = false) }
                showMessage(findErr.localizedMessage ?: "Google Drive authorization expired. Please reconnect.")
                return@launch
            }
            driveBackupStore?.saveConnectedAccount(email)
            if (existingMeta != null) {
                driveBackupStore?.updateBackupMetadata(
                    fileId = existingMeta.id,
                    timestampMs = _driveState.value.lastBackupAtMs,
                    summary = _driveState.value.lastBackupSummary,
                    email = email
                )
            }
            _driveState.update { state ->
                state.copy(
                    isConnected = true,
                    oauthConnected = true,
                    connectedEmail = email.ifBlank { state.connectedEmail },
                    driveFileId = existingMeta?.id ?: state.driveFileId,
                    isSyncing = false
                )
            }
            syncPersistedDriveConfigToCloud()
            val emailText = if (email.isNotBlank()) " ($email)" else ""
            showMessage("Google Drive connected$emailText")
        }
    }

    fun backupAllToGoogleDrive(
        accessToken: String,
        silent: Boolean = false,
        hintEmail: String = "",
        onUnauthorized: suspend () -> Unit = {}
    ) {
        if (!isAdminSession) return
        val jsonPayload = buildFullBackupJson()
        val currentTaka = takaJomaStore?.loadAll() ?: _takaJomaList.value
        val currentSales = monthlySalesStore?.loadAll() ?: _monthlySales.value
        val summaryText = listOf(
            "${allTransactions.size} transactions",
            "${_members.value.size} members",
            "${currentTaka.size} deposits",
            "${currentSales.size} sales"
        ).joinToString(" • ")

        viewModelScope.launch {
            _driveState.update { it.copy(isSyncing = true) }
            val knownFileId = _driveState.value.driveFileId.takeIf { it.isNotBlank() }
            val uploadOutcome = withContext(Dispatchers.IO) {
                val email = if (_driveState.value.connectedEmail.isBlank()) {
                    GoogleDriveBackupClient.fetchUserEmail(accessToken).orEmpty()
                        .ifBlank { hintEmail.trim() }
                } else {
                    _driveState.value.connectedEmail
                }
                val uploadRes = GoogleDriveBackupClient.uploadFullBackup(
                    accessToken = accessToken,
                    knownFileId = knownFileId,
                    jsonPayload = jsonPayload
                )
                email to uploadRes
            }
            val (email, uploadRes) = uploadOutcome
            uploadRes.onSuccess { meta ->
                val nowMs = System.currentTimeMillis()
                driveBackupStore?.updateBackupMetadata(
                    fileId = meta.id,
                    timestampMs = nowMs,
                    summary = summaryText,
                    email = email
                )
                _driveState.update { state ->
                    state.copy(
                        isConnected = true,
                        oauthConnected = true,
                        connectedEmail = email.ifBlank { state.connectedEmail },
                        driveFileId = meta.id,
                        lastBackupAtMs = nowMs,
                        lastBackupSummary = summaryText,
                        isSyncing = false
                    )
                }
                syncPersistedDriveConfigToCloud()
                if (!silent) {
                    showMessage("All data backed up to Google Drive")
                }
            }.onFailure { err ->
                if (err is DriveUnauthorizedException) {
                    onUnauthorized()
                }
                _driveState.update { it.copy(isSyncing = false) }
                if (!silent) {
                    showMessage(err.localizedMessage ?: "Google Drive backup failed")
                }
            }
        }
    }

    fun restoreAllFromGoogleDrive(
        accessToken: String,
        hintEmail: String = "",
        onUnauthorized: suspend () -> Unit = {}
    ) {
        if (!isAdminSession) return
        viewModelScope.launch {
            _driveState.update { it.copy(isSyncing = true) }
            val knownFileId = _driveState.value.driveFileId.takeIf { it.isNotBlank() }
            val downloadOutcome = withContext(Dispatchers.IO) {
                val email = if (_driveState.value.connectedEmail.isBlank()) {
                    GoogleDriveBackupClient.fetchUserEmail(accessToken).orEmpty()
                        .ifBlank { hintEmail.trim() }
                } else {
                    _driveState.value.connectedEmail
                }
                val downloadRes = GoogleDriveBackupClient.downloadFullBackup(
                    accessToken = accessToken,
                    knownFileId = knownFileId
                )
                email to downloadRes
            }
            val (email, downloadRes) = downloadOutcome
            downloadRes.onSuccess { (meta, jsonPayload) ->
                val restoredOk = restoreFromJson(jsonPayload)
                if (restoredOk) {
                    val nowMs = System.currentTimeMillis()
                    val summary = "${allTransactions.size} transactions • ${_members.value.size} members"
                    driveBackupStore?.updateBackupMetadata(
                        fileId = meta.id,
                        timestampMs = nowMs,
                        summary = summary,
                        email = email
                    )
                    _driveState.update { state ->
                        state.copy(
                            isConnected = true,
                            oauthConnected = true,
                            connectedEmail = email.ifBlank { state.connectedEmail },
                            driveFileId = meta.id,
                            lastBackupAtMs = nowMs,
                            lastBackupSummary = summary,
                            isSyncing = false
                        )
                    }
                    syncPersistedDriveConfigToCloud()
                } else {
                    _driveState.update { it.copy(isSyncing = false) }
                }
            }.onFailure { err ->
                if (err is DriveUnauthorizedException) {
                    onUnauthorized()
                }
                _driveState.update { it.copy(isSyncing = false) }
                showMessage(err.localizedMessage ?: "Failed to restore from Google Drive")
            }
        }
    }

    fun connectAndBackupSafUri(
        context: Context,
        uri: Uri,
        emailHint: String = ""
    ) {
        if (!isAdminSession) return
        val appContext = context.applicationContext
        val uriString = uri.toString()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                GoogleDriveBackupClient.persistSafPermission(appContext, uri)
            }
            val displayName = withContext(Dispatchers.IO) {
                GoogleDriveBackupClient.resolveSafDisplayName(appContext, uri)
            }
            driveBackupStore?.saveSafDocument(
                uriString = uriString,
                displayName = displayName,
                email = emailHint
            )
            _driveState.update { state ->
                state.copy(
                    isConnected = true,
                    safDocumentUri = uriString,
                    safDisplayName = displayName,
                    connectedEmail = state.connectedEmail.ifBlank { emailHint.trim() }
                )
            }
            backupAllToSafUri(
                context = appContext,
                uriString = uriString,
                silent = false,
                emailHint = emailHint
            )
        }
    }

    fun backupAllToSafUri(
        context: Context,
        uriString: String,
        silent: Boolean = false,
        emailHint: String = ""
    ) {
        if (!isAdminSession || uriString.isBlank()) return
        val appContext = context.applicationContext
        val jsonPayload = buildFullBackupJson()
        val currentTaka = takaJomaStore?.loadAll() ?: _takaJomaList.value
        val currentSales = monthlySalesStore?.loadAll() ?: _monthlySales.value
        val summaryText = listOf(
            "${allTransactions.size} transactions",
            "${_members.value.size} members",
            "${currentTaka.size} deposits",
            "${currentSales.size} sales"
        ).joinToString(" • ")

        viewModelScope.launch {
            _driveState.update { it.copy(isSyncing = true) }
            val writeRes = withContext(Dispatchers.IO) {
                GoogleDriveBackupClient.writeJsonToSafUri(appContext, uriString, jsonPayload)
            }
            writeRes.onSuccess { displayName ->
                val nowMs = System.currentTimeMillis()
                driveBackupStore?.updateSafBackupMetadata(
                    uriString = uriString,
                    displayName = displayName,
                    timestampMs = nowMs,
                    summary = summaryText,
                    email = emailHint
                )
                _driveState.update { state ->
                    state.copy(
                        isConnected = true,
                        safDocumentUri = uriString,
                        safDisplayName = displayName.ifBlank { state.safDisplayName },
                        connectedEmail = state.connectedEmail.ifBlank { emailHint.trim() },
                        lastBackupAtMs = nowMs,
                        lastBackupSummary = summaryText,
                        isSyncing = false
                    )
                }
                syncPersistedDriveConfigToCloud()
                if (!silent) {
                    showMessage("Saved all data to Google Drive file ($displayName)")
                }
            }.onFailure { err ->
                _driveState.update { it.copy(isSyncing = false) }
                if (!silent) {
                    showMessage(err.localizedMessage ?: "Backup to Google Drive file failed. Please relink the file.")
                }
            }
        }
    }

    fun restoreAllFromSafUri(
        context: Context,
        uriString: String,
        persistPermission: Boolean = false,
        emailHint: String = ""
    ) {
        if (!isAdminSession || uriString.isBlank()) return
        val appContext = context.applicationContext
        val uri = uriString.toUri()
        viewModelScope.launch {
            _driveState.update { it.copy(isSyncing = true) }
            val readRes = withContext(Dispatchers.IO) {
                if (persistPermission) {
                    GoogleDriveBackupClient.persistSafPermission(appContext, uri)
                }
                GoogleDriveBackupClient.readJsonFromSafUri(appContext, uriString)
            }
            readRes.onSuccess { (displayName, jsonText) ->
                val restoredOk = restoreFromJson(jsonText)
                if (restoredOk) {
                    val nowMs = System.currentTimeMillis()
                    val summary = "${allTransactions.size} transactions • ${_members.value.size} members"
                    if (persistPermission || _driveState.value.safDocumentUri == uriString) {
                        driveBackupStore?.updateSafBackupMetadata(
                            uriString = uriString,
                            displayName = displayName,
                            timestampMs = nowMs,
                            summary = summary,
                            email = emailHint
                        )
                    }
                    _driveState.update { state ->
                        val keepSaf = persistPermission || state.safDocumentUri == uriString
                        state.copy(
                            isConnected = state.isConnected || keepSaf,
                            safDocumentUri = if (keepSaf) uriString else state.safDocumentUri,
                            safDisplayName = if (keepSaf) displayName else state.safDisplayName,
                            connectedEmail = state.connectedEmail.ifBlank { emailHint.trim() },
                            lastBackupAtMs = nowMs,
                            lastBackupSummary = summary,
                            isSyncing = false
                        )
                    }
                    syncPersistedDriveConfigToCloud()
                } else {
                    _driveState.update { it.copy(isSyncing = false) }
                }
            }.onFailure { err ->
                _driveState.update { it.copy(isSyncing = false) }
                showMessage(err.localizedMessage ?: "Failed to restore from Google Drive file")
            }
        }
    }

    fun setDriveAutoBackup(enabled: Boolean) {
        if (!isAdminSession) return
        driveBackupStore?.setAutoBackupEnabled(enabled)
        _driveState.update { it.copy(autoBackupEnabled = enabled) }
        syncPersistedDriveConfigToCloud()
        showMessage(
            if (enabled) "Automatic Google Drive backup enabled"
            else "Automatic Google Drive backup paused"
        )
    }

    fun disconnectGoogleDrive(context: Context? = null) {
        if (!isAdminSession) return
        val existingSafUri = _driveState.value.safDocumentUri
        if (context != null && existingSafUri.isNotBlank()) {
            GoogleDriveBackupClient.releaseSafPermission(context, existingSafUri)
        }
        driveBackupStore?.disconnect()
        _driveState.update {
            DriveUiState(
                isConnected = false,
                connectedEmail = "",
                driveFileId = "",
                lastBackupAtMs = 0L,
                lastBackupSummary = "",
                autoBackupEnabled = true,
                isSyncing = false,
                oauthConnected = false,
                safDocumentUri = "",
                safDisplayName = ""
            )
        }
        syncPersistedDriveConfigToCloud()
        showMessage("Google Drive account disconnected")
    }

    private fun syncPersistedDriveConfigToCloud() {
        val status = driveBackupStore?.loadStatus() ?: _driveState.value.let {
            GoogleDriveBackupStatus(
                isConnected = it.isConnected,
                connectedEmail = it.connectedEmail,
                driveFileId = it.driveFileId,
                lastBackupAtMs = it.lastBackupAtMs,
                lastBackupSummary = it.lastBackupSummary,
                autoBackupEnabled = it.autoBackupEnabled,
                oauthConnected = it.oauthConnected,
                safDocumentUri = it.safDocumentUri,
                safDisplayName = it.safDisplayName
            )
        }
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_APP_CONFIG,
            CloudAutoSyncRepository.DOC_DRIVE_BACKUP
        )
        pushCloudMutation {
            syncDriveBackupConfig(status)
        }
    }

    fun saveMember(
        editingId: String?,
        name: String,
        designation: String,
        phone: String,
        depot: String,
        isActive: Boolean,
        email: String = "",
        address: String = "",
        joiningDate: String = "",
        code: String = "",
        area: String = "",
        onComplete: (Boolean) -> Unit
    ) {
        if (!isAdminSession) {
            onComplete(false)
            return
        }
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        val cleanDesignation = designation.trim()
        val cleanPhone = phone.trim()
        val cleanDepot = depot.trim()
        val cleanEmail = email.trim()
        val cleanAddress = address.trim()
        val cleanJoiningDate = joiningDate.trim()
        val cleanCode = code.trim().replace(Regex("\\s+"), " ")
        val cleanArea = area.trim()
        if (cleanCode.isEmpty()) {
            showMessage("Please enter a member code")
            onComplete(false)
            return
        }
        if (cleanName.isEmpty()) {
            showMessage("Please enter the member name")
            onComplete(false)
            return
        }

        val conflictsAdmin = cleanCode.equals(reservedAdminLoginId.trim(), ignoreCase = true) ||
            cleanCode.equals(AppLockCredentialStore.DEFAULT_LOGIN_ID, ignoreCase = true)
        val conflictsMember = _members.value.any { other ->
            other.id != editingId &&
                other.code.trim().replace(Regex("\\s+"), " ").equals(cleanCode, ignoreCase = true)
        }
        if (conflictsAdmin || conflictsMember) {
            showMessage("An account already exists with this Code (Login ID). Please use a different code.")
            onComplete(false)
            return
        }

        var savedMember: Member? = null
        val updated = if (editingId == null) {
            val defaultHash = PasswordHasher.hashPassword(cleanCode, minLength = 1)
            val created = Member(
                id = UUID.randomUUID().toString(),
                name = cleanName,
                designation = cleanDesignation,
                phone = cleanPhone,
                depot = cleanDepot,
                code = cleanCode,
                area = cleanArea,
                isActive = isActive,
                passwordHash = defaultHash,
                isDefaultPassword = true,
                email = cleanEmail,
                address = cleanAddress,
                joiningDate = cleanJoiningDate.ifBlank { BanglaFormatters.todayIso() }
            )
            savedMember = created
            _members.value + created
        } else {
            _members.value.map { existing ->
                if (existing.id == editingId) {
                    val refreshedHash = if (existing.isDefaultPassword || existing.passwordHash.isBlank()) {
                        PasswordHasher.hashPassword(cleanCode, minLength = 1)
                    } else {
                        existing.passwordHash
                    }
                    existing.copy(
                        name = cleanName,
                        designation = cleanDesignation,
                        phone = cleanPhone,
                        depot = cleanDepot,
                        code = cleanCode,
                        area = cleanArea,
                        isActive = isActive,
                        passwordHash = refreshedHash,
                        email = cleanEmail,
                        address = cleanAddress,
                        joiningDate = cleanJoiningDate
                    ).also { savedMember = it }
                } else {
                    existing
                }
            }
        }
        memberStore.saveAll(updated)
        _members.value = updated
        savedMember?.let { m ->
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_MEMBERS,
                CloudAutoSyncRepository.sanitizeId(m.id)
            )
            pushCloudMutation { upsertMember(m) }
        }
        notifyAdminDataChanged()
        showMessage(
            if (editingId == null) {
                "$cleanName added successfully (Login ID & Password: $cleanCode)"
            } else {
                "$cleanName updated successfully"
            }
        )
        onComplete(true)
    }

    fun toggleMemberActive(memberId: String, isActive: Boolean) {
        if (!isAdminSession) return
        var target: Member? = null
        val updated = _members.value.map { member ->
            if (member.id == memberId) {
                member.copy(isActive = isActive).also { target = it }
            } else {
                member
            }
        }
        memberStore.saveAll(updated)
        _members.value = updated
        target?.let { m ->
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_MEMBERS,
                CloudAutoSyncRepository.sanitizeId(m.id)
            )
            pushCloudMutation { upsertMember(m) }
        }
        notifyAdminDataChanged()
    }

    fun resetMemberPassword(memberId: String) {
        if (!isAdminSession) return
        val target = _members.value.firstOrNull { it.id == memberId }
        if (target != null && target.code.isBlank()) {
            showMessage("Please set the member Code before resetting the password")
            return
        }
        val resetMember = memberStore.resetMemberPasswordToDefault(memberId) ?: return
        _members.value = _members.value.map { if (it.id == memberId) resetMember else it }
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_MEMBERS,
            CloudAutoSyncRepository.sanitizeId(resetMember.id)
        )
        pushCloudMutation { upsertMember(resetMember) }
        notifyAdminDataChanged()
        showMessage("${resetMember.name}'s password has been reset to '${resetMember.code}'")
    }

    fun deleteMember(member: Member) {
        if (!isAdminSession) return
        val updated = _members.value.filterNot { it.id == member.id }
        memberStore.saveAll(updated)
        _members.value = updated
        cloudSyncPrefs?.markPendingDelete(
            CloudAutoSyncRepository.COL_MEMBERS,
            CloudAutoSyncRepository.sanitizeId(member.id)
        )
        pushCloudMutation { deleteMember(member.id) }
        notifyAdminDataChanged()
        showMessage("Member deleted")
    }

    fun setOnlinePaymentEnabled(enabled: Boolean) {
        if (!isAdminSession) return
        val updated = _onlinePaymentConfig.value.copy(isOnlinePaymentEnabled = enabled)
        onlinePaymentStore.save(updated)
        _onlinePaymentConfig.value = updated
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_APP_CONFIG,
            CloudAutoSyncRepository.DOC_ONLINE_PAYMENT
        )
        pushCloudMutation { syncOnlinePaymentConfig(updated) }
        notifyAdminDataChanged()
        showMessage(if (enabled) "Online payment enabled" else "Online payment disabled")
    }

    fun togglePaymentAccountEnabled(accountId: String, enabled: Boolean) {
        if (!isAdminSession) return
        val updatedAccounts = _onlinePaymentConfig.value.accounts.map { account ->
            if (account.id == accountId) account.copy(isEnabled = enabled) else account
        }
        val updated = _onlinePaymentConfig.value.copy(accounts = updatedAccounts)
        onlinePaymentStore.save(updated)
        _onlinePaymentConfig.value = updated
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_APP_CONFIG,
            CloudAutoSyncRepository.DOC_ONLINE_PAYMENT
        )
        pushCloudMutation { syncOnlinePaymentConfig(updated) }
        notifyAdminDataChanged()
    }

    fun updatePaymentAccount(
        accountId: String,
        accountNumber: String,
        accountType: String,
        isEnabled: Boolean
    ) {
        if (!isAdminSession) return
        val updatedAccounts = _onlinePaymentConfig.value.accounts.map { account ->
            if (account.id == accountId) {
                account.copy(
                    accountNumber = accountNumber.trim(),
                    accountType = accountType.trim().ifBlank { account.accountType },
                    isEnabled = isEnabled
                )
            } else {
                account
            }
        }
        val updated = _onlinePaymentConfig.value.copy(accounts = updatedAccounts)
        onlinePaymentStore.save(updated)
        _onlinePaymentConfig.value = updated
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_APP_CONFIG,
            CloudAutoSyncRepository.DOC_ONLINE_PAYMENT
        )
        pushCloudMutation { syncOnlinePaymentConfig(updated) }
        notifyAdminDataChanged()
        val accountName = updatedAccounts.firstOrNull { it.id == accountId }
            ?.channel?.title?.ifBlank { null } ?: "Payment account"
        showMessage("$accountName saved successfully")
    }

    fun recordOnlinePaymentTransaction(
        channel: PaymentChannel,
        type: TxType,
        amount: Double,
        note: String,
        onComplete: (Boolean) -> Unit
    ) {
        saveTransaction(
            editingId = null,
            type = type,
            amount = amount,
            category = "Online Payment (${channel.title})",
            note = note.trim().ifBlank { "${channel.title} Payment" },
            date = BanglaFormatters.todayIso(),
            account = AccountType.BANK,
            onComplete = onComplete
        )
    }

    fun submitMemberTakaJoma(
        submissionId: String,
        memberId: String,
        name: String,
        code: String,
        depot: String,
        bankName: String = "",
        amount: Double,
        date: String,
        note: String,
        photoPath: String,
        slipHash: String = "",
        designation: String = "",
        area: String = "",
        onComplete: (Boolean) -> Unit
    ) {
        val store = takaJomaStore
        if (store == null) {
            onComplete(false)
            return
        }
        val effectiveMemberId = if (isAdminSession) {
            memberId.ifBlank { "admin_manual" }
        } else {
            scopedMemberId ?: memberId
        }
        val storedMember = memberStore.findById(effectiveMemberId)
        val cleanName = name.trim().ifEmpty { storedMember?.name.orEmpty().trim() }
        val cleanCode = code.trim().ifEmpty { storedMember?.code.orEmpty().trim() }
        val cleanDepot = depot.trim().ifEmpty { storedMember?.depot.orEmpty().trim() }
        val cleanDesignation = designation.trim().ifEmpty { storedMember?.designation.orEmpty().trim() }
        val cleanArea = area.trim().ifEmpty { storedMember?.area.orEmpty().trim() }
        val cleanBank = bankName.trim()
        val cleanPhotoPath = photoPath.trim()
        if (cleanName.isEmpty() || cleanCode.isEmpty() || cleanDepot.isEmpty() || amount <= 0.0) {
            showMessage("Please fill in name, code, depot, and a valid amount")
            onComplete(false)
            return
        }
        val resolvedHash = slipHash.trim().ifBlank {
            if (cleanPhotoPath.isNotBlank()) {
                com.example.data.MemberPhotoStorage.computeFileSha256(cleanPhotoPath)
            } else {
                ""
            }
        }
        val entry = MemberTakaJoma(
            id = submissionId.ifBlank { UUID.randomUUID().toString() },
            memberId = effectiveMemberId,
            name = cleanName,
            code = cleanCode,
            depot = cleanDepot,
            designation = cleanDesignation,
            area = cleanArea,
            bankName = cleanBank,
            amount = amount,
            date = date.trim().ifBlank { BanglaFormatters.todayIso() },
            note = note.trim(),
            photoPath = cleanPhotoPath,
            slipHash = resolvedHash,
            submittedByRole = if (isAdminSession) "ADMIN" else "MEMBER",
            approvalStatus = if (isAdminSession) {
                TakaJomaStatus.APPROVED.wireValue
            } else {
                TakaJomaStatus.PENDING.wireValue
            },
            reviewedAtMs = if (isAdminSession) System.currentTimeMillis() else 0L
        )
        store.tryAddSubmission(entry)
            .onSuccess {
                var linkedTx: HisabTransaction? = null
                if (isAdminSession && entry.resolvedStatus == TakaJomaStatus.APPROVED) {
                    linkedTx = upsertApprovedTakaJomaTransaction(entry, txDate = entry.date)
                }
                _takaJomaList.value = loadScopedTakaJoma()
                cloudSyncPrefs?.markPendingUpsert(
                    CloudAutoSyncRepository.COL_TAKA_JOMA,
                    CloudAutoSyncRepository.sanitizeId(entry.id)
                )
                linkedTx?.let {
                    cloudSyncPrefs?.markPendingUpsert(
                        CloudAutoSyncRepository.COL_TRANSACTIONS,
                        CloudAutoSyncRepository.sanitizeId(it.id)
                    )
                }
                pushCloudMutation {
                    upsertTakaJoma(entry)
                    linkedTx?.let { upsertTransaction(it) }
                }
                notifyAdminDataChanged()
                val depositLabel = if (cleanBank.isNotBlank()) "$cleanName ($cleanBank)" else cleanName
                showMessage(
                    if (isAdminSession) {
                        "$depositLabel deposit submitted successfully"
                    } else {
                        "$depositLabel deposit submitted successfully (awaiting admin approval)"
                    }
                )
                onComplete(true)
            }
            .onFailure { err ->
                showMessage(err.localizedMessage ?: "This slip has already been submitted. Duplicate slips are not allowed.")
                onComplete(false)
            }
    }

    fun approveMemberTakaJoma(entry: MemberTakaJoma) {
        setMemberTakaJomaStatus(entry, TakaJomaStatus.APPROVED)
    }

    fun rejectMemberTakaJoma(entry: MemberTakaJoma) {
        setMemberTakaJomaStatus(entry, TakaJomaStatus.REJECTED)
    }

    fun setMemberTakaJomaStatus(entry: MemberTakaJoma, status: TakaJomaStatus) {
        if (!isAdminSession) return
        val store = takaJomaStore ?: return
        val updatedSubmissions = store.updateApprovalStatus(entry.id, status)
        val reviewedEntry = updatedSubmissions.firstOrNull { it.id == entry.id }
            ?: entry.copy(approvalStatus = status.wireValue)
        var linkedTx: HisabTransaction? = null
        if (status == TakaJomaStatus.APPROVED) {
            linkedTx = upsertApprovedTakaJomaTransaction(
                entry = reviewedEntry,
                txDate = BanglaFormatters.todayIso()
            )
        } else {
            removeLinkedTakaJomaTransaction(entry.id)
        }
        _takaJomaList.value = loadScopedTakaJoma()
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_TAKA_JOMA,
            CloudAutoSyncRepository.sanitizeId(reviewedEntry.id)
        )
        if (linkedTx != null) {
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_TRANSACTIONS,
                CloudAutoSyncRepository.sanitizeId(linkedTx.id)
            )
        } else {
            cloudSyncPrefs?.markPendingDelete(
                CloudAutoSyncRepository.COL_TRANSACTIONS,
                CloudAutoSyncRepository.sanitizeId("${TAKA_JOMA_TX_PREFIX}${entry.id}")
            )
        }
        pushCloudMutation {
            upsertTakaJoma(reviewedEntry)
            if (linkedTx != null) {
                upsertTransaction(linkedTx)
            } else {
                deleteTransaction("${TAKA_JOMA_TX_PREFIX}${entry.id}")
            }
        }
        notifyAdminDataChanged()
        val msg = when (status) {
            TakaJomaStatus.APPROVED -> "${entry.name} deposit approved successfully (${BanglaFormatters.formatBdt(entry.amount)} added to ledger)"
            TakaJomaStatus.REJECTED -> "${entry.name} deposit rejected"
            TakaJomaStatus.PENDING -> "${entry.name} deposit marked as pending"
        }
        showMessage(msg)
    }

    fun deleteMemberTakaJoma(entry: MemberTakaJoma) {
        if (!isAdminSession) {
            showMessage("Members are not allowed to delete deposit records")
            return
        }
        val store = takaJomaStore ?: return
        store.deleteSubmission(
            submissionId = entry.id,
            requesterMemberId = null
        )
        removeLinkedTakaJomaTransaction(entry.id)
        _takaJomaList.value = loadScopedTakaJoma()
        cloudSyncPrefs?.markPendingDelete(
            CloudAutoSyncRepository.COL_TAKA_JOMA,
            CloudAutoSyncRepository.sanitizeId(entry.id)
        )
        cloudSyncPrefs?.markPendingDelete(
            CloudAutoSyncRepository.COL_TRANSACTIONS,
            CloudAutoSyncRepository.sanitizeId("${TAKA_JOMA_TX_PREFIX}${entry.id}")
        )
        pushCloudMutation {
            deleteTakaJoma(entry.id)
            deleteTransaction("${TAKA_JOMA_TX_PREFIX}${entry.id}")
        }
        notifyAdminDataChanged()
        showMessage("Deposit record deleted")
    }

    private fun upsertApprovedTakaJomaTransaction(
        entry: MemberTakaJoma,
        txDate: String
    ): HisabTransaction {
        val txId = "${TAKA_JOMA_TX_PREFIX}${entry.id}"
        val cleanDate = txDate.trim().ifBlank { BanglaFormatters.todayIso() }
        val now = Timestamp.now()
        val existingTx = allTransactions.firstOrNull { it.id == txId }
        val linkedTx = buildApprovedTakaJomaTransaction(
            entry = entry,
            txDate = cleanDate,
            existingCreatedAt = existingTx?.createdAt,
            now = now
        )
        val updatedAll = (listOf(linkedTx) + allTransactions.filterNot { it.id == txId })
            .sortedWith(
                compareByDescending<HisabTransaction> { it.date }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        localStore.saveAll(updatedAll)
        allTransactions = updatedAll
        _transactions.value = filterForOwner(updatedAll)
        return linkedTx
    }

    private fun removeLinkedTakaJomaTransaction(submissionId: String) {
        val txId = "${TAKA_JOMA_TX_PREFIX}$submissionId"
        if (allTransactions.none { it.id == txId }) return
        val updatedAll = allTransactions.filterNot { it.id == txId }
        localStore.saveAll(updatedAll)
        allTransactions = updatedAll
        _transactions.value = filterForOwner(updatedAll)
    }

    fun uploadMonthlySales(
        yearMonth: String,
        records: List<MonthlySalesRecord>,
        replaceMonth: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isAdminSession) {
            onComplete(false)
            return
        }
        val store = monthlySalesStore
        if (store == null || records.isEmpty()) {
            onComplete(false)
            return
        }
        val removedIds = if (replaceMonth) {
            _monthlySales.value.filter { it.yearMonth == yearMonth }.map { it.id }
        } else {
            emptyList()
        }
        val updated = store.uploadForMonth(
            yearMonth = yearMonth,
            newRecords = records,
            replaceMonth = replaceMonth
        )
        _monthlySales.value = updated
        val monthRecords = updated.filter { it.yearMonth == yearMonth }
        removedIds.forEach {
            cloudSyncPrefs?.markPendingDelete(
                CloudAutoSyncRepository.COL_MONTHLY_SALES,
                CloudAutoSyncRepository.sanitizeId(it)
            )
        }
        monthRecords.forEach {
            cloudSyncPrefs?.markPendingUpsert(
                CloudAutoSyncRepository.COL_MONTHLY_SALES,
                CloudAutoSyncRepository.sanitizeId(it.id)
            )
        }
        pushCloudMutation {
            removedIds.forEach { deleteMonthlySales(it) }
            monthRecords.forEach { upsertMonthlySales(it) }
        }
        notifyAdminDataChanged()
        val monthLabel = BanglaFormatters.formatBanglaMonth(yearMonth)
        showMessage(
            if (records.size == 1) {
                "${records.first().name} ($monthLabel) sales submitted successfully"
            } else {
                "$monthLabel sales (${records.size} records) submitted successfully"
            }
        )
        onComplete(true)
    }

    fun saveMonthlySalesRecord(
        yearMonth: String,
        editingId: String?,
        code: String,
        name: String,
        tpSales: Double,
        tpPlusVat: Double,
        onComplete: (Boolean) -> Unit
    ) {
        if (!isAdminSession) {
            onComplete(false)
            return
        }
        val store = monthlySalesStore
        if (store == null) {
            onComplete(false)
            return
        }
        val existing = editingId?.let { id -> _monthlySales.value.firstOrNull { it.id == id } }
        val record = MonthlySalesRecord(
            id = editingId ?: UUID.randomUUID().toString(),
            yearMonth = yearMonth,
            code = code.trim(),
            name = name.trim().ifBlank { code.trim() },
            tpSales = tpSales.coerceAtLeast(0.0),
            tpPlusVat = tpPlusVat.coerceAtLeast(0.0),
            createdAtMs = existing?.createdAtMs ?: System.currentTimeMillis()
        )
        _monthlySales.value = store.upsertRecord(record)
        cloudSyncPrefs?.markPendingUpsert(
            CloudAutoSyncRepository.COL_MONTHLY_SALES,
            CloudAutoSyncRepository.sanitizeId(record.id)
        )
        pushCloudMutation { upsertMonthlySales(record) }
        notifyAdminDataChanged()
        showMessage(
            if (editingId == null) {
                "${record.name} sales submitted successfully"
            } else {
                "${record.name} sales updated successfully"
            }
        )
        onComplete(true)
    }

    fun deleteMonthlySalesRecord(recordId: String) {
        if (!isAdminSession) return
        val store = monthlySalesStore ?: return
        _monthlySales.value = store.deleteRecord(recordId)
        cloudSyncPrefs?.markPendingDelete(
            CloudAutoSyncRepository.COL_MONTHLY_SALES,
            CloudAutoSyncRepository.sanitizeId(recordId)
        )
        pushCloudMutation { deleteMonthlySales(recordId) }
        notifyAdminDataChanged()
        showMessage("Sales record deleted")
    }

    fun clearMonthlySales(yearMonth: String) {
        if (!isAdminSession) return
        val store = monthlySalesStore ?: return
        val removedIds = _monthlySales.value.filter { it.yearMonth == yearMonth }.map { it.id }
        _monthlySales.value = store.clearMonth(yearMonth)
        removedIds.forEach {
            cloudSyncPrefs?.markPendingDelete(
                CloudAutoSyncRepository.COL_MONTHLY_SALES,
                CloudAutoSyncRepository.sanitizeId(it)
            )
        }
        pushCloudMutation { removedIds.forEach { deleteMonthlySales(it) } }
        notifyAdminDataChanged()
        showMessage("Cleared all sales records for ${BanglaFormatters.formatBanglaMonth(yearMonth)}")
    }

    fun selectAiVoiceOption(option: FemaleAiVoiceOption) {
        if (!isAdminSession) return
        cachedVoiceResult = null
        _aiVoiceState.update {
            it.copy(
                selectedVoice = option,
                hasCachedAudio = false,
                status = if (it.status is AiVoiceStatus.Error) AiVoiceStatus.Idle else it.status
            )
        }
    }

    fun selectAiVoiceScope(scope: AiVoiceScope) {
        if (!isAdminSession) return
        val defaultMemberId = if (scope == AiVoiceScope.SINGLE_MEMBER) {
            _aiVoiceState.value.selectedMemberId ?: _members.value.firstOrNull()?.id
        } else {
            _aiVoiceState.value.selectedMemberId
        }
        _aiVoiceState.update {
            it.copy(
                selectedScope = scope,
                selectedMemberId = defaultMemberId,
                status = if (it.status is AiVoiceStatus.Error) AiVoiceStatus.Idle else it.status
            )
        }
    }

    fun selectAiVoiceMember(memberId: String) {
        if (!isAdminSession) return
        _aiVoiceState.update {
            it.copy(
                selectedMemberId = memberId,
                status = if (it.status is AiVoiceStatus.Error) AiVoiceStatus.Idle else it.status
            )
        }
    }

    fun speakMemberSalesDuesWithAi() {
        if (!isAdminSession) return
        aiVoiceJob?.cancel()
        val currentState = _aiVoiceState.value
        aiVoiceJob = viewModelScope.launch {
            val result = aiVoiceAssistant.generateVoiceReport(
                members = _members.value,
                monthlySales = _monthlySales.value,
                submissions = _takaJomaList.value,
                scope = currentState.selectedScope,
                selectedMemberId = currentState.selectedMemberId,
                voiceOption = currentState.selectedVoice,
                onStatus = { nextStatus ->
                    _aiVoiceState.update { it.copy(status = nextStatus) }
                }
            )
            result.onSuccess { generated ->
                cachedVoiceResult = generated
                _aiVoiceState.update {
                    it.copy(
                        status = AiVoiceStatus.Playing,
                        lastScript = generated.script,
                        hasCachedAudio = true
                    )
                }
                aiVoiceAssistant.playPcmAudio(generated.audioBytes, generated.mimeType)
                _aiVoiceState.update { it.copy(status = AiVoiceStatus.Ready) }
            }.onFailure { err ->
                val message = err.localizedMessage ?: "Could not start voice summary."
                _aiVoiceState.update {
                    it.copy(status = AiVoiceStatus.Error(message))
                }
                showMessage(message)
            }
        }
    }

    fun replayAiVoice() {
        if (!isAdminSession) return
        val cached = cachedVoiceResult ?: run {
            speakMemberSalesDuesWithAi()
            return
        }
        aiVoiceJob?.cancel()
        aiVoiceJob = viewModelScope.launch {
            _aiVoiceState.update { it.copy(status = AiVoiceStatus.Playing) }
            aiVoiceAssistant.playPcmAudio(cached.audioBytes, cached.mimeType)
            _aiVoiceState.update { it.copy(status = AiVoiceStatus.Ready) }
        }
    }

    fun stopAiVoice() {
        aiVoiceJob?.cancel()
        aiVoiceJob = null
        _aiVoiceState.update { state ->
            state.copy(
                status = if (state.hasCachedAudio) AiVoiceStatus.Ready else AiVoiceStatus.Idle
            )
        }
    }

    fun clearAiVoiceError() {
        _aiVoiceState.update { state ->
            if (state.status is AiVoiceStatus.Error) {
                state.copy(status = if (state.hasCachedAudio) AiVoiceStatus.Ready else AiVoiceStatus.Idle)
            } else {
                state
            }
        }
    }

    override fun onCleared() {
        aiVoiceJob?.cancel()
        super.onCleared()
    }

    companion object {
        const val DEFAULT_ADMIN_OWNER_ID = "local_rafiqul"
        const val TAKA_JOMA_TX_PREFIX = "taka_joma_"
        const val TAKA_JOMA_CATEGORY = "Member Deposit"

        internal fun resolveAccountTypeForBankName(bankName: String): AccountType {
            val cleaned = bankName.trim()
            if (cleaned.isEmpty() ||
                cleaned.contains("Cash", ignoreCase = true) ||
                cleaned.contains("\u0995\u09CD\u09AF\u09BE\u09B6")
            ) {
                return AccountType.CASH
            }
            val isMfs = BangladeshBankDirectory.MOBILE_BANKING_OPTIONS.any { item ->
                cleaned.contains(item.enName, ignoreCase = true) ||
                    cleaned.contains(item.bnName, ignoreCase = true) ||
                    cleaned.contains(item.shortCode, ignoreCase = true)
            } ||
                cleaned.contains("bKash", ignoreCase = true) ||
                cleaned.contains("bikash", ignoreCase = true) ||
                cleaned.contains("Nagad", ignoreCase = true) ||
                cleaned.contains("nogod", ignoreCase = true) ||
                cleaned.contains("Rocket", ignoreCase = true) ||
                cleaned.contains("Upay", ignoreCase = true) ||
                cleaned.contains("Mobile Bank", ignoreCase = true) ||
                cleaned.contains("MFS", ignoreCase = true) ||
                cleaned.contains("\u09AC\u09BF\u0995\u09BE\u09B6") ||
                cleaned.contains("\u09A8\u0997\u09A6")
            return if (isMfs) AccountType.MOBILE_BANK else AccountType.BANK
        }

        internal fun buildApprovedTakaJomaNote(
            entry: MemberTakaJoma,
            approvedDateIso: String
        ): String {
            val identity = if (entry.code.isNotBlank()) {
                "${entry.name} (${entry.code})"
            } else {
                entry.name
            }
            val methodShort = entry.bankName.substringBefore(" (").trim()
            val originalDateTag = if (entry.date.isNotBlank() && entry.date != approvedDateIso) {
                "Submitted: ${BanglaFormatters.formatBanglaDate(entry.date)}"
            } else {
                null
            }
            return listOfNotNull(
                identity.takeIf { it.isNotBlank() },
                methodShort.takeIf { it.isNotBlank() },
                originalDateTag
            ).joinToString(" • ").ifBlank { TAKA_JOMA_CATEGORY }
        }

        internal fun buildApprovedTakaJomaTransaction(
            entry: MemberTakaJoma,
            txDate: String,
            existingCreatedAt: Timestamp? = null,
            now: Timestamp = Timestamp.now()
        ): HisabTransaction {
            val cleanDate = txDate.trim().ifBlank { BanglaFormatters.todayIso() }
            return HisabTransaction(
                id = "${TAKA_JOMA_TX_PREFIX}${entry.id}",
                userId = DEFAULT_ADMIN_OWNER_ID,
                type = TxType.INCOME.wireValue,
                amount = entry.amount.coerceAtLeast(0.0),
                category = TAKA_JOMA_CATEGORY,
                note = buildApprovedTakaJomaNote(entry, cleanDate),
                date = cleanDate,
                account = resolveAccountTypeForBankName(entry.bankName).wireValue,
                createdAt = existingCreatedAt ?: now,
                updatedAt = now
            )
        }
    }
}
