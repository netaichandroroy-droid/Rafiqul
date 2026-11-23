package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import com.example.ai.configureAppCheck
import com.example.auth.AppLockCredentialStore
import com.example.auth.AppSession
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.onGoogleSignInClicked
import com.example.auth.safeFirebaseAuth
import com.example.auth.signOutFromGoogleCloud
import com.example.auth.switchGoogleCloudAccount
import com.example.data.CloudAutoSyncRepository
import com.example.data.MemberAppLinkStore
import com.example.data.MemberLoginResult
import com.example.data.MemberStore
import com.example.ui.HisabMainApp
import com.example.ui.MemberMainApp
import com.example.ui.screens.AppLoginScreen
import com.example.ui.theme.HisabKhataTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private var deepLinkLoginId by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        deepLinkLoginId = extractLoginIdFromIntent(intent)
        setContent {
            HisabKhataTheme {
                AppContent(initialLoginId = deepLinkLoginId)
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
    }

    private fun extractLoginIdFromIntent(intent: Intent?): String {
        return runCatching {
            intent?.data?.getQueryParameter("member")?.trim().orEmpty()
        }.getOrDefault("")
    }
}

@Composable
fun AppContent(initialLoginId: String = "") {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) {
        runCatching { CredentialManager.create(context) }.getOrNull()
    }
    val credentialStore = remember(context) { AppLockCredentialStore(context) }
    val memberStore = remember(context) { MemberStore(context) }
    val memberAppLinkStore = remember(context) { MemberAppLinkStore(context) }
    val firebaseAuth = remember { safeFirebaseAuth() }

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

    val isAdminSession = activeSession == AppSession.Admin

    LaunchedEffect(isAdminSession, firebaseAuth) {
        if (isAdminSession && firebaseAuth != null && runCatching { firebaseAuth.currentUser }.getOrNull() == null) {
            delay(350)
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
    val cloudSyncRepository = remember(context, uid, isAdminSession) {
        if (isAdminSession && uid.isNotBlank()) {
            runCatching { CloudAutoSyncRepository(context) }.getOrNull()
        } else {
            null
        }
    }

    DisposableEffect(uid, isAdminSession, cloudSyncRepository) {
        if (isAdminSession && uid.isNotBlank() && cloudSyncRepository != null) {
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
            onBeforeSignOut = {
                cloudSyncRepository?.stopAutoSync()
            },
            onSignOutComplete = {
                currentUser = runCatching { safeFirebaseAuth()?.currentUser }.getOrNull()
                authError = null
            }
        )
    }

    when (val session = activeSession) {
        AppSession.LoggedOut -> {
            val downloadUrl = remember(activeSession) {
                memberAppLinkStore.getEffectiveMemberLink()
            }
            AppLoginScreen(
                memberAppDownloadUrl = downloadUrl,
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
                                "আপনার সদস্য অ্যাকাউন্টটি নিষ্ক্রিয় আছে। অ্যাডমিনের সাথে যোগাযোগ করুন।"
                            }

                            MemberLoginResult.WrongPassword,
                            MemberLoginResult.NotFound -> {
                                "লগইন আইডি অথবা পাসওয়ার্ড সঠিক নয়।"
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
                    onSwitchCloudAccount = handleSwitchCloudAccount
                )
            }
        }

        is AppSession.MemberUser -> {
            val member = remember(session.memberId) {
                memberStore.findById(session.memberId)?.takeIf { it.isActive }
            }
            if (member == null) {
                LaunchedEffect(session.memberId) {
                    credentialStore.logout()
                    activeSession = AppSession.LoggedOut
                }
            } else {
                MemberMainApp(
                    initialMember = member,
                    credentialStore = credentialStore,
                    onMemberLogout = {
                        credentialStore.logout()
                        activeSession = AppSession.LoggedOut
                    }
                )
            }
        }
    }
}
