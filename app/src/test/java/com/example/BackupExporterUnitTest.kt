package com.example

import com.example.data.BackupExporter
import com.example.model.AccountType
import com.example.model.HisabTransaction
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.OnlinePaymentAccount
import com.example.model.OnlinePaymentConfig
import com.example.model.PaymentChannel
import com.example.model.TakaJomaStatus
import com.example.model.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupExporterUnitTest {

    @Test
    fun `full app backup serializes and restores all app datasets cleanly`() {
        val transactions = listOf(
            HisabTransaction(
                id = "tx_1",
                userId = "local_rafiqul",
                type = TxType.INCOME.wireValue,
                amount = 5500.0,
                category = "মেম্বার টাকা জমা",
                note = "রহিম (M-101)",
                date = "2026-11-22",
                account = AccountType.BANK.wireValue
            )
        )
        val members = listOf(
            Member(
                id = "m_1",
                name = "রহিম",
                designation = "SPO",
                phone = "01700000000",
                depot = "Dhaka",
                code = "M-101",
                area = "Mirpur",
                isActive = true
            )
        )
        val takaJoma = listOf(
            MemberTakaJoma(
                id = "tj_1",
                memberId = "m_1",
                name = "রহিম",
                code = "M-101",
                depot = "Dhaka",
                area = "Mirpur",
                bankName = "বিকাশ (bKash)",
                amount = 5500.0,
                date = "2026-11-22",
                approvalStatus = TakaJomaStatus.APPROVED.wireValue
            )
        )
        val sales = listOf(
            MonthlySalesRecord(
                id = "s_1",
                yearMonth = "2026-11",
                code = "M-101",
                name = "রহিম",
                tpSales = 40000.0,
                tpPlusVat = 46000.0
            )
        )
        val paymentConfig = OnlinePaymentConfig(
            isOnlinePaymentEnabled = true,
            accounts = listOf(
                OnlinePaymentAccount(
                    id = "bkash",
                    channel = PaymentChannel.BKASH,
                    accountNumber = "01711112222",
                    accountType = "Personal",
                    isEnabled = true
                )
            )
        )

        val json = BackupExporter.buildFullAppBackupJson(
            transactions = transactions,
            members = members,
            memberTakaJoma = takaJoma,
            monthlySales = sales,
            onlinePaymentConfig = paymentConfig,
            memberAppDownloadUrl = "https://example.com/member"
        )

        val parsed = BackupExporter.parseFullAppBackupJson(json).getOrThrow()
        assertTrue(parsed.isFullSnapshot)
        assertEquals(2, parsed.formatVersion)
        assertEquals(1, parsed.transactions.size)
        assertEquals(5500.0, parsed.transactions.first().amount, 0.001)
        assertEquals(1, parsed.members.size)
        assertEquals("Mirpur", parsed.members.first().area)
        assertEquals(1, parsed.memberTakaJoma.size)
        assertEquals(TakaJomaStatus.APPROVED, parsed.memberTakaJoma.first().resolvedStatus)
        assertEquals(1, parsed.monthlySales.size)
        assertEquals(46000.0, parsed.monthlySales.first().tpPlusVat, 0.001)
        assertNotNull(parsed.onlinePaymentConfig)
        assertEquals("01711112222", parsed.onlinePaymentConfig?.accounts?.first()?.accountNumber)
        assertEquals("https://example.com/member", parsed.memberAppDownloadUrl)
    }

    @Test
    fun `legacy transaction array JSON parses with isFullSnapshot false`() {
        val legacyJson = """
            [
              {
                "id": "legacy_1",
                "type": "expense",
                "amount": 320.0,
                "category": "খাবার",
                "note": "দুপুরের খাবার",
                "date": "2026-11-20",
                "account": "cash"
              }
            ]
        """.trimIndent()

        val parsed = BackupExporter.parseFullAppBackupJson(legacyJson).getOrThrow()
        assertFalse(parsed.isFullSnapshot)
        assertEquals(1, parsed.formatVersion)
        assertEquals(1, parsed.transactions.size)
        assertEquals(320.0, parsed.transactions.first().amount, 0.001)
        assertEquals("খাবার", parsed.transactions.first().category)
    }

    @Test
    fun `invalid JSON returns failure`() {
        val result = BackupExporter.parseFullAppBackupJson("not valid json")
        assertTrue(result.isFailure)
    }
}
