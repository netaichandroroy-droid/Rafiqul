package com.example

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ai.MondayAutoCallScheduler
import com.example.ai.configureAppCheck
import com.example.auth.AppLockCredentialStore
import com.example.auth.AppSession
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.safeFirebaseAuth
import com.example.auth.signOutFromGoogleCloud
import com.example.auth.switchGoogleCloudAccount
import com.example.data.AppAutoUpdateController
import com.example.data.CloudAutoSyncRepository
import com.example.data.MemberLoginResult
import com.example.data.MemberStore
import com.example.ui.HisabMainApp
import com.example.ui.MemberMainApp
import com.example.ui.components.AppUpdateReadyBanner
import com.example.ui.screens.AppLoginScreen
import com.example.ui.theme.HisabKhataTheme
import kotlinx.coroutines.delay

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

class MainActivity : ComponentActivity() {

    private var deepLinkLoginId by mutableStateOf("")
    private var openAiCallCenterSignal by mutableStateOf(0)
    private var autoStartMondayCallsSignal by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        deepLinkLoginId = extractLoginIdFromIntent(intent)
        if (intent?.getBooleanExtra(MondayAutoCallScheduler.EXTRA_OPEN_AI_CALL_CENTER, false) == true) {
            openAiCallCenterSignal++
        }
        if (intent?.getBooleanExtra(MondayAutoCallScheduler.EXTRA_AUTO_START_MONDAY_CALLS, false) == true) {
            autoStartMondayCallsSignal++
        }
        setContent {
            HisabKhataTheme {
                AppContent(
                    initialLoginId = deepLinkLoginId,
                    openAiCallCenterSignal = openAiCallCenterSignal,
                    autoStartMondayCallsSignal = autoStartMondayCallsSignal
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val incomingId = extractLoginIdFromIntent(intent)
        if (incomingId.isNotBlank()) {
            deepLinkLoginId = incomingId
        }
        if (intent.getBooleanExtra(MondayAutoCallScheduler.EXTRA_OPEN_AI_CALL_CENTER, false)) {
            openAiCallCenterSignal++
        }
        if (intent.getBooleanExtra(MondayAutoCallScheduler.EXTRA_AUTO_START_MONDAY_CALLS, false)) {
            autoStartMondayCallsSignal++
        }
    }

    private fun extractLoginIdFromIntent(intent: Intent?): String {
        return runCatching {
            intent?.data?.getQueryParameter("member")?.trim().orEmpty()
        }.getOrDefault("")
    }
}

@Composable
fun AppContent(
    initialLoginId: String = "",
    openAiCallCenterSignal: Int = 0,
    autoStartMondayCallsSignal: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }
    val credentialStore = remember(context) { AppLockCredentialStore(context) }
    val memberStore = remember(context) { MemberStore(context) }
    val firebaseAuth = remember { safeFirebaseAuth() }
    val appUpdateController = remember(context) { AppAutoUpdateController(context) }
    val appUpdateState by appUpdateController.state.collectAsStateWithLifecycle()

    val appUpdateLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        appUpdateController.handleUpdateFlowResult(result.resultCode)
    }

    LaunchedEffect(appUpdateController) {
        delay(350)
        appUpdateController.checkForUpdates(
            activity = context.findActivity(),
            updateLauncher = appUpdateLauncher,
            userInitiated = false
        )
    }

    DisposableEffect(lifecycleOwner, appUpdateController) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                appUpdateController.checkResumeState(
                    activity = context.findActivity(),
                    updateLauncher = appUpdateLauncher
                )
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            appUpdateController.dispose()
        }
    }

    val handleToggleAutoUpdate: (Boolean) -> Unit = { enabled ->
        appUpdateController.setAutoUpdateEnabled(enabled)
        if (enabled) {
            appUpdateController.checkForUpdates(
                activity = context.findActivity(),
                updateLauncher = appUpdateLauncher,
                userInitiated = false
            )
        }
    }

    val handleCheckAppUpdate: () -> Unit = {
        appUpdateController.checkForUpdates(
            activity = context.findActivity(),
            updateLauncher = appUpdateLauncher,
            userInitiated = true
        )
    }

    val handleCompleteAppUpdate: () -> Unit = {
        appUpdateController.completeUpdate(activity = context.findActivity())
    }

    val handleOpenAppUpdateReleases: () -> Unit = {
        appUpdateController.openReleasePage(activity = context.findActivity())
    }

    var currentUser by remember(firebaseAuth) {
        mutableStateOf(runCatching { firebaseAuth?.currentUser }.getOrNull())
    }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var activeSession by remember { mutableStateOf(credentialStore.getActiveSession()) }

    LaunchedEffect(firebaseAuth) {
        firebaseAuth?.authStateFlow()?.collect { user ->
            currentUser = user
            if (user != null) {
                isSigningIn = false
                authError = null
            }
        }
    }

    var remoteSyncTick by remember { mutableStateOf(0) }

    LaunchedEffect(firebaseAuth) {
        if (firebaseAuth != null && runCatching { firebaseAuth.currentUser }.getOrNull() == null) {
            delay(250)
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    currentUser = runCatching { firebaseAuth.currentUser }.getOrNull()
                    authError = null
                },
                onUnauthenticated = {},
                scope = scope
            )
        }
    }

    val uid = currentUser?.uid.orEmpty()
    val cloudSyncRepository = remember(context, uid) {
        if (uid.isNotBlank()) {
            runCatching { CloudAutoSyncRepository(context) }.getOrNull()
        } else {
            null
        }
    }

    LaunchedEffect(cloudSyncRepository) {
        cloudSyncRepository?.remoteUpdates?.collect {
            remoteSyncTick++
        }
    }

    DisposableEffect(uid, cloudSyncRepository) {
        if (uid.isNotBlank() && cloudSyncRepository != null) {
            runCatching { cloudSyncRepository.startAutoSync(uid) }
        }
        onDispose {
            runCatching { cloudSyncRepository?.stopAutoSync() }
        }
    }

    val handleCloudSignIn: () -> Unit = {
        isSigningIn = true
        authError = null
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                currentUser = runCatching { safeFirebaseAuth()?.currentUser }.getOrNull()
                isSigningIn = false
                authError = null
            },
            onAuthError = { msg ->
                isSigningIn = false
                authError = msg
            },
            scope = scope,
            onAuthCancelled = {
                isSigningIn = false
            }
        )
    }

    val handleSwitchCloudAccount: () -> Unit = {
        isSigningIn = true
        authError = null
        switchGoogleCloudAccount(
            context = context,
            credentialManager = credentialManager,
            onBeforeSignOut = {
                cloudSyncRepository?.stopAutoSync()
            },
            onAuthSuccess = {
                currentUser = runCatching { safeFirebaseAuth()?.currentUser }.getOrNull()
                isSigningIn = false
                authError = null
            },
            onAuthError = { msg ->
                currentUser = runCatching { safeFirebaseAuth()?.currentUser }.getOrNull()
                isSigningIn = false
                authError = msg
            },
            scope = scope,
            onAuthCancelled = {
                currentUser = runCatching { safeFirebaseAuth()?.currentUser }.getOrNull()
                isSigningIn = false
            }
        )
    }

    val handleCloudSignOut: () -> Unit = {
        signOutFromGoogleCloud(
            credentialManager = credentialManager,
            scope = scope,
            context = context,
            onBeforeSignOut = {
                cloudSyncRepository?.stopAutoSync()
            },
            onSignOutComplete = {
                currentUser = runCatching { safeFirebaseAuth()?.currentUser }.getOrNull()
                authError = null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (val session = activeSession) {
            AppSession.LoggedOut -> {
                AppLoginScreen(
                    initialLoginId = initialLoginId,
                    onLoginAttempt = { loginId, password ->
                        if (credentialStore.login(loginId, password)) {
                            activeSession = AppSession.Admin
                            null
                        } else {
                            when (val memberResult = memberStore.verifyMemberLogin(loginId, password)) {
                                is MemberLoginResult.Success -> {
                                    credentialStore.loginAsMember(memberResult.member.id)
                                    activeSession = AppSession.MemberUser(memberResult.member.id)
                                    null
                                }

                                is MemberLoginResult.Inactive -> {
                                    "Your member account is inactive. Please contact the admin."
                                }

                                MemberLoginResult.WrongPassword,
                                MemberLoginResult.NotFound -> {
                                    "Invalid Login ID or password."
                                }
                            }
                        }
                    }
                )
            }

            AppSession.Admin -> {
                val adminSyncOwnerKey = uid.ifBlank { "local" }
                key(adminSyncOwnerKey) {
                    HisabMainApp(
                        credentialStore = credentialStore,
                        onAppLogout = {
                            credentialStore.logout()
                            activeSession = AppSession.LoggedOut
                        },
                        cloudSyncRepository = cloudSyncRepository,
                        syncOwnerUid = adminSyncOwnerKey,
                        onCloudSignOut = handleCloudSignOut,
                        isCloudSigningIn = isSigningIn,
                        cloudAuthError = authError,
                        onCloudSignIn = handleCloudSignIn,
                        onSwitchCloudAccount = handleSwitchCloudAccount,
                        appUpdateState = appUpdateState,
                        onToggleAutoUpdate = handleToggleAutoUpdate,
                        onCheckAppUpdate = handleCheckAppUpdate,
                        onCompleteAppUpdate = handleCompleteAppUpdate,
                        onOpenAppUpdateLink = handleOpenAppUpdateReleases,
                        openAiCallCenterSignal = openAiCallCenterSignal,
                        autoStartMondayCallsSignal = autoStartMondayCallsSignal
                    )
                }
            }

            is AppSession.MemberUser -> {
                val member = remember(session.memberId, remoteSyncTick) {
                    memberStore.findById(session.memberId)?.takeIf { it.isActive }
                }
                if (member == null) {
                    LaunchedEffect(session.memberId) {
                        credentialStore.logout()
                        activeSession = AppSession.LoggedOut
                    }
                } else {
                    val memberSyncOwnerKey = uid.ifBlank { "local" }
                    key("${session.memberId}_$memberSyncOwnerKey") {
                        MemberMainApp(
                            initialMember = member,
                            credentialStore = credentialStore,
                            onMemberLogout = {
                                credentialStore.logout()
                                activeSession = AppSession.LoggedOut
                            },
                            cloudSyncRepository = cloudSyncRepository,
                            syncOwnerUid = memberSyncOwnerKey,
                            appUpdateState = appUpdateState,
                            onToggleAutoUpdate = handleToggleAutoUpdate,
                            onCheckAppUpdate = handleCheckAppUpdate,
                            onCompleteAppUpdate = handleCompleteAppUpdate,
                            onOpenAppUpdateLink = handleOpenAppUpdateReleases
                        )
                    }
                }
            }
        }

        AppUpdateReadyBanner(
            updateState = appUpdateState,
            onRestartToInstall = handleCompleteAppUpdate,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }
}
