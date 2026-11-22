package com.example.ui

import androidx.lifecycle.ViewModel
import com.example.auth.AppLockCredentialStore
import com.example.auth.PasswordHasher
import com.example.data.BackupExporter
import com.example.data.LocalTransactionStore
import com.example.data.MemberStore
import com.example.data.MemberTakaJomaStore
import com.example.data.OnlinePaymentStore
import com.example.model.AccountType
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.OnlinePaymentConfig
import com.example.model.PaymentChannel
import com.example.model.TxFilter
import com.example.model.TxType
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    MEMBER_TAKA_JOMA
}

data class HisabUiControls(
    val selectedTab: HisabTab = HisabTab.HOME,
    val settingsSubPage: SettingsSubPage = SettingsSubPage.MAIN,
    val filter: TxFilter = TxFilter.ALL,
    val reportMonth: String = BanglaFormatters.currentYearMonth(),
    val isSaving: Boolean = false,
    val snackbarMessage: String? = null
)

class LocalHisabViewModel(
    private val localStore: LocalTransactionStore,
    private val memberStore: MemberStore,
    private val onlinePaymentStore: OnlinePaymentStore,
    private val takaJomaStore: MemberTakaJomaStore? = null,
    private val ownerUserId: String = DEFAULT_ADMIN_OWNER_ID,
    private val reservedAdminLoginId: String = AppLockCredentialStore.DEFAULT_LOGIN_ID
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

    private val _controls = MutableStateFlow(HisabUiControls())
    val controls: StateFlow<HisabUiControls> = _controls.asStateFlow()

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
        _takaJomaList.value = loadScopedTakaJoma()
        if (isAdminSession) {
            _members.value = memberStore.loadAll()
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
        val cleanCategory = category.trim()
        val cleanNote = note.trim().ifBlank { cleanCategory }
        val now = Timestamp.now()

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
                    )
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
        val isOwned = if (isAdminSession) {
            transaction.userId == DEFAULT_ADMIN_OWNER_ID || transaction.userId.isBlank()
        } else {
            transaction.userId == ownerUserId
        }
        if (!isOwned) return
        val updatedAll = allTransactions.filterNot { it.id == transaction.id }
        localStore.saveAll(updatedAll)
        allTransactions = updatedAll
        _transactions.value = filterForOwner(updatedAll)
        _controls.update { it.copy(snackbarMessage = "লেনদেন মুছে ফেলা হয়েছে") }
    }

    fun restoreFromJson(jsonText: String) {
        val parsed = BackupExporter.parseJsonBackup(jsonText)
        if (parsed.isFailure) {
            _controls.update {
                it.copy(snackbarMessage = parsed.exceptionOrNull()?.message ?: "সঠিক JSON ব্যাকআপ নির্বাচন করুন")
            }
            return
        }
        val now = Timestamp.now()
        val restored = parsed.getOrThrow().map { item ->
            HisabTransaction(
                id = UUID.randomUUID().toString(),
                userId = ownerUserId,
                type = item.type.wireValue,
                amount = item.amount,
                category = item.category,
                note = item.note,
                date = item.date,
                account = item.account.wireValue,
                createdAt = now,
                updatedAt = now
            )
        }
        val combinedAll = (restored + allTransactions).sortedWith(
            compareByDescending<HisabTransaction> { it.date }
                .thenByDescending { it.createdAt?.seconds ?: 0L }
        )
        localStore.saveAll(combinedAll)
        allTransactions = combinedAll
        _transactions.value = filterForOwner(combinedAll)
        _controls.update {
            it.copy(snackbarMessage = "${BanglaFormatters.toBanglaDigits(restored.size.toString())}টি লেনদেন রিস্টোর হয়েছে")
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

        val updated = if (editingId == null) {
            val defaultHash = PasswordHasher.hashPassword(cleanName, minLength = 1)
            _members.value + Member(
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
                    )
                } else {
                    existing
                }
            }
        }
        memberStore.saveAll(updated)
        _members.value = updated
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
        val updated = _members.value.map { member ->
            if (member.id == memberId) member.copy(isActive = isActive) else member
        }
        memberStore.saveAll(updated)
        _members.value = updated
    }

    fun resetMemberPassword(memberId: String) {
        val resetMember = memberStore.resetMemberPasswordToDefault(memberId) ?: return
        _members.value = _members.value.map { if (it.id == memberId) resetMember else it }
        showMessage("${resetMember.name}-এর পাসওয়ার্ড রিসেট করে '${resetMember.name}' করা হয়েছে")
    }

    fun deleteMember(member: Member) {
        val updated = _members.value.filterNot { it.id == member.id }
        memberStore.saveAll(updated)
        _members.value = updated
        showMessage("সদস্য মুছে ফেলা হয়েছে")
    }

    fun setOnlinePaymentEnabled(enabled: Boolean) {
        val updated = _onlinePaymentConfig.value.copy(isOnlinePaymentEnabled = enabled)
        onlinePaymentStore.save(updated)
        _onlinePaymentConfig.value = updated
        showMessage(if (enabled) "অনলাইন পেমেন্ট চালু হয়েছে" else "অনলাইন পেমেন্ট বন্ধ হয়েছে")
    }

    fun togglePaymentAccountEnabled(accountId: String, enabled: Boolean) {
        val updatedAccounts = _onlinePaymentConfig.value.accounts.map { account ->
            if (account.id == accountId) account.copy(isEnabled = enabled) else account
        }
        val updated = _onlinePaymentConfig.value.copy(accounts = updatedAccounts)
        onlinePaymentStore.save(updated)
        _onlinePaymentConfig.value = updated
    }

    fun updatePaymentAccount(
        accountId: String,
        accountNumber: String,
        accountType: String,
        isEnabled: Boolean
    ) {
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
            com.example.data.MemberPhotoStorage.computeFileSha256(cleanPhotoPath)
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
            submittedByRole = if (isAdminSession) "ADMIN" else "MEMBER"
        )
        store.tryAddSubmission(entry)
            .onSuccess {
                _takaJomaList.value = loadScopedTakaJoma()
                showMessage("টাকা জমা ফর্ম সফলভাবে সংরক্ষিত হয়েছে")
                onComplete(true)
            }
            .onFailure { err ->
                showMessage(err.localizedMessage ?: "এই স্লিপটি আগেও জমা দেওয়া হয়েছে। এক স্লিপ দুইবার জমা দেওয়া যাবে না।")
                onComplete(false)
            }
    }

    fun deleteMemberTakaJoma(entry: MemberTakaJoma) {
        val store = takaJomaStore ?: return
        store.deleteSubmission(
            submissionId = entry.id,
            requesterMemberId = if (isAdminSession) null else scopedMemberId
        )
        _takaJomaList.value = loadScopedTakaJoma()
        showMessage("টাকা জমা রেকর্ড মুছে ফেলা হয়েছে")
    }

    companion object {
        const val DEFAULT_ADMIN_OWNER_ID = "local_rafiqul"
    }
}
