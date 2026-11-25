package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.model.MemberSalesDuesCalculator
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.TakaJomaStatus
import com.example.ui.components.MonthlySalesTableCard
import com.example.ui.components.SectionPanelCard
import com.example.ui.components.isMatchingMember
import com.example.ui.theme.BrandHeroEnd
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.NagadAccent

@Composable
fun MemberHomeScreen(
    member: Member,
    submissions: List<MemberTakaJoma>,
    monthlySales: List<MonthlySalesRecord> = emptyList(),
    selectedSalesMonth: String = BanglaFormatters.currentYearMonth(),
    onShiftSalesMonth: (Int) -> Unit = {},
    onOpenTakaJomaForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val salesSummary = remember(member, monthlySales, submissions) {
        MemberSalesDuesCalculator.calculateForMember(
            member = member,
            monthlySales = monthlySales,
            submissions = submissions
        )
    }
    val totalSubmitTaka = salesSummary.approvedJoma
    val pendingSubmitTaka = salesSummary.pendingJoma
    val salesAmount = salesSummary.totalSales
    val duesAmount = salesSummary.dues
    val advanceAmount = salesSummary.advance

    val monthSalesRecords = remember(monthlySales, selectedSalesMonth) {
        monthlySales.filter { it.yearMonth == selectedSalesMonth }
    }
    val matchedMemberSalesRows = remember(monthSalesRecords, member.code, member.name) {
        monthSalesRecords.filter {
            isMatchingMember(record = it, memberCode = member.code, memberName = member.name)
        }
    }

    val currentMonth = remember { BanglaFormatters.currentYearMonth() }
    val monthApprovedSubmissions = remember(submissions, currentMonth) {
        submissions.filter {
            it.date.startsWith(currentMonth) && it.resolvedStatus == TakaJomaStatus.APPROVED
        }
    }
    val monthSubmitTotal = remember(monthApprovedSubmissions) {
        monthApprovedSubmissions.sumOf { it.amount }
    }

    var onlyMineFilter by remember { mutableStateOf(false) }
    var inspectingSalesRecord by remember { mutableStateOf<MonthlySalesRecord?>(null) }

    val displayedSalesRows = remember(monthSalesRecords, matchedMemberSalesRows, onlyMineFilter) {
        if (onlyMineFilter && matchedMemberSalesRows.isNotEmpty()) {
            matchedMemberSalesRows
        } else {
            monthSalesRecords
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hero Dashboard displaying Sales, Dues, and Total Submit Taka
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .testTag("member_home_hero_card"),
                shape = RoundedCornerShape(20.dp),
                color = Color.Transparent,
                contentColor = Color.White,
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.48f),
                            Color.White.copy(alpha = 0.16f)
                        )
                    )
                ),
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    BrandHeroStart.copy(alpha = 0.68f),
                                    BrandHeroEnd.copy(alpha = 0.64f)
                                )
                            )
                        )
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color.Transparent
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Welcome, ${member.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val details = listOfNotNull(
                                member.designation.takeIf { it.isNotBlank() },
                                member.depot.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")
                            if (details.isNotBlank()) {
                                Text(
                                    text = details,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.80f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${submissions.size} deposit(s)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sales, Dues, Total Submit Taka 3-metric grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HeroSummaryMetricTile(
                            title = "TP+VAT sales",
                            value = BanglaFormatters.formatBdt(salesAmount),
                            highlightColor = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("member_home_sales_box")
                        )
                        HeroSummaryMetricTile(
                            title = "Dues",
                            value = BanglaFormatters.formatBdt(duesAmount),
                            highlightColor = if (duesAmount > 0.0) {
                                Color(0xFFFFD5D5)
                            } else {
                                Color(0xFFC8F7DC)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("member_home_dues_box")
                        )
                        HeroSummaryMetricTile(
                            title = "Total Deposit",
                            value = BanglaFormatters.formatBdt(totalSubmitTaka),
                            highlightColor = Color(0xFFC8F7DC),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("member_home_submit_taka_box")
                        )
                    }

                    if (pendingSubmitTaka > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFF3CD).copy(alpha = 0.22f)
                        ) {
                            Text(
                                text = "Pending Deposits: ${BanglaFormatters.formatBdt(pendingSubmitTaka)} (${salesSummary.pendingCount} awaiting admin approval)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFE082),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (advanceAmount > 0.0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Advance Deposit Balance: ${BanglaFormatters.formatBdt(advanceAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8F7DC),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            }
        }

        // Quick Action Card for submitting Deposit Form
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Quick Action",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onOpenTakaJomaForm,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("member_home_go_taka_joma_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Deposit Form", maxLines = 1)
                }
            }
        }

        // Detailed Breakdown Card (Sales / Total Submit Taka / Dues) + Current Month
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly & Overall Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = BanglaFormatters.formatBanglaMonth(currentMonth),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )

                if (salesSummary.uploadedTpSales > 0.0) {
                    MemberMetricListRow(
                        label = "Total TP sales (Info Only)",
                        value = BanglaFormatters.formatBdt(salesSummary.uploadedTpSales),
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    )
                }
                MemberMetricListRow(
                    label = "Total TP+VAT sales",
                    value = BanglaFormatters.formatBdt(salesAmount),
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "Approved Deposits",
                    value = BanglaFormatters.formatBdt(totalSubmitTaka),
                    valueColor = IncomeEmerald
                )
                if (pendingSubmitTaka > 0.0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    )
                    MemberMetricListRow(
                        label = "Pending Deposits (${salesSummary.pendingCount})",
                        value = BanglaFormatters.formatBdt(pendingSubmitTaka),
                        valueColor = NagadAccent
                    )
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "Current Dues (TP+VAT sales − Deposit)",
                    value = BanglaFormatters.formatBdt(duesAmount),
                    valueColor = if (duesAmount > 0.0) ExpenseCrimson else IncomeEmerald
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "This Month Approved (${monthApprovedSubmissions.size})",
                    value = BanglaFormatters.formatBdt(monthSubmitTotal),
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Uploaded Monthly Sales Table (code, name, TP sales, TP+Vat)
        item {
            MonthlySalesTableCard(
                title = "Sales List (${BanglaFormatters.formatBanglaMonth(selectedSalesMonth)})",
                records = displayedSalesRows,
                emptyMessage = "No sales uploaded for ${BanglaFormatters.formatBanglaMonth(selectedSalesMonth)} yet.",
                highlightedMemberCode = member.code,
                highlightedMemberName = member.name,
                headerAction = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onShiftSalesMonth(-1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("member_sales_prev_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month"
                            )
                        }
                        IconButton(
                            onClick = { onShiftSalesMonth(1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("member_sales_next_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month"
                            )
                        }
                    }
                },
                filterContent = if (matchedMemberSalesRows.isNotEmpty()) {
                    {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !onlyMineFilter,
                                onClick = { onlyMineFilter = false },
                                label = {
                                    Text("All (${monthSalesRecords.size})")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                            FilterChip(
                                selected = onlyMineFilter,
                                onClick = { onlyMineFilter = true },
                                label = {
                                    Text("Mine (${matchedMemberSalesRows.size})")
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                } else {
                    null
                },
                onRowClick = { record -> inspectingSalesRecord = record }
            )
        }
    }

    inspectingSalesRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { inspectingSalesRecord = null },
            title = { Text("${record.name} (${record.code.ifBlank { "-" }})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Month: ${BanglaFormatters.formatBanglaMonth(record.yearMonth)}")
                    Text("code: ${record.code.ifBlank { "-" }}")
                    Text("name: ${record.name}")
                    Text("TP sales: ${BanglaFormatters.formatBdt(record.tpSales)}")
                    Text(
                        text = "TP+VAT sales: ${BanglaFormatters.formatBdt(record.tpPlusVat)}",
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingSalesRecord = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun HeroSummaryMetricTile(
    title: String,
    value: String,
    highlightColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String = ""
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.14f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = highlightColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MemberMetricListRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}
