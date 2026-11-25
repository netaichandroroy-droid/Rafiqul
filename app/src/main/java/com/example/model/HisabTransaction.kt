package com.example.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TxType(val wireValue: String, val bnLabel: String) {
    INCOME("income", "Income"),
    EXPENSE("expense", "Expense");

    companion object {
        fun fromRaw(raw: String?): TxType =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: EXPENSE
    }
}

enum class AccountType(val wireValue: String, val bnTitle: String, val bnShort: String) {
    CASH("cash", "Cash", "Cash"),
    MOBILE_BANK("mobile_bank", "Mobile Bank", "Mobile"),
    BANK("bank", "Bank", "Bank");

    companion object {
        fun fromRaw(raw: String?): AccountType =
            entries.firstOrNull { it.wireValue.equals(raw, ignoreCase = true) } ?: CASH
    }
}

enum class TxFilter(val bnLabel: String) {
    ALL("All"),
    INCOME("Income"),
    EXPENSE("Expense")
}

object HisabCategories {
    val incomeCategories = listOf(
        "Salary",
        "Business",
        "Gift",
        "Investment",
        "Other Income"
    )

    val expenseCategories = listOf(
        "Food",
        "Groceries",
        "Transport",
        "House Rent",
        "Utilities",
        "Medical",
        "Education",
        "Shopping",
        "Entertainment",
        "Other"
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
