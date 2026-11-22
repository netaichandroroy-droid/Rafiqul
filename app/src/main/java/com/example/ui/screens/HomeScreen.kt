package com.example.ui.screens

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AccountType
import com.example.model.BanglaFormatters
import com.example.model.HisabCategories
import com.example.model.HisabTransaction
import com.example.model.TxType
import com.example.ui.components.EmptyTransactionsState
import com.example.ui.components.HeroBalanceCard
import com.example.ui.components.SectionPanelCard
import com.example.ui.components.TransactionFormFields
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.DarkExpenseSurface
import com.example.ui.theme.DarkExpenseText
import com.example.ui.theme.DarkIncomeSurface
import com.example.ui.theme.DarkIncomeText
import com.example.ui.theme.ExpenseCrimsonDark
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmeraldDark
import com.example.ui.theme.IncomeSurface

@Composable
fun HomeScreen(
    transactions: List<HisabTransaction>,
    isSaving: Boolean,
    onSaveInlineTransaction: (
        type: TxType,
        amount: Double,
        category: String,
        note: String,
        date: String,
        account: AccountType,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onEdit: (HisabTransaction) -> Unit,
    onDelete: (HisabTransaction) -> Unit,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIncome = remember(transactions) {
        transactions.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }
    val balance = totalIncome - totalExpense

    val todayIso = remember { BanglaFormatters.todayIso() }
    val todayIncome = remember(transactions, todayIso) {
        transactions.filter { it.date == todayIso && it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val todayExpense = remember(transactions, todayIso) {
        transactions.filter { it.date == todayIso && it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }
    val recent = remember(transactions) { transactions.take(5) }

    val incomeSurface = IncomeSurface
    val incomeText = IncomeEmeraldDark
    val expenseSurface = ExpenseSurface
    val expenseText = ExpenseCrimsonDark

    var inlineType by remember { mutableStateOf(TxType.EXPENSE) }
    var inlineAmount by remember { mutableStateOf("") }
    var inlineCategory by remember { mutableStateOf(HisabCategories.forType(TxType.EXPENSE).first()) }
    var inlineNote by remember { mutableStateOf("") }
    var inlineDate by remember { mutableStateOf(todayIso) }
    var inlineAccount by remember { mutableStateOf(AccountType.CASH) }
    var inlineError by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            HeroBalanceCard(
                balance = balance,
                totalIncome = totalIncome,
                totalExpense = totalExpense
            )
        }

        // Today's BDT (৳) Income & Expense summary card
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "আজকের হিসাব (${BanglaFormatters.formatBanglaDate(todayIso)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = incomeSurface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "আজকের আয়",
                                style = MaterialTheme.typography.labelSmall,
                                color = incomeText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BanglaFormatters.formatBdt(todayIncome),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = incomeText
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = expenseSurface
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "আজকের খরচ",
                                style = MaterialTheme.typography.labelSmall,
                                color = expenseText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BanglaFormatters.formatBdt(todayExpense),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = expenseText
                            )
                        }
                    }
                }
            }
        }

        // Direct Daily Income & Expense Entry Form with BDT (৳) currency display
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "দৈনিক আয় ও খরচ এন্ট্রি ফর্ম",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "টাকার পরিমাণ (৳), তারিখ, ক্যাটাগরি ও নোট দিয়ে সংরক্ষণ করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                TransactionFormFields(
                    selectedType = inlineType,
                    onTypeChange = { txType ->
                        inlineType = txType
                        val list = HisabCategories.forType(txType)
                        if (inlineCategory !in list) {
                            inlineCategory = list.first()
                        }
                    },
                    amountText = inlineAmount,
                    onAmountChange = {
                        inlineAmount = it
                        inlineError = null
                    },
                    selectedCategory = inlineCategory,
                    onCategoryChange = { inlineCategory = it },
                    noteText = inlineNote,
                    onNoteChange = { inlineNote = it },
                    dateIso = inlineDate,
                    onDateChange = { inlineDate = it },
                    selectedAccount = inlineAccount,
                    onAccountChange = { inlineAccount = it },
                    validationError = inlineError,
                    isSaving = isSaving,
                    submitLabel = stringResource(R.string.save_transaction),
                    tagPrefix = "inline",
                    onSubmit = {
                        val amount = inlineAmount.toDoubleOrNull()
                        if (amount == null || amount <= 0.0 || amount > 1_000_000_000.0) {
                            inlineError = "অনুগ্রহ করে সঠিক টাকার পরিমাণ লিখুন।"
                            return@TransactionFormFields
                        }
                        onSaveInlineTransaction(
                            inlineType,
                            amount,
                            inlineCategory,
                            inlineNote,
                            inlineDate,
                            inlineAccount
                        ) { ok ->
                            if (ok) {
                                inlineAmount = ""
                                inlineNote = ""
                                inlineError = null
                            }
                        }
                    }
                )
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_transactions),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(
                        onClick = onSeeAll,
                        modifier = Modifier.testTag("see_all_transactions_button")
                    ) {
                        Text(text = stringResource(R.string.see_all))
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (recent.isEmpty()) {
                    EmptyTransactionsState()
                } else {
                    recent.forEachIndexed { index, tx ->
                        TransactionItemRow(
                            transaction = tx,
                            onEdit = onEdit,
                            onDelete = onDelete,
                            showDivider = index < recent.lastIndex
                        )
                    }
                }
            }
        }
    }
}
