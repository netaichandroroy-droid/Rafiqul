package com.example.model

enum class ReportCategory(
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val menuTestTag: String,
    val chipTestTag: String
) {
    MONTHLY(
        title = "মাসিক রিপোর্ট",
        shortLabel = "মাসিক",
        subtitle = "আয়, খরচ ও ব্যালেন্স হিসাব",
        menuTestTag = "report_menu_monthly",
        chipTestTag = "report_chip_monthly"
    ),
    MPO_WISE(
        title = "MPO ভিত্তিক রিপোর্ট",
        shortLabel = "MPO ভিত্তিক",
        subtitle = "MPO অনুযায়ী সেলস, জমা ও বকেয়া",
        menuTestTag = "report_menu_mpo_wise",
        chipTestTag = "report_chip_mpo_wise"
    ),
    AM_WISE(
        title = "AM ভিত্তিক রিপোর্ট",
        shortLabel = "AM ভিত্তিক",
        subtitle = "AM ও এরিয়া অনুযায়ী সেলস ও বকেয়া",
        menuTestTag = "report_menu_am_wise",
        chipTestTag = "report_chip_am_wise"
    ),
    REGION_WISE(
        title = "রিজিয়ন ভিত্তিক রিপোর্ট",
        shortLabel = "রিজিয়ন ভিত্তিক",
        subtitle = "রিজিয়ন / ডিপো অনুযায়ী সমন্বিত হিসাব",
        menuTestTag = "report_menu_region_wise",
        chipTestTag = "report_chip_region_wise"
    )
}

enum class ReportPeriodScope(val bnLabel: String) {
    SELECTED_MONTH("নির্বাচিত মাস"),
    ALL_TIME("সর্বমোট (সব মাস)")
}

data class MemberReportItem(
    val summary: MemberSalesDuesSummary,
    val designation: String = "",
    val phone: String = "",
    val isActive: Boolean = true
) {
    val collectionPercent: Int
        get() = if (summary.totalSales > 0.0) {
            ((summary.approvedJoma / summary.totalSales) * 100.0).toInt().coerceIn(0, 100)
        } else if (summary.approvedJoma > 0.0) {
            100
        } else {
            0
        }
}

data class GroupedReportRow(
    val key: String,
    val title: String,
    val leaderLabel: String,
    val memberCount: Int,
    val totalTpSales: Double,
    val totalTpPlusVat: Double,
    val approvedJoma: Double,
    val pendingJoma: Double,
    val dues: Double,
    val advance: Double,
    val members: List<MemberReportItem>
) {
    val collectionPercent: Int
        get() = if (totalTpPlusVat > 0.0) {
            ((approvedJoma / totalTpPlusVat) * 100.0).toInt().coerceIn(0, 100)
        } else if (approvedJoma > 0.0) {
            100
        } else {
            0
        }
}

data class ReportTotals(
    val rowCount: Int,
    val memberCount: Int,
    val totalTpSales: Double,
    val totalTpPlusVat: Double,
    val approvedJoma: Double,
    val pendingJoma: Double,
    val dues: Double,
    val advance: Double
) {
    val collectionPercent: Int
        get() = if (totalTpPlusVat > 0.0) {
            ((approvedJoma / totalTpPlusVat) * 100.0).toInt().coerceIn(0, 100)
        } else if (approvedJoma > 0.0) {
            100
        } else {
            0
        }
}

data class MpoReportResult(
    val items: List<MemberReportItem>,
    val filteredByMpoDesignation: Boolean,
    val hasExplicitMpoMembers: Boolean
)

object ReportAnalyticsCalculator {

    private val mpoRegex = Regex("""(?i)\b(m\.?p\.?o|sr\.?\s*mpo|senior\s*mpo|medical\s*promotion)\b""")
    private val amRegex = Regex("""(?i)\b(a\.?m|sr\.?\s*am|area\s*manager)\b""")
    private val rmRegex = Regex("""(?i)\b(r\.?m|r\.?s\.?m|region\s*manager|regional\s*manager)\b""")

    fun isMpoDesignation(designation: String): Boolean {
        val normalized = MemberSalesDuesCalculator.normalizeText(designation)
        if (normalized.isEmpty()) return false
        return mpoRegex.containsMatchIn(normalized) || normalized.contains("mpo", ignoreCase = true)
    }

    fun isAmDesignation(designation: String): Boolean {
        val normalized = MemberSalesDuesCalculator.normalizeText(designation)
        if (normalized.isEmpty()) return false
        return amRegex.containsMatchIn(normalized) ||
            normalized.equals("am", ignoreCase = true) ||
            normalized.contains("area manager", ignoreCase = true)
    }

    fun isRmDesignation(designation: String): Boolean {
        val normalized = MemberSalesDuesCalculator.normalizeText(designation)
        if (normalized.isEmpty()) return false
        return rmRegex.containsMatchIn(normalized) ||
            normalized.contains("region", ignoreCase = true) ||
            normalized.equals("rm", ignoreCase = true) ||
            normalized.equals("rsm", ignoreCase = true)
    }

    fun buildScopedMemberItems(
        members: List<Member>,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>,
        selectedMonth: String,
        periodScope: ReportPeriodScope
    ): List<MemberReportItem> {
        val scopedSales = when (periodScope) {
            ReportPeriodScope.SELECTED_MONTH -> monthlySales.filter { it.yearMonth == selectedMonth }
            ReportPeriodScope.ALL_TIME -> monthlySales
        }
        val scopedSubs = when (periodScope) {
            ReportPeriodScope.SELECTED_MONTH -> submissions.filter { it.date.startsWith(selectedMonth) }
            ReportPeriodScope.ALL_TIME -> submissions
        }
        val scopedMembers = when (periodScope) {
            ReportPeriodScope.SELECTED_MONTH -> members.map { it.copy(salesAmount = 0.0) }
            ReportPeriodScope.ALL_TIME -> members
        }

        val memberById = members.associateBy { it.id }
        val summaries = MemberSalesDuesCalculator.buildAllMemberSummaries(
            members = scopedMembers,
            monthlySales = scopedSales,
            submissions = scopedSubs
        )

        return summaries.map { summary ->
            val member = memberById[summary.memberId]
            MemberReportItem(
                summary = summary,
                designation = member?.designation.orEmpty().trim(),
                phone = member?.phone.orEmpty().trim(),
                isActive = member?.isActive ?: true
            )
        }
    }

    fun buildMpoReport(
        members: List<Member>,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>,
        selectedMonth: String,
        periodScope: ReportPeriodScope,
        onlyMpoDesignation: Boolean = true
    ): MpoReportResult {
        val allItems = buildScopedMemberItems(
            members = members,
            monthlySales = monthlySales,
            submissions = submissions,
            selectedMonth = selectedMonth,
            periodScope = periodScope
        )
        val hasExplicitMpo = allItems.any { isMpoDesignation(it.designation) }
        val filtered = if (onlyMpoDesignation && hasExplicitMpo) {
            allItems.filter { isMpoDesignation(it.designation) }
        } else {
            allItems
        }
        return MpoReportResult(
            items = filtered,
            filteredByMpoDesignation = onlyMpoDesignation && hasExplicitMpo,
            hasExplicitMpoMembers = hasExplicitMpo
        )
    }

    fun buildAmWiseReport(
        members: List<Member>,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>,
        selectedMonth: String,
        periodScope: ReportPeriodScope
    ): List<GroupedReportRow> {
        val allItems = buildScopedMemberItems(
            members = members,
            monthlySales = monthlySales,
            submissions = submissions,
            selectedMonth = selectedMonth,
            periodScope = periodScope
        )
        if (allItems.isEmpty()) return emptyList()

        val groups = linkedMapOf<String, MutableList<MemberReportItem>>()
        for (item in allItems) {
            val cleanArea = MemberSalesDuesCalculator.normalizeText(item.summary.area)
            val key = if (cleanArea.isNotEmpty()) {
                "AREA:${cleanArea.lowercase()}"
            } else if (isAmDesignation(item.designation)) {
                "AM:${MemberSalesDuesCalculator.normalizeText(item.summary.name).lowercase()}"
            } else {
                "UNASSIGNED"
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        return groups.entries.map { (key, groupItems) ->
            val displayArea = groupItems.firstNotNullOfOrNull {
                MemberSalesDuesCalculator.normalizeText(it.summary.area).takeIf { a -> a.isNotEmpty() }
            }
            val amLeaders = groupItems
                .filter { isAmDesignation(it.designation) }
                .map { it.summary.name }
                .distinct()

            val title = when {
                !displayArea.isNullOrBlank() -> displayArea
                amLeaders.isNotEmpty() -> amLeaders.first()
                else -> "অনির্ধারিত এরিয়া (AM)"
            }
            val leaderLabel = when {
                amLeaders.isNotEmpty() -> "AM: ${amLeaders.joinToString(", ")}"
                else -> {
                    val depots = groupItems.mapNotNull {
                        it.summary.depot.takeIf { d -> d.isNotBlank() }
                    }.distinct()
                    if (depots.isNotEmpty()) "ডিপো: ${depots.joinToString(", ")}" else "AM / এরিয়া গ্রুপ"
                }
            }

            aggregateGroup(
                key = key,
                title = title,
                leaderLabel = leaderLabel,
                groupItems = groupItems
            )
        }.sortedWith(
            compareByDescending<GroupedReportRow> { it.totalTpPlusVat }
                .thenByDescending { it.dues }
                .thenBy { it.title.lowercase() }
        )
    }

    fun buildRegionWiseReport(
        members: List<Member>,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>,
        selectedMonth: String,
        periodScope: ReportPeriodScope
    ): List<GroupedReportRow> {
        val allItems = buildScopedMemberItems(
            members = members,
            monthlySales = monthlySales,
            submissions = submissions,
            selectedMonth = selectedMonth,
            periodScope = periodScope
        )
        if (allItems.isEmpty()) return emptyList()

        val groups = linkedMapOf<String, MutableList<MemberReportItem>>()
        for (item in allItems) {
            val cleanDepot = MemberSalesDuesCalculator.normalizeText(item.summary.depot)
            val key = if (cleanDepot.isNotEmpty()) {
                "REGION:${cleanDepot.lowercase()}"
            } else {
                "UNASSIGNED"
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        return groups.entries.map { (key, groupItems) ->
            val displayRegion = groupItems.firstNotNullOfOrNull {
                MemberSalesDuesCalculator.normalizeText(it.summary.depot).takeIf { d -> d.isNotEmpty() }
            } ?: "অনির্ধারিত রিজিয়ন / ডিপো"

            val rmLeaders = groupItems
                .filter { isRmDesignation(it.designation) }
                .map { it.summary.name }
                .distinct()
            val areaCount = groupItems
                .mapNotNull { it.summary.area.trim().takeIf { a -> a.isNotEmpty() } }
                .distinctBy { it.lowercase() }
                .size

            val leaderLabel = buildString {
                if (rmLeaders.isNotEmpty()) {
                    append("RM: ${rmLeaders.joinToString(", ")}")
                    if (areaCount > 0) append(" • ")
                }
                if (areaCount > 0) {
                    append("${BanglaFormatters.toBanglaDigits(areaCount.toString())}টি এরিয়া")
                } else if (rmLeaders.isEmpty()) {
                    append("রিজিয়ন / ডিপো সারাংশ")
                }
            }

            aggregateGroup(
                key = key,
                title = displayRegion,
                leaderLabel = leaderLabel,
                groupItems = groupItems
            )
        }.sortedWith(
            compareByDescending<GroupedReportRow> { it.totalTpPlusVat }
                .thenByDescending { it.dues }
                .thenBy { it.title.lowercase() }
        )
    }

    private fun aggregateGroup(
        key: String,
        title: String,
        leaderLabel: String,
        groupItems: List<MemberReportItem>
    ): GroupedReportRow {
        val totalTpSales = groupItems.sumOf { it.summary.uploadedTpSales }
        val totalTpPlusVat = groupItems.sumOf { it.summary.totalSales }
        val approvedJoma = groupItems.sumOf { it.summary.approvedJoma }
        val pendingJoma = groupItems.sumOf { it.summary.pendingJoma }
        val dues = (totalTpPlusVat - approvedJoma).coerceAtLeast(0.0)
        val advance = (approvedJoma - totalTpPlusVat).coerceAtLeast(0.0)

        return GroupedReportRow(
            key = key,
            title = title,
            leaderLabel = leaderLabel,
            memberCount = groupItems.size,
            totalTpSales = totalTpSales,
            totalTpPlusVat = totalTpPlusVat,
            approvedJoma = approvedJoma,
            pendingJoma = pendingJoma,
            dues = dues,
            advance = advance,
            members = groupItems.sortedByDescending { it.summary.dues }
        )
    }

    fun calculateMemberTotals(items: List<MemberReportItem>): ReportTotals {
        val tpPlusVat = items.sumOf { it.summary.totalSales }
        val approved = items.sumOf { it.summary.approvedJoma }
        return ReportTotals(
            rowCount = items.size,
            memberCount = items.size,
            totalTpSales = items.sumOf { it.summary.uploadedTpSales },
            totalTpPlusVat = tpPlusVat,
            approvedJoma = approved,
            pendingJoma = items.sumOf { it.summary.pendingJoma },
            dues = items.sumOf { it.summary.dues },
            advance = items.sumOf { it.summary.advance }
        )
    }

    fun calculateGroupTotals(rows: List<GroupedReportRow>): ReportTotals {
        return ReportTotals(
            rowCount = rows.size,
            memberCount = rows.sumOf { it.memberCount },
            totalTpSales = rows.sumOf { it.totalTpSales },
            totalTpPlusVat = rows.sumOf { it.totalTpPlusVat },
            approvedJoma = rows.sumOf { it.approvedJoma },
            pendingJoma = rows.sumOf { it.pendingJoma },
            dues = rows.sumOf { it.dues },
            advance = rows.sumOf { it.advance }
        )
    }
}
