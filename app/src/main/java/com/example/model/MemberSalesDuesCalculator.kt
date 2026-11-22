package com.example.model

data class MemberSalesDuesSummary(
    val memberId: String,
    val code: String,
    val name: String,
    val area: String = "",
    val depot: String = "",
    val uploadedTpSales: Double = 0.0,
    val uploadedSales: Double,
    val manualSales: Double,
    val totalSales: Double,
    val approvedJoma: Double,
    val pendingJoma: Double,
    val rejectedJoma: Double,
    val approvedCount: Int,
    val pendingCount: Int,
    val dues: Double,
    val advance: Double
)

data class MemberSalesDuesTotals(
    val totalTpSales: Double = 0.0,
    val totalSales: Double,
    val totalApprovedJoma: Double,
    val totalPendingJoma: Double,
    val totalDues: Double,
    val totalAdvance: Double
)

object MemberSalesDuesCalculator {

    fun normalizeText(raw: String): String {
        return raw.trim().replace(Regex("\\s+"), " ")
    }

    fun sortMembersByArea(members: List<Member>): List<Member> {
        return members.sortedWith(
            compareBy<Member>(
                { it.area.trim().isEmpty() },
                { normalizeText(it.area).lowercase() },
                { normalizeText(it.name).lowercase() },
                { it.code.trim().lowercase() }
            )
        )
    }

    fun matchesSalesRecord(
        record: MonthlySalesRecord,
        memberCode: String,
        memberName: String
    ): Boolean {
        val cleanCode = memberCode.trim()
        val cleanName = normalizeText(memberName)
        val recCode = record.code.trim()
        val recName = normalizeText(record.name)
        val codeMatch = cleanCode.isNotEmpty() && recCode.equals(cleanCode, ignoreCase = true)
        val nameMatch = cleanName.isNotEmpty() && recName.equals(cleanName, ignoreCase = true)
        return codeMatch || nameMatch
    }

    fun matchesSubmissionToMember(
        entry: MemberTakaJoma,
        member: Member
    ): Boolean {
        if (member.id.isNotBlank() && entry.memberId == member.id) {
            return true
        }
        val allowIdentityFallback = entry.memberId.isBlank() || entry.memberId == "admin_manual"
        if (!allowIdentityFallback) return false

        val cleanMemberCode = member.code.trim()
        val cleanEntryCode = entry.code.trim()
        if (cleanMemberCode.isNotEmpty() && cleanEntryCode.equals(cleanMemberCode, ignoreCase = true)) {
            return true
        }
        val cleanMemberName = normalizeText(member.name)
        val cleanEntryName = normalizeText(entry.name)
        return cleanMemberName.isNotEmpty() && cleanEntryName.equals(cleanMemberName, ignoreCase = true)
    }

    fun calculateForMember(
        member: Member,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>
    ): MemberSalesDuesSummary {
        val matchedSales = monthlySales.filter { matchesSalesRecord(it, member.code, member.name) }
        // Informational only: TP sales never contributes to Dues
        val uploadedTpSales = matchedSales.sumOf { it.tpSales.coerceAtLeast(0.0) }
        // Dues is strictly calculated from TP+Vat (+ optional manual adjustment)
        val uploadedTpPlusVat = matchedSales.sumOf { it.tpPlusVat.coerceAtLeast(0.0) }
        val manualSales = member.salesAmount.coerceAtLeast(0.0)
        val totalSales = uploadedTpPlusVat + manualSales

        val matchedSubmissions = submissions.filter { matchesSubmissionToMember(it, member) }
        val approvedEntries = matchedSubmissions.filter { it.resolvedStatus == TakaJomaStatus.APPROVED }
        val pendingEntries = matchedSubmissions.filter { it.resolvedStatus == TakaJomaStatus.PENDING }
        val rejectedEntries = matchedSubmissions.filter { it.resolvedStatus == TakaJomaStatus.REJECTED }

        val approvedJoma = approvedEntries.sumOf { it.amount.coerceAtLeast(0.0) }
        val pendingJoma = pendingEntries.sumOf { it.amount.coerceAtLeast(0.0) }
        val rejectedJoma = rejectedEntries.sumOf { it.amount.coerceAtLeast(0.0) }

        val dues = (totalSales - approvedJoma).coerceAtLeast(0.0)
        val advance = (approvedJoma - totalSales).coerceAtLeast(0.0)

        return MemberSalesDuesSummary(
            memberId = member.id,
            code = member.code.trim(),
            name = member.name.trim(),
            area = member.area.trim(),
            depot = member.depot.trim(),
            uploadedTpSales = uploadedTpSales,
            uploadedSales = uploadedTpPlusVat,
            manualSales = manualSales,
            totalSales = totalSales,
            approvedJoma = approvedJoma,
            pendingJoma = pendingJoma,
            rejectedJoma = rejectedJoma,
            approvedCount = approvedEntries.size,
            pendingCount = pendingEntries.size,
            dues = dues,
            advance = advance
        )
    }

    fun buildAllMemberSummaries(
        members: List<Member>,
        monthlySales: List<MonthlySalesRecord>,
        submissions: List<MemberTakaJoma>
    ): List<MemberSalesDuesSummary> {
        val summaries = mutableListOf<MemberSalesDuesSummary>()
        val matchedSalesIds = hashSetOf<String>()
        val matchedSubmissionIds = hashSetOf<String>()

        for (member in members) {
            val memberSales = monthlySales.filter { matchesSalesRecord(it, member.code, member.name) }
            memberSales.forEach { matchedSalesIds.add(it.id) }

            val memberSubs = submissions.filter { matchesSubmissionToMember(it, member) }
            memberSubs.forEach { matchedSubmissionIds.add(it.id) }

            summaries.add(calculateForMember(member, memberSales, memberSubs))
        }

        val unmatchedSales = monthlySales.filterNot { it.id in matchedSalesIds }
        val unmatchedSubs = submissions.filterNot { it.id in matchedSubmissionIds }

        val extraGroups = linkedMapOf<String, Pair<MutableList<MonthlySalesRecord>, MutableList<MemberTakaJoma>>>()
        fun groupKey(code: String, name: String): String {
            val cleanCode = code.trim().uppercase()
            val cleanName = normalizeText(name).uppercase()
            return if (cleanCode.isNotEmpty()) "CODE:$cleanCode" else "NAME:$cleanName"
        }

        for (sale in unmatchedSales) {
            val key = groupKey(sale.code, sale.name)
            val pair = extraGroups.getOrPut(key) { mutableListOf<MonthlySalesRecord>() to mutableListOf() }
            pair.first.add(sale)
        }
        for (sub in unmatchedSubs) {
            val key = groupKey(sub.code, sub.name)
            val existingKey = if (extraGroups.containsKey(key)) {
                key
            } else {
                val altNameKey = "NAME:${normalizeText(sub.name).uppercase()}"
                val foundByName = extraGroups.entries.firstOrNull { (_, value) ->
                    value.first.any { matchesSalesRecord(it, sub.code, sub.name) }
                }?.key
                foundByName ?: if (extraGroups.containsKey(altNameKey)) altNameKey else key
            }
            val pair = extraGroups.getOrPut(existingKey) { mutableListOf<MonthlySalesRecord>() to mutableListOf() }
            pair.second.add(sub)
        }

        extraGroups.forEach { (key, pair) ->
            val salesRows = pair.first
            val subRows = pair.second
            val sampleCode = salesRows.firstOrNull { it.code.isNotBlank() }?.code
                ?: subRows.firstOrNull { it.code.isNotBlank() }?.code.orEmpty()
            val sampleName = salesRows.firstOrNull { it.name.isNotBlank() }?.name
                ?: subRows.firstOrNull { it.name.isNotBlank() }?.name.orEmpty()
            val sampleArea = subRows.firstOrNull { it.area.isNotBlank() }?.area.orEmpty()
            val sampleDepot = subRows.firstOrNull { it.depot.isNotBlank() }?.depot.orEmpty()

            val totalTpSales = salesRows.sumOf { it.tpSales.coerceAtLeast(0.0) }
            // Dues uses TP+Vat only, never TP sales
            val totalTpPlusVat = salesRows.sumOf { it.tpPlusVat.coerceAtLeast(0.0) }
            val approvedEntries = subRows.filter { it.resolvedStatus == TakaJomaStatus.APPROVED }
            val pendingEntries = subRows.filter { it.resolvedStatus == TakaJomaStatus.PENDING }
            val rejectedEntries = subRows.filter { it.resolvedStatus == TakaJomaStatus.REJECTED }
            val approvedJoma = approvedEntries.sumOf { it.amount.coerceAtLeast(0.0) }
            val pendingJoma = pendingEntries.sumOf { it.amount.coerceAtLeast(0.0) }
            val rejectedJoma = rejectedEntries.sumOf { it.amount.coerceAtLeast(0.0) }

            summaries.add(
                MemberSalesDuesSummary(
                    memberId = "unlinked_$key",
                    code = sampleCode.trim(),
                    name = sampleName.trim().ifBlank { sampleCode.trim() },
                    area = sampleArea.trim(),
                    depot = sampleDepot.trim(),
                    uploadedTpSales = totalTpSales,
                    uploadedSales = totalTpPlusVat,
                    manualSales = 0.0,
                    totalSales = totalTpPlusVat,
                    approvedJoma = approvedJoma,
                    pendingJoma = pendingJoma,
                    rejectedJoma = rejectedJoma,
                    approvedCount = approvedEntries.size,
                    pendingCount = pendingEntries.size,
                    dues = (totalTpPlusVat - approvedJoma).coerceAtLeast(0.0),
                    advance = (approvedJoma - totalTpPlusVat).coerceAtLeast(0.0)
                )
            )
        }

        return summaries.sortedWith(
            compareByDescending<MemberSalesDuesSummary> { it.dues }
                .thenByDescending { it.totalSales }
                .thenBy { it.name.lowercase() }
        )
    }

    fun calculateTotals(summaries: List<MemberSalesDuesSummary>): MemberSalesDuesTotals {
        return MemberSalesDuesTotals(
            totalTpSales = summaries.sumOf { it.uploadedTpSales },
            totalSales = summaries.sumOf { it.totalSales },
            totalApprovedJoma = summaries.sumOf { it.approvedJoma },
            totalPendingJoma = summaries.sumOf { it.pendingJoma },
            totalDues = summaries.sumOf { it.dues },
            totalAdvance = summaries.sumOf { it.advance }
        )
    }
}
