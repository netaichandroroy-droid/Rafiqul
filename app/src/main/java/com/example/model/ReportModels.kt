package com.example.model

enum class ReportCategory(
    val title: String,
    val shortLabel: String,
    val subtitle: String,
    val menuTestTag: String,
    val chipTestTag: String
) {
    MONTHLY(
        title = "Monthly Report",
        shortLabel = "Monthly",
        subtitle = "Income, expense, and balance summary",
        menuTestTag = "report_menu_monthly",
        chipTestTag = "report_chip_monthly"
    ),
    MPO_WISE(
        title = "MPO Wise Report",
        shortLabel = "MPO Wise",
        subtitle = "TP+VAT sales, deposits, and dues by MPO",
        menuTestTag = "report_menu_mpo_wise",
        chipTestTag = "report_chip_mpo_wise"
    ),
    ASM_DEPOT_WISE(
        title = "ASM Depot Wise Report",
        shortLabel = "ASM Depot",
        subtitle = "Depot-wise TP+VAT sales, Deposit, Dues, and Total",
        menuTestTag = "report_menu_asm_depot_wise",
        chipTestTag = "report_chip_asm_depot_wise"
    ),
    ASM_NAME_WISE(
        title = "ASM Name Wise Report",
        shortLabel = "ASM Name",
        subtitle = "ASM name-wise TP+VAT sales, Deposit, Dues, and Total",
        menuTestTag = "report_menu_asm_name_wise",
        chipTestTag = "report_chip_asm_name_wise"
    ),
    AM_WISE(
        title = "RSM / RSC / AM Area Report",
        shortLabel = "RSM/RSC/AM",
        subtitle = "Area-wise Sales, Deposit, Dues, and Total for RSM, RSC, and AM",
        menuTestTag = "report_menu_am_wise",
        chipTestTag = "report_chip_am_wise"
    ),
    REGION_WISE(
        title = "Region Wise Report",
        shortLabel = "Region Wise",
        subtitle = "Consolidated summary by region and depot",
        menuTestTag = "report_menu_region_wise",
        chipTestTag = "report_chip_region_wise"
    )
}

enum class ReportPeriodScope(val bnLabel: String) {
    SELECTED_MONTH("Selected Month"),
    ALL_TIME("All Months (Total)")
}

enum class AsmNameReportScope(val bnLabel: String) {
    DEPOT_TEAM("Including Depot Team"),
    ASM_DIRECT("ASM Direct Only")
}

enum class AreaManagerRoleFilter(val bnLabel: String, val roleTitle: String) {
    ALL("All (RSM/RSC/AM)", "RSM / RSC / AM"),
    RSM("RSM", "RSM"),
    RSC("RSC", "RSC"),
    AM("AM", "AM")
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

data class AsmNameReportResult(
    val rows: List<GroupedReportRow>,
    val hasExplicitAsmMembers: Boolean,
    val fallbackNotice: String? = null
)

data class AreaRoleReportResult(
    val rows: List<GroupedReportRow>,
    val hasExplicitRoleMembers: Boolean,
    val fallbackShowingAllAreas: Boolean
)

object ReportAnalyticsCalculator {

    private val mpoRegex = Regex("""(?i)\b(m\.?p\.?o|sr\.?\s*mpo|senior\s*mpo|medical\s*promotion)\b""")
    private val amRegex = Regex("""(?i)\b(a\.?m|sr\.?\s*am|area\s*manager)\b""")
    private val asmRegex = Regex("""(?i)\b(a\.?s\.?m|sr\.?\s*asm|assistant\s*sales\s*manager|area\s*sales\s*manager)\b""")
    private val rscRegex = Regex("""(?i)\b(r\.?s\.?c|sr\.?\s*rsc|regional\s*sales\s*coordinator|sales\s*coordinator)\b""")
    private val rmRegex = Regex("""(?i)\b(r\.?m|r\.?s\.?m|sr\.?\s*rsm|region\s*manager|regional\s*manager|regional\s*sales\s*manager)\b""")

    fun isMpoDesignation(designation: String): Boolean {
        val normalized = MemberSalesDuesCalculator.normalizeText(designation)
        if (normalized.isEmpty()) return false
        return mpoRegex.containsMatchIn(normalized) || normalized.contains("mpo", ignoreCase = true)
    }

    fun isAsmDesignation(designation: String): Boolean {
        val normalized = MemberSalesDuesCalculator.normalizeText(designation)
        if (normalized.isEmpty()) return false
        return asmRegex.containsMatchIn(normalized) ||
            normalized.equals("asm", ignoreCase = true) ||
            normalized.contains("asm", ignoreCase = true) ||
            normalized.contains("area sales manager", ignoreCase = true) ||
            normalized.contains("assistant sales manager", ignoreCase = true)
    }

    fun isRscDesignation(designation: String): Boolean {
        val normalized = MemberSalesDuesCalculator.normalizeText(designation)
        if (normalized.isEmpty()) return false
        return rscRegex.containsMatchIn(normalized) ||
            normalized.equals("rsc", ignoreCase = true) ||
            normalized.contains("rsc", ignoreCase = true) ||
            normalized.contains("coordinator", ignoreCase = true)
    }

    fun isAmDesignation(designation: String): Boolean {
        val normalized = MemberSalesDuesCalculator.normalizeText(designation)
        if (normalized.isEmpty() || isAsmDesignation(normalized)) return false
        return amRegex.containsMatchIn(normalized) ||
            normalized.equals("am", ignoreCase = true) ||
            normalized.contains("area manager", ignoreCase = true)
    }

    fun isRsmDesignation(designation: String): Boolean {
        val normalized = MemberSalesDuesCalculator.normalizeText(designation)
        if (normalized.isEmpty() || isRscDesignation(normalized)) return false
        return rmRegex.containsMatchIn(normalized) ||
            normalized.equals("rm", ignoreCase = true) ||
            normalized.equals("rsm", ignoreCase = true) ||
            normalized.contains("region manager", ignoreCase = true) ||
            normalized.contains("regional manager", ignoreCase = true) ||
            normalized.contains("regional sales manager", ignoreCase = true)
    }

    fun isRmDesignation(designation: String): Boolean = isRsmDesignation(designation)

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

    fun buildAsmDepotWiseReport(
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
                "ASM_DEPOT:${cleanDepot.lowercase()}"
            } else {
                "UNASSIGNED"
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        return groups.entries.map { (key, groupItems) ->
            val displayDepot = groupItems.firstNotNullOfOrNull {
                MemberSalesDuesCalculator.normalizeText(it.summary.depot).takeIf { d -> d.isNotEmpty() }
            } ?: "Unassigned Depot"

            val asmLeaders = groupItems
                .filter { isAsmDesignation(it.designation) }
                .map { it.summary.name }
                .distinct()
            val areaCount = groupItems
                .mapNotNull { it.summary.area.trim().takeIf { a -> a.isNotEmpty() } }
                .distinctBy { it.lowercase() }
                .size

            val leaderLabel = buildString {
                if (asmLeaders.isNotEmpty()) {
                    append("ASM: ${asmLeaders.joinToString(", ")}")
                    if (areaCount > 0) append(" • ")
                }
                if (areaCount > 0) {
                    append("$areaCount ${if (areaCount == 1) "Area" else "Areas"}")
                } else if (asmLeaders.isEmpty()) {
                    append("Depot Consolidated Summary")
                }
            }

            aggregateGroup(
                key = key,
                title = displayDepot,
                leaderLabel = leaderLabel,
                groupItems = groupItems
            )
        }.sortedWith(
            compareByDescending<GroupedReportRow> { it.totalTpPlusVat }
                .thenByDescending { it.dues }
                .thenBy { it.title.lowercase() }
        )
    }

    fun buildAsmNameWiseReport(
        members: List<Member>,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>,
        selectedMonth: String,
        periodScope: ReportPeriodScope,
        scopeMode: AsmNameReportScope = AsmNameReportScope.DEPOT_TEAM
    ): AsmNameReportResult {
        val allItems = buildScopedMemberItems(
            members = members,
            monthlySales = monthlySales,
            submissions = submissions,
            selectedMonth = selectedMonth,
            periodScope = periodScope
        )
        if (allItems.isEmpty()) {
            return AsmNameReportResult(rows = emptyList(), hasExplicitAsmMembers = false)
        }

        val asmItems = allItems.filter { isAsmDesignation(it.designation) }
        if (asmItems.isEmpty()) {
            val fallbackRows = buildAsmDepotWiseReport(
                members = members,
                monthlySales = monthlySales,
                submissions = submissions,
                selectedMonth = selectedMonth,
                periodScope = periodScope
            )
            return AsmNameReportResult(
                rows = fallbackRows,
                hasExplicitAsmMembers = false,
                fallbackNotice = "No member has the 'ASM' designation assigned yet — showing depot-wise totals instead."
            )
        }

        val rows = asmItems.map { asm ->
            val asmDepot = MemberSalesDuesCalculator.normalizeText(asm.summary.depot)
            val asmArea = MemberSalesDuesCalculator.normalizeText(asm.summary.area)

            val matchedMembers = when (scopeMode) {
                AsmNameReportScope.ASM_DIRECT -> listOf(asm)
                AsmNameReportScope.DEPOT_TEAM -> {
                    val sameDepotAsmCount = if (asmDepot.isNotEmpty()) {
                        asmItems.count {
                            MemberSalesDuesCalculator.normalizeText(it.summary.depot)
                                .equals(asmDepot, ignoreCase = true)
                        }
                    } else {
                        0
                    }

                    val team = when {
                        sameDepotAsmCount > 1 && asmDepot.isNotEmpty() && asmArea.isNotEmpty() -> {
                            allItems.filter { item ->
                                MemberSalesDuesCalculator.normalizeText(item.summary.depot)
                                    .equals(asmDepot, ignoreCase = true) &&
                                    MemberSalesDuesCalculator.normalizeText(item.summary.area)
                                        .equals(asmArea, ignoreCase = true)
                            }
                        }
                        asmDepot.isNotEmpty() -> {
                            allItems.filter { item ->
                                MemberSalesDuesCalculator.normalizeText(item.summary.depot)
                                    .equals(asmDepot, ignoreCase = true)
                            }
                        }
                        asmArea.isNotEmpty() -> {
                            allItems.filter { item ->
                                MemberSalesDuesCalculator.normalizeText(item.summary.area)
                                    .equals(asmArea, ignoreCase = true)
                            }
                        }
                        else -> listOf(asm)
                    }
                    (listOf(asm) + team).distinctBy { it.summary.memberId }
                }
            }

            val metaParts = buildList {
                if (asm.summary.code.isNotBlank()) add("Code: ${asm.summary.code}")
                if (asmDepot.isNotEmpty()) add("Depot: $asmDepot")
                if (asmArea.isNotEmpty()) add("Area: $asmArea")
                if (isEmpty()) add("ASM")
            }

            aggregateGroup(
                key = "ASM_NAME:${asm.summary.memberId}",
                title = asm.summary.name,
                leaderLabel = metaParts.joinToString(" • "),
                groupItems = matchedMembers
            )
        }.sortedWith(
            compareByDescending<GroupedReportRow> { it.totalTpPlusVat }
                .thenByDescending { it.dues }
                .thenBy { it.title.lowercase() }
        )

        return AsmNameReportResult(
            rows = rows,
            hasExplicitAsmMembers = true
        )
    }

    fun buildAreaRoleWiseReport(
        members: List<Member>,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>,
        selectedMonth: String,
        periodScope: ReportPeriodScope,
        roleFilter: AreaManagerRoleFilter = AreaManagerRoleFilter.ALL
    ): AreaRoleReportResult {
        val allItems = buildScopedMemberItems(
            members = members,
            monthlySales = monthlySales,
            submissions = submissions,
            selectedMonth = selectedMonth,
            periodScope = periodScope
        )
        if (allItems.isEmpty()) {
            return AreaRoleReportResult(
                rows = emptyList(),
                hasExplicitRoleMembers = false,
                fallbackShowingAllAreas = false
            )
        }

        val matchesFilterRole: (MemberReportItem) -> Boolean = { item ->
            when (roleFilter) {
                AreaManagerRoleFilter.ALL ->
                    isRsmDesignation(item.designation) ||
                        isRscDesignation(item.designation) ||
                        isAmDesignation(item.designation)
                AreaManagerRoleFilter.RSM -> isRsmDesignation(item.designation)
                AreaManagerRoleFilter.RSC -> isRscDesignation(item.designation)
                AreaManagerRoleFilter.AM -> isAmDesignation(item.designation)
            }
        }

        val matchingRoleMembers = allItems.filter(matchesFilterRole)
        val hasExplicitRoleMembers = matchingRoleMembers.isNotEmpty()

        // Group items into Area buckets
        val groups = linkedMapOf<String, MutableList<MemberReportItem>>()
        for (item in allItems) {
            val cleanArea = MemberSalesDuesCalculator.normalizeText(item.summary.area)
            val key = when {
                cleanArea.isNotEmpty() -> "AREA:${cleanArea.lowercase()}"
                isAmDesignation(item.designation) ||
                    isRscDesignation(item.designation) ||
                    isRsmDesignation(item.designation) ->
                    "ROLE:${MemberSalesDuesCalculator.normalizeText(item.summary.name).lowercase()}"
                else -> "UNASSIGNED"
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }

        // Map depots led by RSM / RSC who don't have a specific area assigned
        val depotWideLeadersByRole = matchingRoleMembers
            .filter {
                MemberSalesDuesCalculator.normalizeText(it.summary.area).isEmpty() &&
                    MemberSalesDuesCalculator.normalizeText(it.summary.depot).isNotEmpty()
            }
            .groupBy { MemberSalesDuesCalculator.normalizeText(it.summary.depot).lowercase() }

        val allAreaEntries = groups.entries.toList()
        val filteredEntries = if (roleFilter == AreaManagerRoleFilter.ALL || !hasExplicitRoleMembers) {
            allAreaEntries
        } else {
            allAreaEntries.filter { (_, groupItems) ->
                val directLeaderInArea = groupItems.any(matchesFilterRole)
                val groupDepotsLower = groupItems
                    .map { MemberSalesDuesCalculator.normalizeText(it.summary.depot).lowercase() }
                    .filter { it.isNotEmpty() }
                val depotLeaderMatches = groupDepotsLower.any { depotKey ->
                    depotWideLeadersByRole[depotKey].orEmpty().isNotEmpty()
                }
                directLeaderInArea || depotLeaderMatches
            }
        }

        val effectiveEntries = filteredEntries.ifEmpty { allAreaEntries }

        val rows = effectiveEntries.map { (key, groupItems) ->
            val displayArea = groupItems.firstNotNullOfOrNull {
                MemberSalesDuesCalculator.normalizeText(it.summary.area).takeIf { a -> a.isNotEmpty() }
            }
            val groupDepotsLower = groupItems
                .map { MemberSalesDuesCalculator.normalizeText(it.summary.depot).lowercase() }
                .filter { it.isNotEmpty() }
                .distinct()

            val depotLeaders = groupDepotsLower.flatMap { depotKey ->
                allItems.filter { leader ->
                    MemberSalesDuesCalculator.normalizeText(leader.summary.area).isEmpty() &&
                        MemberSalesDuesCalculator.normalizeText(leader.summary.depot)
                            .equals(depotKey, ignoreCase = true)
                }
            }

            val rsmLeaders = (groupItems.filter { isRsmDesignation(it.designation) } +
                depotLeaders.filter { isRsmDesignation(it.designation) })
                .map { it.summary.name }
                .distinct()
            val rscLeaders = (groupItems.filter { isRscDesignation(it.designation) } +
                depotLeaders.filter { isRscDesignation(it.designation) })
                .map { it.summary.name }
                .distinct()
            val amLeaders = (groupItems.filter { isAmDesignation(it.designation) } +
                depotLeaders.filter { isAmDesignation(it.designation) })
                .map { it.summary.name }
                .distinct()

            val title = when {
                !displayArea.isNullOrBlank() -> displayArea
                rsmLeaders.isNotEmpty() -> rsmLeaders.first()
                rscLeaders.isNotEmpty() -> rscLeaders.first()
                amLeaders.isNotEmpty() -> amLeaders.first()
                else -> "Unassigned Area"
            }

            val leaderParts = buildList {
                if (rsmLeaders.isNotEmpty() && (roleFilter == AreaManagerRoleFilter.ALL || roleFilter == AreaManagerRoleFilter.RSM)) {
                    add("RSM: ${rsmLeaders.joinToString(", ")}")
                }
                if (rscLeaders.isNotEmpty() && (roleFilter == AreaManagerRoleFilter.ALL || roleFilter == AreaManagerRoleFilter.RSC)) {
                    add("RSC: ${rscLeaders.joinToString(", ")}")
                }
                if (amLeaders.isNotEmpty() && (roleFilter == AreaManagerRoleFilter.ALL || roleFilter == AreaManagerRoleFilter.AM)) {
                    add("AM: ${amLeaders.joinToString(", ")}")
                }
            }

            val leaderLabel = if (leaderParts.isNotEmpty()) {
                leaderParts.joinToString(" • ")
            } else {
                val depots = groupItems.mapNotNull {
                    it.summary.depot.takeIf { d -> d.isNotBlank() }
                }.distinct()
                if (depots.isNotEmpty()) "Depot: ${depots.joinToString(", ")}" else "${roleFilter.roleTitle} / Area Group"
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

        return AreaRoleReportResult(
            rows = rows,
            hasExplicitRoleMembers = hasExplicitRoleMembers,
            fallbackShowingAllAreas = roleFilter != AreaManagerRoleFilter.ALL && !hasExplicitRoleMembers
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
                else -> "Unassigned Area (AM)"
            }
            val leaderLabel = when {
                amLeaders.isNotEmpty() -> "AM: ${amLeaders.joinToString(", ")}"
                else -> {
                    val depots = groupItems.mapNotNull {
                        it.summary.depot.takeIf { d -> d.isNotBlank() }
                    }.distinct()
                    if (depots.isNotEmpty()) "Depot: ${depots.joinToString(", ")}" else "AM / Area Group"
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
            } ?: "Unassigned Region / Depot"

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
                    append("$areaCount ${if (areaCount == 1) "Area" else "Areas"}")
                } else if (rmLeaders.isEmpty()) {
                    append("Region / Depot Summary")
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
