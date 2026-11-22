package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.ai.configureAppCheck
import com.example.auth.AppLockCredentialStore
import com.example.auth.AppSession
import com.example.data.MemberAppLinkStore
import com.example.data.MemberLoginResult
import com.example.data.MemberStore
import com.example.ui.HisabMainApp
import com.example.ui.MemberMainApp
import com.example.ui.screens.AppLoginScreen
import com.example.ui.theme.HisabKhataTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        val prefillLoginId = intent?.data?.getQueryParameter("member")
            ?.trim()
            .orEmpty()
        setContent {
            HisabKhataTheme {
                AppContent(initialLoginId = prefillLoginId)
            }
        }
    }
}

@Composable
fun AppContent(initialLoginId: String = "") {
    val context = LocalContext.current
    val credentialStore = remember(context) { AppLockCredentialStore(context) }
    val memberStore = remember(context) { MemberStore(context) }
    val memberAppLinkStore = remember(context) { MemberAppLinkStore(context) }
    var activeSession by remember { mutableStateOf(credentialStore.getActiveSession()) }

    when (val session = activeSession) {
        AppSession.LoggedOut -> {
            val downloadUrl = remember(activeSession) { memberAppLinkStore.getEffectiveMemberLink() }
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
            HisabMainApp(
                credentialStore = credentialStore,
                onAppLogout = {
                    credentialStore.logout()
                    activeSession = AppSession.LoggedOut
                }
            )
        }

        is AppSession.MemberUser -> {
            val member = memberStore.findById(session.memberId)?.takeIf { it.isActive }
            if (member == null) {
                credentialStore.logout()
                activeSession = AppSession.LoggedOut
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
