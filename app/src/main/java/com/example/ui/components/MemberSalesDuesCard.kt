package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.model.MemberSalesDuesCalculator
import com.example.model.MemberSalesDuesSummary
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent

@Composable
fun MemberSalesDuesCard(
    members: List<Member>,
    monthlySales: List<MonthlySalesRecord>,
    submissions: List<MemberTakaJoma>,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val scrollState = rememberScrollState()
    val pillsScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var onlyDuesFilter by remember { mutableStateOf(false) }
    var inspectingSummary by remember { mutableStateOf<MemberSalesDuesSummary?>(null) }

    val summaries = remember(members, monthlySales, submissions) {
        MemberSalesDuesCalculator.buildAllMemberSummaries(
            members = members,
            monthlySales = monthlySales,
            submissions = submissions
        )
    }
    val totals = remember(summaries) {
        MemberSalesDuesCalculator.calculateTotals(summaries)
    }
    val displayedSummaries = remember(summaries, onlyDuesFilter) {
        if (onlyDuesFilter) summaries.filter { it.dues > 0.0 } else summaries
    }

    val tableHeaderStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Bold
    )
    val tableBodyStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 13.sp
    )

    fun measureCol(
        header: String,
        values: List<String>,
        minWidth: Dp,
        maxWidth: Dp
    ): Dp {
        val headerPx = textMeasurer.measure(header, style = tableHeaderStyle).size.width
        val maxValPx = values.maxOfOrNull { value ->
            textMeasurer.measure(value.ifBlank { "-" }, style = tableBodyStyle).size.width
        } ?: 0
        val widestDp = with(density) { maxOf(headerPx, maxValPx).toDp() } + 14.dp
        return widestDp.coerceIn(minWidth, maxWidth)
    }

    val codeColW = remember(displayedSummaries, density) {
        measureCol("Code", displayedSummaries.map { it.code }, 50.dp, 88.dp)
    }
    val nameColW = remember(displayedSummaries, density) {
        measureCol("Name", displayedSummaries.map { it.name }, 78.dp, 150.dp)
    }
    val salesColW = remember(displayedSummaries, totals, density) {
        measureCol(
            "TP+VAT sales",
            displayedSummaries.map { BanglaFormatters.formatBdt(it.totalSales) } +
                BanglaFormatters.formatBdt(totals.totalSales),
            90.dp,
            142.dp
        )
    }
    val jomaColW = remember(displayedSummaries, totals, density) {
        measureCol(
            "Deposit",
            displayedSummaries.map { BanglaFormatters.formatBdt(it.approvedJoma) } +
                BanglaFormatters.formatBdt(totals.totalApprovedJoma),
            74.dp,
            122.dp
        )
    }
    val duesColW = remember(displayedSummaries, totals, density) {
        measureCol(
            "Dues",
            displayedSummaries.map { BanglaFormatters.formatBdt(it.dues) } +
                BanglaFormatters.formatBdt(totals.totalDues),
            74.dp,
            122.dp
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_member_sales_dues_card"),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = "Member TP+VAT sales & Dues",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Dues = TP+VAT sales minus approved deposits (TP sales excluded)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(pillsScrollState)
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Total TP+VAT sales: ${BanglaFormatters.formatBdt(totals.totalSales)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IncomeSurface
                ) {
                    Text(
                        text = "Approved Deposit: ${BanglaFormatters.formatBdt(totals.totalApprovedJoma)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (totals.totalDues > 0.0) ExpenseSurface else IncomeSurface
                ) {
                    Text(
                        text = "Total Dues: ${BanglaFormatters.formatBdt(totals.totalDues)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (totals.totalDues > 0.0) ExpenseCrimson else IncomeEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (summaries.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                val duesCount = summaries.count { it.dues > 0.0 }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !onlyDuesFilter,
                        onClick = { onlyDuesFilter = false },
                        label = { Text("All (${summaries.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("member_dues_filter_all")
                    )
                    FilterChip(
                        selected = onlyDuesFilter,
                        onClick = { onlyDuesFilter = true },
                        label = { Text("Has Dues ($duesCount)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseSurface,
                            selectedLabelColor = ExpenseCrimson
                        ),
                        modifier = Modifier.testTag("member_dues_filter_only_dues")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (displayedSummaries.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (summaries.isEmpty()) {
                            "Add members or upload a sales sheet to view sales and dues."
                        } else {
                            "No members currently have dues."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    val borderOverhead = 6.dp
                    val baseTotal = codeColW + nameColW + salesColW + jomaColW + duesColW + borderOverhead
                    val extraWidth = (maxWidth - baseTotal).coerceAtLeast(0.dp)
                    val resolvedCodeW = codeColW + (extraWidth * 0.12f)
                    val resolvedNameW = nameColW + (extraWidth * 0.34f)
                    val resolvedSalesW = salesColW + (extraWidth * 0.18f)
                    val resolvedJomaW = jomaColW + (extraWidth * 0.18f)
                    val resolvedDuesW = duesColW + (extraWidth * 0.18f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Column(
                            modifier = Modifier.border(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("admin_member_sales_dues_header"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                DuesHeaderCell("Code", resolvedCodeW, tableHeaderStyle, TextAlign.Center)
                                DuesVerticalBorder(borderColor)
                                DuesHeaderCell("Name", resolvedNameW, tableHeaderStyle, TextAlign.Start)
                                DuesVerticalBorder(borderColor)
                                DuesHeaderCell("TP+VAT sales", resolvedSalesW, tableHeaderStyle, TextAlign.End)
                                DuesVerticalBorder(borderColor)
                                DuesHeaderCell("Deposit", resolvedJomaW, tableHeaderStyle, TextAlign.End)
                                DuesVerticalBorder(borderColor)
                                DuesHeaderCell("Dues", resolvedDuesW, tableHeaderStyle, TextAlign.End)
                            }

                            HorizontalDivider(thickness = 1.dp, color = borderColor)

                            displayedSummaries.forEachIndexed { index, summary ->
                                val rowBg = if (index % 2 == 0) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                }

                                Row(
                                    modifier = Modifier
                                        .height(IntrinsicSize.Min)
                                        .background(rowBg)
                                        .clickable { inspectingSummary = summary }
                                        .testTag("admin_member_dues_row_${summary.memberId}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedCodeW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 5.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = summary.code.ifBlank { "-" },
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    DuesVerticalBorder(borderColor)

                                    Box(
                                        modifier = Modifier
                                            .width(resolvedNameW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Column {
                                            Text(
                                                text = summary.name,
                                                style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (summary.pendingCount > 0) {
                                                Text(
                                                    text = "Pending: ${BanglaFormatters.formatBdt(summary.pendingJoma)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = NagadAccent,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    DuesVerticalBorder(borderColor)

                                    Box(
                                        modifier = Modifier
                                            .width(resolvedSalesW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = BanglaFormatters.formatBdt(summary.totalSales),
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    DuesVerticalBorder(borderColor)

                                    Box(
                                        modifier = Modifier
                                            .width(resolvedJomaW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = BanglaFormatters.formatBdt(summary.approvedJoma),
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            color = IncomeEmerald,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    DuesVerticalBorder(borderColor)

                                    Box(
                                        modifier = Modifier
                                            .width(resolvedDuesW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 9.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = BanglaFormatters.formatBdt(summary.dues),
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            color = if (summary.dues > 0.0) ExpenseCrimson else IncomeEmerald,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                HorizontalDivider(thickness = 1.dp, color = borderColor)
                            }

                            // Totals footer row
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(resolvedCodeW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Total",
                                        style = tableHeaderStyle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                DuesVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedNameW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "${displayedSummaries.size} members",
                                        style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DuesVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedSalesW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = BanglaFormatters.formatBdt(displayedSummaries.sumOf { it.totalSales }),
                                        style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DuesVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedJomaW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = BanglaFormatters.formatBdt(displayedSummaries.sumOf { it.approvedJoma }),
                                        style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                        color = IncomeEmerald,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                DuesVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedDuesW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    val footerDues = displayedSummaries.sumOf { it.dues }
                                    Text(
                                        text = BanglaFormatters.formatBdt(footerDues),
                                        style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                        color = if (footerDues > 0.0) ExpenseCrimson else IncomeEmerald,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inspectingSummary?.let { item ->
        AlertDialog(
            onDismissRequest = { inspectingSummary = null },
            title = { Text("${item.name}${if (item.code.isNotBlank()) " (${item.code})" else ""}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (item.area.isNotBlank()) {
                        Text("Area: ${item.area}")
                    }
                    if (item.depot.isNotBlank()) {
                        Text("Depot: ${item.depot}")
                    }
                    if (item.uploadedTpSales > 0.0) {
                        Text(
                            text = "TP sales (Info): ${BanglaFormatters.formatBdt(item.uploadedTpSales)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Uploaded TP+VAT sales: ${BanglaFormatters.formatBdt(item.uploadedSales)}")
                    if (item.manualSales > 0.0) {
                        Text("Manual Sales: ${BanglaFormatters.formatBdt(item.manualSales)}")
                    }
                    Text(
                        text = "Total TP+VAT sales: ${BanglaFormatters.formatBdt(item.totalSales)}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Approved Deposits (${item.approvedCount}): ${BanglaFormatters.formatBdt(item.approvedJoma)}",
                        fontWeight = FontWeight.SemiBold,
                        color = IncomeEmerald
                    )
                    if (item.pendingJoma > 0.0) {
                        val afterPendingDues = (item.totalSales - (item.approvedJoma + item.pendingJoma)).coerceAtLeast(0.0)
                        Text(
                            text = "Pending Deposits (${item.pendingCount}): ${BanglaFormatters.formatBdt(item.pendingJoma)}",
                            color = NagadAccent,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Projected Dues after approval: ${BanglaFormatters.formatBdt(afterPendingDues)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = if (item.advance > 0.0) {
                            "Advance Deposit: ${BanglaFormatters.formatBdt(item.advance)}"
                        } else {
                            "Current Dues (TP+VAT sales − Deposit): ${BanglaFormatters.formatBdt(item.dues)}"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (item.dues > 0.0) ExpenseCrimson else IncomeEmerald
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingSummary = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun DuesHeaderCell(
    text: String,
    width: Dp,
    style: TextStyle,
    textAlign: TextAlign
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 6.dp, vertical = 10.dp),
        contentAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            style = style,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DuesVerticalBorder(color: Color) {
    VerticalDivider(
        thickness = 1.dp,
        color = color,
        modifier = Modifier.fillMaxHeight()
    )
}
