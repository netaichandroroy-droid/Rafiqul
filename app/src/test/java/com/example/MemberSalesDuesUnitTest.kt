package com.example

import com.example.model.Member
import com.example.model.MemberSalesDuesCalculator
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.TakaJomaStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class MemberSalesDuesUnitTest {

    @Test
    fun takaJomaStatus_fromRaw_handlesPendingApprovedRejectedAndLegacy() {
        assertEquals(TakaJomaStatus.PENDING, TakaJomaStatus.fromRaw("PENDING"))
        assertEquals(TakaJomaStatus.PENDING, TakaJomaStatus.fromRaw("pending"))
        assertEquals(TakaJomaStatus.PENDING, TakaJomaStatus.fromRaw("অপেক্ষমাণ"))
        assertEquals(TakaJomaStatus.REJECTED, TakaJomaStatus.fromRaw("REJECTED"))
        assertEquals(TakaJomaStatus.REJECTED, TakaJomaStatus.fromRaw("বাতিল"))
        assertEquals(TakaJomaStatus.APPROVED, TakaJomaStatus.fromRaw("APPROVED"))
        assertEquals(TakaJomaStatus.APPROVED, TakaJomaStatus.fromRaw(""))
        assertEquals(TakaJomaStatus.APPROVED, TakaJomaStatus.fromRaw(null))
    }

    @Test
    fun calculateForMember_reducesDuesOnlyWhenTakaJomaIsApproved() {
        val member = Member(
            id = "m_1",
            name = "Karim",
            designation = "SR",
            phone = "01711111111",
            depot = "Dhaka",
            code = "C-101",
            salesAmount = 5000.0
        )
        val sales = listOf(
            MonthlySalesRecord(
                id = "s_1",
                yearMonth = "2026-11",
                code = "C-101",
                name = "Karim",
                tpSales = 10000.0,
                tpPlusVat = 15000.0
            )
        )
        val pendingSubmission = MemberTakaJoma(
            id = "tj_1",
            memberId = "m_1",
            name = "Karim",
            code = "C-101",
            depot = "Dhaka",
            amount = 8000.0,
            date = "2026-11-22",
            approvalStatus = TakaJomaStatus.PENDING.wireValue
        )
        val rejectedSubmission = MemberTakaJoma(
            id = "tj_2",
            memberId = "m_1",
            name = "Karim",
            code = "C-101",
            depot = "Dhaka",
            amount = 2000.0,
            date = "2026-11-22",
            approvalStatus = TakaJomaStatus.REJECTED.wireValue
        )

        // Before approval: totalSales = 15000 + 5000 = 20000, dues = 20000, pending = 8000
        val beforeApproval = MemberSalesDuesCalculator.calculateForMember(
            member = member,
            monthlySales = sales,
            submissions = listOf(pendingSubmission, rejectedSubmission)
        )
        assertEquals(20000.0, beforeApproval.totalSales, 0.001)
        assertEquals(0.0, beforeApproval.approvedJoma, 0.001)
        assertEquals(8000.0, beforeApproval.pendingJoma, 0.001)
        assertEquals(20000.0, beforeApproval.dues, 0.001)
        assertEquals(0.0, beforeApproval.advance, 0.001)

        // After Admin approves tj_1: approvedJoma = 8000, dues decreases to 12000
        val approvedSubmission = pendingSubmission.copy(
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        )
        val afterApproval = MemberSalesDuesCalculator.calculateForMember(
            member = member,
            monthlySales = sales,
            submissions = listOf(approvedSubmission, rejectedSubmission)
        )
        assertEquals(8000.0, afterApproval.approvedJoma, 0.001)
        assertEquals(0.0, afterApproval.pendingJoma, 0.001)
        assertEquals(12000.0, afterApproval.dues, 0.001)
        assertEquals(0.0, afterApproval.advance, 0.001)
    }

    @Test
    fun calculateForMember_computesAdvanceWhenApprovedExceedsSales() {
        val member = Member(
            id = "m_2",
            name = "Rahim",
            designation = "SR",
            phone = "01722222222",
            depot = "Bogura",
            code = "B-02",
            salesAmount = 0.0
        )
        val sales = listOf(
            MonthlySalesRecord(
                id = "s_2",
                yearMonth = "2026-11",
                code = "b-02",
                name = "Rahim",
                tpSales = 4000.0,
                tpPlusVat = 4500.0
            )
        )
        val submission = MemberTakaJoma(
            id = "tj_3",
            memberId = "admin_manual",
            name = "Rahim",
            code = "B-02",
            depot = "Bogura",
            amount = 6000.0,
            date = "2026-11-22",
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        )

        val summary = MemberSalesDuesCalculator.calculateForMember(
            member = member,
            monthlySales = sales,
            submissions = listOf(submission)
        )
        assertEquals(4500.0, summary.totalSales, 0.001)
        assertEquals(6000.0, summary.approvedJoma, 0.001)
        assertEquals(0.0, summary.dues, 0.001)
        assertEquals(1500.0, summary.advance, 0.001)
    }

    @Test
    fun buildAllMemberSummaries_includesUnlinkedUploadedSalesAndComputesTotals() {
        val members = listOf(
            Member(
                id = "m_1",
                name = "Karim",
                designation = "SR",
                phone = "017",
                depot = "Dhaka",
                code = "K-01"
            )
        )
        val sales = listOf(
            MonthlySalesRecord(
                id = "s_k",
                yearMonth = "2026-11",
                code = "K-01",
                name = "Karim",
                tpSales = 9000.0,
                tpPlusVat = 10000.0
            ),
            MonthlySalesRecord(
                id = "s_u",
                yearMonth = "2026-11",
                code = "U-99",
                name = "Unlisted Member",
                tpSales = 4000.0,
                tpPlusVat = 5000.0
            )
        )
        val submissions = listOf(
            MemberTakaJoma(
                id = "tj_k",
                memberId = "m_1",
                name = "Karim",
                code = "K-01",
                depot = "Dhaka",
                amount = 4000.0,
                date = "2026-11-22",
                approvalStatus = TakaJomaStatus.APPROVED.wireValue
            )
        )

        val summaries = MemberSalesDuesCalculator.buildAllMemberSummaries(
            members = members,
            monthlySales = sales,
            submissions = submissions
        )
        assertEquals(2, summaries.size)

        val totals = MemberSalesDuesCalculator.calculateTotals(summaries)
        assertEquals(13000.0, totals.totalTpSales, 0.001)
        assertEquals(15000.0, totals.totalSales, 0.001)
        assertEquals(4000.0, totals.totalApprovedJoma, 0.001)
        assertEquals(11000.0, totals.totalDues, 0.001)
    }

    @Test
    fun calculateForMember_usesTpPlusVatAndNeverTpSalesForDues() {
        val member = Member(
            id = "m_vat",
            name = "Sumon",
            designation = "SR",
            phone = "01733333333",
            depot = "Sylhet",
            code = "S-10",
            salesAmount = 0.0
        )
        val sales = listOf(
            MonthlySalesRecord(
                id = "s_vat",
                yearMonth = "2026-11",
                code = "S-10",
                name = "Sumon",
                tpSales = 10000.0,
                tpPlusVat = 11500.0
            )
        )
        val approved = MemberTakaJoma(
            id = "tj_vat",
            memberId = "m_vat",
            name = "Sumon",
            code = "S-10",
            depot = "Sylhet",
            amount = 5000.0,
            date = "2026-11-22",
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        )

        val summary = MemberSalesDuesCalculator.calculateForMember(
            member = member,
            monthlySales = sales,
            submissions = listOf(approved)
        )
        assertEquals(10000.0, summary.uploadedTpSales, 0.001)
        assertEquals(11500.0, summary.uploadedSales, 0.001)
        assertEquals(11500.0, summary.totalSales, 0.001)
        // Must reduce from TP+Vat (11500 - 5000 = 6500), NOT TP sales (10000 - 5000 = 5000)
        assertEquals(6500.0, summary.dues, 0.001)
    }

    @Test
    fun sortMembersByArea_ordersAlphabeticallyByAreaWithBlankAreasLast() {
        val members = listOf(
            Member(id = "1", name = "Zahir", designation = "SR", phone = "01", depot = "D1", area = "Mirpur"),
            Member(id = "2", name = "NoArea", designation = "SR", phone = "02", depot = "D2", area = ""),
            Member(id = "3", name = "Babul", designation = "SR", phone = "03", depot = "D3", area = "agrabad"),
            Member(id = "4", name = "Anis", designation = "SR", phone = "04", depot = "D4", area = "Agrabad"),
            Member(id = "5", name = "Karim", designation = "SR", phone = "05", depot = "D5", area = "Dhanmondi")
        )

        val sorted = MemberSalesDuesCalculator.sortMembersByArea(members)
        assertEquals(listOf("4", "3", "5", "1", "2"), sorted.map { it.id })
    }

    @Test
    fun buildAllMemberSummaries_doesNotAddPendingSubmissionUntilAdminApproves() {
        val member = Member(
            id = "m_pending",
            name = "Habib",
            designation = "SR",
            phone = "01744444444",
            depot = "Rajshahi",
            code = "H-01",
            area = "Boalia"
        )
        val sales = listOf(
            MonthlySalesRecord(
                id = "s_h1",
                yearMonth = "2026-11",
                code = "H-01",
                name = "Habib",
                tpSales = 8000.0,
                tpPlusVat = 9200.0
            )
        )
        val pending1 = MemberTakaJoma(
            id = "tj_p1",
            memberId = "m_pending",
            name = "Habib",
            code = "H-01",
            depot = "Rajshahi",
            amount = 3000.0,
            date = "2026-11-22",
            submittedByRole = "MEMBER",
            approvalStatus = TakaJomaStatus.PENDING.wireValue
        )
        val pending2 = MemberTakaJoma(
            id = "tj_p2",
            memberId = "m_pending",
            name = "Habib",
            code = "H-01",
            depot = "Rajshahi",
            amount = 2200.0,
            date = "2026-11-22",
            submittedByRole = "MEMBER",
            approvalStatus = TakaJomaStatus.PENDING.wireValue
        )

        // Neither pending entry is added before Admin approves
        val beforeSummaries = MemberSalesDuesCalculator.buildAllMemberSummaries(
            members = listOf(member),
            monthlySales = sales,
            submissions = listOf(pending1, pending2)
        )
        val beforeTotals = MemberSalesDuesCalculator.calculateTotals(beforeSummaries)
        assertEquals(0.0, beforeTotals.totalApprovedJoma, 0.001)
        assertEquals(5200.0, beforeTotals.totalPendingJoma, 0.001)
        assertEquals(9200.0, beforeTotals.totalDues, 0.001)

        // Pending list for Admin Home contains both before approval
        val pendingBefore = listOf(pending1, pending2).filter { it.resolvedStatus == TakaJomaStatus.PENDING }
        assertEquals(listOf("tj_p1", "tj_p2"), pendingBefore.map { it.id })

        // Approve pending1 only -> pending1 is added and removed from Admin Home pending list
        val updatedSubmissions = listOf(
            pending1.copy(approvalStatus = TakaJomaStatus.APPROVED.wireValue),
            pending2
        )
        val afterSummaries = MemberSalesDuesCalculator.buildAllMemberSummaries(
            members = listOf(member),
            monthlySales = sales,
            submissions = updatedSubmissions
        )
        val afterTotals = MemberSalesDuesCalculator.calculateTotals(afterSummaries)
        assertEquals(3000.0, afterTotals.totalApprovedJoma, 0.001)
        assertEquals(2200.0, afterTotals.totalPendingJoma, 0.001)
        assertEquals(6200.0, afterTotals.totalDues, 0.001)

        val pendingAfter = updatedSubmissions.filter { it.resolvedStatus == TakaJomaStatus.PENDING }
        assertEquals(listOf("tj_p2"), pendingAfter.map { it.id })
    }

    @Test
    fun buildApprovedTakaJomaTransaction_createsIncomeForTodayWithCorrectAccount() {
        val cashEntry = MemberTakaJoma(
            id = "tj_cash_1",
            memberId = "m_1",
            name = "Karim",
            code = "C-101",
            depot = "Dhaka",
            bankName = "ক্যাশ (Cash)",
            amount = 4500.0,
            date = "2026-11-22",
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        )
        val bkashEntry = MemberTakaJoma(
            id = "tj_bkash_1",
            memberId = "m_2",
            name = "Rahim",
            code = "R-202",
            depot = "Bogura",
            bankName = "বিকাশ (bKash)",
            amount = 7500.0,
            date = "2026-11-21",
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        )

        val today = "2026-11-22"
        val cashTx = com.example.ui.LocalHisabViewModel.buildApprovedTakaJomaTransaction(
            entry = cashEntry,
            txDate = today
        )
        assertEquals("taka_joma_tj_cash_1", cashTx.id)
        assertEquals(com.example.model.TxType.INCOME, cashTx.resolvedType)
        assertEquals(com.example.model.AccountType.CASH, cashTx.resolvedAccount)
        assertEquals(4500.0, cashTx.amount, 0.001)
        assertEquals(today, cashTx.date)
        assertEquals("Member Deposit", cashTx.category)

        val mobileBankTx = com.example.ui.LocalHisabViewModel.buildApprovedTakaJomaTransaction(
            entry = bkashEntry,
            txDate = today
        )
        assertEquals("taka_joma_tj_bkash_1", mobileBankTx.id)
        assertEquals(com.example.model.TxType.INCOME, mobileBankTx.resolvedType)
        assertEquals(com.example.model.AccountType.MOBILE_BANK, mobileBankTx.resolvedAccount)
        assertEquals(7500.0, mobileBankTx.amount, 0.001)
        assertEquals(today, mobileBankTx.date)

        val sonaliEntry = MemberTakaJoma(
            id = "tj_bank_1",
            memberId = "m1",
            name = "Karim",
            code = "M-101",
            depot = "Dhaka",
            bankName = "Sonali Bank PLC",
            amount = 12000.0,
            date = "2026-11-21",
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        )
        val bankTx = com.example.ui.LocalHisabViewModel.buildApprovedTakaJomaTransaction(
            entry = sonaliEntry,
            txDate = today
        )
        assertEquals(com.example.model.AccountType.BANK, bankTx.resolvedAccount)

        val islamiEntry = MemberTakaJoma(
            id = "tj_bank_2",
            memberId = "m1",
            name = "Karim",
            code = "M-101",
            depot = "Dhaka",
            bankName = "Islami Bank Bangladesh PLC",
            amount = 8000.0,
            date = "2026-11-21",
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        )
        val islamiTx = com.example.ui.LocalHisabViewModel.buildApprovedTakaJomaTransaction(
            entry = islamiEntry,
            txDate = today
        )

        val breakdown = com.example.model.AccountsBreakdownCalculator.build(
            transactions = listOf(cashTx, mobileBankTx, bankTx, islamiTx),
            submissions = listOf(cashEntry, bkashEntry, sonaliEntry, islamiEntry)
        )
        assertEquals(4500.0, breakdown.cashSummary.balance, 0.001)
        assertEquals(7500.0, breakdown.mobileBankSummary.balance, 0.001)
        assertEquals(20000.0, breakdown.bankSummary.balance, 0.001)
        assertEquals(20000.0, breakdown.totalAllBanksOnly, 0.001)
        assertEquals(27500.0, breakdown.totalMobileAndBanks, 0.001)
        assertEquals(32000.0, breakdown.grandTotalBalance, 0.001)
        assertEquals(2, breakdown.bankWiseChannels.size)
        assertEquals(1, breakdown.mobileBankChannels.size)
    }

    @Test
    fun calculateDepositsSalesNetSummary_computesDepositsMinusSalesUsingApprovedOnly() {
        val members = listOf(
            Member(
                id = "m1",
                name = "Karim",
                designation = "SR",
                phone = "01711111111",
                depot = "Dhaka",
                code = "K-01",
                salesAmount = 2500.0
            ),
            Member(
                id = "m2",
                name = "Rahim",
                designation = "SR",
                phone = "01722222222",
                depot = "Bogura",
                code = "R-02",
                salesAmount = 0.0
            )
        )
        val sales = listOf(
            MonthlySalesRecord(
                id = "s1",
                yearMonth = "2026-11",
                code = "K-01",
                name = "Karim",
                tpSales = 9000.0,
                tpPlusVat = 10000.0
            ),
            MonthlySalesRecord(
                id = "s2",
                yearMonth = "2026-11",
                code = "R-02",
                name = "Rahim",
                tpSales = 4500.0,
                tpPlusVat = 5000.0
            )
        )
        val submissions = listOf(
            MemberTakaJoma(
                id = "d1",
                memberId = "m1",
                name = "Karim",
                code = "K-01",
                depot = "Dhaka",
                amount = 12000.0,
                date = "2026-11-22",
                approvalStatus = TakaJomaStatus.APPROVED.wireValue
            ),
            MemberTakaJoma(
                id = "d2",
                memberId = "m2",
                name = "Rahim",
                code = "R-02",
                depot = "Bogura",
                amount = 8000.0,
                date = "2026-11-22",
                approvalStatus = TakaJomaStatus.APPROVED.wireValue
            ),
            MemberTakaJoma(
                id = "d3",
                memberId = "m1",
                name = "Karim",
                code = "K-01",
                depot = "Dhaka",
                amount = 4000.0,
                date = "2026-11-22",
                approvalStatus = TakaJomaStatus.PENDING.wireValue
            ),
            MemberTakaJoma(
                id = "d4",
                memberId = "m2",
                name = "Rahim",
                code = "R-02",
                depot = "Bogura",
                amount = 1500.0,
                date = "2026-11-22",
                approvalStatus = TakaJomaStatus.REJECTED.wireValue
            )
        )

        // Approved deposits = 12000 + 8000 = 20000; total sales = 10000 + 5000 + 2500 = 17500; net = +2500
        val positiveSummary = MemberSalesDuesCalculator.calculateDepositsSalesNetSummary(
            members = members,
            monthlySales = sales,
            submissions = submissions
        )
        assertEquals(20000.0, positiveSummary.totalDeposits, 0.001)
        assertEquals(4000.0, positiveSummary.pendingDeposits, 0.001)
        assertEquals(17500.0, positiveSummary.totalSales, 0.001)
        assertEquals(2500.0, positiveSummary.netBalance, 0.001)
        assertEquals(2, positiveSummary.approvedDepositCount)
        assertEquals(2, positiveSummary.salesRecordCount)

        // When only d2 (8000) is approved, net balance = 8000 - 17500 = -9500
        val negativeSummary = MemberSalesDuesCalculator.calculateDepositsSalesNetSummary(
            members = members,
            monthlySales = sales,
            submissions = listOf(submissions[1])
        )
        assertEquals(8000.0, negativeSummary.totalDeposits, 0.001)
        assertEquals(17500.0, negativeSummary.totalSales, 0.001)
        assertEquals(-9500.0, negativeSummary.netBalance, 0.001)
    }
}
