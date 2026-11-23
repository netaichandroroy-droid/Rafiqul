package com.example.model

enum class TakaJomaStatus(
    val wireValue: String,
    val bnLabel: String
) {
    PENDING("PENDING", "Pending"),
    APPROVED("APPROVED", "Approved"),
    REJECTED("REJECTED", "Rejected");

    companion object {
        fun fromRaw(raw: String?): TakaJomaStatus {
            val cleaned = raw?.trim().orEmpty()
            return when {
                cleaned.equals(PENDING.wireValue, ignoreCase = true) ||
                    cleaned.equals(PENDING.bnLabel, ignoreCase = true) ||
                    cleaned == "\u0985\u09AA\u09C7\u0995\u09CD\u09B7\u09AE\u09BE\u09A3" -> PENDING
                cleaned.equals(REJECTED.wireValue, ignoreCase = true) ||
                    cleaned.equals(REJECTED.bnLabel, ignoreCase = true) ||
                    cleaned == "\u09AC\u09BE\u09A4\u09BF\u09B2" -> REJECTED
                else -> APPROVED
            }
        }
    }
}

data class MemberTakaJoma(
    val id: String,
    val memberId: String,
    val name: String,
    val code: String,
    val depot: String,
    val designation: String = "",
    val area: String = "",
    val bankName: String = "",
    val amount: Double,
    val date: String,
    val note: String = "",
    val photoPath: String = "",
    val slipHash: String = "",
    val submittedByRole: String = "MEMBER",
    val createdAtMs: Long = System.currentTimeMillis(),
    val approvalStatus: String = TakaJomaStatus.APPROVED.wireValue,
    val reviewedAtMs: Long = 0L
) {
    val resolvedStatus: TakaJomaStatus
        get() = TakaJomaStatus.fromRaw(approvalStatus)
}

enum class PaymentMethodCategory {
    CASH,
    MFS,
    BANK
}

data class BankItem(
    val bnName: String,
    val enName: String,
    val shortCode: String,
    val isMobileBanking: Boolean = false,
    val searchAliases: String = "",
    val category: PaymentMethodCategory = PaymentMethodCategory.BANK
) {
    val displayTitle: String
        get() = if (enName.isNotBlank()) enName else bnName
}

object BangladeshBankDirectory {
    val CASH_OPTIONS: List<BankItem> = listOf(
        BankItem(
            bnName = "Cash",
            enName = "Cash",
            shortCode = "CASH",
            isMobileBanking = false,
            searchAliases = "cash hand cash deposit",
            category = PaymentMethodCategory.CASH
        )
    )

    val MOBILE_BANKING_OPTIONS: List<BankItem> = listOf(
        BankItem(
            bnName = "Nagad",
            enName = "Nagad",
            shortCode = "NAGAD",
            isMobileBanking = true,
            searchAliases = "nogod nagad mfs",
            category = PaymentMethodCategory.MFS
        ),
        BankItem(
            bnName = "bKash",
            enName = "bKash",
            shortCode = "BKASH",
            isMobileBanking = true,
            searchAliases = "bikash bkash mfs",
            category = PaymentMethodCategory.MFS
        )
    )

    val ALL_BANKS: List<BankItem> = listOf(
        BankItem("Sonali Bank PLC", "Sonali Bank PLC", "SONALI"),
        BankItem("Janata Bank PLC", "Janata Bank PLC", "JANATA"),
        BankItem("Agrani Bank PLC", "Agrani Bank PLC", "AGRANI"),
        BankItem("Rupali Bank PLC", "Rupali Bank PLC", "RUPALI"),
        BankItem("Bangladesh Krishi Bank", "Bangladesh Krishi Bank", "BKB"),
        BankItem("RAKUB", "RAKUB", "RAKUB"),
        BankItem("BASIC Bank Ltd", "BASIC Bank Ltd", "BASIC"),
        BankItem("Islami Bank Bangladesh PLC", "Islami Bank Bangladesh PLC", "IBBL"),
        BankItem("Dutch-Bangla Bank PLC", "Dutch-Bangla Bank PLC", "DBBL"),
        BankItem("BRAC Bank PLC", "BRAC Bank PLC", "BRAC"),
        BankItem("Pubali Bank PLC", "Pubali Bank PLC", "PUBALI"),
        BankItem("The City Bank PLC", "The City Bank PLC", "CITY"),
        BankItem("Eastern Bank PLC", "Eastern Bank PLC", "EBL"),
        BankItem("Prime Bank PLC", "Prime Bank PLC", "PRIME"),
        BankItem("UCB PLC", "UCB PLC", "UCB"),
        BankItem("Bank Asia PLC", "Bank Asia PLC", "ASIA"),
        BankItem("Mutual Trust Bank PLC", "Mutual Trust Bank PLC", "MTB"),
        BankItem("Trust Bank PLC", "Trust Bank PLC", "TRUST"),
        BankItem("Mercantile Bank PLC", "Mercantile Bank PLC", "MBL"),
        BankItem("Southeast Bank PLC", "Southeast Bank PLC", "SEBL"),
        BankItem("Jamuna Bank PLC", "Jamuna Bank PLC", "JBL"),
        BankItem("ONE Bank PLC", "ONE Bank PLC", "ONE"),
        BankItem("Dhaka Bank PLC", "Dhaka Bank PLC", "DBL"),
        BankItem("Uttara Bank PLC", "Uttara Bank PLC", "UTTARA"),
        BankItem("National Bank Ltd", "National Bank Ltd", "NBL"),
        BankItem("Al-Arafah Islami Bank PLC", "Al-Arafah Islami Bank PLC", "AIBL"),
        BankItem("EXIM Bank PLC", "EXIM Bank PLC", "EXIM"),
        BankItem("Shahjalal Islami Bank PLC", "Shahjalal Islami Bank PLC", "SJIBL"),
        BankItem("First Security Islami Bank", "First Security Islami Bank", "FSIBL"),
        BankItem("Social Islami Bank PLC", "Social Islami Bank PLC", "SIBL"),
        BankItem("AB Bank PLC", "AB Bank PLC", "ABBL"),
        BankItem("NCC Bank PLC", "NCC Bank PLC", "NCC"),
        BankItem("Premier Bank PLC", "Premier Bank PLC", "PREMIER"),
        BankItem("Standard Bank PLC", "Standard Bank PLC", "SBL"),
        BankItem("IFIC Bank PLC", "IFIC Bank PLC", "IFIC"),
        BankItem("NRBC Bank PLC", "NRBC Bank PLC", "NRBC"),
        BankItem("NRB Bank PLC", "NRB Bank PLC", "NRB"),
        BankItem("Midland Bank PLC", "Midland Bank PLC", "MDB"),
        BankItem("Modhumoti Bank PLC", "Modhumoti Bank PLC", "MODHU"),
        BankItem("SBAC Bank PLC", "SBAC Bank PLC", "SBAC"),
        BankItem("Padma Bank PLC", "Padma Bank PLC", "PADMA"),
        BankItem("Meghna Bank PLC", "Meghna Bank PLC", "MEGHNA"),
        BankItem("Shimanto Bank PLC", "Shimanto Bank PLC", "SHIMANTO"),
        BankItem("Community Bank Bangladesh", "Community Bank Bangladesh", "CBBL"),
        BankItem("Bengal Commercial Bank", "Bengal Commercial Bank", "BGCB"),
        BankItem("Global Islami Bank PLC", "Global Islami Bank PLC", "GIB"),
        BankItem("Union Bank PLC", "Union Bank PLC", "UNION"),
        BankItem("Citizens Bank PLC", "Citizens Bank PLC", "CITIZENS"),
        BankItem("Standard Chartered Bank", "Standard Chartered Bank", "SCB"),
        BankItem("HSBC Bangladesh", "HSBC Bangladesh", "HSBC")
    )

    val ALL_PAYMENT_METHODS: List<BankItem> = CASH_OPTIONS + MOBILE_BANKING_OPTIONS + ALL_BANKS

    fun filterPaymentMethods(query: String): List<BankItem> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return ALL_PAYMENT_METHODS
        return ALL_PAYMENT_METHODS.filter { item ->
            item.bnName.contains(trimmed, ignoreCase = true) ||
                item.enName.contains(trimmed, ignoreCase = true) ||
                item.shortCode.contains(trimmed, ignoreCase = true) ||
                item.searchAliases.contains(trimmed, ignoreCase = true)
        }
    }
}
