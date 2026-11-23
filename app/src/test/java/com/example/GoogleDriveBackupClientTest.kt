package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.DriveTokenResult
import com.example.data.GoogleDriveBackupClient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GoogleDriveBackupClientTest {

    @Test
    fun `buildAuthorizationRequest binds account when email provided`() {
        val reqWithoutEmail = GoogleDriveBackupClient.buildAuthorizationRequest(null)
        assertNull(reqWithoutEmail.account)
        assertEquals(2, reqWithoutEmail.requestedScopes.size)

        val reqWithEmail = GoogleDriveBackupClient.buildAuthorizationRequest("  netaichandroroy@gmail.com  ")
        assertNotNull(reqWithEmail.account)
        assertEquals("netaichandroroy@gmail.com", reqWithEmail.account?.name)
        assertEquals(GoogleDriveBackupClient.GOOGLE_ACCOUNT_TYPE, reqWithEmail.account?.type)
        assertEquals(2, reqWithEmail.requestedScopes.size)
    }

    @Test
    fun `createGoogleAccountPickerIntent targets google accounts`() {
        val intent = GoogleDriveBackupClient.createGoogleAccountPickerIntent("user@gmail.com")
        assertNotNull(intent)
        val allowedTypes = intent.getStringArrayExtra("allowableAccountTypes")
        assertNotNull(allowedTypes)
        assertTrue(allowedTypes!!.contains(GoogleDriveBackupClient.GOOGLE_ACCOUNT_TYPE))
    }

    @Test
    fun `acquireAccessTokenViaGoogleAuthUtil returns failure when email blank`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val result = GoogleDriveBackupClient.acquireAccessTokenViaGoogleAuthUtil(context, "   ")
        assertTrue(result is DriveTokenResult.Failure)
    }
}
