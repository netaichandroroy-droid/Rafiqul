package com.example.ui

import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.util.Log
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.auth.AppLockCredentialStore
import com.example.data.CloudAutoSyncRepository
import com.example.data.CloudSyncPrefsStore
import com.example.data.DriveTokenResult
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
import com.google.android.gms.common.api.CommonStatusCodes
import com.example.model.HisabTransaction
import com.example.model.ReportAnalyticsCalculator
import com.example.model.ReportCategory
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
import com.example.ui.screens.reportCategoryIcon
import com.example.ui.theme.BrandDeep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DrivePendingAction {
    CONNECT,
    AUTO_CONNECT_FROM_CLOUD,
    SWITCH_ACCOUNT,
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
    onAppLogout: () -> Unit,
    cloudSyncRepository: CloudAutoSyncRepository? = null,
    syncOwnerUid: String = "local",
    onCloudSignOut: () -> Unit = {},
    isCloudSigningIn: Boolean = false,
    cloudAuthError: String? = null,
    onCloudSignIn: () -> Unit = {},
    onSwitchCloudAccount: () -> Unit = onCloudSignOut
) {
    val context = LocalContext.current
    val localStore = remember(context) { LocalTransactionStore(context) }
    val memberStore = remember(context) { MemberStore(context) }
    val onlinePaymentStore = remember(context) { OnlinePaymentStore(context) }
    val takaJomaStore = remember(context) { MemberTakaJomaStore(context) }
    val monthlySalesStore = remember(context) { MonthlySalesStore(context) }
    val memberAppLinkStore = remember(context) { MemberAppLinkStore(context) }
    val driveBackupStore = remember(context) { GoogleDriveBackupStore(context) }
    val cloudSyncPrefs = remember(context) { CloudSyncPrefsStore(context) }
    val viewModel: LocalHisabViewModel = viewModel(
        key = "local_hisab_$syncOwnerUid",
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
                    driveBackupStore = driveBackupStore,
                    cloudSyncRepository = cloudSyncRepository,
                    cloudSyncPrefs = cloudSyncPrefs
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
    val cloudSyncState by viewModel.cloudSyncState.collectAsStateWithLifecycle()
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
    var showReportPopup by remember { mutableStateOf(false) }
    var exportPictureCategory by remember { mutableStateOf(ReportCategory.MONTHLY) }
    var memberAppDownloadUrl by remember { mutableStateOf(memberAppLinkStore.getDownloadUrl()) }
    var appLoginId by remember { mutableStateOf(credentialStore.getLoginId()) }
    var pendingDriveAction by remember { mutableStateOf<DrivePendingAction?>(null) }
    var pendingDriveEmail by remember { mutableStateOf("") }
    var autoAttemptedCloudEmail by remember { mutableStateOf("") }
    var primedDriveEmail by remember { mutableStateOf("") }
    var showDriveBlockedDialog by remember { mutableStateOf(false) }
    var driveAuthBlockedEmail by remember { mutableStateOf<String?>(null) }
    val signedInCloudEmail = cloudSyncState.accountEmail.trim()

    val executeDriveWithToken: (DrivePendingAction, String, String) -> Unit = { action, token, hintEmail ->
        val onUnauthorized: suspend () -> Unit = {
            GoogleDriveBackupClient.clearCachedToken(context, token)
        }
        when (action) {
            DrivePendingAction.CONNECT,
            DrivePendingAction.AUTO_CONNECT_FROM_CLOUD,
            DrivePendingAction.SWITCH_ACCOUNT -> viewModel.completeDriveConnect(
                accessToken = token,
                hintEmail = hintEmail,
                onUnauthorized = onUnauthorized
            )
            DrivePendingAction.BACKUP_MANUAL -> viewModel.backupAllToGoogleDrive(
                accessToken = token,
                silent = false,
                hintEmail = hintEmail,
                onUnauthorized = onUnauthorized
            )
            DrivePendingAction.BACKUP_SILENT -> viewModel.backupAllToGoogleDrive(
                accessToken = token,
                silent = true,
                hintEmail = hintEmail,
                onUnauthorized = onUnauthorized
            )
            DrivePendingAction.RESTORE -> viewModel.restoreAllFromGoogleDrive(
                accessToken = token,
                hintEmail = hintEmail,
                onUnauthorized = onUnauthorized
            )
        }
    }

    val driveConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val action = pendingDriveAction
        val email = pendingDriveEmail
        pendingDriveAction = null
        pendingDriveEmail = ""
        if (action == null) return@rememberLauncherForActivityResult
        val isInteractive = action != DrivePendingAction.BACKUP_SILENT &&
            action != DrivePendingAction.AUTO_CONNECT_FROM_CLOUD
        if (activityResult.resultCode != Activity.RESULT_OK) {
            if (action == DrivePendingAction.AUTO_CONNECT_FROM_CLOUD && email.isNotBlank()) {
                driveBackupStore.markAutoLinkSkipped(email)
            }
            if (isInteractive) {
                val blockedEmail = email.ifBlank { signedInCloudEmail.ifBlank { "Google Account" } }
                driveAuthBlockedEmail = blockedEmail
                showDriveBlockedDialog = true
            }
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            when (val res = GoogleDriveBackupClient.acquireAccessTokenViaGoogleAuthUtil(context, email)) {
                is DriveTokenResult.Success -> {
                    driveAuthBlockedEmail = null
                    executeDriveWithToken(action, res.accessToken, res.accountEmail)
                }
                is DriveTokenResult.NeedsConsent -> {
                    if (isInteractive) {
                        val blockedEmail = email.ifBlank { signedInCloudEmail.ifBlank { "Google Account" } }
                        driveAuthBlockedEmail = blockedEmail
                        showDriveBlockedDialog = true
                    }
                }
                is DriveTokenResult.Failure -> {
                    if (isInteractive) {
                        if (!res.isNetworkError) {
                            driveAuthBlockedEmail = email.ifBlank { signedInCloudEmail.ifBlank { "Google Account" } }
                            showDriveBlockedDialog = true
                        } else {
                            viewModel.showMessage(res.message)
                        }
                    }
                }
            }
        }
    }

    val accountPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { activityResult ->
        val action = pendingDriveAction
        pendingDriveAction = null
        pendingDriveEmail = ""
        if (action == null) return@rememberLauncherForActivityResult
        val isInteractive = action != DrivePendingAction.BACKUP_SILENT
        if (activityResult.resultCode != Activity.RESULT_OK) {
            if (isInteractive) {
                viewModel.showMessage("Google account selection cancelled")
            }
            return@rememberLauncherForActivityResult
        }
        val chosenEmail = activityResult.data
            ?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
            ?.trim()
            .orEmpty()
        if (chosenEmail.isEmpty()) {
            if (isInteractive) {
                viewModel.showMessage("No Gmail account selected")
            }
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            when (val res = GoogleDriveBackupClient.acquireAccessTokenViaGoogleAuthUtil(context, chosenEmail)) {
                is DriveTokenResult.Success -> {
                    executeDriveWithToken(action, res.accessToken, res.accountEmail)
                }
                is DriveTokenResult.NeedsConsent -> {
                    if (isInteractive) {
                        pendingDriveAction = action
                        pendingDriveEmail = res.accountEmail
                        runCatching {
                            driveConsentLauncher.launch(res.intent)
                        }.onFailure { err ->
                            pendingDriveAction = null
                            pendingDriveEmail = ""
                            viewModel.showMessage(
                                err.localizedMessage ?: "Could not open Google Drive authorization screen"
                            )
                        }
                    }
                }
                is DriveTokenResult.Failure -> {
                    if (isInteractive) {
                        viewModel.showMessage(res.message)
                    }
                }
            }
        }
    }

    val launchNativeDriveFallback: (DrivePendingAction, String) -> Unit = { action, preferredEmail ->
        val canLaunchUi = action != DrivePendingAction.BACKUP_SILENT &&
            action != DrivePendingAction.AUTO_CONNECT_FROM_CLOUD
        val targetEmail = if (action == DrivePendingAction.SWITCH_ACCOUNT) "" else preferredEmail.trim()
        if (targetEmail.isNotEmpty()) {
            scope.launch {
                when (val res = GoogleDriveBackupClient.acquireAccessTokenViaGoogleAuthUtil(context, targetEmail)) {
                    is DriveTokenResult.Success -> {
                        driveAuthBlockedEmail = null
                        executeDriveWithToken(action, res.accessToken, res.accountEmail)
                    }
                    is DriveTokenResult.NeedsConsent -> {
                        if (action == DrivePendingAction.AUTO_CONNECT_FROM_CLOUD) {
                            driveBackupStore.markAutoLinkSkipped(targetEmail)
                        } else if (canLaunchUi) {
                            pendingDriveAction = action
                            pendingDriveEmail = res.accountEmail
                            runCatching {
                                driveConsentLauncher.launch(res.intent)
                            }.onFailure { err ->
                                pendingDriveAction = null
                                pendingDriveEmail = ""
                                viewModel.showMessage(
                                    err.localizedMessage ?: "Could not open Google Drive authorization screen"
                                )
                            }
                        }
                    }
                    is DriveTokenResult.Failure -> {
                        if (action == DrivePendingAction.AUTO_CONNECT_FROM_CLOUD) {
                            driveBackupStore.markAutoLinkSkipped(targetEmail)
                        }
                        if (canLaunchUi) {
                            val keepBoundEmail = action == DrivePendingAction.CONNECT && signedInCloudEmail.isNotBlank()
                            if (res.isNetworkError) {
                                viewModel.showMessage(res.message)
                            } else if (keepBoundEmail) {
                                driveAuthBlockedEmail = targetEmail
                                showDriveBlockedDialog = true
                            } else {
                                pendingDriveAction = action
                                pendingDriveEmail = ""
                                runCatching {
                                    accountPickerLauncher.launch(
                                        GoogleDriveBackupClient.createGoogleAccountPickerIntent(null)
                                    )
                                }.onFailure {
                                    pendingDriveAction = null
                                    driveAuthBlockedEmail = targetEmail
                                    showDriveBlockedDialog = true
                                }
                            }
                        }
                    }
                }
            }
        } else if (canLaunchUi) {
            pendingDriveAction = action
            pendingDriveEmail = ""
            runCatching {
                accountPickerLauncher.launch(
                    GoogleDriveBackupClient.createGoogleAccountPickerIntent(
                        driveState.connectedEmail.takeIf { it.isNotBlank() }
                    )
                )
            }.onFailure { err ->
                pendingDriveAction = null
                viewModel.showMessage(
                    err.localizedMessage ?: "Could not open Google account picker"
                )
            }
        }
    }

    val authResolutionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { activityResult ->
        val action = pendingDriveAction
        val preferredEmail = pendingDriveEmail
        pendingDriveAction = null
        pendingDriveEmail = ""
        if (action == null) return@rememberLauncherForActivityResult
        val canLaunchUi = action != DrivePendingAction.BACKUP_SILENT &&
            action != DrivePendingAction.AUTO_CONNECT_FROM_CLOUD
        val authClient = context.findActivity()?.let { Identity.getAuthorizationClient(it) }
            ?: Identity.getAuthorizationClient(context)

        if (activityResult.resultCode == Activity.RESULT_OK && activityResult.data != null) {
            try {
                val authResult = authClient.getAuthorizationResultFromIntent(activityResult.data)
                val accessToken = authResult.accessToken
                if (!accessToken.isNullOrBlank()) {
                    driveAuthBlockedEmail = null
                    executeDriveWithToken(action, accessToken, preferredEmail)
                    return@rememberLauncherForActivityResult
                }
            } catch (e: ApiException) {
                Log.w("DriveAuth", "AuthorizationClient resolution ApiException (${e.statusCode}): ${e.message}")
            } catch (e: Exception) {
                Log.w("DriveAuth", "AuthorizationClient resolution parse failed: ${e.message}")
            }
        } else if (activityResult.data != null) {
            try {
                authClient.getAuthorizationResultFromIntent(activityResult.data)
            } catch (e: ApiException) {
                Log.w(
                    "DriveAuth",
                    "AuthorizationClient non-OK (resultCode=${activityResult.resultCode}, status=${e.statusCode}): ${e.message}"
                )
                if (e.statusCode == CommonStatusCodes.NETWORK_ERROR && canLaunchUi) {
                    viewModel.showMessage("No internet connection. Please turn on network and try again.")
                    return@rememberLauncherForActivityResult
                }
            } catch (_: Exception) {
                // Fall through to native GoogleAuthUtil / AccountManager flow
            }
        } else {
            Log.w(
                "DriveAuth",
                "AuthorizationClient returned resultCode=${activityResult.resultCode} with null data"
            )
            if (activityResult.resultCode == Activity.RESULT_CANCELED) {
                if (action == DrivePendingAction.AUTO_CONNECT_FROM_CLOUD && preferredEmail.isNotBlank()) {
                    driveBackupStore.markAutoLinkSkipped(preferredEmail)
                }
                if (canLaunchUi) {
                    driveAuthBlockedEmail = preferredEmail.ifBlank { signedInCloudEmail.ifBlank { "Google Account" } }
                    showDriveBlockedDialog = true
                }
                return@rememberLauncherForActivityResult
            }
        }

        if (canLaunchUi) {
            launchNativeDriveFallback(action, preferredEmail)
        }
    }

    val requestDriveAuthAndRun: (DrivePendingAction) -> Unit = { action ->
        val canLaunchUi = action != DrivePendingAction.BACKUP_SILENT &&
            action != DrivePendingAction.AUTO_CONNECT_FROM_CLOUD
        val forceAccountChooser = action == DrivePendingAction.SWITCH_ACCOUNT ||
            (action == DrivePendingAction.CONNECT && signedInCloudEmail.isBlank())
        val preferredEmail = when {
            forceAccountChooser -> ""
            action == DrivePendingAction.AUTO_CONNECT_FROM_CLOUD -> signedInCloudEmail
            action == DrivePendingAction.CONNECT -> signedInCloudEmail
            else -> driveState.connectedEmail.trim().ifBlank { signedInCloudEmail }
        }

        if (forceAccountChooser) {
            launchNativeDriveFallback(action, "")
        } else {
            val activity = context.findActivity()
            val authClient = if (activity != null) {
                Identity.getAuthorizationClient(activity)
            } else {
                Identity.getAuthorizationClient(context)
            }
            val request = GoogleDriveBackupClient.buildAuthorizationRequest(
                accountEmail = preferredEmail.takeIf { it.isNotBlank() }
            )
            authClient.authorize(request)
                .addOnSuccessListener { result ->
                    if (result.hasResolution()) {
                        val pendingIntent = result.pendingIntent
                        if (canLaunchUi && pendingIntent != null) {
                            pendingDriveAction = action
                            pendingDriveEmail = preferredEmail
                            runCatching {
                                authResolutionLauncher.launch(
                                    IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                                )
                            }.onFailure {
                                pendingDriveAction = null
                                pendingDriveEmail = ""
                                launchNativeDriveFallback(action, preferredEmail)
                            }
                        } else if (action == DrivePendingAction.AUTO_CONNECT_FROM_CLOUD && preferredEmail.isNotBlank()) {
                            // Never force-show OAuth 403 block automatically after sign-in
                            driveBackupStore.markAutoLinkSkipped(preferredEmail)
                        }
                    } else {
                        val token = result.accessToken
                        if (!token.isNullOrBlank()) {
                            driveAuthBlockedEmail = null
                            executeDriveWithToken(action, token, preferredEmail)
                        } else if (canLaunchUi || preferredEmail.isNotBlank()) {
                            launchNativeDriveFallback(action, preferredEmail)
                        }
                    }
                }
                .addOnFailureListener { err ->
                    Log.w("DriveAuth", "AuthorizationClient.authorize failed: ${err.message}")
                    if (canLaunchUi || preferredEmail.isNotBlank()) {
                        launchNativeDriveFallback(action, preferredEmail)
                    }
                }
        }
    }

    val driveSafCreateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            driveAuthBlockedEmail = null
            showDriveBlockedDialog = false
            viewModel.connectAndBackupSafUri(
                context = context,
                uri = uri,
                emailHint = signedInCloudEmail
            )
        }
    }

    val driveSafRestoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            driveAuthBlockedEmail = null
            showDriveBlockedDialog = false
            viewModel.restoreAllFromSafUri(
                context = context,
                uriString = uri.toString(),
                persistPermission = true,
                emailHint = signedInCloudEmail
            )
        }
    }

    val pictureExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            val targetCategory = exportPictureCategory
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val bitmap = when (targetCategory) {
                            ReportCategory.MONTHLY -> ReportPictureRenderer.renderHisabReportBitmap(
                                context = context,
                                transactions = transactions,
                                selectedMonth = controls.reportMonth
                            )
                            ReportCategory.MPO_WISE -> {
                                val res = ReportAnalyticsCalculator.buildMpoReport(
                                    members = members,
                                    monthlySales = monthlySales,
                                    submissions = takaJomaList,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope,
                                    onlyMpoDesignation = true
                                )
                                val totals = ReportAnalyticsCalculator.calculateMemberTotals(res.items)
                                ReportPictureRenderer.renderMpoWiseReportBitmap(
                                    context = context,
                                    items = res.items,
                                    totals = totals,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope
                                )
                            }
                            ReportCategory.ASM_DEPOT_WISE -> {
                                val groups = ReportAnalyticsCalculator.buildAsmDepotWiseReport(
                                    members = members,
                                    monthlySales = monthlySales,
                                    submissions = takaJomaList,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope
                                )
                                val totals = ReportAnalyticsCalculator.calculateGroupTotals(groups)
                                ReportPictureRenderer.renderGroupedSummaryReportBitmap(
                                    context = context,
                                    category = ReportCategory.ASM_DEPOT_WISE,
                                    groups = groups,
                                    totals = totals,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope
                                )
                            }
                            ReportCategory.ASM_NAME_WISE -> {
                                val res = ReportAnalyticsCalculator.buildAsmNameWiseReport(
                                    members = members,
                                    monthlySales = monthlySales,
                                    submissions = takaJomaList,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope,
                                    scopeMode = controls.asmNameScope
                                )
                                val totals = ReportAnalyticsCalculator.calculateGroupTotals(res.rows)
                                ReportPictureRenderer.renderGroupedSummaryReportBitmap(
                                    context = context,
                                    category = ReportCategory.ASM_NAME_WISE,
                                    groups = res.rows,
                                    totals = totals,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope
                                )
                            }
                            ReportCategory.AM_WISE -> {
                                val res = ReportAnalyticsCalculator.buildAreaRoleWiseReport(
                                    members = members,
                                    monthlySales = monthlySales,
                                    submissions = takaJomaList,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope,
                                    roleFilter = controls.areaRoleFilter
                                )
                                val totals = ReportAnalyticsCalculator.calculateGroupTotals(res.rows)
                                ReportPictureRenderer.renderGroupedSummaryReportBitmap(
                                    context = context,
                                    category = ReportCategory.AM_WISE,
                                    groups = res.rows,
                                    totals = totals,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope
                                )
                            }
                            ReportCategory.REGION_WISE -> {
                                val groups = ReportAnalyticsCalculator.buildRegionWiseReport(
                                    members = members,
                                    monthlySales = monthlySales,
                                    submissions = takaJomaList,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope
                                )
                                val totals = ReportAnalyticsCalculator.calculateGroupTotals(groups)
                                ReportPictureRenderer.renderGroupedSummaryReportBitmap(
                                    context = context,
                                    category = ReportCategory.REGION_WISE,
                                    groups = groups,
                                    totals = totals,
                                    selectedMonth = controls.reportMonth,
                                    periodScope = controls.reportPeriodScope
                                )
                            }
                        }
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            ReportPictureRenderer.writeBitmapAsPng(bitmap, out)
                        } ?: run {
                            bitmap.recycle()
                            false
                        }
                    }.getOrDefault(false)
                }
                viewModel.showMessage(
                    if (ok) "${targetCategory.title} image (PNG) saved" else "Failed to save image"
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
                viewModel.showMessage(if (ok) "Full JSON backup saved" else "JSON backup failed")
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
                    viewModel.showMessage("Could not read file")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSharedData()
    }

    LaunchedEffect(cloudSyncState.lastSyncedAtMs) {
        val syncedUrl = memberAppLinkStore.getDownloadUrl()
        if (syncedUrl != memberAppDownloadUrl) {
            memberAppDownloadUrl = syncedUrl
        }
        val syncedLoginId = credentialStore.getLoginId()
        if (syncedLoginId != appLoginId) {
            appLoginId = syncedLoginId
        }
    }

    LaunchedEffect(
        driveState.isConnected,
        driveState.oauthConnected,
        driveState.connectedEmail,
        cloudSyncState.isConnected,
        signedInCloudEmail
    ) {
        val restoredEmail = driveState.connectedEmail.trim()
        if (driveState.oauthConnected && restoredEmail.isNotBlank()) {
            if (!primedDriveEmail.equals(restoredEmail, ignoreCase = true)) {
                primedDriveEmail = restoredEmail
                withContext(Dispatchers.IO) {
                    GoogleDriveBackupClient.acquireAccessTokenViaGoogleAuthUtil(context, restoredEmail)
                }
            }
            if (signedInCloudEmail.isNotBlank()) {
                autoAttemptedCloudEmail = signedInCloudEmail
            }
            return@LaunchedEffect
        }
        if (driveState.isConnected || !cloudSyncState.isConnected || signedInCloudEmail.isBlank()) {
            if (signedInCloudEmail.isBlank()) autoAttemptedCloudEmail = ""
            return@LaunchedEffect
        }
        if (autoAttemptedCloudEmail.equals(signedInCloudEmail, ignoreCase = true)) {
            return@LaunchedEffect
        }
        if (driveBackupStore.wasAutoLinkSkippedFor(signedInCloudEmail)) {
            autoAttemptedCloudEmail = signedInCloudEmail
            return@LaunchedEffect
        }
        autoAttemptedCloudEmail = signedInCloudEmail
        delay(250)
        requestDriveAuthAndRun(DrivePendingAction.AUTO_CONNECT_FROM_CLOUD)
    }

    LaunchedEffect(autoBackupRevision) {
        if (autoBackupRevision > 0 && driveState.isConnected && driveState.autoBackupEnabled) {
            delay(1200)
            if (driveState.safDocumentUri.isNotBlank()) {
                viewModel.backupAllToSafUri(
                    context = context,
                    uriString = driveState.safDocumentUri,
                    silent = true,
                    emailHint = signedInCloudEmail
                )
            }
            if (driveState.oauthConnected) {
                requestDriveAuthAndRun(DrivePendingAction.BACKUP_SILENT)
            }
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
                            val isReportsTab = tab == HisabTab.REPORTS
                            StylishNavTab(
                                id = tab.name,
                                label = tab.label,
                                icon = tabIcon(tab, selected),
                                selected = selected,
                                testTag = "nav_${tab.name.lowercase()}",
                                onClick = {
                                    if (isReportsTab) {
                                        showReportPopup = true
                                    } else {
                                        showReportPopup = false
                                        viewModel.selectTab(tab)
                                    }
                                },
                                popupContent = if (isReportsTab) {
                                    {
                                        ReportNavbarPopupMenu(
                                            expanded = showReportPopup,
                                            selectedCategory = controls.selectedReportCategory,
                                            isReportsTabActive = controls.selectedTab == HisabTab.REPORTS,
                                            onDismiss = { showReportPopup = false },
                                            onSelectCategory = { category ->
                                                showReportPopup = false
                                                viewModel.openReportCategory(category)
                                            }
                                        )
                                    }
                                } else {
                                    null
                                }
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
                            val isReportsTab = tab == HisabTab.REPORTS
                            Box {
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = {
                                        if (isReportsTab) {
                                            showReportPopup = true
                                        } else {
                                            showReportPopup = false
                                            viewModel.selectTab(tab)
                                        }
                                    },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) },
                                    modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                )
                                if (isReportsTab) {
                                    ReportNavbarPopupMenu(
                                        expanded = showReportPopup,
                                        selectedCategory = controls.selectedReportCategory,
                                        isReportsTabActive = controls.selectedTab == HisabTab.REPORTS,
                                        onDismiss = { showReportPopup = false },
                                        onSelectCategory = { category ->
                                            showReportPopup = false
                                            viewModel.openReportCategory(category)
                                        }
                                    )
                                }
                            }
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
                                exportPictureCategory = controls.selectedReportCategory
                                val fileSlug = when (controls.selectedReportCategory) {
                                    ReportCategory.MONTHLY -> "hisab-report-${controls.reportMonth}.png"
                                    ReportCategory.MPO_WISE -> "mpo-report-${controls.reportMonth}.png"
                                    ReportCategory.ASM_DEPOT_WISE -> "asm-depot-report-${controls.reportMonth}.png"
                                    ReportCategory.ASM_NAME_WISE -> "asm-name-report-${controls.reportMonth}.png"
                                    ReportCategory.AM_WISE -> "area-role-report-${controls.reportMonth}.png"
                                    ReportCategory.REGION_WISE -> "region-report-${controls.reportMonth}.png"
                                }
                                pictureExportLauncher.launch(fileSlug)
                            },
                            members = members,
                            monthlySales = monthlySales,
                            memberTakaJomaList = takaJomaList,
                            selectedCategory = controls.selectedReportCategory,
                            periodScope = controls.reportPeriodScope,
                            asmNameScope = controls.asmNameScope,
                            areaRoleFilter = controls.areaRoleFilter,
                            onSelectCategory = viewModel::openReportCategory,
                            onSelectPeriodScope = viewModel::setReportPeriodScope,
                            onSelectAsmNameScope = viewModel::setAsmNameScope,
                            onSelectAreaRoleFilter = viewModel::setAreaRoleFilter
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
                                cloudSyncState = cloudSyncState,
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
                                    exportPictureCategory = ReportCategory.MONTHLY
                                    pictureExportLauncher.launch("hisab-report-${controls.reportMonth}.png")
                                },
                                onToggleCloudAutoSync = viewModel::setCloudAutoSyncEnabled,
                                onSyncCloudNow = viewModel::syncCloudNow,
                                onCloudSignOut = onCloudSignOut,
                                isCloudSigningIn = isCloudSigningIn,
                                cloudAuthError = cloudAuthError,
                                onCloudSignIn = onCloudSignIn,
                                onSwitchCloudAccount = onSwitchCloudAccount,
                                onConnectGoogleDrive = {
                                    driveBackupStore.clearAutoLinkSkipped()
                                    requestDriveAuthAndRun(
                                        if (driveState.oauthConnected) {
                                            DrivePendingAction.SWITCH_ACCOUNT
                                        } else {
                                            DrivePendingAction.CONNECT
                                        }
                                    )
                                },
                                onLinkGoogleDriveFile = {
                                    driveSafCreateLauncher.launch(GoogleDriveBackupClient.BACKUP_FILE_NAME)
                                },
                                onRestoreFromDriveFile = {
                                    driveSafRestoreLauncher.launch(
                                        arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")
                                    )
                                },
                                onShowDriveAccessHelp = {
                                    showDriveBlockedDialog = true
                                },
                                driveAuthBlockedEmail = driveAuthBlockedEmail,
                                onBackupToGoogleDrive = {
                                    if (driveState.safDocumentUri.isNotBlank()) {
                                        viewModel.backupAllToSafUri(
                                            context = context,
                                            uriString = driveState.safDocumentUri,
                                            silent = false,
                                            emailHint = signedInCloudEmail
                                        )
                                    }
                                    if (driveState.oauthConnected) {
                                        requestDriveAuthAndRun(DrivePendingAction.BACKUP_MANUAL)
                                    } else if (driveState.safDocumentUri.isBlank()) {
                                        requestDriveAuthAndRun(DrivePendingAction.BACKUP_MANUAL)
                                    }
                                },
                                onRestoreFromGoogleDrive = {
                                    if (driveState.oauthConnected) {
                                        requestDriveAuthAndRun(DrivePendingAction.RESTORE)
                                    } else if (driveState.safDocumentUri.isNotBlank()) {
                                        viewModel.restoreAllFromSafUri(
                                            context = context,
                                            uriString = driveState.safDocumentUri,
                                            emailHint = signedInCloudEmail
                                        )
                                    } else {
                                        driveSafRestoreLauncher.launch(
                                            arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")
                                        )
                                    }
                                },
                                onToggleDriveAutoBackup = viewModel::setDriveAutoBackup,
                                onDisconnectGoogleDrive = {
                                    if (signedInCloudEmail.isNotBlank()) {
                                        driveBackupStore.markAutoLinkSkipped(signedInCloudEmail)
                                    }
                                    driveAuthBlockedEmail = null
                                    viewModel.disconnectGoogleDrive(context)
                                },
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
                viewModel.syncAdminCredentialsToCloud(
                    loginId = appLoginId,
                    passwordHash = credentialStore.getPasswordHash(),
                    isDefaultPassword = credentialStore.isUsingDefaultPassword()
                )
                showChangePasswordDialog = false
                viewModel.showMessage("Password updated successfully")
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
                            "Custom download link removed"
                        } else {
                            "Member app download link saved"
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
                        viewModel.showMessage("Could not share app APK")
                    }
                }
            },
            onCopyLink = { linkText ->
                val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                cm?.setPrimaryClip(android.content.ClipData.newPlainText("Member App Link", linkText))
                viewModel.showMessage("Member mobile link copied")
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

    if (showDriveBlockedDialog) {
        val displayEmail = driveAuthBlockedEmail?.takeIf { it.isNotBlank() }
            ?: signedInCloudEmail.ifBlank { "Google Account" }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDriveBlockedDialog = false },
            title = {
                Text("Connect Google Drive Backup File")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Direct Drive API access is currently unavailable for $displayEmail.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Recommended: Link a Google Drive Backup File",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Tap 'Link Drive File' below, choose Drive / My Drive in the file picker, and tap Save. Auto-backup works seamlessly with any Gmail account.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    androidx.compose.material3.OutlinedButton(
                        onClick = {
                            showDriveBlockedDialog = false
                            requestDriveAuthAndRun(DrivePendingAction.SWITCH_ACCOUNT)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("drive_403_switch_gmail_button")
                    ) {
                        Text("Try Another Account")
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = {
                        showDriveBlockedDialog = false
                        driveSafCreateLauncher.launch(GoogleDriveBackupClient.BACKUP_FILE_NAME)
                    },
                    modifier = Modifier.testTag("drive_403_link_saf_button")
                ) {
                    Text("Link Drive File")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDriveBlockedDialog = false },
                    modifier = Modifier.testTag("drive_403_dismiss_button")
                ) {
                    Text("Close")
                }
            }
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
        title = { Text("Member Mobile Link (Login System)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Members can open the link below and sign in using their Code (Login ID) and password.",
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
                                text = "Active Member Link:",
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
                                contentDescription = "Copy Link",
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
                    label = { Text("Custom Link (Optional, https://...)") },
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
                    Text("Share Member Link & APK")
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(
                onClick = {
                    val result = onSave(urlInput)
                    result.onFailure { err ->
                        errorText = err.localizedMessage ?: "Please enter a valid URL."
                    }
                },
                modifier = Modifier.testTag("member_app_link_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
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
        SettingsSubPage.MEMBER_TAKA_JOMA -> "Member Deposit"
        SettingsSubPage.SALES_UPLOAD -> "Sales Upload"
    }
    val subtitle = when (settingsSubPage) {
        SettingsSubPage.MAIN -> stringResource(R.string.app_subtitle)
        SettingsSubPage.MEMBER_LIST -> "All members directory & active status"
        SettingsSubPage.ONLINE_PAYMENT -> "Online payment channels & accounts"
        SettingsSubPage.MEMBER_TAKA_JOMA -> "Deposit form & submission history"
        SettingsSubPage.SALES_UPLOAD -> "Monthly code, name, TP sales, TP+VAT sales"
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
                        contentDescription = "Go Back",
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

@Composable
private fun ReportNavbarPopupMenu(
    expanded: Boolean,
    selectedCategory: ReportCategory,
    isReportsTabActive: Boolean,
    onDismiss: () -> Unit,
    onSelectCategory: (ReportCategory) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(x = 0.dp, y = (-8).dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("navbar_report_popup_menu")
    ) {
        Text(
            text = "Report Menu",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
        ReportCategory.entries.forEachIndexed { index, category ->
            val isActiveItem = isReportsTabActive && selectedCategory == category
            DropdownMenuItem(
                leadingIcon = {
                    Icon(
                        imageVector = reportCategoryIcon(category),
                        contentDescription = null,
                        tint = if (isActiveItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = if (isActiveItem) {
                    {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Selected",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    null
                },
                text = {
                    Column(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text(
                            text = category.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isActiveItem) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isActiveItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = category.subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                onClick = { onSelectCategory(category) },
                modifier = Modifier
                    .background(
                        if (isActiveItem) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        else Color.Transparent
                    )
                    .testTag(category.menuTestTag)
            )
            if (index < ReportCategory.entries.lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    }
}
