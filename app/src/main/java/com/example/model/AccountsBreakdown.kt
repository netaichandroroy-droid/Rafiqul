package com.example.model

data class AccountTypeSummary(
    val accountType: AccountType,
    val txCount: Int,
    val income: Double,
    val expense: Double
) {
    val balance: Double get() = income - expense
}

data class AccountChannelTotal(
    val label: String,
    val accountType: AccountType,
    val txCount: Int,
    val income: Double,
    val expense: Double
) {
    val balance: Double get() = income - expense
}

data class AccountsBreakdownSummary(
    val cashSummary: AccountTypeSummary,
    val mobileBankSummary: AccountTypeSummary,
    val bankSummary: AccountTypeSummary,
    val mobileBankChannels: List<AccountChannelTotal>,
    val bankWiseChannels: List<AccountChannelTotal>,
    val totalAllBanksOnly: Double,
    val totalMobileAndBanks: Double,
    val totalIncome: Double,
    val totalExpense: Double,
    val grandTotalBalance: Double
)

object AccountsBreakdownCalculator {

    private val TAKA_JOMA_PREFIX = "taka_joma_"

    fun build(
        transactions: List<HisabTransaction>,
        submissions: List<MemberTakaJoma> = emptyList()
    ): AccountsBreakdownSummary {
        val submissionById = submissions.associateBy { it.id }

        var cashCount = 0
        var cashIncome = 0.0
        var cashExpense = 0.0

        var mobileCount = 0
        var mobileIncome = 0.0
        var mobileExpense = 0.0

        var bankCount = 0
        var bankIncome = 0.0
        var bankExpense = 0.0

        val mobileChannelMap = linkedMapOf<String, ChannelAccumulator>()
        val bankChannelMap = linkedMapOf<String, ChannelAccumulator>()

        for (tx in transactions) {
            val amount = tx.amount.coerceAtLeast(0.0)
            val isIncome = tx.resolvedType == TxType.INCOME
            val resolved = resolveChannel(tx, submissionById)

            when (resolved.accountType) {
                AccountType.CASH -> {
                    cashCount += 1
                    if (isIncome) cashIncome += amount else cashExpense += amount
                }
                AccountType.MOBILE_BANK -> {
                    mobileCount += 1
                    if (isIncome) mobileIncome += amount else mobileExpense += amount
                    val acc = mobileChannelMap.getOrPut(resolved.channelLabel) {
                        ChannelAccumulator(resolved.channelLabel, AccountType.MOBILE_BANK)
                    }
                    acc.txCount += 1
                    if (isIncome) acc.income += amount else acc.expense += amount
                }
                AccountType.BANK -> {
                    bankCount += 1
                    if (isIncome) bankIncome += amount else bankExpense += amount
                    val acc = bankChannelMap.getOrPut(resolved.channelLabel) {
                        ChannelAccumulator(resolved.channelLabel, AccountType.BANK)
                    }
                    acc.txCount += 1
                    if (isIncome) acc.income += amount else acc.expense += amount
                }
            }
        }

        val cashSummary = AccountTypeSummary(AccountType.CASH, cashCount, cashIncome, cashExpense)
        val mobileSummary = AccountTypeSummary(AccountType.MOBILE_BANK, mobileCount, mobileIncome, mobileExpense)
        val bankSummary = AccountTypeSummary(AccountType.BANK, bankCount, bankIncome, bankExpense)

        val mobileChannels = mobileChannelMap.values
            .map { it.toChannelTotal() }
            .sortedByDescending { kotlin.math.abs(it.balance) }
        val bankChannels = bankChannelMap.values
            .map { it.toChannelTotal() }
            .sortedByDescending { kotlin.math.abs(it.balance) }

        val totalIncome = cashIncome + mobileIncome + bankIncome
        val totalExpense = cashExpense + mobileExpense + bankExpense
        val totalAllBanksOnly = bankSummary.balance
        val totalMobileAndBanks = mobileSummary.balance + bankSummary.balance
        val grandTotal = cashSummary.balance + mobileSummary.balance + bankSummary.balance

        return AccountsBreakdownSummary(
            cashSummary = cashSummary,
            mobileBankSummary = mobileBankSummaryOrFallback(mobileSummary),
            bankSummary = bankSummary,
            mobileBankChannels = mobileChannels,
            bankWiseChannels = bankChannels,
            totalAllBanksOnly = totalAllBanksOnly,
            totalMobileAndBanks = totalMobileAndBanks,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            grandTotalBalance = grandTotal
        )
    }

    private fun mobileBankSummaryOrFallback(summary: AccountTypeSummary): AccountTypeSummary = summary

    private data class ResolvedChannel(
        val accountType: AccountType,
        val channelLabel: String
    )

    private class ChannelAccumulator(
        val label: String,
        val accountType: AccountType,
        var txCount: Int = 0,
        var income: Double = 0.0,
        var expense: Double = 0.0
    ) {
        fun toChannelTotal() = AccountChannelTotal(
            label = label,
            accountType = accountType,
            txCount = txCount,
            income = income,
            expense = expense
        )
    }

    private fun resolveChannel(
        tx: HisabTransaction,
        submissionById: Map<String, MemberTakaJoma>
    ): ResolvedChannel {
        if (tx.id.startsWith(TAKA_JOMA_PREFIX)) {
            val subId = tx.id.removePrefix(TAKA_JOMA_PREFIX)
            val sub = submissionById[subId]
            if (sub != null && sub.bankName.isNotBlank()) {
                return classifyMethodName(sub.bankName)
            }
            val noteMethod = extractMethodFromLinkedNote(tx.note)
            if (noteMethod.isNotBlank()) {
                return classifyMethodName(noteMethod)
            }
        }

        return when (tx.resolvedAccount) {
            AccountType.CASH -> ResolvedChannel(AccountType.CASH, "Cash")
            AccountType.MOBILE_BANK -> {
                val matchedMfs = matchKnownMobileBank("${tx.note} ${tx.category}")
                ResolvedChannel(AccountType.MOBILE_BANK, matchedMfs ?: "Mobile Bank")
            }
            AccountType.BANK -> {
                val matchedBank = matchKnownBank("${tx.note} ${tx.category}")
                if (matchedBank != null) {
                    ResolvedChannel(AccountType.BANK, matchedBank)
                } else {
                    val matchedMfs = matchKnownMobileBank("${tx.note} ${tx.category}")
                    if (matchedMfs != null && tx.id.startsWith(TAKA_JOMA_PREFIX)) {
                        ResolvedChannel(AccountType.MOBILE_BANK, matchedMfs)
                    } else {
                        ResolvedChannel(AccountType.BANK, "General Bank")
                    }
                }
            }
        }
    }

    private fun extractMethodFromLinkedNote(note: String): String {
        val parts = note.split("•").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return ""
        val candidate = parts[1]
        if (candidate.startsWith("Submitted:", ignoreCase = true)) return ""
        return candidate
    }

    private fun classifyMethodName(rawName: String): ResolvedChannel {
        val cleaned = rawName.trim()
        val methodShort = cleaned.substringBefore(" (").trim()
        if (cleaned.isEmpty() ||
            methodShort.equals("Cash", ignoreCase = true) ||
            cleaned.contains("Cash", ignoreCase = true) ||
            cleaned.contains("\u0995\u09CD\u09AF\u09BE\u09B6")
        ) {
            return ResolvedChannel(AccountType.CASH, "Cash")
        }

        val matchedMfs = matchKnownMobileBank(cleaned)
        if (matchedMfs != null) {
            return ResolvedChannel(AccountType.MOBILE_BANK, matchedMfs)
        }
        if (cleaned.contains("Mobile Bank", ignoreCase = true) ||
            cleaned.contains("MFS", ignoreCase = true) ||
            cleaned.contains("Rocket", ignoreCase = true) ||
            cleaned.contains("Upay", ignoreCase = true)
        ) {
            return ResolvedChannel(AccountType.MOBILE_BANK, methodShort.ifBlank { "Mobile Bank" })
        }

        val matchedBank = matchKnownBank(cleaned)
        return ResolvedChannel(AccountType.BANK, matchedBank ?: methodShort.ifBlank { "General Bank" })
    }

    private fun matchKnownMobileBank(text: String): String? {
        for (item in BangladeshBankDirectory.MOBILE_BANKING_OPTIONS) {
            if (text.contains(item.enName, ignoreCase = true) ||
                text.contains(item.bnName, ignoreCase = true) ||
                text.contains(item.shortCode, ignoreCase = true)
            ) {
                return item.enName
            }
        }
        if (text.contains("bKash", ignoreCase = true) || text.contains("bikash", ignoreCase = true)) {
            return "bKash"
        }
        if (text.contains("Nagad", ignoreCase = true) || text.contains("nogod", ignoreCase = true)) {
            return "Nagad"
        }
        if (text.contains("Rocket", ignoreCase = true)) return "Rocket"
        if (text.contains("Upay", ignoreCase = true)) return "Upay"
        return null
    }

    private fun matchKnownBank(text: String): String? {
        for (item in BangladeshBankDirectory.ALL_BANKS) {
            val baseName = item.enName
                .removeSuffix(" PLC")
                .removeSuffix(" Ltd")
                .trim()
            if (text.contains(item.enName, ignoreCase = true) ||
                (baseName.length >= 4 && text.contains(baseName, ignoreCase = true)) ||
                (item.shortCode.length >= 3 && Regex("""\b${Regex.escape(item.shortCode)}\b""", RegexOption.IGNORE_CASE).containsMatchIn(text))
            ) {
                return item.enName
            }
        }
        return null
    }
}
