package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.MemberTakaJoma
import com.example.model.TxType
import com.example.ui.components.AdminMemberTakaJomaTableCard
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.ExpenseCrimsonDark
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmeraldDark
import com.example.ui.theme.IncomeSurface

@Composable
fun TransactionsScreen(
    transactions: List<HisabTransaction>,
    modifier: Modifier = Modifier,
    memberTakaJomaList: List<MemberTakaJoma> = emptyList(),
    onApproveMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onRejectMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onDeleteMemberTakaJoma: (MemberTakaJoma) -> Unit = {},
    onOpenMemberTakaJoma: (() -> Unit)? = null
) {
    val currentMonth = remember { BanglaFormatters.currentYearMonth() }
    val monthTransactions = remember(transactions, currentMonth) {
        transactions.filter { it.date.startsWith(currentMonth) }
    }
    val monthIncome = remember(monthTransactions) {
        monthTransactions.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val monthExpense = remember(monthTransactions) {
        monthTransactions.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }
    val monthNetBalance = monthIncome - monthExpense

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionPanelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transactions_monthly_summary_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "This Month's Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = BanglaFormatters.formatBanglaMonth(currentMonth),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "${monthTransactions.size} entries",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonthlySummaryMetricBox(
                        label = stringResource(R.string.total_income),
                        amount = BanglaFormatters.formatBdt(monthIncome),
                        containerColor = IncomeSurface,
                        contentColor = IncomeEmeraldDark,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("monthly_summary_income")
                    )
                    MonthlySummaryMetricBox(
                        label = stringResource(R.string.total_expense),
                        amount = BanglaFormatters.formatBdt(monthExpense),
                        containerColor = ExpenseSurface,
                        contentColor = ExpenseCrimsonDark,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("monthly_summary_expense")
                    )
                    MonthlySummaryMetricBox(
                        label = "Net Balance",
                        amount = BanglaFormatters.formatBdt(monthNetBalance),
                        containerColor = if (monthNetBalance >= 0) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            ExpenseSurface
                        },
                        contentColor = if (monthNetBalance >= 0) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            ExpenseCrimsonDark
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("monthly_summary_net_balance")
                    )
                }
            }
        }

        item {
            AdminMemberTakaJomaTableCard(
                submissions = memberTakaJomaList,
                onDeleteSubmission = onDeleteMemberTakaJoma,
                onApproveSubmission = onApproveMemberTakaJoma,
                onRejectSubmission = onRejectMemberTakaJoma,
                onOpenForm = onOpenMemberTakaJoma
            )
        }
    }
}

@Composable
private fun MonthlySummaryMetricBox(
    label: String,
    amount: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = amount,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
