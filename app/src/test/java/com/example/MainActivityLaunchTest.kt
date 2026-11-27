package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
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
            composeRule.onNodeWithTag("login_depot_title").assertIsDisplayed()
            composeRule.onNodeWithText("RANGPUR DEPOT").assertIsDisplayed()
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
            composeRule.onNodeWithTag("firestore_net_balance_card").assertIsDisplayed()
            composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
            composeRule.onNodeWithText("Rafiqul Islam").assertIsDisplayed()

            composeRule.onNodeWithTag("nav_transactions").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("nav_reports").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("report_selection_screen").assertIsDisplayed()
            composeRule.onNodeWithTag("compact_report_calendar_bar").assertIsDisplayed()
            composeRule.onNodeWithTag("overview_calendar_button").assertIsDisplayed()
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

    @Test
    fun `homeScreen renders top firestore net balance summary card above hero card`() {
        MemberStore(context).saveAll(
            listOf(
                Member(
                    id = "m1",
                    name = "TestMember",
                    designation = "SR",
                    phone = "01700000000",
                    depot = "Dhaka",
                    code = "M-101",
                    salesAmount = 500.0,
                    isActive = true
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
                    amount = 3500.0,
                    date = "2026-11-22",
                    approvalStatus = TakaJomaStatus.APPROVED.wireValue
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
            composeRule.onNodeWithTag("firestore_net_balance_card").assertIsDisplayed()
            composeRule.onNodeWithTag("firestore_net_balance_amount").assertIsDisplayed()
            composeRule.onNodeWithTag("firestore_total_deposits_value").assertIsDisplayed()
            composeRule.onNodeWithTag("firestore_total_sales_value").assertIsDisplayed()
            composeRule.onNodeWithTag("firestore_net_balance_status").assertIsDisplayed()
            composeRule.onNodeWithTag("hero_balance_card").assertIsDisplayed()
            composeRule.onNodeWithText("+৳ 1,000").assertIsDisplayed()
            composeRule.onNodeWithText("৳ 3,500").assertIsDisplayed()
            composeRule.onNodeWithText("৳ 2,500").assertIsDisplayed()
        }
    }

    @Test
    fun `member deposit tab displays 3d form with photo upload options and profile shows auto update card`() {
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
            composeRule.onNodeWithTag("member_nav_taka_joma").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("taka_joma_3d_form_card").assertIsDisplayed()
            composeRule.onNodeWithTag("taka_joma_photo_upload_section")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("taka_joma_pick_photo_button")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("taka_joma_camera_button")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("taka_joma_submit_button")
                .performScrollTo()
                .assertIsDisplayed()

            composeRule.onNodeWithTag("member_nav_profile").performClick()
            composeRule.waitForIdle()
            composeRule.onNode(hasScrollAction())
                .performScrollToNode(hasTestTag("app_auto_update_card"))
            composeRule.onNodeWithTag("app_auto_update_card")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("app_auto_update_switch")
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `submitting member deposit displays successful message banners`() {
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
            composeRule.onNodeWithTag("member_nav_taka_joma").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("taka_joma_amount_input")
                .performScrollTo()
                .performTextInput("2500")
            composeRule.onNodeWithTag("taka_joma_payment_dropdown")
                .performScrollTo()
                .performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("taka_joma_dropdown_menu")
                .performScrollTo()
            composeRule.onNodeWithTag("bank_row_CASH")
                .performScrollTo()
                .performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("taka_joma_submit_button")
                .performScrollTo()
                .performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("app_success_banner").assertIsDisplayed()
            composeRule.onNodeWithTag("taka_joma_success_banner")
                .performScrollTo()
                .assertIsDisplayed()
        }
    }

    @Test
    fun `settings AI Call Center opens Monday dues queue without third party text`() {
        MemberStore(context).saveAll(
            listOf(
                Member(
                    id = "m_due_ui",
                    name = "DueMember",
                    designation = "SR",
                    phone = "01799001122",
                    depot = "Rangpur",
                    code = "D-201",
                    salesAmount = 4200.0,
                    isActive = true
                )
            )
        )
        AppLockCredentialStore(context).login("rafiqul", "rafiqul")

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            composeRule.onNodeWithTag("nav_settings").performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("settings_ai_call_center_item")
                .performScrollTo()
                .assertIsDisplayed()
                .performClick()
            composeRule.waitForIdle()

            composeRule.onNodeWithTag("ai_call_center_screen").assertIsDisplayed()
            composeRule.onNodeWithTag("ai_call_monday_switch").assertIsDisplayed()
            composeRule.onNodeWithText("Nadia (Female)").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithText("Sadia (Female)").performScrollTo().assertIsDisplayed()
            composeRule.onNodeWithTag("ai_call_sim_dial_switch")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("ai_call_start_queue_button")
                .performScrollTo()
                .assertIsDisplayed()
            composeRule.onNodeWithTag("ai_call_center_screen")
                .performScrollToNode(hasTestTag("ai_call_center_queue_card"))
            composeRule.onNodeWithTag("ai_call_center_queue_card").assertIsDisplayed()
            composeRule.onNodeWithTag("ai_call_center_screen")
                .performScrollToNode(hasTestTag("ai_call_queue_item_m_due_ui"))
            composeRule.onNodeWithTag("ai_call_queue_item_m_due_ui").assertIsDisplayed()
            composeRule.onNodeWithText("Free AI Call").assertIsDisplayed()
            composeRule.onNodeWithText("Gemini", substring = true, ignoreCase = true).assertDoesNotExist()
            composeRule.onNodeWithText("Firebase", substring = true, ignoreCase = true).assertDoesNotExist()
            composeRule.onNodeWithText("Twilio", substring = true, ignoreCase = true).assertDoesNotExist()
            composeRule.onNodeWithText("OpenAI", substring = true, ignoreCase = true).assertDoesNotExist()
        }
    }
}

