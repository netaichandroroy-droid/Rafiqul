package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.auth.PasswordHasher
import com.example.data.AppAutoUpdateController
import com.example.data.AppUpdatePrefsStore
import com.example.data.MemberAppLinkStore
import com.example.data.MemberLoginResult
import com.example.data.MemberStore
import com.example.data.MemberTakaJomaStore
import com.example.data.OnlinePaymentStore
import com.example.model.BangladeshBankDirectory
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.PaymentChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemberAndPaymentStoreTest {

    @Test
    fun memberStore_savesLoadsAndPersistsActiveToggle() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MemberStore(context)
        assertTrue(store.loadAll().isEmpty())

        val initial = listOf(
            Member(
                id = "m1",
                name = "Karim Ahmed",
                designation = "Manager",
                phone = "01711000001",
                depot = "Dhaka",
                code = "K-101",
                area = "Mirpur",
                isActive = true
            ),
            Member(
                id = "m2",
                name = "Rahim Uddin",
                designation = "Officer",
                phone = "01811000002",
                depot = "Chattogram",
                code = "R-102",
                area = "Agrabad",
                isActive = false
            )
        )
        store.saveAll(initial)

        val loaded = MemberStore(context).loadAll()
        assertEquals(2, loaded.size)
        assertEquals("K-101", loaded[0].code)
        assertEquals("Karim Ahmed", loaded[0].name)
        assertEquals("Manager", loaded[0].designation)
        assertEquals("Mirpur", loaded[0].area)
        assertEquals("01711000001", loaded[0].phone)
        assertEquals("Dhaka", loaded[0].depot)
        assertEquals("R-102", loaded[1].code)
        assertEquals("Agrabad", loaded[1].area)
        assertTrue(loaded[0].isActive)
        assertFalse(loaded[1].isActive)
    }

    @Test
    fun memberLogin_usesMemberCodeAsIdAndPassword_respectsActiveStatus_andSupportsPasswordChange() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MemberStore(context)
        store.saveAll(
            listOf(
                Member(
                    id = "m_netai",
                    name = "netai",
                    designation = "it",
                    phone = "01314277737",
                    depot = "rangpur",
                    code = "N-101",
                    isActive = true,
                    passwordHash = PasswordHasher.hashPassword("N-101", minLength = 1),
                    isDefaultPassword = true
                ),
                Member(
                    id = "m_blocked",
                    name = "blockedUser",
                    designation = "sales",
                    phone = "01700000000",
                    depot = "dhaka",
                    code = "B-102",
                    isActive = false,
                    passwordHash = PasswordHasher.hashPassword("B-102", minLength = 1),
                    isDefaultPassword = true
                )
            )
        )

        // Active member logs in with Code as both ID (case-insensitive) and Password
        val okLogin = store.verifyMemberLogin("n-101", "N-101")
        assertTrue(okLogin is MemberLoginResult.Success)

        // Logging in with member name instead of code is rejected
        assertTrue(store.verifyMemberLogin("netai", "netai") is MemberLoginResult.NotFound)

        // Wrong password fails
        assertTrue(store.verifyMemberLogin("N-101", "wrong") is MemberLoginResult.WrongPassword)

        // Inactive member is blocked
        assertTrue(store.verifyMemberLogin("B-102", "B-102") is MemberLoginResult.Inactive)

        // Change password
        val changed = store.changeMemberPassword("m_netai", "N-101", "secret99")
        assertTrue(changed.isSuccess)
        assertFalse(changed.getOrThrow().isDefaultPassword)
        assertTrue(store.verifyMemberLogin("N-101", "N-101") is MemberLoginResult.WrongPassword)
        assertTrue(store.verifyMemberLogin("N-101", "secret99") is MemberLoginResult.Success)

        // Reset back to default (member code)
        val reset = store.resetMemberPasswordToDefault("m_netai")
        assertTrue(reset?.isDefaultPassword == true)
        assertTrue(store.verifyMemberLogin("N-101", "N-101") is MemberLoginResult.Success)
    }

    @Test
    fun onlinePaymentStore_savesAndPersistsChannelSettings() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = OnlinePaymentStore(context)
        val initial = store.load()
        assertTrue(initial.isOnlinePaymentEnabled)
        assertEquals(4, initial.accounts.size)

        val updatedAccounts = initial.accounts.map { acc ->
            if (acc.channel == PaymentChannel.BKASH) {
                acc.copy(accountNumber = "01700000000", accountType = "Merchant", isEnabled = true)
            } else {
                acc
            }
        }
        store.save(initial.copy(isOnlinePaymentEnabled = true, accounts = updatedAccounts))

        val reloaded = OnlinePaymentStore(context).load()
        val bkash = reloaded.accounts.first { it.channel == PaymentChannel.BKASH }
        assertEquals("01700000000", bkash.accountNumber)
        assertEquals("Merchant", bkash.accountType)
        assertTrue(bkash.isEnabled)
    }

    @Test
    fun onlinePaymentStore_isolatesAdminAndMemberData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val adminStore = OnlinePaymentStore(context)
        val memberStore = OnlinePaymentStore(context, ownerScope = "member_m_netai")

        val adminConfig = adminStore.load()
        val updatedAdmin = adminConfig.copy(
            accounts = adminConfig.accounts.map { acc ->
                if (acc.channel == PaymentChannel.BKASH) {
                    acc.copy(accountNumber = "01999999999", isEnabled = true)
                } else {
                    acc
                }
            }
        )
        adminStore.save(updatedAdmin)

        // Member store must not see Admin's payment numbers
        val memberConfig = memberStore.load()
        val memberBkash = memberConfig.accounts.first { it.channel == PaymentChannel.BKASH }
        assertEquals("", memberBkash.accountNumber)
        assertFalse(memberBkash.isEnabled)
    }

    @Test
    fun memberStore_updatesAndPersistsProfileFields() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MemberStore(context)
        store.saveAll(
            listOf(
                Member(
                    id = "m_1",
                    name = "Rafiq",
                    designation = "Supervisor",
                    phone = "01711111111",
                    depot = "Rangpur",
                    joiningDate = "2025-01-15"
                ),
                Member(
                    id = "m_2",
                    name = "Sajib",
                    designation = "Officer",
                    phone = "01822222222",
                    depot = "Bogura"
                )
            )
        )

        val result = store.updateMemberProfile(
            memberId = "m_1",
            phone = "01799999999",
            email = "rafiq@example.com",
            address = "Rangpur Sadar",
            profilePhotoPath = "/data/user/0/app/files/member_avatars/m_1.jpg"
        )
        assertTrue(result.isSuccess)

        val reloaded = MemberStore(context).findById("m_1")!!
        assertEquals("01799999999", reloaded.phone)
        assertEquals("rafiq@example.com", reloaded.email)
        assertEquals("Rangpur Sadar", reloaded.address)
        assertEquals("2025-01-15", reloaded.joiningDate)
        assertEquals("/data/user/0/app/files/member_avatars/m_1.jpg", reloaded.profilePhotoPath)

        // Ensure other member remains untouched
        val other = MemberStore(context).findById("m_2")!!
        assertEquals("01822222222", other.phone)
        assertEquals("", other.email)

        // Missing member returns failure
        val missing = store.updateMemberProfile("unknown", "010", "a@b.c", "addr")
        assertTrue(missing.isFailure)
    }

    @Test
    fun memberTakaJomaStore_persistsAndIsolatesByMember() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MemberTakaJomaStore(context)

        store.addSubmission(
            MemberTakaJoma(
                id = "tj_1",
                memberId = "m_1",
                name = "Rafiq",
                code = "C-101",
                depot = "Rangpur",
                bankName = "সোনালী ব্যাংক (Sonali Bank PLC)",
                amount = 1500.0,
                date = "2026-11-20",
                note = "bKash",
                photoPath = "/files/taka_joma_photos/tj_1.jpg",
                submittedByRole = "MEMBER"
            )
        )
        store.addSubmission(
            MemberTakaJoma(
                id = "tj_admin",
                memberId = "admin_manual",
                name = "Admin Entry",
                code = "ADM-01",
                depot = "Dhaka",
                bankName = "ইসলামী ব্যাংক বাংলাদেশ (Islami Bank Bangladesh PLC)",
                amount = 5000.0,
                date = "2026-11-20",
                submittedByRole = "ADMIN"
            )
        )

        val adminAll = store.loadAll()
        assertEquals(2, adminAll.size)

        val memberOnly = store.loadForMember("m_1")
        assertEquals(1, memberOnly.size)
        assertEquals("C-101", memberOnly[0].code)
        assertEquals("Rangpur", memberOnly[0].depot)
        assertEquals("সোনালী ব্যাংক (Sonali Bank PLC)", memberOnly[0].bankName)
        assertEquals(1500.0, memberOnly[0].amount, 0.001)
        assertTrue(BangladeshBankDirectory.ALL_BANKS.size >= 40)

        // Member cannot delete Admin's entry
        store.deleteSubmission("tj_admin", requesterMemberId = "m_1")
        assertEquals(2, store.loadAll().size)

        // Member can delete own entry
        store.deleteSubmission("tj_1", requesterMemberId = "m_1")
        assertEquals(0, store.loadForMember("m_1").size)
        assertEquals(1, store.loadAll().size)
    }

    @Test
    fun allActiveMembersInMemberList_canLoginCaseInsensitivelyWithCode() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MemberStore(context)
        store.saveAll(
            listOf(
                Member(
                    id = "m_1",
                    name = "Karim Ahmed",
                    designation = "Sales",
                    phone = "01711111111",
                    depot = "Dhaka",
                    code = "K-101",
                    isActive = true,
                    passwordHash = PasswordHasher.hashPassword("K-101", minLength = 1),
                    isDefaultPassword = true
                ),
                Member(
                    id = "m_2",
                    name = "Netai Das",
                    designation = "IT",
                    phone = "01822222222",
                    depot = "Rangpur",
                    code = "N-102",
                    isActive = true,
                    passwordHash = "",
                    isDefaultPassword = true
                )
            )
        )

        // Member 1 logs in with lowercase & extra whitespace around code
        val res1 = store.verifyMemberLogin("  k-101 ", "k-101")
        assertTrue(res1 is MemberLoginResult.Success)

        // Member 2 logs in with code even without precomputed hash
        val res2 = store.verifyMemberLogin("n-102", "N-102")
        assertTrue(res2 is MemberLoginResult.Success)
    }

    @Test
    fun memberAppLinkStore_validatesAndPersistsDownloadUrl() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val linkStore = MemberAppLinkStore(context)
        assertEquals("", linkStore.getDownloadUrl())

        // Invalid schemes rejected
        assertTrue(linkStore.saveDownloadUrl("javascript:alert(1)").isFailure)
        assertTrue(linkStore.saveDownloadUrl("file:///sdcard/app.apk").isFailure)

        // Valid URL saved & normalized
        val saved = linkStore.saveDownloadUrl("example.com/member-app.apk")
        assertTrue(saved.isSuccess)
        assertEquals("https://example.com/member-app.apk", MemberAppLinkStore(context).getDownloadUrl())

        // Clearing URL works
        assertTrue(linkStore.saveDownloadUrl("").isSuccess)
        assertEquals("", MemberAppLinkStore(context).getDownloadUrl())
    }

    @Test
    fun memberStore_updatesAndPersistsSalesAmount() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MemberStore(context)
        store.saveAll(
            listOf(
                Member(
                    id = "m_sales",
                    name = "Kamal",
                    designation = "Sales",
                    phone = "01755555555",
                    depot = "Sylhet"
                )
            )
        )

        assertTrue(store.updateMemberSales("m_sales", -100.0).isFailure)
        val updated = store.updateMemberSales("m_sales", 45000.0)
        assertTrue(updated.isSuccess)
        assertEquals(45000.0, MemberStore(context).findById("m_sales")!!.salesAmount, 0.001)
    }

    @Test
    fun bangladeshBankDirectory_includesCashNagadAndBkash_andMatchesAliases() {
        assertEquals(listOf("CASH"), BangladeshBankDirectory.CASH_OPTIONS.map { it.shortCode })
        val mfsCodes = BangladeshBankDirectory.MOBILE_BANKING_OPTIONS.map { it.shortCode }
        assertTrue(mfsCodes.contains("NAGAD"))
        assertTrue(mfsCodes.contains("BKASH"))
        assertEquals(
            BangladeshBankDirectory.CASH_OPTIONS.size +
                BangladeshBankDirectory.MOBILE_BANKING_OPTIONS.size +
                BangladeshBankDirectory.ALL_BANKS.size,
            BangladeshBankDirectory.ALL_PAYMENT_METHODS.size
        )
        assertEquals("CASH", BangladeshBankDirectory.ALL_PAYMENT_METHODS[0].shortCode)
        assertEquals("NAGAD", BangladeshBankDirectory.ALL_PAYMENT_METHODS[1].shortCode)
        assertEquals("BKASH", BangladeshBankDirectory.ALL_PAYMENT_METHODS[2].shortCode)

        val cashMatches = BangladeshBankDirectory.filterPaymentMethods("cash")
        assertTrue(cashMatches.any { it.shortCode == "CASH" })

        val bikashMatches = BangladeshBankDirectory.filterPaymentMethods("bikash")
        assertTrue(bikashMatches.any { it.shortCode == "BKASH" })

        val nagadMatches = BangladeshBankDirectory.filterPaymentMethods("nogod")
        assertTrue(nagadMatches.any { it.shortCode == "NAGAD" })
    }

    @Test
    fun memberTakaJomaStore_rejectsDuplicateSlipHashAndPhotoPath() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = MemberTakaJomaStore(context)

        val first = MemberTakaJoma(
            id = "tj_slip_1",
            memberId = "m_1",
            name = "Rafiq",
            code = "C-101",
            depot = "Rangpur",
            bankName = "বিকাশ (bKash)",
            amount = 2000.0,
            date = "2026-11-21",
            photoPath = "/files/taka_joma_photos/tj_slip_1.jpg",
            slipHash = "abc123sha256"
        )
        assertTrue(store.tryAddSubmission(first).isSuccess)
        assertTrue(store.isDuplicateSlip("abc123sha256"))

        // Second submission with a new id/photoPath but identical slipHash must be rejected
        val duplicateSlipSecond = first.copy(
            id = "tj_slip_2",
            photoPath = "/files/taka_joma_photos/tj_slip_2.jpg"
        )
        val duplicateResult = store.tryAddSubmission(duplicateSlipSecond)
        assertTrue(duplicateResult.isFailure)
        assertEquals(1, store.loadAll().size)

        // Different slipHash succeeds
        val distinctSlip = first.copy(
            id = "tj_slip_3",
            photoPath = "/files/taka_joma_photos/tj_slip_3.jpg",
            slipHash = "def456sha256"
        )
        assertTrue(store.tryAddSubmission(distinctSlip).isSuccess)
        assertEquals(2, store.loadAll().size)
    }

    @Test
    fun memberTakaJoma_submittedByMember_appearsInAdminViewModel_whileAdminDataStaysIsolated() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val txStore = com.example.data.LocalTransactionStore(context)
        val memberStore = MemberStore(context)
        val takaJomaStore = MemberTakaJomaStore(context)

        val member = Member(
            id = "m_10",
            name = "Karim",
            designation = "Sales",
            phone = "01712345678",
            depot = "Bogura"
        )
        memberStore.saveAll(listOf(member))

        val adminVm = com.example.ui.LocalHisabViewModel(
            localStore = txStore,
            memberStore = memberStore,
            onlinePaymentStore = OnlinePaymentStore(context),
            takaJomaStore = takaJomaStore
        )
        val memberVm = com.example.ui.LocalHisabViewModel(
            localStore = txStore,
            memberStore = memberStore,
            onlinePaymentStore = OnlinePaymentStore(context, ownerScope = "member_m_10"),
            takaJomaStore = takaJomaStore,
            ownerUserId = "member_m_10"
        )

        var ok = false
        memberVm.submitMemberTakaJoma(
            submissionId = "tj_m10_1",
            memberId = member.id,
            name = member.name,
            code = "BG-09",
            depot = member.depot,
            bankName = "নগদ (Nagad)",
            amount = 3500.0,
            date = "2026-11-21",
            note = "",
            photoPath = "/files/taka_joma_photos/tj_m10_1.jpg",
            slipHash = "hash_m10_1"
        ) { ok = it }
        assertTrue(ok)

        // Admin refreshes and immediately sees the member's submission with all form headers
        adminVm.refreshSharedData()
        val adminEntries = adminVm.takaJomaList.value
        assertEquals(1, adminEntries.size)
        assertEquals("Karim", adminEntries[0].name)
        assertEquals("BG-09", adminEntries[0].code)
        assertEquals("Bogura", adminEntries[0].depot)
        assertEquals("নগদ (Nagad)", adminEntries[0].bankName)
        assertEquals(3500.0, adminEntries[0].amount, 0.001)

        // Member still cannot see Admin's member directory
        assertTrue(memberVm.members.value.isEmpty())
    }

    @Test
    fun saveMember_persistsAndUpdatesCodeAndArea() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val txStore = com.example.data.LocalTransactionStore(context)
        val memberStore = MemberStore(context)
        val takaJomaStore = MemberTakaJomaStore(context)

        val adminVm = com.example.ui.LocalHisabViewModel(
            localStore = txStore,
            memberStore = memberStore,
            onlinePaymentStore = OnlinePaymentStore(context),
            takaJomaStore = takaJomaStore
        )

        var savedOk = false
        adminVm.saveMember(
            editingId = null,
            name = "Netai",
            designation = "IT",
            phone = "011",
            depot = "Rangpur",
            isActive = true,
            code = "N-001",
            area = "Sadar"
        ) { savedOk = it }
        assertTrue(savedOk)

        val savedMember = adminVm.members.value.first()
        assertEquals("N-001", savedMember.code)
        assertEquals("Sadar", savedMember.area)
        assertEquals("Netai", savedMember.name)

        var updatedOk = false
        adminVm.saveMember(
            editingId = savedMember.id,
            name = "Netai",
            designation = "Senior IT",
            phone = "011",
            depot = "Rangpur",
            isActive = true,
            code = "N-002",
            area = "Uttara"
        ) { updatedOk = it }
        assertTrue(updatedOk)

        val reloaded = MemberStore(context).findById(savedMember.id)!!
        assertEquals("N-002", reloaded.code)
        assertEquals("Uttara", reloaded.area)
        assertEquals("Senior IT", reloaded.designation)
        assertTrue(memberStore.verifyMemberLogin("N-002", "N-002") is MemberLoginResult.Success)

        // Blank code is rejected
        var blankOk = true
        adminVm.saveMember(
            editingId = null,
            name = "Kamal",
            designation = "SR",
            phone = "012",
            depot = "Dhaka",
            isActive = true,
            code = "   ",
            area = "Mirpur"
        ) { blankOk = it }
        assertFalse(blankOk)

        // Duplicate code is rejected
        var dupOk = true
        adminVm.saveMember(
            editingId = null,
            name = "Kamal",
            designation = "SR",
            phone = "012",
            depot = "Dhaka",
            isActive = true,
            code = "n-002",
            area = "Mirpur"
        ) { dupOk = it }
        assertFalse(dupOk)
    }

    @Test
    fun submitMemberTakaJoma_autoFillsMissingMemberListFieldsAndPersistsDesigArea() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val txStore = com.example.data.LocalTransactionStore(context)
        val memberStore = MemberStore(context)
        val takaJomaStore = MemberTakaJomaStore(context)

        val member = Member(
            id = "m_auto",
            name = "Netai",
            designation = "IT",
            phone = "011",
            depot = "Rangpur",
            code = "N-101",
            area = "Sadar"
        )
        memberStore.saveAll(listOf(member))

        val memberVm = com.example.ui.LocalHisabViewModel(
            localStore = txStore,
            memberStore = memberStore,
            onlinePaymentStore = OnlinePaymentStore(context, ownerScope = "member_m_auto"),
            takaJomaStore = takaJomaStore,
            ownerUserId = "member_m_auto"
        )

        var ok = false
        memberVm.submitMemberTakaJoma(
            submissionId = "tj_auto_1",
            memberId = member.id,
            name = "",
            code = "",
            depot = "",
            bankName = "বিকাশ (bKash)",
            amount = 2500.0,
            date = "2026-11-21",
            note = "",
            photoPath = "/files/taka_joma_photos/tj_auto_1.jpg",
            slipHash = "hash_auto_1"
        ) { ok = it }
        assertTrue(ok)

        val saved = takaJomaStore.loadAll().first { it.id == "tj_auto_1" }
        assertEquals("Netai", saved.name)
        assertEquals("N-101", saved.code)
        assertEquals("Rangpur", saved.depot)
        assertEquals("IT", saved.designation)
        assertEquals("Sadar", saved.area)
    }

    @Test
    fun appAutoUpdatePrefsAndController_persistToggleAndHandleChecksSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = AppUpdatePrefsStore(context)
        assertTrue(prefs.isAutoUpdateEnabled())

        val controller = AppAutoUpdateController(context)
        assertTrue(controller.state.value.autoUpdateEnabled)

        controller.setAutoUpdateEnabled(false)
        assertFalse(controller.state.value.autoUpdateEnabled)
        assertFalse(AppUpdatePrefsStore(context).isAutoUpdateEnabled())

        controller.setAutoUpdateEnabled(true)
        assertTrue(controller.state.value.autoUpdateEnabled)
        assertTrue(AppUpdatePrefsStore(context).isAutoUpdateEnabled())

        // Manual check must not throw in headless/Robolectric environments without Play Store
        controller.checkForUpdates(activity = null, updateLauncher = null, userInitiated = true)
    }

    @Test
    fun aiCallCenterPrefsStore_persistsConfigRecordsAndComputesNextMonday() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = com.example.data.AiCallCenterPrefsStore(context)

        val defaultCfg = store.loadConfig()
        assertTrue(defaultCfg.autoCallMondayEnabled)
        assertEquals(10, defaultCfg.scheduledHour)

        store.saveConfig(
            defaultCfg.copy(
                autoCallMondayEnabled = false,
                scheduledHour = 14,
                autoVoiceGreeting = false
            )
        )
        val reloaded = com.example.data.AiCallCenterPrefsStore(context).loadConfig()
        assertFalse(reloaded.autoCallMondayEnabled)
        assertEquals(14, reloaded.scheduledHour)
        assertFalse(reloaded.autoVoiceGreeting)

        // Dial number sanitization
        assertEquals("+8801711000001", com.example.data.AiCallCenterPrefsStore.sanitizeDialNumber(" +880 1711-000001 "))
        assertEquals("", com.example.data.AiCallCenterPrefsStore.sanitizeDialNumber("123"))

        // Monday trigger always resolves to Monday
        val sunCal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 22, 9, 0, 0) // Sunday
        }
        val nextTrigger = com.example.data.AiCallCenterPrefsStore.nextMondayTriggerMs(
            hour = 10,
            minute = 0,
            nowMs = sunCal.timeInMillis
        )
        val targetCal = java.util.Calendar.getInstance().apply { timeInMillis = nextTrigger }
        assertEquals(java.util.Calendar.MONDAY, targetCal.get(java.util.Calendar.DAY_OF_WEEK))
        assertEquals(10, targetCal.get(java.util.Calendar.HOUR_OF_DAY))

        // Cycle record persistence
        val record = com.example.data.AiCallRecord(
            memberId = "m_due_1",
            memberName = "Rahim",
            phone = "01711000001",
            duesAmount = 1500.0,
            status = com.example.data.AiCallStatus.CALLED,
            updatedAtMs = sunCal.timeInMillis,
            scriptPreview = "Reminder sent"
        )
        store.upsertCallRecord(record, nowMs = sunCal.timeInMillis)
        val loadedRecords = store.loadCallRecordsForCurrentCycle(nowMs = sunCal.timeInMillis)
        assertEquals(com.example.data.AiCallStatus.CALLED, loadedRecords["m_due_1"]?.status)

        // Free AI Call mode defaults to alsoDialPhoneNumber = false
        val freeCfg = com.example.data.AiCallCenterPrefsStore(context).loadConfig()
        assertFalse(freeCfg.alsoDialPhoneNumber)
        assertEquals(com.example.ai.FemaleAiVoiceOption.AOEDE, freeCfg.selectedVoice)

        // Scheduled Monday auto-start due check
        store.saveConfig(
            freeCfg.copy(
                autoCallMondayEnabled = true,
                scheduledHour = 10,
                scheduledMinute = 0
            )
        )
        store.setLastRunMondayKey("")
        store.setPendingAutoStartMondayKey("")
        val monMorning = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 23, 9, 30, 0) // Monday 9:30 AM
        }
        assertFalse(store.isScheduledMondayAutoStartDue(monMorning.timeInMillis))
        val monAtSchedule = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 23, 10, 15, 0) // Monday 10:15 AM
        }
        assertTrue(store.isScheduledMondayAutoStartDue(monAtSchedule.timeInMillis))
        store.setLastRunMondayKey(com.example.data.AiCallCenterPrefsStore.currentCycleKey(monAtSchedule.timeInMillis))
        assertFalse(store.isScheduledMondayAutoStartDue(monAtSchedule.timeInMillis))
    }

    @Test
    fun aiCallCenterEngine_buildDueQueueFiltersPositiveDuesSortsDescendingAndFlagsNoPhone() {
        val members = listOf(
            Member(id = "m_high", name = "Karim", designation = "SR", phone = "01711111111", depot = "Rangpur", code = "K-01", salesAmount = 5000.0),
            Member(id = "m_low", name = "Rahim", designation = "SR", phone = "", depot = "Rangpur", code = "R-02", salesAmount = 1200.0),
            Member(id = "m_paid", name = "Jamal", designation = "SR", phone = "01822222222", depot = "Rangpur", code = "J-03", salesAmount = 1000.0)
        )
        val submissions = listOf(
            MemberTakaJoma(
                id = "j_paid",
                memberId = "m_paid",
                name = "Jamal",
                code = "J-03",
                depot = "Rangpur",
                amount = 1000.0,
                date = "2026-11-22",
                approvalStatus = com.example.model.TakaJomaStatus.APPROVED.wireValue
            )
        )

        val queue = com.example.ai.AiCallCenterEngine.buildDueQueue(
            members = members,
            monthlySales = emptyList(),
            submissions = submissions,
            savedRecords = emptyMap()
        )

        assertEquals(2, queue.size)
        assertEquals("m_high", queue[0].memberId)
        assertEquals(com.example.data.AiCallStatus.PENDING, queue[0].callStatus)
        assertEquals("m_low", queue[1].memberId)
        assertEquals(com.example.data.AiCallStatus.NO_PHONE, queue[1].callStatus)
    }
}

