package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.auth.AppLockCredentialStore
import com.example.auth.PasswordHasher
import com.example.data.LocalTransactionStore
import com.example.data.MemberStore
import com.example.data.MemberTakaJomaStore
import com.example.data.MonthlySalesStore
import com.example.model.HisabTransaction
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.TakaJomaStatus
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MainActivityLaunchTest {

    @get:Rule
    val composeRule = createEmptyComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        AppLockCredentialStore(context).logout()
    }

    @Test
    fun `launch MainActivity logged out renders login screen`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("app_login_id_input").assertIsDisplayed()
            composeRule.onNodeWithTag("app_login_submit_button")
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    @Config(sdk = [24])
    fun `launch MainActivity on Android 7 API 24 renders cleanly`() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("app_login_id_input").assertIsDisplayed()
            val store = AppLockCredentialStore(context)
            assertTrue(store.verifyCredentials("rafiqul", "rafiqul"))
        }
    }

    @Test
    fun `launch MainActivity with missing member falls back safely to login screen`() {
        AppLockCredentialStore(context).loginAsMember("non_existent_member")

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("app_login_id_input").assertIsDisplayed()
        }
    }

    @Test
    fun `launch MainActivity as admin renders home screen and switches all tabs safely`() {
        MemberStore(context).saveAll(
            listOf(
                Member(
                    id = "m1",
                    name = "TestMember",
                    designation = "SR",
                    phone = "01700000000",
                    depot = "Dhaka",
                    code = "M-101",
                    isActive = true
                )
            )
        )
        LocalTransactionStore(context).saveAll(
            listOf(
                HisabTransaction(
                    id = "tx1",
                    userId = "local_rafiqul",
                    type = "income",
                    amount = 1500.0,
                    category = "বিক্রয়",
                    note = "নোট",
                    date = "2026-11-22",
                    account = "cash"
                )
            )
        )
        MemberTakaJomaStore(context).saveAll(
            listOf(
                MemberTakaJoma(
                    id = "j1",
                    memberId = "m1",
                    name = "TestMember",
                    code = "M-101",
                    depot = "Dhaka",
                    amount = 500.0,
                    date = "2026-11-22",
                    approvalStatus = TakaJomaStatus.PENDING.wireValue
                )
            )
        )
        MonthlySalesStore(context).saveAll(
            listOf(
                MonthlySalesRecord(
                    id = "s1",
                    yearMonth = "2026-11",
                    code = "M-101",
                    name = "TestMember",
                    tpSales = 1800.0,
                    tpPlusVat = 2000.0
                )
            )
        )
        AppLockCredentialStore(context).login("rafiqul", "rafiqul")

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
            composeRule.onNodeWithText("Rafiqul Islam").assertIsDisplayed()

            composeRule.onNodeWithTag("nav_transactions").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("nav_reports").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("report_selection_screen").assertIsDisplayed()
            composeRule.onNodeWithTag("report_menu_monthly").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("report_menu_mpo_wise").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("report_menu_am_wise").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("report_menu_region_wise").performScrollTo().assertIsDisplayed()

            composeRule.onNodeWithTag("report_menu_mpo_wise").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("report_full_page_card_mpo_wise").assertIsDisplayed()
            composeRule.onNodeWithTag("report_totals_strip")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("report_subpage_back_button").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("report_menu_am_wise").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("report_full_page_card_am_wise").assertIsDisplayed()
            composeRule.onNodeWithTag("report_totals_strip")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("report_subpage_back_button").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("report_menu_region_wise").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("report_full_page_card_region_wise").assertIsDisplayed()
            composeRule.onNodeWithTag("report_totals_strip")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("report_subpage_back_button").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("report_menu_monthly").performScrollTo().performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("report_full_page_card_monthly").assertIsDisplayed()

            composeRule.onNodeWithTag("nav_accounts").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("accounts_total_hero_card").assertIsDisplayed()
            composeRule.onNodeWithTag("account_card_cash").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("account_card_mobile_bank").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("account_card_bank").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("all_accounts_grand_total_row").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("all_banks_combined_total_row").performScrollTo().assertIsDisplayed()

            composeRule.onNodeWithTag("nav_settings").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun `launch MainActivity as member renders member home screen`() {
        MemberStore(context).saveAll(
            listOf(
                Member(
                    id = "m1",
                    name = "TestMember",
                    designation = "SR",
                    phone = "01700000000",
                    depot = "Dhaka",
                    code = "M-101",
                    isActive = true
                )
            )
        )
        AppLockCredentialStore(context).loginAsMember("m1")

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("member_home_hero_card").assertIsDisplayed()
        }
    }

    @Test
    fun `passwordHasher fallback produces identical PBKDF2 output to JCE`() {
        val hash = PasswordHasher.hashPassword("rafiqul", minLength = 4)
        assertTrue(PasswordHasher.verifyPassword("rafiqul", hash))

        val method = PasswordHasher::class.java.getDeclaredMethod(
            "derivePbkdf2HmacSha256Fallback",
            CharArray::class.java,
            ByteArray::class.java,
            Int::class.javaPrimitiveType,
            Int::class.javaPrimitiveType
        )
        method.isAccessible = true
        val parts = hash.split('$')
        val decoder = java.util.Base64.getDecoder()
        val salt = decoder.decode(parts[2])
        val expected = decoder.decode(parts[3])
        val actual = method.invoke(
            PasswordHasher,
            "rafiqul".toCharArray(),
            salt,
            parts[1].toInt(),
            expected.size
        ) as ByteArray
        assertTrue(expected.contentEquals(actual))
    }
}
