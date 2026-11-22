package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AppLockCredentialStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppLockCredentialStoreTest {

    @Test
    fun defaultCredentials_rafiqul_succeed_and_passwordCanBeChanged() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AppLockCredentialStore(context)

        assertEquals("rafiqul", store.getLoginId())
        assertTrue(store.isUsingDefaultPassword())
        assertFalse(store.isLoggedIn())
        assertTrue(store.verifyCredentials("rafiqul", "rafiqul"))
        assertTrue(store.verifyCredentials("RAFIQUL", "rafiqul"))
        assertFalse(store.verifyCredentials("rafiqul", "wrongpass"))
        assertFalse(store.verifyCredentials("other", "rafiqul"))

        // Reject wrong current password on change
        val badChange = store.changePassword("wrongpass", "newpass123")
        assertTrue(badChange.isFailure)
        assertTrue(store.verifyCredentials("rafiqul", "rafiqul"))

        // Change password successfully
        val goodChange = store.changePassword("rafiqul", "newpass123")
        assertTrue(goodChange.isSuccess)
        assertFalse(store.isUsingDefaultPassword())

        // Old password no longer works; new password works
        assertFalse(store.verifyCredentials("rafiqul", "rafiqul"))
        assertTrue(store.verifyCredentials("rafiqul", "newpass123"))
    }

    @Test
    fun loginSession_persistsAcrossInstances_untilExplicitLogout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AppLockCredentialStore(context)

        assertFalse(store.isLoggedIn())
        assertFalse(store.login("rafiqul", "wrong"))
        assertFalse(store.isLoggedIn())

        assertTrue(store.login("rafiqul", "rafiqul"))
        assertTrue(store.isLoggedIn())

        // Simulate app restart with a fresh AppLockCredentialStore instance
        val restartedStore = AppLockCredentialStore(context)
        assertTrue(restartedStore.isLoggedIn())

        // Explicit logout clears session and persists
        restartedStore.logout()
        assertFalse(restartedStore.isLoggedIn())
        val afterLogoutStore = AppLockCredentialStore(context)
        assertFalse(afterLogoutStore.isLoggedIn())
        assertEquals(com.example.auth.AppSession.LoggedOut, afterLogoutStore.getActiveSession())
    }

    @Test
    fun memberSession_persistsAcrossInstances_untilExplicitLogout() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AppLockCredentialStore(context)

        store.loginAsMember("m_netai")
        assertTrue(store.isLoggedIn())
        assertEquals(com.example.auth.AppSession.MemberUser("m_netai"), store.getActiveSession())

        val restarted = AppLockCredentialStore(context)
        assertEquals(com.example.auth.AppSession.MemberUser("m_netai"), restarted.getActiveSession())

        restarted.logout()
        assertEquals(com.example.auth.AppSession.LoggedOut, AppLockCredentialStore(context).getActiveSession())
    }
}
