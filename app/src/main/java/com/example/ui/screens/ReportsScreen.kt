package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.TxType
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.DarkExpenseText
import com.example.ui.theme.DarkIncomeText
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.BrandPrimary

@Composable
fun ReportsScreen(
    transactions: List<HisabTransaction>,
    selectedMonth: String,
    onShiftMonth: (Int) -> Unit,
    onExportCsv: () -> Unit,
    modifier: Modifier = Modifier
) {
    val monthItems = remember(transactions, selectedMonth) {
        transactions.filter { it.date.startsWith(selectedMonth) }
    }
    val monthIncome = remember(monthItems) {
        monthItems.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
    }
    val monthExpense = remember(monthItems) {
        monthItems.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
    }
    val totalVolume = monthIncome + monthExpense
    val incomeRatio by animateFloatAsState(
        targetValue = if (totalVolume > 0) (monthIncome / totalVolume).toFloat() else 0f,
        label = "incomeRatio"
    )
    val expenseRatio by animateFloatAsState(
        targetValue = if (totalVolume > 0) (monthExpense / totalVolume).toFloat() else 0f,
        label = "expenseRatio"
    )

    val topExpenseCategories = remember(monthItems) {
        monthItems
            .filter { it.resolvedType == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(5)
    }

    val incomeColor = IncomeEmerald
    val expenseColor = ExpenseCrimson

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.monthly_report),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { onShiftMonth(-1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("prev_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "আগের মাস"
                            )
                        }
                        Text(
                            text = BanglaFormatters.formatBanglaMonth(selectedMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        IconButton(
                            onClick = { onShiftMonth(1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("next_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "পরের মাস"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReportStatBox(
                        label = stringResource(R.string.month_income),
                        value = BanglaFormatters.formatBdt(monthIncome),
                        valueColor = incomeColor,
                        modifier = Modifier.weight(1f)
                    )
                    ReportStatBox(
                        label = stringResource(R.string.month_expense),
                        value = BanglaFormatters.formatBdt(monthExpense),
                        valueColor = expenseColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.income_vs_expense),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                ComparisonBarRow(
                    label = "আয়",
                    amount = BanglaFormatters.formatBdt(monthIncome),
                    fraction = incomeRatio,
                    barColor = BrandPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                ComparisonBarRow(
                    label = "খরচ",
                    amount = BanglaFormatters.formatBdt(monthExpense),
                    fraction = expenseRatio,
                    barColor = expenseColor
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onExportCsv,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("reports_export_csv_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.download_csv))
                }
            }
        }

        if (topExpenseCategories.isNotEmpty()) {
            item {
                SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "খরচের খাতভিত্তিক হিসাব",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    topExpenseCategories.forEach { (category, amount) ->
                        val ratio = if (monthExpense > 0) (amount / monthExpense).toFloat() else 0f
                        ComparisonBarRow(
                            label = category,
                            amount = BanglaFormatters.formatBdt(amount),
                            fraction = ratio,
                            barColor = expenseColor,
                            labelWidthDp = 96
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun ComparisonBarRow(
    label: String,
    amount: String,
    fraction: Float,
    barColor: Color,
    labelWidthDp: Int = 48
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(labelWidthDp.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(10.dp))
                    .background(barColor)
            )
        }
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
