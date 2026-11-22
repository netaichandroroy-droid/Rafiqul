package com.example.ui.screens

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
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Hero Dashboard displaying Sales, Dues, and Total Submit Taka
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(colors = listOf(BrandHeroStart, BrandHeroEnd))
                    )
                    .padding(18.dp)
                    .testTag("member_home_hero_card")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "স্বাগতম, ${member.name}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val subtitle = listOfNotNull(
                                member.designation.takeIf { it.isNotBlank() },
                                member.depot.takeIf { it.isNotBlank() }
                            ).joinToString(" • ").ifBlank { "মেম্বার ড্যাশবোর্ড" }
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.80f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = "${BanglaFormatters.toBanglaDigits(submissions.size.toString())}টি জমা",
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
                            title = "TP+Vat",
                            subtitle = "বিক্রয় (Dues ভিত্তি)",
                            value = BanglaFormatters.formatBdt(salesAmount),
                            highlightColor = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("member_home_sales_box")
                        )
                        HeroSummaryMetricTile(
                            title = "Dues",
                            subtitle = if (advanceAmount > 0.0) "অগ্রিম আছে" else "TP+Vat বকেয়া",
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
                            title = "Total Submit Taka",
                            subtitle = "অনুমোদিত জমা",
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
                                text = "অপেক্ষমাণ জমা: ${BanglaFormatters.formatBdt(pendingSubmitTaka)} (${BanglaFormatters.toBanglaDigits(salesSummary.pendingCount.toString())}টি অ্যাডমিন অনুমোদনের পর যোগ হবে)",
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
                            text = "অগ্রিম জমা ব্যালেন্স: ${BanglaFormatters.formatBdt(advanceAmount)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFC8F7DC),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Quick Action Card for submitting Taka Joma
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "দ্রুত কার্যক্রম",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "নতুন টাকা জমা ফর্ম দাখিল করুন (অ্যাডমিন অনুমোদনের পর টাকা যোগ হবে)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

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
                    Text("টাকা জমা ফর্ম", maxLines = 1)
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
                        text = "মাসিক ও সার্বিক সারাংশ",
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
                        label = "মোট TP sales (তথ্য মাত্র)",
                        value = BanglaFormatters.formatBdt(salesSummary.uploadedTpSales),
                        valueColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    )
                }
                MemberMetricListRow(
                    label = "মোট TP+Vat (Sales)",
                    value = BanglaFormatters.formatBdt(salesAmount),
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "অনুমোদিত জমা টাকা (Approved)",
                    value = BanglaFormatters.formatBdt(totalSubmitTaka),
                    valueColor = IncomeEmerald
                )
                if (pendingSubmitTaka > 0.0) {
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    )
                    MemberMetricListRow(
                        label = "অপেক্ষমাণ জমা (${BanglaFormatters.toBanglaDigits(salesSummary.pendingCount.toString())}টি)",
                        value = BanglaFormatters.formatBdt(pendingSubmitTaka),
                        valueColor = NagadAccent
                    )
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "বর্তমান বকেয়া (Dues = TP+Vat − জমা)",
                    value = BanglaFormatters.formatBdt(duesAmount),
                    valueColor = if (duesAmount > 0.0) ExpenseCrimson else IncomeEmerald
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "চলতি মাসে অনুমোদিত জমা (${BanglaFormatters.toBanglaDigits(monthApprovedSubmissions.size.toString())}টি)",
                    value = BanglaFormatters.formatBdt(monthSubmitTotal),
                    valueColor = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "* অনুমোদিত টাকা জমা দিলে শুধুমাত্র TP+Vat থেকে Dues কমবে (TP sales থেকে নয়)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Uploaded Monthly Sales Table (code, name, TP sales, TP+Vat)
        item {
            MonthlySalesTableCard(
                title = "Sales তালিকা (${BanglaFormatters.formatBanglaMonth(selectedSalesMonth)})",
                subtitle = "অ্যাডমিন সেটিংস থেকে মাস অনুযায়ী আপলোডকৃত বিক্রয় হিসাব",
                records = displayedSalesRows,
                emptyMessage = "${BanglaFormatters.formatBanglaMonth(selectedSalesMonth)}-এর কোনো Sales এখনো আপলোড করা হয়নি।",
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
                                contentDescription = "আগের মাস"
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
                                contentDescription = "পরের মাস"
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
                                    Text("সকল (${BanglaFormatters.toBanglaDigits(monthSalesRecords.size.toString())})")
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
                                    Text("আমার (${BanglaFormatters.toBanglaDigits(matchedMemberSalesRows.size.toString())})")
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
                    Text("মাস: ${BanglaFormatters.formatBanglaMonth(record.yearMonth)}")
                    Text("code: ${record.code.ifBlank { "-" }}")
                    Text("name: ${record.name}")
                    Text("TP sales: ${BanglaFormatters.formatBdt(record.tpSales)}")
                    Text(
                        text = "TP+Vat: ${BanglaFormatters.formatBdt(record.tpPlusVat)}",
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingSalesRecord = null }) {
                    Text("বন্ধ করুন")
                }
            }
        )
    }
}

@Composable
private fun HeroSummaryMetricTile(
    title: String,
    subtitle: String,
    value: String,
    highlightColor: Color,
    modifier: Modifier = Modifier
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
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
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
            .padding(vertical = 11.dp),
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
