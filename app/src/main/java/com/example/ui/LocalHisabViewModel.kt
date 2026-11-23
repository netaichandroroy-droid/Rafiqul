package com.example.ui

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
import com.example.data.CloudSyncUiState
import com.example.data.DriveUnauthorizedException
import com.example.data.GoogleDriveBackupClient
import com.example.data.GoogleDriveBackupStore
import com.example.data.LocalTransactionStore
import com.example.data.MemberAppLinkStore
import com.example.data.MemberStore
import com.example.data.MemberTakaJomaStore
import com.example.data.MonthlySalesStore
import com.example.data.OnlinePaymentStore
import com.example.model.AccountType
import com.example.model.BanglaFormatters
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
    HOME("হোম"),
    TRANSACTIONS("লেনদেন"),
    REPORTS("রিপোর্ট"),
    ACCOUNTS("অ্যাকাউন্ট"),
    SETTINGS("সেটিংস")
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
    val isSyncing: Boolean = false
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
    private val cloudSyncRepository: CloudAutoSyncRepository? = null
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
                autoBackupEnabled = status.autoBackupEnabled
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

    init {
        cloudSyncRepository?.let { repo ->
            viewModelScope.launch {
                repo.remoteUpdates.collect {
                    refreshSharedData()
                }
            }
        }
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
            if (enabled) "অ্যাপ অটো-সিঙ্ক চালু হয়েছে"
            else "অ্যাপ অটো-সিঙ্ক সাময়িকভাবে বন্ধ হয়েছে"
        )
    }

    fun syncCloudNow() {
        val repo = cloudSyncRepository ?: return
        viewModelScope.launch {
            val res = repo.syncAllNow()
            res.onSuccess {
                showMessage("সব ডেটা ক্লাউডে সফলভাবে সিঙ্ক হয়েছে")
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "ক্লাউড সিঙ্ক ব্যর্থ হয়েছে")
            }
        }
    }

    fun syncAdminCredentialsToCloud(
        loginId: String,
        passwordHash: String,
        isDefaultPassword: Boolean
    ) {
        pushCloudMutation {
            syncAdminCredentials(
                loginId = loginId,
                passwordHash = passwordHash,
                isDefaultPassword = isDefaultPassword
            )
        }
    }

    fun syncMemberToCloud(member: Member) {
        pushCloudMutation {
            upsertMember(member)
        }
    }

    private fun notifyAdminDataChanged() {
        if (isAdminSession && _driveState.value.isConnected && _driveState.value.autoBackupEnabled) {
            _autoBackupRevision.update { it + 1 }
        }
    }

    fun notifyMemberAppLinkUpdated() {
        notifyAdminDataChanged()
        val url = memberAppLinkStore?.getDownloadUrl().orEmpty()
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
        if (isAdminSession) {
            _members.value = memberStore.loadAll()
            _onlinePaymentConfig.value = onlinePaymentStore.load()
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
            showMessage("মেম্বার লেনদেন এডিট করার অনুমতি নেই")
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
        savedTx?.let { tx -> pushCloudMutation { upsertTransaction(tx) } }
        notifyAdminDataChanged()
        _controls.update {
            it.copy(
                snackbarMessage = if (editingId == null) {
                    "নতুন লেনদেন সংরক্ষিত হয়েছে"
                } else {
                    "লেনদেন হালনাগাদ হয়েছে"
                }
            )
        }
        onComplete(true)
    }

    fun deleteTransaction(transaction: HisabTransaction) {
        if (!isAdminSession) {
            showMessage("মেম্বার লেনদেন মুছে ফেলার অনুমতি নেই")
            return
        }
        val isOwned = transaction.userId == DEFAULT_ADMIN_OWNER_ID || transaction.userId.isBlank()
        if (!isOwned) return
        val updatedAll = allTransactions.filterNot { it.id == transaction.id }
        localStore.saveAll(updatedAll)
        allTransactions = updatedAll
        _transactions.value = filterForOwner(updatedAll)
        pushCloudMutation { deleteTransaction(transaction.id) }
        notifyAdminDataChanged()
        _controls.update { it.copy(snackbarMessage = "লেনদেন মুছে ফেলা হয়েছে") }
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
                it.copy(snackbarMessage = parsed.exceptionOrNull()?.message ?: "সঠিক JSON ব্যাকআপ নির্বাচন করুন")
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
            pushCloudMutation { restored.forEach { upsertTransaction(it) } }
            notifyAdminDataChanged()
            _controls.update {
                it.copy(snackbarMessage = "${BanglaFormatters.toBanglaDigits(restored.size.toString())}টি লেনদেন রিস্টোর হয়েছে")
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

        memberStore.saveAll(backup.members)
        _members.value = memberStore.loadAll()

        takaJomaStore?.let { store ->
            store.saveAll(backup.memberTakaJoma)
            _takaJomaList.value = loadScopedTakaJoma()
        }

        monthlySalesStore?.let { store ->
            store.saveAll(backup.monthlySales)
            _monthlySales.value = store.loadAll()
        }

        backup.onlinePaymentConfig?.let { cfg ->
            onlinePaymentStore.save(cfg)
            _onlinePaymentConfig.value = cfg
        }

        backup.memberAppDownloadUrl?.let { url ->
            memberAppLinkStore?.saveDownloadUrl(url)
        }

        pushCloudMutation { syncAllNow() }
        notifyAdminDataChanged()
        _controls.update {
            it.copy(snackbarMessage = "সব ডেটা রিস্টোর হয়েছে (${backup.buildBanglaSummary()})")
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
                showMessage(findErr.localizedMessage ?: "Google Drive অনুমোদনের মেয়াদ শেষ হয়েছে। অনুগ্রহ করে আবার সংযুক্ত করুন।")
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
                    connectedEmail = email.ifBlank { state.connectedEmail },
                    driveFileId = existingMeta?.id ?: state.driveFileId,
                    isSyncing = false
                )
            }
            val emailText = if (email.isNotBlank()) " ($email)" else ""
            showMessage("Google Drive সংযুক্ত হয়েছে$emailText")
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
            "${BanglaFormatters.toBanglaDigits(allTransactions.size.toString())} লেনদেন",
            "${BanglaFormatters.toBanglaDigits(_members.value.size.toString())} মেম্বার",
            "${BanglaFormatters.toBanglaDigits(currentTaka.size.toString())} টাকা জমা",
            "${BanglaFormatters.toBanglaDigits(currentSales.size.toString())} সেলস"
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
                        connectedEmail = email.ifBlank { state.connectedEmail },
                        driveFileId = meta.id,
                        lastBackupAtMs = nowMs,
                        lastBackupSummary = summaryText,
                        isSyncing = false
                    )
                }
                if (!silent) {
                    showMessage("সব ডেটা Google Drive-এ সফলভাবে জমা হয়েছে")
                }
            }.onFailure { err ->
                if (err is DriveUnauthorizedException) {
                    onUnauthorized()
                }
                _driveState.update { it.copy(isSyncing = false) }
                if (!silent) {
                    showMessage(err.localizedMessage ?: "Google Drive ব্যাকআপ ব্যর্থ হয়েছে")
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
                    val summary = "${BanglaFormatters.toBanglaDigits(allTransactions.size.toString())} লেনদেন • ${BanglaFormatters.toBanglaDigits(_members.value.size.toString())} মেম্বার"
                    driveBackupStore?.updateBackupMetadata(
                        fileId = meta.id,
                        timestampMs = nowMs,
                        summary = summary,
                        email = email
                    )
                    _driveState.update { state ->
                        state.copy(
                            isConnected = true,
                            connectedEmail = email.ifBlank { state.connectedEmail },
                            driveFileId = meta.id,
                            lastBackupAtMs = nowMs,
                            lastBackupSummary = summary,
                            isSyncing = false
                        )
                    }
                } else {
                    _driveState.update { it.copy(isSyncing = false) }
                }
            }.onFailure { err ->
                if (err is DriveUnauthorizedException) {
                    onUnauthorized()
                }
                _driveState.update { it.copy(isSyncing = false) }
                showMessage(err.localizedMessage ?: "Google Drive থেকে রিস্টোর ব্যর্থ হয়েছে")
            }
        }
    }

    fun setDriveAutoBackup(enabled: Boolean) {
        if (!isAdminSession) return
        driveBackupStore?.setAutoBackupEnabled(enabled)
        _driveState.update { it.copy(autoBackupEnabled = enabled) }
        showMessage(
            if (enabled) "স্বয়ংক্রিয় Google Drive ব্যাকআপ চালু হয়েছে"
            else "স্বয়ংক্রিয় Google Drive ব্যাকআপ বন্ধ হয়েছে"
        )
    }

    fun disconnectGoogleDrive() {
        if (!isAdminSession) return
        driveBackupStore?.disconnect()
        _driveState.update {
            DriveUiState(
                isConnected = false,
                connectedEmail = "",
                driveFileId = "",
                lastBackupAtMs = 0L,
                lastBackupSummary = "",
                autoBackupEnabled = true,
                isSyncing = false
            )
        }
        showMessage("Google Drive অ্যাকাউন্ট বিচ্ছিন্ন করা হয়েছে")
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
        val cleanCode = code.trim()
        val cleanArea = area.trim()
        if (cleanName.isEmpty()) {
            showMessage("সদস্যের নাম লিখুন")
            onComplete(false)
            return
        }

        val conflictsAdmin = cleanName.equals(reservedAdminLoginId.trim(), ignoreCase = true) ||
            cleanName.equals(AppLockCredentialStore.DEFAULT_LOGIN_ID, ignoreCase = true)
        val conflictsMember = _members.value.any { other ->
            other.id != editingId &&
                other.name.trim().replace(Regex("\\s+"), " ").equals(cleanName, ignoreCase = true)
        }
        if (conflictsAdmin || conflictsMember) {
            showMessage("এই নামে ইতিমধ্যে একটি লগইন অ্যাকাউন্ট রয়েছে। ভিন্ন নাম ব্যবহার করুন।")
            onComplete(false)
            return
        }

        var savedMember: Member? = null
        val updated = if (editingId == null) {
            val defaultHash = PasswordHasher.hashPassword(cleanName, minLength = 1)
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
                        PasswordHasher.hashPassword(cleanName, minLength = 1)
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
        savedMember?.let { m -> pushCloudMutation { upsertMember(m) } }
        notifyAdminDataChanged()
        showMessage(
            if (editingId == null) {
                "সদস্য যোগ হয়েছে (Login ID ও পাসওয়ার্ড: $cleanName)"
            } else {
                "সদস্যের তথ্য হালনাগাদ হয়েছে"
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
        target?.let { m -> pushCloudMutation { upsertMember(m) } }
        notifyAdminDataChanged()
    }

    fun resetMemberPassword(memberId: String) {
        if (!isAdminSession) return
        val resetMember = memberStore.resetMemberPasswordToDefault(memberId) ?: return
        _members.value = _members.value.map { if (it.id == memberId) resetMember else it }
        pushCloudMutation { upsertMember(resetMember) }
        notifyAdminDataChanged()
        showMessage("${resetMember.name}-এর পাসওয়ার্ড রিসেট করে '${resetMember.name}' করা হয়েছে")
    }

    fun deleteMember(member: Member) {
        if (!isAdminSession) return
        val updated = _members.value.filterNot { it.id == member.id }
        memberStore.saveAll(updated)
        _members.value = updated
        pushCloudMutation { deleteMember(member.id) }
        notifyAdminDataChanged()
        showMessage("সদস্য মুছে ফেলা হয়েছে")
    }

    fun setOnlinePaymentEnabled(enabled: Boolean) {
        if (!isAdminSession) return
        val updated = _onlinePaymentConfig.value.copy(isOnlinePaymentEnabled = enabled)
        onlinePaymentStore.save(updated)
        _onlinePaymentConfig.value = updated
        pushCloudMutation { syncOnlinePaymentConfig(updated) }
        notifyAdminDataChanged()
        showMessage(if (enabled) "অনলাইন পেমেন্ট চালু হয়েছে" else "অনলাইন পেমেন্ট বন্ধ হয়েছে")
    }

    fun togglePaymentAccountEnabled(accountId: String, enabled: Boolean) {
        if (!isAdminSession) return
        val updatedAccounts = _onlinePaymentConfig.value.accounts.map { account ->
            if (account.id == accountId) account.copy(isEnabled = enabled) else account
        }
        val updated = _onlinePaymentConfig.value.copy(accounts = updatedAccounts)
        onlinePaymentStore.save(updated)
        _onlinePaymentConfig.value = updated
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
        pushCloudMutation { syncOnlinePaymentConfig(updated) }
        notifyAdminDataChanged()
        showMessage("পেমেন্ট অ্যাকাউন্ট সংরক্ষিত হয়েছে")
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
            category = "অনলাইন পেমেন্ট (${channel.title})",
            note = note.trim().ifBlank { "${channel.title} পেমেন্ট" },
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
            showMessage("নাম, কোড, ডিপো ও টাকার পরিমাণ সঠিকভাবে পূরণ করুন")
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
                pushCloudMutation {
                    upsertTakaJoma(entry)
                    linkedTx?.let { upsertTransaction(it) }
                }
                notifyAdminDataChanged()
                showMessage(
                    if (isAdminSession) {
                        "টাকা জমা ফর্ম সফলভাবে সংরক্ষিত হয়েছে"
                    } else {
                        "টাকা জমা ফর্ম জমা হয়েছে (অ্যাডমিন অনুমোদনের অপেক্ষায়)"
                    }
                )
                onComplete(true)
            }
            .onFailure { err ->
                showMessage(err.localizedMessage ?: "এই স্লিপটি আগেও জমা দেওয়া হয়েছে। এক স্লিপ দুইবার জমা দেওয়া যাবে না।")
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
            TakaJomaStatus.APPROVED -> "${entry.name}-এর ${BanglaFormatters.formatBdt(entry.amount)} জমা অনুমোদিত হয়ে আজকের হিসাবে যোগ হয়েছে"
            TakaJomaStatus.REJECTED -> "${entry.name}-এর জমা বাতিল করা হয়েছে"
            TakaJomaStatus.PENDING -> "${entry.name}-এর জমা অপেক্ষমাণ করা হয়েছে"
        }
        showMessage(msg)
    }

    fun deleteMemberTakaJoma(entry: MemberTakaJoma) {
        if (!isAdminSession) {
            showMessage("মেম্বার টাকা জমা মুছে ফেলার অনুমতি নেই")
            return
        }
        val store = takaJomaStore ?: return
        store.deleteSubmission(
            submissionId = entry.id,
            requesterMemberId = null
        )
        removeLinkedTakaJomaTransaction(entry.id)
        _takaJomaList.value = loadScopedTakaJoma()
        pushCloudMutation {
            deleteTakaJoma(entry.id)
            deleteTransaction("${TAKA_JOMA_TX_PREFIX}${entry.id}")
        }
        notifyAdminDataChanged()
        showMessage("টাকা জমা রেকর্ড মুছে ফেলা হয়েছে")
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
        pushCloudMutation {
            removedIds.forEach { deleteMonthlySales(it) }
            monthRecords.forEach { upsertMonthlySales(it) }
        }
        notifyAdminDataChanged()
        showMessage(
            "${BanglaFormatters.formatBanglaMonth(yearMonth)}-এর ${BanglaFormatters.toBanglaDigits(records.size.toString())}টি Sales রেকর্ড আপলোড হয়েছে"
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
        pushCloudMutation { upsertMonthlySales(record) }
        notifyAdminDataChanged()
        showMessage(if (editingId == null) "নতুন Sales যোগ হয়েছে" else "Sales তথ্য হালনাগাদ হয়েছে")
        onComplete(true)
    }

    fun deleteMonthlySalesRecord(recordId: String) {
        if (!isAdminSession) return
        val store = monthlySalesStore ?: return
        _monthlySales.value = store.deleteRecord(recordId)
        pushCloudMutation { deleteMonthlySales(recordId) }
        notifyAdminDataChanged()
        showMessage("Sales রেকর্ড মুছে ফেলা হয়েছে")
    }

    fun clearMonthlySales(yearMonth: String) {
        if (!isAdminSession) return
        val store = monthlySalesStore ?: return
        val removedIds = _monthlySales.value.filter { it.yearMonth == yearMonth }.map { it.id }
        _monthlySales.value = store.clearMonth(yearMonth)
        pushCloudMutation { removedIds.forEach { deleteMonthlySales(it) } }
        notifyAdminDataChanged()
        showMessage("${BanglaFormatters.formatBanglaMonth(yearMonth)}-এর সব Sales মুছে ফেলা হয়েছে")
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
                val message = err.localizedMessage ?: "AI ভয়েস চালু করা যায়নি।"
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
        const val TAKA_JOMA_CATEGORY = "মেম্বার টাকা জমা"

        internal fun resolveAccountTypeForBankName(bankName: String): AccountType {
            val cleaned = bankName.trim()
            return if (cleaned.isEmpty() ||
                cleaned.contains("Cash", ignoreCase = true) ||
                cleaned.contains("ক্যাশ")
            ) {
                AccountType.CASH
            } else {
                AccountType.BANK
            }
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
                "দাখিল: ${BanglaFormatters.formatBanglaDate(entry.date)}"
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
