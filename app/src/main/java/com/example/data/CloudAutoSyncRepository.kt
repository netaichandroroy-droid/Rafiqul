package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.auth.AppLockCredentialStore
import com.example.auth.awaitGmsTask
import com.example.model.AccountType
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.OnlinePaymentAccount
import com.example.model.OnlinePaymentConfig
import com.example.model.PaymentChannel
import com.example.model.TakaJomaStatus
import com.example.model.TxType
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

data class CloudSyncUiState(
    val autoSyncEnabled: Boolean = true,
    val isConnected: Boolean = false,
    val accountEmail: String = "",
    val accountName: String = "",
    val isSyncing: Boolean = false,
    val lastSyncedAtMs: Long = 0L,
    val errorMessage: String? = null
)

class CloudAutoSyncRepository(
    context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
    )
) {
    private val appContext = context.applicationContext
    private val auth = Firebase.auth
    private val syncPrefs = CloudSyncPrefsStore(appContext)
    private val localTxStore = LocalTransactionStore(appContext)
    private val memberStore = MemberStore(appContext)
    private val takaJomaStore = MemberTakaJomaStore(appContext)
    private val monthlySalesStore = MonthlySalesStore(appContext)
    private val onlinePaymentStore = OnlinePaymentStore(appContext)
    private val memberAppLinkStore = MemberAppLinkStore(appContext)
    private val credentialStore = AppLockCredentialStore(appContext)
    private val driveBackupStore = GoogleDriveBackupStore(appContext)

    private val localWriteLock = Any()
    private val knownDocIds = ConcurrentHashMap.newKeySet<String>()
    private val isFlushingPending = AtomicBoolean(false)

    private var syncScope: CoroutineScope? = null
    private var activeSyncJob: Job? = null
    private var activeUid: String? = null

    private val _syncState = MutableStateFlow(
        CloudSyncUiState(
            autoSyncEnabled = syncPrefs.isAutoSyncEnabled(),
            isConnected = auth.currentUser != null,
            accountEmail = auth.currentUser?.email.orEmpty(),
            accountName = auth.currentUser?.displayName.orEmpty(),
            lastSyncedAtMs = syncPrefs.getLastSyncedAtMs()
        )
    )
    val syncState: StateFlow<CloudSyncUiState> = _syncState.asStateFlow()

    private val _remoteUpdates = MutableSharedFlow<Unit>(extraBufferCapacity = 16)
    val remoteUpdates: SharedFlow<Unit> = _remoteUpdates.asSharedFlow()

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Google Sign-In is required for cloud sync.")
    }

    fun isAutoSyncActive(): Boolean {
        return _syncState.value.autoSyncEnabled && auth.currentUser != null
    }

    fun refreshAuthMetadata() {
        val user = auth.currentUser
        _syncState.update {
            it.copy(
                isConnected = user != null,
                accountEmail = user?.email.orEmpty(),
                accountName = user?.displayName.orEmpty()
            )
        }
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        syncPrefs.setAutoSyncEnabled(enabled)
        _syncState.update { it.copy(autoSyncEnabled = enabled, errorMessage = null) }
        val uid = auth.currentUser?.uid
        if (enabled && !uid.isNullOrBlank()) {
            startAutoSync(uid)
        } else if (!enabled) {
            stopAutoSync()
        }
    }

    fun startAutoSync(uid: String) {
        if (uid.isBlank() || auth.currentUser?.uid != uid) return
        refreshAuthMetadata()
        if (!syncPrefs.isAutoSyncEnabled()) return
        if (activeUid == uid && activeSyncJob?.isActive == true) return

        stopAutoSync()
        activeUid = uid
        knownDocIds.clear()

        val job = SupervisorJob()
        val scope = CoroutineScope(Dispatchers.IO + job)
        syncScope = scope

        activeSyncJob = scope.launch {
            _syncState.update { it.copy(isSyncing = true, errorMessage = null) }
            try {
                seedLocalToCloudIfNeeded(uid)
                if (auth.currentUser?.uid != uid || !syncPrefs.isAutoSyncEnabled()) return@launch

                launch { observeTransactions(uid) }
                launch { observeMembers(uid) }
                launch { observeTakaJoma(uid) }
                launch { observeMonthlySales(uid) }
                launch { observeAppConfig(uid) }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.w("CloudAutoSync", "Auto-sync initialization error: ${t.message}")
                _syncState.update {
                    it.copy(
                        isSyncing = false,
                        errorMessage = t.localizedMessage ?: "Could not start cloud sync"
                    )
                }
            }
        }
    }

    fun stopAutoSync() {
        activeSyncJob?.cancel()
        activeSyncJob = null
        syncScope = null
        activeUid = null
        _syncState.update { it.copy(isSyncing = false) }
    }

    suspend fun syncAllNow(): Result<Unit> = withContext(Dispatchers.IO) {
        var currentPath = "users"
        return@withContext try {
            val uid = requireUserId()
            currentPath = "users/$uid"
            _syncState.update { it.copy(isSyncing = true, errorMessage = null) }
            pushMissingLocalRecords(uid, overwriteExisting = true)
            markSyncedNow()
            _syncState.update { it.copy(isSyncing = false, errorMessage = null) }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, currentPath)
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    errorMessage = e.localizedMessage ?: "Sync failed"
                )
            }
            Result.failure(e)
        }
    }

    private suspend fun seedLocalToCloudIfNeeded(uid: String) {
        if (!syncPrefs.isSeededForUid(uid)) {
            pushMissingLocalRecords(uid, overwriteExisting = false)
            syncPrefs.markSeededForUid(uid)
            markSyncedNow()
        } else if (syncPrefs.hasPendingMutations()) {
            flushPendingMutations(uid)
            markSyncedNow()
        } else {
            _syncState.update { it.copy(isSyncing = false) }
        }
    }

    private fun triggerPendingFlushIfNeeded(uid: String) {
        if (!syncPrefs.hasPendingMutations()) return
        if (auth.currentUser?.uid != uid || !syncPrefs.isAutoSyncEnabled()) return
        val scope = syncScope ?: return
        if (isFlushingPending.get()) return
        scope.launch {
            flushPendingMutations(uid)
        }
    }

    private suspend fun flushPendingMutations(uid: String) {
        if (auth.currentUser?.uid != uid) return
        if (!isFlushingPending.compareAndSet(false, true)) return
        try {
            // Transactions
            syncPrefs.getPendingDeletes(COL_TRANSACTIONS).forEach { id ->
                deleteTransaction(id)
            }
            val localTxById = localTxStore.loadAll().associateBy { sanitizeId(it.id) }
            syncPrefs.getPendingUpserts(COL_TRANSACTIONS).forEach { id ->
                val tx = localTxById[id]
                if (tx != null) {
                    upsertTransaction(tx)
                } else {
                    syncPrefs.clearPendingUpsert(COL_TRANSACTIONS, id)
                }
            }

            // Members
            syncPrefs.getPendingDeletes(COL_MEMBERS).forEach { id ->
                deleteMember(id)
            }
            val localMembersById = memberStore.loadAll().associateBy { sanitizeId(it.id) }
            syncPrefs.getPendingUpserts(COL_MEMBERS).forEach { id ->
                val member = localMembersById[id]
                if (member != null) {
                    upsertMember(member)
                } else {
                    syncPrefs.clearPendingUpsert(COL_MEMBERS, id)
                }
            }

            // Taka Joma
            syncPrefs.getPendingDeletes(COL_TAKA_JOMA).forEach { id ->
                deleteTakaJoma(id)
            }
            val localTakaById = takaJomaStore.loadAll().associateBy { sanitizeId(it.id) }
            syncPrefs.getPendingUpserts(COL_TAKA_JOMA).forEach { id ->
                val entry = localTakaById[id]
                if (entry != null) {
                    upsertTakaJoma(entry)
                } else {
                    syncPrefs.clearPendingUpsert(COL_TAKA_JOMA, id)
                }
            }

            // Monthly Sales
            syncPrefs.getPendingDeletes(COL_MONTHLY_SALES).forEach { id ->
                deleteMonthlySales(id)
            }
            val localSalesById = monthlySalesStore.loadAll().associateBy { sanitizeId(it.id) }
            syncPrefs.getPendingUpserts(COL_MONTHLY_SALES).forEach { id ->
                val record = localSalesById[id]
                if (record != null) {
                    upsertMonthlySales(record)
                } else {
                    syncPrefs.clearPendingUpsert(COL_MONTHLY_SALES, id)
                }
            }

            // App Config
            syncPrefs.getPendingUpserts(COL_APP_CONFIG).forEach { docId ->
                when (docId) {
                    DOC_ONLINE_PAYMENT -> syncOnlinePaymentConfig(onlinePaymentStore.load())
                    DOC_MEMBER_APP_LINK -> syncMemberAppLink(memberAppLinkStore.getDownloadUrl())
                    DOC_ADMIN_AUTH -> syncAdminCredentials(
                        loginId = credentialStore.getLoginId(),
                        passwordHash = credentialStore.getPasswordHash(),
                        isDefaultPassword = credentialStore.isUsingDefaultPassword()
                    )
                    DOC_DRIVE_BACKUP -> syncDriveBackupConfig(driveBackupStore.loadStatus())
                    else -> syncPrefs.clearPendingUpsert(COL_APP_CONFIG, docId)
                }
            }
        } finally {
            isFlushingPending.set(false)
        }
    }

    private suspend fun pushMissingLocalRecords(uid: String, overwriteExisting: Boolean) {
        val userDoc = db.collection("users").document(uid)

        // Apply any queued deletes first so local removals stay deleted
        syncPrefs.getPendingDeletes(COL_TRANSACTIONS).forEach { deleteTransaction(it) }
        syncPrefs.getPendingDeletes(COL_MEMBERS).forEach { deleteMember(it) }
        syncPrefs.getPendingDeletes(COL_TAKA_JOMA).forEach { deleteTakaJoma(it) }
        syncPrefs.getPendingDeletes(COL_MONTHLY_SALES).forEach { deleteMonthlySales(it) }

        val pendingTxUpserts = syncPrefs.getPendingUpserts(COL_TRANSACTIONS)
        val pendingMemberUpserts = syncPrefs.getPendingUpserts(COL_MEMBERS)
        val pendingTakaUpserts = syncPrefs.getPendingUpserts(COL_TAKA_JOMA)
        val pendingSalesUpserts = syncPrefs.getPendingUpserts(COL_MONTHLY_SALES)
        val pendingConfigUpserts = syncPrefs.getPendingUpserts(COL_APP_CONFIG)

        // 1. Transactions
        val txCollection = userDoc.collection(COL_TRANSACTIONS)
        val cloudTxSnap = try {
            txCollection.get().awaitGmsTask()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, txCollection.path)
            throw e
        }
        val cloudTxIds = cloudTxSnap.documents.mapTo(HashSet()) { it.id }
        cloudTxIds.forEach { knownDocIds.add("$COL_TRANSACTIONS/$it") }
        localTxStore.loadAll().forEach { tx ->
            val docId = sanitizeId(tx.id)
            if (overwriteExisting || !cloudTxIds.contains(docId) || pendingTxUpserts.contains(docId)) {
                upsertTransaction(tx)
            }
        }

        // 2. Members
        val memberCollection = userDoc.collection(COL_MEMBERS)
        val cloudMemberSnap = try {
            memberCollection.get().awaitGmsTask()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, memberCollection.path)
            throw e
        }
        val cloudMemberIds = cloudMemberSnap.documents.mapTo(HashSet()) { it.id }
        cloudMemberIds.forEach { knownDocIds.add("$COL_MEMBERS/$it") }
        localMembers@ run {
            memberStore.loadAll().forEach { member ->
                val docId = sanitizeId(member.id)
                if (overwriteExisting || !cloudMemberIds.contains(docId) || pendingMemberUpserts.contains(docId)) {
                    upsertMember(member)
                }
            }
        }

        // 3. Taka Joma
        val takaCollection = userDoc.collection(COL_TAKA_JOMA)
        val cloudTakaSnap = try {
            takaCollection.get().awaitGmsTask()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, takaCollection.path)
            throw e
        }
        val cloudTakaIds = cloudTakaSnap.documents.mapTo(HashSet()) { it.id }
        cloudTakaIds.forEach { knownDocIds.add("$COL_TAKA_JOMA/$it") }
        takaJomaStore.loadAll().forEach { entry ->
            val docId = sanitizeId(entry.id)
            if (overwriteExisting || !cloudTakaIds.contains(docId) || pendingTakaUpserts.contains(docId)) {
                upsertTakaJoma(entry)
            }
        }

        // 4. Monthly Sales
        val salesCollection = userDoc.collection(COL_MONTHLY_SALES)
        val cloudSalesSnap = try {
            salesCollection.get().awaitGmsTask()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, salesCollection.path)
            throw e
        }
        val cloudSalesIds = cloudSalesSnap.documents.mapTo(HashSet()) { it.id }
        cloudSalesIds.forEach { knownDocIds.add("$COL_MONTHLY_SALES/$it") }
        monthlySalesStore.loadAll().forEach { record ->
            val docId = sanitizeId(record.id)
            if (overwriteExisting || !cloudSalesIds.contains(docId) || pendingSalesUpserts.contains(docId)) {
                upsertMonthlySales(record)
            }
        }

        // 5. App Config
        val configCollection = userDoc.collection(COL_APP_CONFIG)
        val cloudConfigSnap = try {
            configCollection.get().awaitGmsTask()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, configCollection.path)
            throw e
        }
        val cloudConfigIds = cloudConfigSnap.documents.mapTo(HashSet()) { it.id }
        cloudConfigIds.forEach { knownDocIds.add("$COL_APP_CONFIG/$it") }
        if (overwriteExisting || !cloudConfigIds.contains(DOC_ONLINE_PAYMENT) || pendingConfigUpserts.contains(DOC_ONLINE_PAYMENT)) {
            syncOnlinePaymentConfig(onlinePaymentStore.load())
        }
        if (overwriteExisting || !cloudConfigIds.contains(DOC_MEMBER_APP_LINK) || pendingConfigUpserts.contains(DOC_MEMBER_APP_LINK)) {
            syncMemberAppLink(memberAppLinkStore.getDownloadUrl())
        }
        if (overwriteExisting || !cloudConfigIds.contains(DOC_ADMIN_AUTH) || pendingConfigUpserts.contains(DOC_ADMIN_AUTH)) {
            syncAdminCredentials(
                loginId = credentialStore.getLoginId(),
                passwordHash = credentialStore.getPasswordHash(),
                isDefaultPassword = credentialStore.isUsingDefaultPassword()
            )
        }
        val driveStatus = driveBackupStore.loadStatus()
        if ((driveStatus.oauthConnected && (overwriteExisting || !cloudConfigIds.contains(DOC_DRIVE_BACKUP))) ||
            pendingConfigUpserts.contains(DOC_DRIVE_BACKUP)
        ) {
            syncDriveBackupConfig(driveStatus)
        }
    }

    private suspend fun observeTransactions(uid: String) {
        val path = "users/$uid/$COL_TRANSACTIONS"
        db.collection("users").document(uid).collection(COL_TRANSACTIONS)
            .snapshots()
            .catch { error ->
                handleStreamError(error, uid, path)
            }
            .collect { snapshot ->
                if (snapshot.metadata.isFromCache && snapshot.isEmpty && !syncPrefs.isSeededForUid(uid)) {
                    return@collect
                }
                val pendingDeletes = syncPrefs.getPendingDeletes(COL_TRANSACTIONS)
                val pendingUpserts = syncPrefs.getPendingUpserts(COL_TRANSACTIONS)
                val remote = snapshot.documents.mapNotNull { doc ->
                    knownDocIds.add("$COL_TRANSACTIONS/${doc.id}")
                    if (doc.id in pendingDeletes) null else doc.toHisabTransactionOrNull()
                }
                synchronized(localWriteLock) {
                    val merged = if (pendingUpserts.isEmpty()) {
                        remote
                    } else {
                        val byId = remote.associateByTo(LinkedHashMap()) { sanitizeId(it.id) }
                        localTxStore.loadAll().forEach { localTx ->
                            val key = sanitizeId(localTx.id)
                            if (key in pendingUpserts && key !in pendingDeletes) {
                                byId[key] = localTx
                            }
                        }
                        byId.values.toList()
                    }.sortedWith(
                        compareByDescending<HisabTransaction> { it.date }
                            .thenByDescending { it.createdAt?.seconds ?: 0L }
                    )
                    localTxStore.saveAll(merged)
                }
                if (!snapshot.metadata.isFromCache) {
                    triggerPendingFlushIfNeeded(uid)
                }
                markSyncedNow()
                _remoteUpdates.tryEmit(Unit)
            }
    }

    private suspend fun observeMembers(uid: String) {
        val path = "users/$uid/$COL_MEMBERS"
        db.collection("users").document(uid).collection(COL_MEMBERS)
            .snapshots()
            .catch { error ->
                handleStreamError(error, uid, path)
            }
            .collect { snapshot ->
                if (snapshot.metadata.isFromCache && snapshot.isEmpty && !syncPrefs.isSeededForUid(uid)) {
                    return@collect
                }
                val pendingDeletes = syncPrefs.getPendingDeletes(COL_MEMBERS)
                val pendingUpserts = syncPrefs.getPendingUpserts(COL_MEMBERS)
                synchronized(localWriteLock) {
                    val existingLocal = memberStore.loadAll()
                    val existingPhotos = existingLocal.associate { it.id to it.profilePhotoPath }
                    val remote = snapshot.documents.mapNotNull { doc ->
                        knownDocIds.add("$COL_MEMBERS/${doc.id}")
                        if (doc.id in pendingDeletes) null else doc.toMemberOrNull(existingPhotos[doc.id].orEmpty())
                    }
                    val merged = if (pendingUpserts.isEmpty()) {
                        remote
                    } else {
                        val byId = remote.associateByTo(LinkedHashMap()) { sanitizeId(it.id) }
                        existingLocal.forEach { localMember ->
                            val key = sanitizeId(localMember.id)
                            if (key in pendingUpserts && key !in pendingDeletes) {
                                byId[key] = localMember
                            }
                        }
                        byId.values.toList()
                    }.sortedBy { it.createdAtMs }
                    memberStore.saveAll(merged)
                }
                if (!snapshot.metadata.isFromCache) {
                    triggerPendingFlushIfNeeded(uid)
                }
                markSyncedNow()
                _remoteUpdates.tryEmit(Unit)
            }
    }

    private suspend fun observeTakaJoma(uid: String) {
        val path = "users/$uid/$COL_TAKA_JOMA"
        db.collection("users").document(uid).collection(COL_TAKA_JOMA)
            .snapshots()
            .catch { error ->
                handleStreamError(error, uid, path)
            }
            .collect { snapshot ->
                if (snapshot.metadata.isFromCache && snapshot.isEmpty && !syncPrefs.isSeededForUid(uid)) {
                    return@collect
                }
                val pendingDeletes = syncPrefs.getPendingDeletes(COL_TAKA_JOMA)
                val pendingUpserts = syncPrefs.getPendingUpserts(COL_TAKA_JOMA)
                synchronized(localWriteLock) {
                    val existingLocal = takaJomaStore.loadAll()
                    val existingPhotos = existingLocal.associate { it.id to it.photoPath }
                    val remote = snapshot.documents.mapNotNull { doc ->
                        knownDocIds.add("$COL_TAKA_JOMA/${doc.id}")
                        if (doc.id in pendingDeletes) null else doc.toMemberTakaJomaOrNull(existingPhotos[doc.id].orEmpty())
                    }
                    val merged = if (pendingUpserts.isEmpty()) {
                        remote
                    } else {
                        val byId = remote.associateByTo(LinkedHashMap()) { sanitizeId(it.id) }
                        existingLocal.forEach { localEntry ->
                            val key = sanitizeId(localEntry.id)
                            if (key in pendingUpserts && key !in pendingDeletes) {
                                byId[key] = localEntry
                            }
                        }
                        byId.values.toList()
                    }.sortedByDescending { it.createdAtMs }
                    takaJomaStore.saveAll(merged)
                }
                if (!snapshot.metadata.isFromCache) {
                    triggerPendingFlushIfNeeded(uid)
                }
                markSyncedNow()
                _remoteUpdates.tryEmit(Unit)
            }
    }

    private suspend fun observeMonthlySales(uid: String) {
        val path = "users/$uid/$COL_MONTHLY_SALES"
        db.collection("users").document(uid).collection(COL_MONTHLY_SALES)
            .snapshots()
            .catch { error ->
                handleStreamError(error, uid, path)
            }
            .collect { snapshot ->
                if (snapshot.metadata.isFromCache && snapshot.isEmpty && !syncPrefs.isSeededForUid(uid)) {
                    return@collect
                }
                val pendingDeletes = syncPrefs.getPendingDeletes(COL_MONTHLY_SALES)
                val pendingUpserts = syncPrefs.getPendingUpserts(COL_MONTHLY_SALES)
                synchronized(localWriteLock) {
                    val existingLocal = monthlySalesStore.loadAll()
                    val remote = snapshot.documents.mapNotNull { doc ->
                        knownDocIds.add("$COL_MONTHLY_SALES/${doc.id}")
                        if (doc.id in pendingDeletes) null else doc.toMonthlySalesOrNull()
                    }
                    val merged = if (pendingUpserts.isEmpty()) {
                        remote
                    } else {
                        val byId = remote.associateByTo(LinkedHashMap()) { sanitizeId(it.id) }
                        existingLocal.forEach { localRecord ->
                            val key = sanitizeId(localRecord.id)
                            if (key in pendingUpserts && key !in pendingDeletes) {
                                byId[key] = localRecord
                            }
                        }
                        byId.values.toList()
                    }.sortedByDescending { it.createdAtMs }
                    monthlySalesStore.saveAll(merged)
                }
                if (!snapshot.metadata.isFromCache) {
                    triggerPendingFlushIfNeeded(uid)
                }
                markSyncedNow()
                _remoteUpdates.tryEmit(Unit)
            }
    }

    private suspend fun observeAppConfig(uid: String) {
        val path = "users/$uid/$COL_APP_CONFIG"
        db.collection("users").document(uid).collection(COL_APP_CONFIG)
            .snapshots()
            .catch { error ->
                handleStreamError(error, uid, path)
            }
            .collect { snapshot ->
                if (snapshot.metadata.isFromCache && snapshot.isEmpty) {
                    return@collect
                }
                val pendingConfigUpserts = syncPrefs.getPendingUpserts(COL_APP_CONFIG)
                var changed = false
                synchronized(localWriteLock) {
                    snapshot.documents.forEach { doc ->
                        knownDocIds.add("$COL_APP_CONFIG/${doc.id}")
                        if (doc.id in pendingConfigUpserts) return@forEach
                        val data = doc.data ?: return@forEach
                        when (doc.id) {
                            DOC_ONLINE_PAYMENT -> {
                                val enabled = data["onlinePaymentEnabled"] as? Boolean ?: true
                                val accountsJson = data["accountsJson"] as? String ?: "[]"
                                val accounts = parseAccountsJson(accountsJson)
                                onlinePaymentStore.save(
                                    OnlinePaymentConfig(
                                        isOnlinePaymentEnabled = enabled,
                                        accounts = accounts
                                    )
                                )
                                changed = true
                            }

                            DOC_MEMBER_APP_LINK -> {
                                val url = (data["downloadUrl"] as? String)?.trim().orEmpty()
                                if (url.isEmpty() || MemberAppLinkStore.isValidHttpUrl(url)) {
                                    memberAppLinkStore.saveDownloadUrl(url)
                                    changed = true
                                }
                            }

                            DOC_ADMIN_AUTH -> {
                                val loginId = (data["loginId"] as? String)?.trim().orEmpty()
                                val passwordHash = (data["passwordHash"] as? String)?.trim().orEmpty()
                                val isDefault = data["defaultPassword"] as? Boolean ?: true
                                if (loginId.isNotBlank() && passwordHash.isNotBlank()) {
                                    credentialStore.applySyncedCredentials(
                                        loginId = loginId,
                                        passwordHash = passwordHash,
                                        isDefaultPassword = isDefault
                                    )
                                    changed = true
                                }
                            }

                            DOC_DRIVE_BACKUP -> {
                                val driveConnected = data["driveConnected"] as? Boolean ?: false
                                val driveEmail = (data["driveEmail"] as? String)?.trim().orEmpty()
                                val driveFileId = (data["driveFileId"] as? String)?.trim().orEmpty()
                                val driveAutoBackup = data["driveAutoBackupEnabled"] as? Boolean ?: true
                                val lastBackupAtMs = (data["lastBackupAtMs"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L
                                val lastBackupSummary = (data["lastBackupSummary"] as? String)?.trim().orEmpty()
                                driveBackupStore.applySyncedDriveConfig(
                                    isConnected = driveConnected,
                                    connectedEmail = driveEmail,
                                    driveFileId = driveFileId,
                                    autoBackupEnabled = driveAutoBackup,
                                    lastBackupAtMs = lastBackupAtMs,
                                    lastBackupSummary = lastBackupSummary
                                )
                                changed = true
                            }
                        }
                    }
                }
                if (!snapshot.metadata.isFromCache) {
                    triggerPendingFlushIfNeeded(uid)
                }
                if (changed) {
                    markSyncedNow()
                    _remoteUpdates.tryEmit(Unit)
                }
            }
    }

    private fun handleStreamError(error: Throwable, uid: String, path: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) {
            throw error
        }
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, path)
            _syncState.update {
                it.copy(
                    isSyncing = false,
                    errorMessage = exceptionToLog.localizedMessage ?: "Real-time sync error"
                )
            }
        }
    }

    private fun markSyncedNow() {
        val now = System.currentTimeMillis()
        syncPrefs.setLastSyncedAtMs(now)
        _syncState.update {
            it.copy(
                isSyncing = false,
                lastSyncedAtMs = now,
                errorMessage = null
            )
        }
    }

    suspend fun upsertTransaction(tx: HisabTransaction): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(tx.id)
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            return@withContext Result.failure(IllegalStateException("Google Sign-In is not active."))
        }
        val boundedAmount = tx.amount
        if (boundedAmount.isNaN() || boundedAmount <= 0.0 || boundedAmount > MAX_AMOUNT) {
            return@withContext Result.failure(IllegalArgumentException("Invalid transaction amount."))
        }
        val cleanCategory = tx.category.trim().ifBlank { "Other" }.take(60)
        val cleanNote = tx.note.trim().take(200)
        val cleanDate = if (ISO_DATE_REGEX.matches(tx.date.trim())) {
            tx.date.trim()
        } else {
            BanglaFormatters.todayIso()
        }
        val cleanScope = sanitizeId(tx.userId.ifBlank { LocalTransactionStore.LOCAL_USER_ID })
        val wireType = TxType.fromRaw(tx.type).wireValue
        val wireAccount = AccountType.fromRaw(tx.account).wireValue

        val updateMap = mapOf(
            "ownerScope" to cleanScope,
            "type" to wireType,
            "amount" to boundedAmount,
            "category" to cleanCategory,
            "note" to cleanNote,
            "date" to cleanDate,
            "account" to wireAccount,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createMap = updateMap + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return@withContext upsertDoc(COL_TRANSACTIONS, docId, createMap, updateMap)
    }

    suspend fun deleteTransaction(transactionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(transactionId)
        deleteDoc(COL_TRANSACTIONS, docId)
    }

    suspend fun upsertMember(member: Member): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(member.id)
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In is not active."))
        val cleanName = member.name.trim().take(100)
        if (cleanName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Member name is required."))
        }
        val cleanHash = member.passwordHash.trim().takeIf {
            it.isEmpty() || PBKDF2_REGEX.matches(it)
        }.orEmpty()
        val boundedSales = member.salesAmount.takeIf { !it.isNaN() }?.coerceIn(0.0, MAX_AMOUNT) ?: 0.0

        val updateMap = mapOf(
            "name" to cleanName,
            "designation" to member.designation.trim().take(100),
            "phone" to member.phone.trim().take(40),
            "depot" to member.depot.trim().take(100),
            "code" to member.code.trim().take(50),
            "area" to member.area.trim().take(100),
            "active" to member.isActive,
            "clientCreatedAtMs" to member.createdAtMs.coerceAtLeast(0L),
            "passwordHash" to cleanHash,
            "defaultPassword" to member.isDefaultPassword,
            "email" to member.email.trim().take(120),
            "address" to member.address.trim().take(250),
            "joiningDate" to member.joiningDate.trim().take(40),
            "salesAmount" to boundedSales,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createMap = updateMap + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return@withContext upsertDoc(COL_MEMBERS, docId, createMap, updateMap)
    }

    suspend fun deleteMember(memberId: String): Result<Unit> = withContext(Dispatchers.IO) {
        deleteDoc(COL_MEMBERS, sanitizeId(memberId))
    }

    suspend fun upsertTakaJoma(entry: MemberTakaJoma): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(entry.id)
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In is not active."))
        val cleanName = entry.name.trim().take(100)
        val cleanMemberId = entry.memberId.trim().ifBlank { "admin_manual" }.take(128)
        if (cleanName.isEmpty() || entry.amount.isNaN() || entry.amount <= 0.0 || entry.amount > MAX_AMOUNT) {
            return@withContext Result.failure(IllegalArgumentException("Invalid deposit entry."))
        }
        val cleanRole = if (entry.submittedByRole.equals("ADMIN", ignoreCase = true)) "ADMIN" else "MEMBER"
        val cleanDate = entry.date.trim().ifBlank { BanglaFormatters.todayIso() }.take(40)

        val updateMap = mapOf(
            "memberId" to cleanMemberId,
            "name" to cleanName,
            "code" to entry.code.trim().take(50),
            "depot" to entry.depot.trim().take(100),
            "designation" to entry.designation.trim().take(100),
            "area" to entry.area.trim().take(100),
            "bankName" to entry.bankName.trim().take(120),
            "amount" to entry.amount,
            "date" to cleanDate,
            "note" to entry.note.trim().take(300),
            "slipHash" to entry.slipHash.trim().take(128),
            "submittedByRole" to cleanRole,
            "clientCreatedAtMs" to entry.createdAtMs.coerceAtLeast(0L),
            "approvalStatus" to entry.resolvedStatus.wireValue,
            "reviewedAtMs" to entry.reviewedAtMs.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createMap = updateMap + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return@withContext upsertDoc(COL_TAKA_JOMA, docId, createMap, updateMap)
    }

    suspend fun deleteTakaJoma(submissionId: String): Result<Unit> = withContext(Dispatchers.IO) {
        deleteDoc(COL_TAKA_JOMA, sanitizeId(submissionId))
    }

    suspend fun upsertMonthlySales(record: MonthlySalesRecord): Result<Unit> = withContext(Dispatchers.IO) {
        val docId = sanitizeId(record.id)
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In is not active."))
        val cleanMonth = record.yearMonth.trim()
        val cleanCode = record.code.trim().take(50)
        val cleanName = record.name.trim().ifBlank { cleanCode }.take(120)
        if (!YEAR_MONTH_REGEX.matches(cleanMonth) || cleanName.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid sales month or member name."))
        }
        val cleanTp = record.tpSales.takeIf { !it.isNaN() }?.coerceIn(0.0, MAX_AMOUNT) ?: 0.0
        val cleanVat = record.tpPlusVat.takeIf { !it.isNaN() }?.coerceIn(0.0, MAX_AMOUNT) ?: 0.0

        val updateMap = mapOf(
            "yearMonth" to cleanMonth,
            "code" to cleanCode,
            "name" to cleanName,
            "tpSales" to cleanTp,
            "tpPlusVat" to cleanVat,
            "clientCreatedAtMs" to record.createdAtMs.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createMap = updateMap + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return@withContext upsertDoc(COL_MONTHLY_SALES, docId, createMap, updateMap)
    }

    suspend fun deleteMonthlySales(recordId: String): Result<Unit> = withContext(Dispatchers.IO) {
        deleteDoc(COL_MONTHLY_SALES, sanitizeId(recordId))
    }

    suspend fun syncOnlinePaymentConfig(config: OnlinePaymentConfig): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In is not active."))
        val accountsJson = serializeAccountsJson(config.accounts).take(10000)
        val updateMap = mapOf(
            "onlinePaymentEnabled" to config.isOnlinePaymentEnabled,
            "accountsJson" to accountsJson,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createMap = updateMap + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return@withContext upsertDoc(COL_APP_CONFIG, DOC_ONLINE_PAYMENT, createMap, updateMap)
    }

    suspend fun syncMemberAppLink(downloadUrl: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In is not active."))
        val cleaned = downloadUrl.trim().take(500)
        if (cleaned.isNotEmpty() && !MemberAppLinkStore.isValidHttpUrl(cleaned)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid link URL."))
        }
        val updateMap = mapOf(
            "downloadUrl" to cleaned,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createMap = updateMap + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return@withContext upsertDoc(COL_APP_CONFIG, DOC_MEMBER_APP_LINK, createMap, updateMap)
    }

    suspend fun syncAdminCredentials(
        loginId: String,
        passwordHash: String,
        isDefaultPassword: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In is not active."))
        val cleanId = loginId.trim()
        val cleanHash = passwordHash.trim()
        if (!LOGIN_ID_REGEX.matches(cleanId) || !PBKDF2_REGEX.matches(cleanHash)) {
            return@withContext Result.failure(IllegalArgumentException("Invalid credential sync format."))
        }
        val updateMap = mapOf(
            "loginId" to cleanId,
            "passwordHash" to cleanHash,
            "defaultPassword" to isDefaultPassword,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createMap = updateMap + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return@withContext upsertDoc(COL_APP_CONFIG, DOC_ADMIN_AUTH, createMap, updateMap)
    }

    suspend fun syncDriveBackupConfig(status: GoogleDriveBackupStatus): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = auth.currentUser?.uid
            ?: return@withContext Result.failure(IllegalStateException("Google Sign-In is not active."))
        val cleanEmail = status.connectedEmail.trim().take(120)
        val cleanFileId = status.driveFileId.trim().take(200)
        val cleanSummary = status.lastBackupSummary.trim().take(250)
        val cleanBackupMs = status.lastBackupAtMs.coerceAtLeast(0L)
        val updateMap = mapOf(
            "driveConnected" to status.oauthConnected,
            "driveEmail" to cleanEmail,
            "driveFileId" to cleanFileId,
            "driveAutoBackupEnabled" to status.autoBackupEnabled,
            "lastBackupAtMs" to cleanBackupMs,
            "lastBackupSummary" to cleanSummary,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        val createMap = updateMap + mapOf(
            "userId" to uid,
            "createdAt" to FieldValue.serverTimestamp()
        )
        return@withContext upsertDoc(COL_APP_CONFIG, DOC_DRIVE_BACKUP, createMap, updateMap)
    }

    private suspend fun upsertDoc(
        collectionName: String,
        docId: String,
        createData: Map<String, Any>,
        updateData: Map<String, Any>
    ): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName/$docId"
        var isUpdate = false
        syncPrefs.markPendingUpsert(collectionName, docId)
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection(collectionName).document(docId)
            docPath = docRef.path
            val key = "$collectionName/$docId"
            val alreadyExists = if (knownDocIds.contains(key)) {
                true
            } else {
                val existsRemoteOrCached = try {
                    docRef.get().awaitGmsTask().exists()
                } catch (_: FirebaseFirestoreException) {
                    runCatching { docRef.get(Source.CACHE).awaitGmsTask().exists() }.getOrDefault(false)
                }
                if (existsRemoteOrCached) knownDocIds.add(key)
                existsRemoteOrCached
            }
            if (alreadyExists) {
                isUpdate = true
                try {
                    docRef.update(updateData).awaitGmsTask()
                } catch (e: FirebaseFirestoreException) {
                    if (e.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                        isUpdate = false
                        docRef.set(createData).awaitGmsTask()
                    } else {
                        throw e
                    }
                }
            } else {
                docRef.set(createData).awaitGmsTask()
            }
            knownDocIds.add(key)
            syncPrefs.clearPendingUpsert(collectionName, docId)
            markSyncedNow()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isUpdate) OperationType.UPDATE else OperationType.CREATE,
                docPath
            )
            _syncState.update {
                it.copy(errorMessage = e.localizedMessage ?: "Cloud sync failed")
            }
            Result.failure(e)
        }
    }

    private suspend fun deleteDoc(collectionName: String, docId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/$collectionName/$docId"
        syncPrefs.markPendingDelete(collectionName, docId)
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection(collectionName).document(docId)
            docPath = docRef.path
            docRef.delete().awaitGmsTask()
            knownDocIds.remove("$collectionName/$docId")
            syncPrefs.clearPendingDelete(collectionName, docId)
            markSyncedNow()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            _syncState.update {
                it.copy(errorMessage = e.localizedMessage ?: "Failed to delete from cloud")
            }
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toHisabTransactionOrNull(): HisabTransaction? {
        val data = data ?: return null
        val amount = (data["amount"] as? Number)?.toDouble() ?: return null
        if (amount <= 0.0) return null
        val ownerScope = (data["ownerScope"] as? String)?.trim().orEmpty()
            .ifBlank { LocalTransactionStore.LOCAL_USER_ID }
        val estBehavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        val createdTs = getTimestamp("createdAt", estBehavior)
        val updatedTs = getTimestamp("updatedAt", estBehavior)
        return HisabTransaction(
            id = id,
            userId = ownerScope,
            type = TxType.fromRaw(data["type"] as? String).wireValue,
            amount = amount,
            category = (data["category"] as? String).orEmpty(),
            note = (data["note"] as? String).orEmpty(),
            date = (data["date"] as? String).orEmpty(),
            account = AccountType.fromRaw(data["account"] as? String).wireValue,
            createdAt = createdTs,
            updatedAt = updatedTs
        )
    }

    private fun DocumentSnapshot.toMemberOrNull(localPhotoPath: String): Member? {
        val data = data ?: return null
        val name = (data["name"] as? String)?.trim().orEmpty()
        if (name.isEmpty()) return null
        val estBehavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        val fallbackMs = getTimestamp("createdAt", estBehavior)?.toDate()?.time ?: System.currentTimeMillis()
        return Member(
            id = id,
            name = name,
            designation = (data["designation"] as? String).orEmpty(),
            phone = (data["phone"] as? String).orEmpty(),
            depot = (data["depot"] as? String).orEmpty(),
            code = (data["code"] as? String).orEmpty(),
            area = (data["area"] as? String).orEmpty(),
            isActive = data["active"] as? Boolean ?: true,
            createdAtMs = (data["clientCreatedAtMs"] as? Number)?.toLong() ?: fallbackMs,
            passwordHash = (data["passwordHash"] as? String).orEmpty(),
            isDefaultPassword = data["defaultPassword"] as? Boolean ?: true,
            email = (data["email"] as? String).orEmpty(),
            address = (data["address"] as? String).orEmpty(),
            joiningDate = (data["joiningDate"] as? String).orEmpty(),
            profilePhotoPath = localPhotoPath,
            salesAmount = (data["salesAmount"] as? Number)?.toDouble()?.coerceAtLeast(0.0) ?: 0.0
        )
    }

    private fun DocumentSnapshot.toMemberTakaJomaOrNull(localPhotoPath: String): MemberTakaJoma? {
        val data = data ?: return null
        val name = (data["name"] as? String)?.trim().orEmpty()
        val amount = (data["amount"] as? Number)?.toDouble() ?: return null
        if (name.isEmpty() || amount <= 0.0) return null
        val estBehavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        val fallbackMs = getTimestamp("createdAt", estBehavior)?.toDate()?.time ?: System.currentTimeMillis()
        val role = (data["submittedByRole"] as? String)?.trim().orEmpty().ifBlank { "MEMBER" }
        return MemberTakaJoma(
            id = id,
            memberId = (data["memberId"] as? String).orEmpty(),
            name = name,
            code = (data["code"] as? String).orEmpty(),
            depot = (data["depot"] as? String).orEmpty(),
            designation = (data["designation"] as? String).orEmpty(),
            area = (data["area"] as? String).orEmpty(),
            bankName = (data["bankName"] as? String).orEmpty(),
            amount = amount,
            date = (data["date"] as? String).orEmpty(),
            note = (data["note"] as? String).orEmpty(),
            photoPath = localPhotoPath,
            slipHash = (data["slipHash"] as? String).orEmpty(),
            submittedByRole = role,
            createdAtMs = (data["clientCreatedAtMs"] as? Number)?.toLong() ?: fallbackMs,
            approvalStatus = TakaJomaStatus.fromRaw(data["approvalStatus"] as? String).wireValue,
            reviewedAtMs = (data["reviewedAtMs"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun DocumentSnapshot.toMonthlySalesOrNull(): MonthlySalesRecord? {
        val data = data ?: return null
        val yearMonth = (data["yearMonth"] as? String)?.trim().orEmpty()
        val code = (data["code"] as? String)?.trim().orEmpty()
        val name = (data["name"] as? String)?.trim().orEmpty()
        if (yearMonth.isEmpty() || (code.isEmpty() && name.isEmpty())) return null
        val estBehavior = DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
        val fallbackMs = getTimestamp("createdAt", estBehavior)?.toDate()?.time ?: System.currentTimeMillis()
        return MonthlySalesRecord(
            id = id,
            yearMonth = yearMonth,
            code = code,
            name = name.ifBlank { code },
            tpSales = (data["tpSales"] as? Number)?.toDouble()?.coerceAtLeast(0.0) ?: 0.0,
            tpPlusVat = (data["tpPlusVat"] as? Number)?.toDouble()?.coerceAtLeast(0.0) ?: 0.0,
            createdAtMs = (data["clientCreatedAtMs"] as? Number)?.toLong() ?: fallbackMs
        )
    }

    private fun serializeAccountsJson(accounts: List<OnlinePaymentAccount>): String {
        val array = JSONArray()
        accounts.forEach { acc ->
            array.put(
                JSONObject().apply {
                    put("id", acc.id)
                    put("channel", acc.channel.name)
                    put("accountNumber", acc.accountNumber)
                    put("accountType", acc.accountType)
                    put("isEnabled", acc.isEnabled)
                }
            )
        }
        return array.toString()
    }

    private fun parseAccountsJson(rawJson: String): List<OnlinePaymentAccount> {
        return runCatching {
            val array = JSONArray(rawJson)
            val byId = LinkedHashMap<String, OnlinePaymentAccount>()
            OnlinePaymentConfig.defaultAccounts().forEach { byId[it.id] = it }
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val channel = PaymentChannel.entries.firstOrNull {
                    it.name == obj.optString("channel", "")
                } ?: continue
                if (id.isEmpty()) continue
                byId[id] = OnlinePaymentAccount(
                    id = id,
                    channel = channel,
                    accountNumber = obj.optString("accountNumber", "").trim(),
                    accountType = obj.optString("accountType", "Personal").trim().ifBlank { "Personal" },
                    isEnabled = obj.optBoolean("isEnabled", false)
                )
            }
            byId.values.toList()
        }.getOrElse { OnlinePaymentConfig.defaultAccounts() }
    }

    companion object {
        const val COL_TRANSACTIONS = "transactions"
        const val COL_MEMBERS = "members"
        const val COL_TAKA_JOMA = "takaJoma"
        const val COL_MONTHLY_SALES = "monthlySales"
        const val COL_APP_CONFIG = "appConfig"
        const val DOC_ONLINE_PAYMENT = "online_payment"
        const val DOC_MEMBER_APP_LINK = "member_app_link"
        const val DOC_ADMIN_AUTH = "admin_auth"
        const val DOC_DRIVE_BACKUP = "drive_backup"
        private const val MAX_AMOUNT = 1_000_000_000.0
        private val ISO_DATE_REGEX = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
        private val YEAR_MONTH_REGEX = Regex("^[0-9]{4}-[0-9]{2}$")
        private val LOGIN_ID_REGEX = Regex("^[a-zA-Z0-9_.-]{3,30}$")
        private val PBKDF2_REGEX = Regex("^pbkdf2_sha256\\$[0-9]+\\$[A-Za-z0-9+/=]+\\$[A-Za-z0-9+/=]+$")

        fun sanitizeId(rawId: String): String {
            val cleaned = rawId.trim().replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(128)
            return cleaned.ifBlank { UUID.randomUUID().toString() }
        }
    }
}
