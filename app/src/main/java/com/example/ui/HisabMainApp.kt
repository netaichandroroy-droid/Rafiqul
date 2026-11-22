package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.AppLockCredentialStore
import com.example.data.GoogleDriveBackupClient
import com.example.data.GoogleDriveBackupStore
import com.example.data.LocalTransactionStore
import com.example.data.MemberAppLinkStore
import com.example.data.MemberStore
import com.example.data.MemberTakaJomaStore
import com.example.data.MonthlySalesStore
import com.example.data.OnlinePaymentStore
import com.example.data.ReportPictureRenderer
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.example.model.HisabTransaction
import com.example.model.TxType
import com.example.ui.components.DeleteTransactionDialog
import com.example.ui.components.StylishBottomNavBar
import com.example.ui.components.StylishNavTab
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.ChangePasswordDialog
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MemberEditorModalSheet
import com.example.ui.screens.MemberListScreen
import com.example.ui.screens.MemberTakaJomaScreen
import com.example.ui.screens.OnlinePaymentScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SalesUploadScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TransactionModalSheet
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.BrandDeep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DrivePendingAction {
    CONNECT,
    BACKUP_MANUAL,
    BACKUP_SILENT,
    RESTORE
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun HisabMainApp(
    credentialStore: AppLockCredentialStore,
    onAppLogout: () -> Unit
) {
    val context = LocalContext.current
    val localStore = remember(context) { LocalTransactionStore(context) }
    val memberStore = remember(context) { MemberStore(context) }
    val onlinePaymentStore = remember(context) { OnlinePaymentStore(context) }
    val takaJomaStore = remember(context) { MemberTakaJomaStore(context) }
    val monthlySalesStore = remember(context) { MonthlySalesStore(context) }
    val memberAppLinkStore = remember(context) { MemberAppLinkStore(context) }
    val driveBackupStore = remember(context) { GoogleDriveBackupStore(context) }
    val viewModel: LocalHisabViewModel = viewModel(
        key = "local_hisab",
        factory = viewModelFactory {
            initializer {
                LocalHisabViewModel(
                    localStore = localStore,
                    memberStore = memberStore,
                    onlinePaymentStore = onlinePaymentStore,
                    takaJomaStore = takaJomaStore,
                    monthlySalesStore = monthlySalesStore,
                    reservedAdminLoginId = credentialStore.getLoginId(),
                    memberAppLinkStore = memberAppLinkStore,
                    driveBackupStore = driveBackupStore
                )
            }
        }
    )
    val transactions by viewModel.transactions.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val onlinePaymentConfig by viewModel.onlinePaymentConfig.collectAsStateWithLifecycle()
    val takaJomaList by viewModel.takaJomaList.collectAsStateWithLifecycle()
    val monthlySales by viewModel.monthlySales.collectAsStateWithLifecycle()
    val controls by viewModel.controls.collectAsStateWithLifecycle()
    val driveState by viewModel.driveState.collectAsStateWithLifecycle()
    val autoBackupRevision by viewModel.autoBackupRevision.collectAsStateWithLifecycle()
    val aiVoiceState by viewModel.aiVoiceState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showModal by remember { mutableStateOf(false) }
    var modalInitialType by remember { mutableStateOf(TxType.EXPENSE) }
    var editingTransaction by remember { mutableStateOf<HisabTransaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<HisabTransaction?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showQuickAddMemberSheet by remember { mutableStateOf(false) }
    var showMemberAppLinkDialog by remember { mutableStateOf(false) }
    var memberAppDownloadUrl by remember { mutableStateOf(memberAppLinkStore.getDownloadUrl()) }
    var appLoginId by remember { mutableStateOf(credentialStore.getLoginId()) }
    var pendingDriveAction by remember { mutableStateOf<DrivePendingAction?>(null) }

    val executeDriveWithToken: (DrivePendingAction, String) -> Unit = { action, token ->
        when (action) {
            DrivePendingAction.CONNECT -> viewModel.completeDriveConnect(token)
            DrivePendingAction.BACKUP_MANUAL -> viewModel.backupAllToGoogleDrive(token, silent = false)
            DrivePendingAction.BACKUP_SILENT -> viewModel.backupAllToGoogleDrive(token, silent = true)
            DrivePendingAction.RESTORE -> viewModel.restoreAllFromGoogleDrive(token)
        }
    }

    val authResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val action = pendingDriveAction
        pendingDriveAction = null
        if (action == null) return@rememberLauncherForActivityResult
        val isInteractive = action != DrivePendingAction.BACKUP_SILENT
        if (activityResult.resultCode != Activity.RESULT_OK) {
            if (isInteractive) {
                viewModel.showMessage("Google Drive অনুমোদন বাতিল করা হয়েছে")
            }
            return@rememberLauncherForActivityResult
        }
        try {
            val authClient = context.findActivity()?.let { Identity.getAuthorizationClient(it) }
                ?: Identity.getAuthorizationClient(context)
            val authResult = authClient.getAuthorizationResultFromIntent(activityResult.data)
            val accessToken = authResult.accessToken
            if (!accessToken.isNullOrBlank()) {
                executeDriveWithToken(action, accessToken)
            } else if (isInteractive) {
                viewModel.showMessage("Google Drive টোকেন পাওয়া যায়নি")
            }
        } catch (e: ApiException) {
            if (isInteractive) {
                viewModel.showMessage("Google Drive সংযোগ ব্যর্থ হয়েছে (${e.statusCode})")
            }
        } catch (e: Exception) {
            if (isInteractive) {
                viewModel.showMessage(e.localizedMessage ?: "Google Drive সংযোগ ব্যর্থ হয়েছে")
            }
        }
    }

    val requestDriveAuthAndRun: (DrivePendingAction) -> Unit = { action ->
        val isInteractive = action != DrivePendingAction.BACKUP_SILENT
        val activity = context.findActivity()
        val authClient = if (activity != null) {
            Identity.getAuthorizationClient(activity)
        } else {
            Identity.getAuthorizationClient(context)
        }
        val request = GoogleDriveBackupClient.buildAuthorizationRequest()
        authClient.authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val pendingIntent = result.pendingIntent
                    if (isInteractive && pendingIntent != null) {
                        pendingDriveAction = action
                        authResolutionLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    }
                } else {
                    val token = result.accessToken
                    if (!token.isNullOrBlank()) {
                        executeDriveWithToken(action, token)
                    } else if (isInteractive) {
                        viewModel.showMessage("Google Drive অনুমোদন পাওয়া যায়নি")
                    }
                }
            }
            .addOnFailureListener { err ->
                if (isInteractive) {
                    viewModel.showMessage(
                        err.localizedMessage ?: "Google Drive সেবা এই ডিভাইসে পাওয়া যায়নি"
                    )
                }
            }
    }

    val pictureExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val bitmap = ReportPictureRenderer.renderHisabReportBitmap(
                            context = context,
                            transactions = transactions,
                            selectedMonth = controls.reportMonth
                        )
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            ReportPictureRenderer.writeBitmapAsPng(bitmap, out)
                        } ?: run {
                            bitmap.recycle()
                            false
                        }
                    }.getOrDefault(false)
                }
                viewModel.showMessage(
                    if (ok) "রিপোর্ট ছবি (PNG) সংরক্ষিত হয়েছে" else "ছবি সংরক্ষণ ব্যর্থ হয়েছে"
                )
            }
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val payload = viewModel.buildFullBackupJson()
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(payload.toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                viewModel.showMessage(if (ok) "সম্পূর্ণ JSON ব্যাকআপ সংরক্ষিত হয়েছে" else "JSON ব্যাকআপ ব্যর্থ হয়েছে")
            }
        }
    }

    val jsonRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val jsonText = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                if (jsonText != null) {
                    viewModel.restoreFromJson(jsonText)
                } else {
                    viewModel.showMessage("ফাইল পড়া যায়নি")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSharedData()
    }

    LaunchedEffect(autoBackupRevision) {
        if (autoBackupRevision > 0 && driveState.isConnected && driveState.autoBackupEnabled) {
            delay(1200)
            requestDriveAuthAndRun(DrivePendingAction.BACKUP_SILENT)
        }
    }

    LaunchedEffect(controls.snackbarMessage) {
        val msg = controls.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val isSettingsSubPage = controls.selectedTab == HisabTab.SETTINGS &&
        controls.settingsSubPage != SettingsSubPage.MAIN

    if (isSettingsSubPage) {
        BackHandler {
            viewModel.openSettingsSubPage(SettingsSubPage.MAIN)
        }
    } else if (controls.selectedTab != HisabTab.HOME) {
        BackHandler {
            viewModel.selectTab(HisabTab.HOME)
        }
    }

    val openNewModal: (TxType) -> Unit = { type ->
        editingTransaction = null
        modalInitialType = type
        showModal = true
    }

    val openEditModal: (HisabTransaction) -> Unit = { tx ->
        editingTransaction = tx
        modalInitialType = tx.resolvedType
        showModal = true
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                HisabTopAppBar(
                    settingsSubPage = if (controls.selectedTab == HisabTab.SETTINGS) {
                        controls.settingsSubPage
                    } else {
                        SettingsSubPage.MAIN
                    },
                    onBackFromSubPage = { viewModel.openSettingsSubPage(SettingsSubPage.MAIN) },
                    onOpenMemberList = {
                        viewModel.selectTab(HisabTab.SETTINGS)
                        viewModel.openSettingsSubPage(SettingsSubPage.MEMBER_LIST)
                    },
                    onAddMemberClick = { showQuickAddMemberSheet = true }
                )
            },
            bottomBar = {
                if (!useRail) {
                    StylishBottomNavBar(
                        items = HisabTab.entries.map { tab ->
                            val selected = controls.selectedTab == tab
                            StylishNavTab(
                                id = tab.name,
                                label = tab.label,
                                icon = tabIcon(tab, selected),
                                selected = selected,
                                testTag = "nav_${tab.name.lowercase()}",
                                onClick = { viewModel.selectTab(tab) }
                            )
                        }
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        HisabTab.entries.forEach { tab ->
                            val selected = controls.selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (controls.selectedTab) {
                        HisabTab.HOME -> HomeScreen(
                            transactions = transactions,
                            onEdit = openEditModal,
                            onDelete = { deletingTransaction = it },
                            onSeeAll = { viewModel.selectTab(HisabTab.TRANSACTIONS) },
                            members = members,
                            monthlySales = monthlySales,
                            memberTakaJomaList = takaJomaList,
                            onApproveMemberTakaJoma = viewModel::approveMemberTakaJoma,
                            onRejectMemberTakaJoma = viewModel::rejectMemberTakaJoma,
                            onDeleteMemberTakaJoma = viewModel::deleteMemberTakaJoma,
                            onOpenMemberTakaJoma = {
                                viewModel.selectTab(HisabTab.SETTINGS)
                                viewModel.openSettingsSubPage(SettingsSubPage.MEMBER_TAKA_JOMA)
                            }
                        )

                        HisabTab.TRANSACTIONS -> TransactionsScreen(
                            transactions = transactions,
                            memberTakaJomaList = takaJomaList,
                            onApproveMemberTakaJoma = viewModel::approveMemberTakaJoma,
                            onRejectMemberTakaJoma = viewModel::rejectMemberTakaJoma,
                            onDeleteMemberTakaJoma = viewModel::deleteMemberTakaJoma,
                            onOpenMemberTakaJoma = {
                                viewModel.selectTab(HisabTab.SETTINGS)
                                viewModel.openSettingsSubPage(SettingsSubPage.MEMBER_TAKA_JOMA)
                            }
                        )

                        HisabTab.REPORTS -> ReportsScreen(
                            transactions = transactions,
                            selectedMonth = controls.reportMonth,
                            onShiftMonth = viewModel::shiftReportMonth,
                            onExportPicture = {
                                pictureExportLauncher.launch("hisab-report-${controls.reportMonth}.png")
                            }
                        )

                        HisabTab.ACCOUNTS -> AccountsScreen(
                            transactions = transactions
                        )

                        HisabTab.SETTINGS -> when (controls.settingsSubPage) {
                            SettingsSubPage.MAIN -> SettingsScreen(
                                appLoginId = appLoginId,
                                members = members,
                                memberCount = members.size,
                                activeMemberCount = members.count { it.isActive },
                                isOnlinePaymentEnabled = onlinePaymentConfig.isOnlinePaymentEnabled,
                                takaJomaCount = takaJomaList.size,
                                salesCount = monthlySales.size,
                                memberAppDownloadUrl = memberAppDownloadUrl,
                                transactionCount = transactions.size,
                                isBusy = controls.isSaving,
                                driveState = driveState,
                                aiVoiceState = aiVoiceState,
                                onOpenMemberList = { viewModel.openSettingsSubPage(SettingsSubPage.MEMBER_LIST) },
                                onOpenMemberTakaJoma = { viewModel.openSettingsSubPage(SettingsSubPage.MEMBER_TAKA_JOMA) },
                                onOpenSalesUpload = { viewModel.openSettingsSubPage(SettingsSubPage.SALES_UPLOAD) },
                                onOpenOnlinePayment = { viewModel.openSettingsSubPage(SettingsSubPage.ONLINE_PAYMENT) },
                                onOpenMemberAppLink = { showMemberAppLinkDialog = true },
                                onOpenChangePassword = { showChangePasswordDialog = true },
                                onAppLogout = onAppLogout,
                                onExportJson = { jsonExportLauncher.launch("rafiqul-hisab-full-backup.json") },
                                onRestoreJson = { jsonRestoreLauncher.launch(arrayOf("application/json", "*/*")) },
                                onExportPicture = {
                                    pictureExportLauncher.launch("hisab-report-${controls.reportMonth}.png")
                                },
                                onConnectGoogleDrive = { requestDriveAuthAndRun(DrivePendingAction.CONNECT) },
                                onBackupToGoogleDrive = { requestDriveAuthAndRun(DrivePendingAction.BACKUP_MANUAL) },
                                onRestoreFromGoogleDrive = { requestDriveAuthAndRun(DrivePendingAction.RESTORE) },
                                onToggleDriveAutoBackup = viewModel::setDriveAutoBackup,
                                onDisconnectGoogleDrive = viewModel::disconnectGoogleDrive,
                                onSelectAiVoiceOption = viewModel::selectAiVoiceOption,
                                onSelectAiVoiceScope = viewModel::selectAiVoiceScope,
                                onSelectAiVoiceMember = viewModel::selectAiVoiceMember,
                                onSpeakAiVoiceReport = viewModel::speakMemberSalesDuesWithAi,
                                onReplayAiVoiceReport = viewModel::replayAiVoice,
                                onStopAiVoiceReport = viewModel::stopAiVoice
                            )

                            SettingsSubPage.MEMBER_LIST -> MemberListScreen(
                                members = members,
                                onSaveMember = viewModel::saveMember,
                                onToggleActive = viewModel::toggleMemberActive,
                                onDeleteMember = viewModel::deleteMember,
                                onResetPassword = { member -> viewModel.resetMemberPassword(member.id) }
                            )

                            SettingsSubPage.ONLINE_PAYMENT -> OnlinePaymentScreen(
                                config = onlinePaymentConfig,
                                onToggleMasterEnabled = viewModel::setOnlinePaymentEnabled,
                                onToggleAccountEnabled = viewModel::togglePaymentAccountEnabled,
                                onUpdateAccount = viewModel::updatePaymentAccount,
                                onRecordPaymentTransaction = viewModel::recordOnlinePaymentTransaction,
                                onShowMessage = viewModel::showMessage
                            )

                            SettingsSubPage.MEMBER_TAKA_JOMA -> MemberTakaJomaScreen(
                                submissions = takaJomaList,
                                adminMembers = members,
                                isAdminMode = true,
                                onSubmit = viewModel::submitMemberTakaJoma,
                                onDeleteSubmission = viewModel::deleteMemberTakaJoma
                            )

                            SettingsSubPage.SALES_UPLOAD -> SalesUploadScreen(
                                salesRecords = monthlySales,
                                selectedMonth = controls.salesMonth,
                                members = members,
                                onShiftMonth = viewModel::shiftSalesMonth,
                                onUploadMonthSales = viewModel::uploadMonthlySales,
                                onSaveSingleRecord = viewModel::saveMonthlySalesRecord,
                                onDeleteRecord = viewModel::deleteMonthlySalesRecord,
                                onClearMonth = viewModel::clearMonthlySales,
                                onShowMessage = viewModel::showMessage
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModal) {
        TransactionModalSheet(
            editingTransaction = editingTransaction,
            initialType = modalInitialType,
            isSaving = controls.isSaving,
            onDismiss = { showModal = false },
            onSave = { editingId, type, amount, category, note, date, account ->
                viewModel.saveTransaction(editingId, type, amount, category, note, date, account) { ok ->
                    if (ok) showModal = false
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            currentLoginId = appLoginId,
            onDismiss = { showChangePasswordDialog = false },
            onChangePassword = { currentPw, newPw, newId ->
                credentialStore.changePassword(currentPw, newPw, newId)
            },
            onSuccess = {
                appLoginId = credentialStore.getLoginId()
                showChangePasswordDialog = false
                viewModel.showMessage("পাসওয়ার্ড সফলভাবে পরিবর্তন হয়েছে")
            }
        )
    }

    if (showQuickAddMemberSheet) {
        MemberEditorModalSheet(
            editingMember = null,
            onDismiss = { showQuickAddMemberSheet = false },
            onSave = { editingId, code, name, designation, area, phone, depot, isActive, email, address, joiningDate ->
                viewModel.saveMember(
                    editingId = editingId,
                    name = name,
                    designation = designation,
                    phone = phone,
                    depot = depot,
                    isActive = isActive,
                    email = email,
                    address = address,
                    joiningDate = joiningDate,
                    code = code,
                    area = area
                ) { ok ->
                    if (ok) showQuickAddMemberSheet = false
                }
            }
        )
    }

    if (showMemberAppLinkDialog) {
        MemberAppLinkConfigDialog(
            currentUrl = memberAppDownloadUrl,
            onDismiss = { showMemberAppLinkDialog = false },
            onSave = { rawUrl ->
                val res = memberAppLinkStore.saveDownloadUrl(rawUrl)
                res.onSuccess { saved ->
                    memberAppDownloadUrl = saved
                    showMemberAppLinkDialog = false
                    viewModel.notifyMemberAppLinkUpdated()
                    viewModel.showMessage(
                        if (saved.isBlank()) {
                            "কাস্টম ডাউনলোড লিঙ্ক মুছে ফেলা হয়েছে"
                        } else {
                            "মেম্বার অ্যাপ ডাউনলোড লিঙ্ক সংরক্ষিত হয়েছে"
                        }
                    )
                }
                res.map { Unit }
            },
            onShareApk = {
                scope.launch {
                    val apkUri = withContext(Dispatchers.IO) {
                        memberAppLinkStore.prepareApkShareUri()
                    }
                    val shared = MemberAppLinkStore.shareApkOrLink(
                        context = context,
                        apkUri = apkUri,
                        downloadUrl = memberAppDownloadUrl.ifBlank { MemberAppLinkStore.DEFAULT_MEMBER_APP_URL }
                    )
                    if (!shared) {
                        viewModel.showMessage("অ্যাপ APK শেয়ার করা যায়নি")
                    }
                }
            },
            onCopyLink = { linkText ->
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("Member App Link", linkText))
                viewModel.showMessage("মেম্বার মোবাইল লিঙ্ক কপি হয়েছে")
            }
        )
    }

    deletingTransaction?.let { target ->
        DeleteTransactionDialog(
            transaction = target,
            onConfirm = {
                viewModel.deleteTransaction(target)
                deletingTransaction = null
            },
            onDismiss = { deletingTransaction = null }
        )
    }
}

@Composable
private fun MemberAppLinkConfigDialog(
    currentUrl: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Result<Unit>,
    onShareApk: () -> Unit,
    onCopyLink: (String) -> Unit
) {
    val activeLink = MemberAppLinkStore.resolveDisplayUrl(currentUrl)
    var urlInput by remember(currentUrl) { mutableStateOf(currentUrl) }
    var errorText by remember { mutableStateOf<String?>(null) }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("মেম্বার মোবাইল লিঙ্ক (Login System)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "মেম্বাররা নিচের লিঙ্কে গিয়ে লগইন স্ক্রিনে নিজের নাম (Login ID) ও পাসওয়ার্ড দিয়ে লগইন করতে পারবেন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "সক্রিয় মেম্বার লিঙ্ক:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = activeLink,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(
                            onClick = { onCopyLink(activeLink) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("dialog_copy_member_link_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "লিঙ্ক কপি করুন",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                androidx.compose.material3.OutlinedTextField(
                    value = urlInput,
                    onValueChange = {
                        urlInput = it
                        errorText = null
                    },
                    label = { Text("কাস্টম লিঙ্ক (ঐচ্ছিক, https://...)") },
                    placeholder = { Text(MemberAppLinkStore.DEFAULT_MEMBER_APP_URL) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_app_link_input")
                )
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                androidx.compose.material3.OutlinedButton(
                    onClick = onShareApk,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("মেম্বার লিঙ্ক ও APK শেয়ার করুন")
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    val result = onSave(urlInput)
                    result.onFailure { err ->
                        errorText = err.localizedMessage ?: "সঠিক URL দিন।"
                    }
                },
                modifier = Modifier.testTag("member_app_link_save_button")
            ) {
                Text("সংরক্ষণ")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

@Composable
private fun HisabTopAppBar(
    settingsSubPage: SettingsSubPage,
    onBackFromSubPage: () -> Unit,
    onOpenMemberList: () -> Unit,
    onAddMemberClick: () -> Unit
) {
    val isSubPage = settingsSubPage != SettingsSubPage.MAIN
    val title = when (settingsSubPage) {
        SettingsSubPage.MAIN -> stringResource(R.string.app_name)
        SettingsSubPage.MEMBER_LIST -> "Member List"
        SettingsSubPage.ONLINE_PAYMENT -> "Online Payment"
        SettingsSubPage.MEMBER_TAKA_JOMA -> "মেম্বার টাকা জমা"
        SettingsSubPage.SALES_UPLOAD -> "Sales Upload"
    }
    val subtitle = when (settingsSubPage) {
        SettingsSubPage.MAIN -> stringResource(R.string.app_subtitle)
        SettingsSubPage.MEMBER_LIST -> "সকল সদস্যের তালিকা ও সক্রিয় অবস্থা"
        SettingsSubPage.ONLINE_PAYMENT -> "অনলাইন পেমেন্ট মাধ্যম ও হিসাব"
        SettingsSubPage.MEMBER_TAKA_JOMA -> "টাকা জমা ফর্ম ও জমার তালিকা"
        SettingsSubPage.SALES_UPLOAD -> "মাস অনুযায়ী code, name, TP sales, TP+Vat"
    }

    Surface(
        color = BrandDeep,
        contentColor = Color.White,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSubPage) {
                IconButton(
                    onClick = onBackFromSubPage,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("settings_subpage_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "পিছনে যান",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            } else {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = stringResource(R.string.app_name),
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.5.sp,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (settingsSubPage != SettingsSubPage.MEMBER_LIST) {
                IconButton(
                    onClick = onOpenMemberList,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("top_header_member_list_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Groups,
                        contentDescription = "Member List",
                        tint = Color.White,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(18.dp))
                    .clickable(onClick = onAddMemberClick)
                    .padding(vertical = 6.dp)
                    .testTag("top_header_add_member_button"),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonAdd,
                            contentDescription = "Member +",
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Member +",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

private fun tabIcon(tab: HisabTab, selected: Boolean): ImageVector = when (tab) {
    HisabTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    HisabTab.TRANSACTIONS -> if (selected) Icons.Filled.AccountBalanceWallet else Icons.Outlined.AccountBalanceWallet
    HisabTab.REPORTS -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
    HisabTab.ACCOUNTS -> if (selected) Icons.Filled.AccountBalance else Icons.Outlined.AccountBalance
    HisabTab.SETTINGS -> if (selected) Icons.Filled.Settings else Icons.Outlined.Settings
}
