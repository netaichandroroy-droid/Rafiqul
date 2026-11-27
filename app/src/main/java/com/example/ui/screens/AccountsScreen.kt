package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AccountType
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.TxType
import com.example.ui.components.GlossyNeonIconOrb
import com.example.ui.components.UnifiedPageCardSurface
import com.example.ui.components.UnifiedPageSectionDivider
import com.example.ui.theme.CardBorderSolid
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald

@Composable
fun AccountsScreen(
    transactions: List<HisabTransaction>,
    modifier: Modifier = Modifier
) {
    val incomeColor = IncomeEmerald
    val expenseColor = ExpenseCrimson

    val totalIncome = remember(transactions) {
        transactions.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }
    val netBalance = totalIncome - totalExpense
    val bankNetBalance = remember(transactions) {
        val bankItems = transactions.filter { it.resolvedAccount == AccountType.BANK }
        val bankIncome = bankItems.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
        val bankExpense = bankItems.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
        bankIncome - bankExpense
    }

    UnifiedPageCardSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .testTag("accounts_total_hero_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.accounts_summary),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.06f)
                    ) {
                        Text(
                            text = BanglaFormatters.formatBdt(netBalance),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                AccountListDivider()

                AccountType.entries.forEachIndexed { index, accountType ->
                    val accountItems = transactions.filter { it.resolvedAccount == accountType }
                    val income = accountItems.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
                    val expense = accountItems.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
                    val balance = income - expense

                    AccountListRow(
                        icon = if (accountType == AccountType.CASH) {
                            Icons.Outlined.Payments
                        } else {
                            Icons.Outlined.AccountBalance
                        },
                        title = accountType.bnTitle,
                        txCount = accountItems.size,
                        income = income,
                        expense = expense,
                        balance = balance,
                        incomeColor = incomeColor,
                        expenseColor = expenseColor,
                        modifier = Modifier.testTag("account_card_${accountType.wireValue}")
                    )

                    if (index < AccountType.entries.lastIndex) {
                        AccountListDivider()
                    }
                }
            }

            UnifiedPageSectionDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = "Overall Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                AccountListDivider()

                AccountSummaryListRow(
                    icon = Icons.Outlined.ArrowDownward,
                    iconBg = IncomeSurface,
                    iconTint = IncomeEmerald,
                    label = "Total Income (All Accounts)",
                    value = BanglaFormatters.formatBdt(totalIncome),
                    valueColor = incomeColor
                )
                AccountListDivider()

                AccountSummaryListRow(
                    icon = Icons.Outlined.ArrowUpward,
                    iconBg = ExpenseSurface,
                    iconTint = ExpenseCrimson,
                    label = "Total Expense (All Accounts)",
                    value = BanglaFormatters.formatBdt(totalExpense),
                    valueColor = expenseColor
                )
                AccountListDivider()

                AccountSummaryListRow(
                    icon = Icons.Outlined.AccountBalance,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = "All Banks Combined Balance",
                    value = BanglaFormatters.formatBdt(bankNetBalance),
                    valueColor = if (bankNetBalance >= 0) MaterialTheme.colorScheme.primary else expenseColor,
                    modifier = Modifier.testTag("all_banks_combined_total_row")
                )
                AccountListDivider()

                AccountSummaryListRow(
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconBg = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    label = "Remaining Net Balance",
                    value = BanglaFormatters.formatBdt(netBalance),
                    valueColor = if (netBalance >= 0) MaterialTheme.colorScheme.primary else expenseColor,
                    modifier = Modifier.testTag("all_accounts_grand_total_row")
                )
            }
        }
    }
}

@Composable
private fun AccountListDivider() {
    HorizontalDivider(
        thickness = 0.75.dp,
        color = CardBorderSolid.copy(alpha = 0.35f)
    )
}

@Composable
private fun AccountListRow(
    icon: ImageVector,
    title: String,
    txCount: Int,
    income: Double,
    expense: Double,
    balance: Double,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlossyNeonIconOrb(
            icon = icon,
            contentDescription = title,
            glowColor = if (balance >= 0) NeonCyan else ExpenseCrimson,
            iconTint = Color.White,
            size = 42.dp,
            iconSize = 20.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• $txCount entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Income: ${BanglaFormatters.formatBdt(income)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = incomeColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Expense: ${BanglaFormatters.formatBdt(expense)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = expenseColor,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = BanglaFormatters.formatBdt(balance),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (balance >= 0) MaterialTheme.colorScheme.onSurface else expenseColor,
                textAlign = TextAlign.End
            )
            Text(
                text = "Balance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountSummaryListRow(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GlossyNeonIconOrb(
            icon = icon,
            contentDescription = null,
            glowColor = iconTint,
            iconTint = Color.White,
            size = 38.dp,
            iconSize = 18.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}
