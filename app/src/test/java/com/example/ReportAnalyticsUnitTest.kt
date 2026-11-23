package com.example

import com.example.model.AreaManagerRoleFilter
import com.example.model.AsmNameReportScope
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.ReportAnalyticsCalculator
import com.example.model.ReportPeriodScope
import com.example.model.TakaJomaStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportAnalyticsUnitTest {

    private val members = listOf(
        Member(
            id = "m1",
            name = "Karim MPO",
            designation = "MPO",
            phone = "01711111111",
            depot = "Dhaka",
            code = "M-101",
            area = "Mirpur",
            isActive = true
        ),
        Member(
            id = "m2",
            name = "Rahim AM",
            designation = "AM",
            phone = "01722222222",
            depot = "Dhaka",
            code = "A-201",
            area = "Mirpur",
            isActive = true
        ),
        Member(
            id = "m3",
            name = "Jamal MPO",
            designation = "Sr. MPO",
            phone = "01733333333",
            depot = "Bogura",
            code = "M-102",
            area = "Sadar",
            isActive = true
        ),
        Member(
            id = "m4",
            name = "Salam RM",
            designation = "RM",
            phone = "01744444444",
            depot = "Bogura",
            code = "R-301",
            area = "Sadar",
            isActive = true
        )
    )

    private val sales = listOf(
        MonthlySalesRecord("s1", "2026-11", "M-101", "Karim MPO", 10_000.0, 11_500.0),
        MonthlySalesRecord("s2", "2026-11", "M-102", "Jamal MPO", 8_000.0, 9_200.0),
        MonthlySalesRecord("s3", "2026-10", "M-101", "Karim MPO", 5_000.0, 5_750.0)
    )

    private val submissions = listOf(
        MemberTakaJoma(
            id = "j1",
            memberId = "m1",
            name = "Karim MPO",
            code = "M-101",
            depot = "Dhaka",
            designation = "MPO",
            area = "Mirpur",
            amount = 7_500.0,
            date = "2026-11-15",
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        ),
        MemberTakaJoma(
            id = "j2",
            memberId = "m3",
            name = "Jamal MPO",
            code = "M-102",
            depot = "Bogura",
            designation = "Sr. MPO",
            area = "Sadar",
            amount = 9_200.0,
            date = "2026-11-18",
            approvalStatus = TakaJomaStatus.APPROVED.wireValue
        ),
        MemberTakaJoma(
            id = "j3",
            memberId = "m1",
            name = "Karim MPO",
            code = "M-101",
            depot = "Dhaka",
            designation = "MPO",
            area = "Mirpur",
            amount = 1_000.0,
            date = "2026-11-20",
            approvalStatus = TakaJomaStatus.PENDING.wireValue
        )
    )

    @Test
    fun `mpoReport filters MPO designations and respects selected month vs all time`() {
        val monthOnly = ReportAnalyticsCalculator.buildMpoReport(
            members = members,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH,
            onlyMpoDesignation = true
        )
        assertTrue(monthOnly.filteredByMpoDesignation)
        assertEquals(2, monthOnly.items.size)
        val karim = monthOnly.items.first { it.summary.memberId == "m1" }
        assertEquals(11_500.0, karim.summary.totalSales, 0.01)
        assertEquals(7_500.0, karim.summary.approvedJoma, 0.01)
        assertEquals(4_000.0, karim.summary.dues, 0.01)

        val allTime = ReportAnalyticsCalculator.buildMpoReport(
            members = members,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.ALL_TIME,
            onlyMpoDesignation = true
        )
        val karimAll = allTime.items.first { it.summary.memberId == "m1" }
        assertEquals(17_250.0, karimAll.summary.totalSales, 0.01)
        assertEquals(9_750.0, karimAll.summary.dues, 0.01)
    }

    @Test
    fun `mpoReport falls back to all members when no MPO designation exists`() {
        val nonMpoMembers = listOf(
            members[1].copy(designation = "Officer")
        )
        val res = ReportAnalyticsCalculator.buildMpoReport(
            members = nonMpoMembers,
            monthlySales = emptyList(),
            submissions = emptyList(),
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH,
            onlyMpoDesignation = true
        )
        assertFalse(res.hasExplicitMpoMembers)
        assertFalse(res.filteredByMpoDesignation)
        assertEquals(1, res.items.size)
    }

    @Test
    fun `amWiseReport groups by area and detects AM leader`() {
        val rows = ReportAnalyticsCalculator.buildAmWiseReport(
            members = members,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH
        )
        assertEquals(2, rows.size)
        val mirpur = rows.first { it.title == "Mirpur" }
        assertEquals("AM: Rahim AM", mirpur.leaderLabel)
        assertEquals(2, mirpur.memberCount)
        assertEquals(11_500.0, mirpur.totalTpPlusVat, 0.01)
        assertEquals(7_500.0, mirpur.approvedJoma, 0.01)
        assertEquals(4_000.0, mirpur.dues, 0.01)
    }

    @Test
    fun `regionWiseReport groups by depot and detects RM leader`() {
        val rows = ReportAnalyticsCalculator.buildRegionWiseReport(
            members = members,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH
        )
        assertEquals(2, rows.size)
        val bogura = rows.first { it.title == "Bogura" }
        assertTrue(bogura.leaderLabel.contains("RM: Salam RM"))
        assertEquals(2, bogura.memberCount)
        assertEquals(9_200.0, bogura.totalTpPlusVat, 0.01)
        assertEquals(9_200.0, bogura.approvedJoma, 0.01)
        assertEquals(0.0, bogura.dues, 0.01)
    }

    @Test
    fun `designation helpers distinguish ASM, AM, RSC, and RSM accurately`() {
        assertTrue(ReportAnalyticsCalculator.isAsmDesignation("ASM"))
        assertTrue(ReportAnalyticsCalculator.isAsmDesignation("Area Sales Manager"))
        assertFalse(ReportAnalyticsCalculator.isAmDesignation("ASM"))
        assertTrue(ReportAnalyticsCalculator.isAmDesignation("AM"))
        assertTrue(ReportAnalyticsCalculator.isRscDesignation("RSC"))
        assertTrue(ReportAnalyticsCalculator.isRscDesignation("Regional Sales Coordinator"))
        assertFalse(ReportAnalyticsCalculator.isRsmDesignation("RSC"))
        assertTrue(ReportAnalyticsCalculator.isRsmDesignation("RSM"))
        assertTrue(ReportAnalyticsCalculator.isRsmDesignation("RM"))
    }

    @Test
    fun `asmDepotWiseReport aggregates by depot with ASM leader and totals`() {
        val asmMember = Member(
            id = "m5",
            name = "Tariq ASM",
            designation = "ASM",
            phone = "01755555555",
            depot = "Dhaka",
            code = "ASM-01",
            area = "Mirpur",
            isActive = true
        )
        val rows = ReportAnalyticsCalculator.buildAsmDepotWiseReport(
            members = members + asmMember,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH
        )
        assertEquals(2, rows.size)
        val dhaka = rows.first { it.title == "Dhaka" }
        assertTrue(dhaka.leaderLabel.contains("ASM: Tariq ASM"))
        assertEquals(11_500.0, dhaka.totalTpPlusVat, 0.01)
        assertEquals(7_500.0, dhaka.approvedJoma, 0.01)
        assertEquals(4_000.0, dhaka.dues, 0.01)

        val totals = ReportAnalyticsCalculator.calculateGroupTotals(rows)
        assertEquals(20_700.0, totals.totalTpPlusVat, 0.01)
        assertEquals(16_700.0, totals.approvedJoma, 0.01)
        assertEquals(4_000.0, totals.dues, 0.01)
    }

    @Test
    fun `asmNameWiseReport supports depot team and direct modes`() {
        val asmMember = Member(
            id = "m5",
            name = "Tariq ASM",
            designation = "ASM",
            phone = "01755555555",
            depot = "Dhaka",
            code = "ASM-01",
            area = "",
            isActive = true
        )
        val teamResult = ReportAnalyticsCalculator.buildAsmNameWiseReport(
            members = members + asmMember,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH,
            scopeMode = AsmNameReportScope.DEPOT_TEAM
        )
        assertTrue(teamResult.hasExplicitAsmMembers)
        assertEquals(1, teamResult.rows.size)
        val tariqTeam = teamResult.rows.single()
        assertEquals("Tariq ASM", tariqTeam.title)
        assertEquals(11_500.0, tariqTeam.totalTpPlusVat, 0.01)
        assertEquals(7_500.0, tariqTeam.approvedJoma, 0.01)
        assertEquals(4_000.0, tariqTeam.dues, 0.01)

        val directResult = ReportAnalyticsCalculator.buildAsmNameWiseReport(
            members = members + asmMember,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH,
            scopeMode = AsmNameReportScope.ASM_DIRECT
        )
        assertEquals(0.0, directResult.rows.single().totalTpPlusVat, 0.01)
    }

    @Test
    fun `areaRoleWiseReport filters RSM, RSC, and AM area-wise metrics`() {
        val rscMember = Member(
            id = "m6",
            name = "Nabil RSC",
            designation = "RSC",
            phone = "01766666666",
            depot = "Dhaka",
            code = "RSC-01",
            area = "Mirpur",
            isActive = true
        )
        val rsmMember = Member(
            id = "m7",
            name = "Kamrul RSM",
            designation = "RSM",
            phone = "01777777777",
            depot = "Bogura",
            code = "RSM-01",
            area = "Sadar",
            isActive = true
        )
        val extendedMembers = members + rscMember + rsmMember

        val rscReport = ReportAnalyticsCalculator.buildAreaRoleWiseReport(
            members = extendedMembers,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH,
            roleFilter = AreaManagerRoleFilter.RSC
        )
        assertTrue(rscReport.hasExplicitRoleMembers)
        assertFalse(rscReport.fallbackShowingAllAreas)
        assertEquals(1, rscReport.rows.size)
        assertEquals("Mirpur", rscReport.rows.single().title)
        assertEquals(11_500.0, rscReport.rows.single().totalTpPlusVat, 0.01)
        assertEquals(7_500.0, rscReport.rows.single().approvedJoma, 0.01)
        assertEquals(4_000.0, rscReport.rows.single().dues, 0.01)

        val rsmReport = ReportAnalyticsCalculator.buildAreaRoleWiseReport(
            members = extendedMembers,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH,
            roleFilter = AreaManagerRoleFilter.RSM
        )
        assertEquals(1, rsmReport.rows.size)
        assertEquals("Sadar", rsmReport.rows.single().title)
        assertEquals(9_200.0, rsmReport.rows.single().totalTpPlusVat, 0.01)

        val amReport = ReportAnalyticsCalculator.buildAreaRoleWiseReport(
            members = extendedMembers,
            monthlySales = sales,
            submissions = submissions,
            selectedMonth = "2026-11",
            periodScope = ReportPeriodScope.SELECTED_MONTH,
            roleFilter = AreaManagerRoleFilter.AM
        )
        assertEquals(1, amReport.rows.size)
        assertEquals("Mirpur", amReport.rows.single().title)
    }
}
