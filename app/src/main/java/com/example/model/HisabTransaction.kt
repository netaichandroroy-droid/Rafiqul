package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TxType(val wireValue: String, val bnLabel: String) {
    INCOME("income", "আয়"),
    EXPENSE("expense", "খরচ");

    companion object {
        fun fromRaw(raw: String?): TxType =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: EXPENSE
    }
}

enum class AccountType(val wireValue: String, val bnTitle: String, val bnShort: String) {
    CASH("cash", "নগদ টাকা", "নগদ"),
    BANK("bank", "ব্যাংক", "ব্যাংক");

    companion object {
        fun fromRaw(raw: String?): AccountType =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: CASH
    }
}

enum class TxFilter(val bnLabel: String) {
    ALL("সব"),
    INCOME("আয়"),
    EXPENSE("খরচ")
}

object HisabCategories {
    val incomeCategories = listOf(
        "বেতন",
        "ব্যবসা",
        "উপহার",
        "বিনিয়োগ",
        "অন্যান্য আয়"
    )

    val expenseCategories = listOf(
        "খাবার",
        "বাজার",
        "যাতায়াত",
        "বাসা ভাড়া",
        "বিদ্যুৎ/গ্যাস বিল",
        "চিকিৎসা",
        "শিক্ষা",
        "কেনাকাটা",
        "বিনোদন",
        "অন্যান্য"
    )

    fun forType(type: TxType): List<String> =
        if (type == TxType.INCOME) incomeCategories else expenseCategories
}

@Keep
@IgnoreExtraProperties
data class HisabTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = TxType.EXPENSE.wireValue,
    val amount: Double = 0.0,
    val category: String = "",
    val note: String = "",
    val date: String = "",
    val account: String = AccountType.CASH.wireValue,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val resolvedType: TxType
        get() = TxType.fromRaw(type)

    @get:Exclude
    val resolvedAccount: AccountType
        get() = AccountType.fromRaw(account)

    @get:Exclude
    val displayTitle: String
        get() = note.ifBlank { category.ifBlank { resolvedType.bnLabel } }
}

data class ImportedTransaction(
    val type: TxType,
    val amount: Double,
    val category: String,
    val note: String,
    val date: String,
    val account: AccountType
)
