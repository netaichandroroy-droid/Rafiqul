package com.example.model

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
    val createdAtMs: Long = System.currentTimeMillis()
)

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
        get() = "$bnName ($enName)"
}

object BangladeshBankDirectory {
    val CASH_OPTIONS: List<BankItem> = listOf(
        BankItem(
            bnName = "ক্যাশ",
            enName = "Cash",
            shortCode = "CASH",
            isMobileBanking = false,
            searchAliases = "cash hand cash টাকা ক্যাশ",
            category = PaymentMethodCategory.CASH
        )
    )

    val MOBILE_BANKING_OPTIONS: List<BankItem> = listOf(
        BankItem(
            bnName = "নগদ",
            enName = "Nagad",
            shortCode = "NAGAD",
            isMobileBanking = true,
            searchAliases = "nogod nagad mfs",
            category = PaymentMethodCategory.MFS
        ),
        BankItem(
            bnName = "বিকাশ",
            enName = "bKash",
            shortCode = "BKASH",
            isMobileBanking = true,
            searchAliases = "bikash bkash mfs",
            category = PaymentMethodCategory.MFS
        )
    )

    val ALL_BANKS: List<BankItem> = listOf(
        BankItem("সোনালী ব্যাংক", "Sonali Bank PLC", "SONALI"),
        BankItem("জনতা ব্যাংক", "Janata Bank PLC", "JANATA"),
        BankItem("অগ্রণী ব্যাংক", "Agrani Bank PLC", "AGRANI"),
        BankItem("রূপালী ব্যাংক", "Rupali Bank PLC", "RUPALI"),
        BankItem("বাংলাদেশ কৃষি ব্যাংক", "Bangladesh Krishi Bank", "BKB"),
        BankItem("রাজশাহী কৃষি উন্নয়ন ব্যাংক", "RAKUB", "RAKUB"),
        BankItem("বেসিক ব্যাংক", "BASIC Bank Ltd", "BASIC"),
        BankItem("ইসলামী ব্যাংক বাংলাদেশ", "Islami Bank Bangladesh PLC", "IBBL"),
        BankItem("ডাচ-বাংলা ব্যাংক", "Dutch-Bangla Bank PLC", "DBBL"),
        BankItem("ব্র্যাক ব্যাংক", "BRAC Bank PLC", "BRAC"),
        BankItem("পূবালী ব্যাংক", "Pubali Bank PLC", "PUBALI"),
        BankItem("দি সিটি ব্যাংক", "The City Bank PLC", "CITY"),
        BankItem("ইস্টার্ন ব্যাংক", "Eastern Bank PLC", "EBL"),
        BankItem("প্রাইম ব্যাংক", "Prime Bank PLC", "PRIME"),
        BankItem("ইউনাইটেড কমার্শিয়াল ব্যাংক", "UCB PLC", "UCB"),
        BankItem("ব্যাংক এশিয়া", "Bank Asia PLC", "ASIA"),
        BankItem("মিউচুয়াল ট্রাস্ট ব্যাংক", "Mutual Trust Bank PLC", "MTB"),
        BankItem("ট্রাস্ট ব্যাংক", "Trust Bank PLC", "TRUST"),
        BankItem("মার্কেন্টাইল ব্যাংক", "Mercantile Bank PLC", "MBL"),
        BankItem("সাউথইস্ট ব্যাংক", "Southeast Bank PLC", "SEBL"),
        BankItem("যমুনা ব্যাংক", "Jamuna Bank PLC", "JBL"),
        BankItem("ওয়ান ব্যাংক", "ONE Bank PLC", "ONE"),
        BankItem("ঢাকা ব্যাংক", "Dhaka Bank PLC", "DBL"),
        BankItem("উত্তরা ব্যাংক", "Uttara Bank PLC", "UTTARA"),
        BankItem("ন্যাশনাল ব্যাংক", "National Bank Ltd", "NBL"),
        BankItem("আল-আরাফাহ্ ইসলামী ব্যাংক", "Al-Arafah Islami Bank PLC", "AIBL"),
        BankItem("এক্সিম ব্যাংক", "EXIM Bank PLC", "EXIM"),
        BankItem("শাহ্জালাল ইসলামী ব্যাংক", "Shahjalal Islami Bank PLC", "SJIBL"),
        BankItem("ফার্স্ট সিকিউরিটি ইসলামী ব্যাংক", "First Security Islami Bank", "FSIBL"),
        BankItem("সোশ্যাল ইসলামী ব্যাংক", "Social Islami Bank PLC", "SIBL"),
        BankItem("এবি ব্যাংক", "AB Bank PLC", "ABBL"),
        BankItem("এনসিসি ব্যাংক", "NCC Bank PLC", "NCC"),
        BankItem("প্রিমিয়ার ব্যাংক", "Premier Bank PLC", "PREMIER"),
        BankItem("স্ট্যান্ডার্ড ব্যাংক", "Standard Bank PLC", "SBL"),
        BankItem("আইএফআইসি ব্যাংক", "IFIC Bank PLC", "IFIC"),
        BankItem("এনআরবিসি ব্যাংক", "NRBC Bank PLC", "NRBC"),
        BankItem("এনআরবি ব্যাংক", "NRB Bank PLC", "NRB"),
        BankItem("মিডল্যান্ড ব্যাংক", "Midland Bank PLC", "MDB"),
        BankItem("মধুমতি ব্যাংক", "Modhumoti Bank PLC", "MODHU"),
        BankItem("সাউথ বাংলা এগ্রিকালচার ব্যাংক", "SBAC Bank PLC", "SBAC"),
        BankItem("পদ্মা ব্যাংক", "Padma Bank PLC", "PADMA"),
        BankItem("মেঘনা ব্যাংক", "Meghna Bank PLC", "MEGHNA"),
        BankItem("সীমান্ত ব্যাংক", "Shimanto Bank PLC", "SHIMANTO"),
        BankItem("কমিউনিটি ব্যাংক", "Community Bank Bangladesh", "CBBL"),
        BankItem("বেঙ্গল কমার্শিয়াল ব্যাংক", "Bengal Commercial Bank", "BGCB"),
        BankItem("গ্লোবাল ইসলামী ব্যাংক", "Global Islami Bank PLC", "GIB"),
        BankItem("ইউনিয়ন ব্যাংক", "Union Bank PLC", "UNION"),
        BankItem("সিটিজেনস ব্যাংক", "Citizens Bank PLC", "CITIZENS"),
        BankItem("স্ট্যান্ডার্ড চার্টার্ড ব্যাংক", "Standard Chartered Bank", "SCB"),
        BankItem("এইচএসবিসি ব্যাংক", "HSBC Bangladesh", "HSBC")
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
