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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
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
import com.example.model.GroupedReportRow
import com.example.model.MemberReportItem
import com.example.model.ReportCategory
import com.example.model.ReportTotals
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent

@Composable
fun ReportTotalsSummaryStrip(
    totals: ReportTotals,
    countLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SummaryPillChip(
                text = countLabel,
                bg = MaterialTheme.colorScheme.primaryContainer,
                fg = MaterialTheme.colorScheme.primary
            )
            SummaryPillChip(
                text = "Collection: ${totals.collectionPercent}%",
                bg = IncomeSurface,
                fg = IncomeEmerald
            )
            if (totals.pendingJoma > 0.0) {
                SummaryPillChip(
                    text = "Pending: ${BanglaFormatters.formatBdt(totals.pendingJoma)}",
                    bg = MaterialTheme.colorScheme.surfaceVariant,
                    fg = NagadAccent
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniReportKpiBox(
                label = "TP+VAT sales",
                value = BanglaFormatters.formatBdt(totals.totalTpPlusVat),
                valueColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            MiniReportKpiBox(
                label = "Deposit",
                value = BanglaFormatters.formatBdt(totals.approvedJoma),
                valueColor = IncomeEmerald,
                modifier = Modifier.weight(1f)
            )
            MiniReportKpiBox(
                label = "Dues",
                value = BanglaFormatters.formatBdt(totals.dues),
                valueColor = if (totals.dues > 0.0) ExpenseCrimson else IncomeEmerald,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryPillChip(
    text: String,
    bg: Color,
    fg: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = bg
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun MiniReportKpiBox(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ReportSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Clear",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}

@Composable
fun MpoWiseReportSection(
    items: List<MemberReportItem>,
    totals: ReportTotals,
    hasExplicitMpoMembers: Boolean,
    onlyMpoDesignation: Boolean,
    onToggleOnlyMpo: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedAreaFilter: String?,
    availableAreas: List<String>,
    onSelectAreaFilter: (String?) -> Unit
) {
    var inspectingMember by remember { mutableStateOf<MemberReportItem?>(null) }
    val borderColor = MaterialTheme.colorScheme.outline
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val tableHeaderStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val tableBodyStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp)

    fun measureCol(header: String, values: List<String>, minW: Dp, maxW: Dp): Dp {
        val headerPx = textMeasurer.measure(header, style = tableHeaderStyle).size.width
        val maxValPx = values.maxOfOrNull { textMeasurer.measure(it.ifBlank { "-" }, style = tableBodyStyle).size.width } ?: 0
        return (with(density) { maxOf(headerPx, maxValPx).toDp() } + 14.dp).coerceIn(minW, maxW)
    }

    SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ReportCategory.MPO_WISE.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "TP+VAT sales, deposits, and dues for each MPO",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Outlined.Badge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ReportTotalsSummaryStrip(
            totals = totals,
            countLabel = "MPO: ${items.size}"
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (hasExplicitMpoMembers) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = onlyMpoDesignation,
                    onClick = { onToggleOnlyMpo(true) },
                    label = { Text("MPO Only") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("mpo_filter_only_mpo")
                )
                FilterChip(
                    selected = !onlyMpoDesignation,
                    onClick = { onToggleOnlyMpo(false) },
                    label = { Text("All Members") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.testTag("mpo_filter_all_members")
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
        } else if (items.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Showing all members because no specific MPO designation was assigned.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        ReportSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = "Search code, MPO name, or area...",
            testTag = "mpo_report_search_input"
        )

        if (availableAreas.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedAreaFilter == null,
                    onClick = { onSelectAreaFilter(null) },
                    label = { Text("All Areas") }
                )
                availableAreas.forEach { areaName ->
                    val selected = selectedAreaFilter.equals(areaName, ignoreCase = true)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectAreaFilter(if (selected) null else areaName) },
                        label = { Text(areaName) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (items.isEmpty()) {
            EmptyReportNotice(
                message = "No MPO sales or deposit records found for this period."
            )
        } else {
            val slW = 36.dp
            val codeW = remember(items, density) { measureCol("Code", items.map { it.summary.code }, 48.dp, 86.dp) }
            val nameW = remember(items, density) { measureCol("MPO & Area", items.map { it.summary.name }, 96.dp, 165.dp) }
            val salesW = remember(items, totals, density) {
                measureCol("TP+VAT sales", items.map { BanglaFormatters.formatBdt(it.summary.totalSales) } + BanglaFormatters.formatBdt(totals.totalTpPlusVat), 90.dp, 142.dp)
            }
            val jomaW = remember(items, totals, density) {
                measureCol("Deposit", items.map { BanglaFormatters.formatBdt(it.summary.approvedJoma) } + BanglaFormatters.formatBdt(totals.approvedJoma), 78.dp, 124.dp)
            }
            val duesW = remember(items, totals, density) {
                measureCol("Dues", items.map { BanglaFormatters.formatBdt(it.summary.dues) } + BanglaFormatters.formatBdt(totals.dues), 74.dp, 120.dp)
            }
            val pctW = 72.dp

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val baseTotal = slW + codeW + nameW + salesW + jomaW + duesW + pctW + 8.dp
                val extra = (maxWidth - baseTotal).coerceAtLeast(0.dp)
                val resolvedNameW = nameW + (extra * 0.40f)
                val resolvedSalesW = salesW + (extra * 0.20f)
                val resolvedJomaW = jomaW + (extra * 0.20f)
                val resolvedDuesW = duesW + (extra * 0.20f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.border(1.dp, borderColor)) {
                        Row(
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RTableHeaderCell("SL", slW, tableHeaderStyle, TextAlign.Center)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("Code", codeW, tableHeaderStyle, TextAlign.Center)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("MPO & Area", resolvedNameW, tableHeaderStyle, TextAlign.Start)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("TP+VAT sales", resolvedSalesW, tableHeaderStyle, TextAlign.End)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("Deposit", resolvedJomaW, tableHeaderStyle, TextAlign.End)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("Dues", resolvedDuesW, tableHeaderStyle, TextAlign.End)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("Collection", pctW, tableHeaderStyle, TextAlign.Center)
                        }
                        HorizontalDivider(thickness = 1.dp, color = borderColor)

                        items.forEachIndexed { idx, item ->
                            val rowBg = if (idx % 2 == 0) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            val subInfo = listOfNotNull(
                                item.designation.takeIf { it.isNotBlank() },
                                item.summary.area.takeIf { it.isNotBlank() },
                                item.summary.depot.takeIf { it.isNotBlank() }
                            ).joinToString(" • ")

                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .background(rowBg)
                                    .clickable { inspectingMember = item }
                                    .testTag("mpo_report_row_${item.summary.memberId}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RTableCell(
                                    text = BanglaFormatters.toBanglaDigits((idx + 1).toString().padStart(2, '0')),
                                    width = slW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = item.summary.code.ifBlank { "-" },
                                    width = codeW,
                                    style = tableBodyStyle,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                RVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedNameW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        Text(
                                            text = item.summary.name,
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (subInfo.isNotBlank()) {
                                            Text(
                                                text = subInfo,
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = BanglaFormatters.formatBdt(item.summary.totalSales),
                                    width = resolvedSalesW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End
                                )
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = BanglaFormatters.formatBdt(item.summary.approvedJoma),
                                    width = resolvedJomaW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                    color = IncomeEmerald,
                                    textAlign = TextAlign.End
                                )
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = BanglaFormatters.formatBdt(item.summary.dues),
                                    width = resolvedDuesW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                    color = if (item.summary.dues > 0.0) ExpenseCrimson else IncomeEmerald,
                                    textAlign = TextAlign.End
                                )
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = "${BanglaFormatters.toBanglaDigits(item.collectionPercent.toString())}%",
                                    width = pctW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                    color = BrandPrimary,
                                    textAlign = TextAlign.Center
                                )
                            }
                            HorizontalDivider(thickness = 1.dp, color = borderColor)
                        }

                        // Total Footer Row
                        Row(
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("mpo_report_total_row"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RTableCell(
                                text = "Total",
                                width = slW,
                                style = tableHeaderStyle,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = "All",
                                width = codeW,
                                style = tableHeaderStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = "${items.size} members",
                                width = resolvedNameW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = BanglaFormatters.formatBdt(totals.totalTpPlusVat),
                                width = resolvedSalesW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = BanglaFormatters.formatBdt(totals.approvedJoma),
                                width = resolvedJomaW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = IncomeEmerald,
                                textAlign = TextAlign.End
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = BanglaFormatters.formatBdt(totals.dues),
                                width = resolvedDuesW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = if (totals.dues > 0.0) ExpenseCrimson else IncomeEmerald,
                                textAlign = TextAlign.End
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = "${BanglaFormatters.toBanglaDigits(totals.collectionPercent.toString())}%",
                                width = pctW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = BrandPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    inspectingMember?.let { item ->
        val s = item.summary
        AlertDialog(
            onDismissRequest = { inspectingMember = null },
            title = { Text("${s.name}${if (s.code.isNotBlank()) " (${s.code})" else ""}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (item.designation.isNotBlank()) Text("Designation: ${item.designation}")
                    if (s.area.isNotBlank()) Text("Area: ${s.area}")
                    if (s.depot.isNotBlank()) Text("Depot / Region: ${s.depot}")
                    if (item.phone.isNotBlank()) Text("Phone: ${item.phone}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("TP sales (Info): ${BanglaFormatters.formatBdt(s.uploadedTpSales)}")
                    Text("TP+VAT sales (Total): ${BanglaFormatters.formatBdt(s.totalSales)}", fontWeight = FontWeight.Bold)
                    Text(
                        text = "Approved Deposits (${s.approvedCount}): ${BanglaFormatters.formatBdt(s.approvedJoma)}",
                        color = IncomeEmerald,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (s.pendingJoma > 0.0) {
                        Text(
                            text = "Pending Deposits: ${BanglaFormatters.formatBdt(s.pendingJoma)}",
                            color = NagadAccent
                        )
                    }
                    Text(
                        text = if (s.advance > 0.0) {
                            "Advance Deposit: ${BanglaFormatters.formatBdt(s.advance)}"
                        } else {
                            "Current Dues: ${BanglaFormatters.formatBdt(s.dues)}"
                        },
                        fontWeight = FontWeight.Bold,
                        color = if (s.dues > 0.0) ExpenseCrimson else IncomeEmerald
                    )
                    Text(
                        text = "Collection Rate: ${item.collectionPercent}%",
                        color = BrandPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingMember = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun GroupedWiseReportSection(
    category: ReportCategory,
    groups: List<GroupedReportRow>,
    totals: ReportTotals,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    customTitle: String? = null,
    customSubtitle: String? = null,
    noticeBanner: String? = null,
    headerFilterContent: (@Composable () -> Unit)? = null
) {
    var inspectingGroup by remember { mutableStateOf<GroupedReportRow?>(null) }
    val headerIcon = when (category) {
        ReportCategory.ASM_DEPOT_WISE -> Icons.Outlined.Storefront
        ReportCategory.ASM_NAME_WISE -> Icons.Outlined.ManageAccounts
        ReportCategory.AM_WISE -> Icons.Outlined.SupervisorAccount
        else -> Icons.Outlined.Public
    }
    val resolvedTitle = customTitle ?: category.title
    val subtitleText = customSubtitle ?: when (category) {
        ReportCategory.ASM_DEPOT_WISE -> "TP+VAT sales, deposits, dues, and total by depot"
        ReportCategory.ASM_NAME_WISE -> "TP+VAT sales, deposits, dues, and total by ASM name"
        ReportCategory.AM_WISE -> "TP+VAT sales, deposits, dues, and total by RSM, RSC & AM area"
        else -> "Combined TP+VAT sales, deposits, and dues by region or depot"
    }
    val col2Header = when (category) {
        ReportCategory.ASM_DEPOT_WISE -> "Depot / ASM"
        ReportCategory.ASM_NAME_WISE -> "ASM Name"
        ReportCategory.AM_WISE -> "Area / Lead"
        else -> "Region / Depot"
    }
    val countChipLabel = when (category) {
        ReportCategory.ASM_DEPOT_WISE ->
            "Depots: ${groups.size} • Members: ${totals.memberCount}"
        ReportCategory.ASM_NAME_WISE ->
            "ASMs: ${groups.size} • Members: ${totals.memberCount}"
        ReportCategory.AM_WISE ->
            "Areas: ${groups.size} • Members: ${totals.memberCount}"
        else ->
            "Regions: ${groups.size} • Members: ${totals.memberCount}"
    }
    val searchPlaceholder = when (category) {
        ReportCategory.ASM_DEPOT_WISE -> "Search depot or ASM name..."
        ReportCategory.ASM_NAME_WISE -> "Search ASM name, code, or depot..."
        ReportCategory.AM_WISE -> "Search area or RSM / RSC / AM name..."
        else -> "Search region / depot or RM..."
    }
    val searchTestTag = when (category) {
        ReportCategory.ASM_DEPOT_WISE -> "asm_depot_report_search_input"
        ReportCategory.ASM_NAME_WISE -> "asm_name_report_search_input"
        ReportCategory.AM_WISE -> "am_report_search_input"
        else -> "region_report_search_input"
    }
    val emptyMessage = when (category) {
        ReportCategory.ASM_DEPOT_WISE -> "No ASM depot records found for this period."
        ReportCategory.ASM_NAME_WISE -> "No ASM name records found for this period."
        ReportCategory.AM_WISE -> "No RSM / RSC / AM area records found for this period."
        else -> "No region or depot records found for this period."
    }

    val borderColor = MaterialTheme.colorScheme.outline
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val tableHeaderStyle = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold)
    val tableBodyStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp)

    fun measureCol(header: String, values: List<String>, minW: Dp, maxW: Dp): Dp {
        val headerPx = textMeasurer.measure(header, style = tableHeaderStyle).size.width
        val maxValPx = values.maxOfOrNull { textMeasurer.measure(it.ifBlank { "-" }, style = tableBodyStyle).size.width } ?: 0
        return (with(density) { maxOf(headerPx, maxValPx).toDp() } + 14.dp).coerceIn(minW, maxW)
    }

    SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = headerIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        ReportTotalsSummaryStrip(
            totals = totals,
            countLabel = countChipLabel
        )

        if (headerFilterContent != null) {
            Spacer(modifier = Modifier.height(10.dp))
            headerFilterContent()
        }

        if (!noticeBanner.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = noticeBanner,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ReportSearchField(
            query = searchQuery,
            onQueryChange = onSearchQueryChange,
            placeholder = searchPlaceholder,
            testTag = searchTestTag
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (groups.isEmpty()) {
            EmptyReportNotice(message = emptyMessage)
        } else {
            // Visual progress summary cards for top groups
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groups.forEach { group ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(0.5.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { inspectingGroup = group }
                            .testTag("grouped_report_card_${group.key}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = group.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${group.leaderLabel} • ${group.memberCount} members",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = IncomeSurface
                                ) {
                                    Text(
                                        text = "Collection ${group.collectionPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeEmerald,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((group.collectionPercent / 100f).coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IncomeEmerald)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "TP+VAT sales: ${BanglaFormatters.formatBdt(group.totalTpPlusVat)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Deposit: ${BanglaFormatters.formatBdt(group.approvedJoma)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = IncomeEmerald
                                )
                                Text(
                                    text = "Dues: ${BanglaFormatters.formatBdt(group.dues)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (group.dues > 0.0) ExpenseCrimson else IncomeEmerald
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bordered Table View
            val slW = 36.dp
            val titleW = remember(groups, density) { measureCol(col2Header, groups.map { it.title }, 96.dp, 165.dp) }
            val cntW = 64.dp
            val salesW = remember(groups, totals, density) {
                measureCol("TP+VAT sales", groups.map { BanglaFormatters.formatBdt(it.totalTpPlusVat) } + BanglaFormatters.formatBdt(totals.totalTpPlusVat), 90.dp, 142.dp)
            }
            val jomaW = remember(groups, totals, density) {
                measureCol("Deposit", groups.map { BanglaFormatters.formatBdt(it.approvedJoma) } + BanglaFormatters.formatBdt(totals.approvedJoma), 78.dp, 124.dp)
            }
            val duesW = remember(groups, totals, density) {
                measureCol("Dues", groups.map { BanglaFormatters.formatBdt(it.dues) } + BanglaFormatters.formatBdt(totals.dues), 74.dp, 120.dp)
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val baseTotal = slW + titleW + cntW + salesW + jomaW + duesW + 7.dp
                val extra = (maxWidth - baseTotal).coerceAtLeast(0.dp)
                val resolvedTitleW = titleW + (extra * 0.40f)
                val resolvedSalesW = salesW + (extra * 0.20f)
                val resolvedJomaW = jomaW + (extra * 0.20f)
                val resolvedDuesW = duesW + (extra * 0.20f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.border(1.dp, borderColor)) {
                        Row(
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RTableHeaderCell("SL", slW, tableHeaderStyle, TextAlign.Center)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell(col2Header, resolvedTitleW, tableHeaderStyle, TextAlign.Start)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("Members", cntW, tableHeaderStyle, TextAlign.Center)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("TP+VAT sales", resolvedSalesW, tableHeaderStyle, TextAlign.End)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("Deposit", resolvedJomaW, tableHeaderStyle, TextAlign.End)
                            RVerticalBorder(borderColor)
                            RTableHeaderCell("Dues", resolvedDuesW, tableHeaderStyle, TextAlign.End)
                        }
                        HorizontalDivider(thickness = 1.dp, color = borderColor)

                        groups.forEachIndexed { idx, group ->
                            val rowBg = if (idx % 2 == 0) {
                                MaterialTheme.colorScheme.surface
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .background(rowBg)
                                    .clickable { inspectingGroup = group }
                                    .testTag("grouped_report_table_row_${group.key}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RTableCell(
                                    text = BanglaFormatters.toBanglaDigits((idx + 1).toString().padStart(2, '0')),
                                    width = slW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    textAlign = TextAlign.Center
                                )
                                RVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedTitleW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Column {
                                        Text(
                                            text = group.title,
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = group.leaderLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = BanglaFormatters.toBanglaDigits(group.memberCount.toString()),
                                    width = cntW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = BanglaFormatters.formatBdt(group.totalTpPlusVat),
                                    width = resolvedSalesW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End
                                )
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = BanglaFormatters.formatBdt(group.approvedJoma),
                                    width = resolvedJomaW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                    color = IncomeEmerald,
                                    textAlign = TextAlign.End
                                )
                                RVerticalBorder(borderColor)
                                RTableCell(
                                    text = BanglaFormatters.formatBdt(group.dues),
                                    width = resolvedDuesW,
                                    style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                    color = if (group.dues > 0.0) ExpenseCrimson else IncomeEmerald,
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(thickness = 1.dp, color = borderColor)
                        }

                        // Total Footer Row
                        Row(
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("grouped_report_total_row_${category.name.lowercase()}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RTableCell(
                                text = "Total",
                                width = slW,
                                style = tableHeaderStyle,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = "Total (${groups.size})",
                                width = resolvedTitleW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Start
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = BanglaFormatters.toBanglaDigits(totals.memberCount.toString()),
                                width = cntW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = BanglaFormatters.formatBdt(totals.totalTpPlusVat),
                                width = resolvedSalesW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.End
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = BanglaFormatters.formatBdt(totals.approvedJoma),
                                width = resolvedJomaW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = IncomeEmerald,
                                textAlign = TextAlign.End
                            )
                            RVerticalBorder(borderColor)
                            RTableCell(
                                text = BanglaFormatters.formatBdt(totals.dues),
                                width = resolvedDuesW,
                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                color = if (totals.dues > 0.0) ExpenseCrimson else IncomeEmerald,
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
    }

    inspectingGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { inspectingGroup = null },
            title = { Text("${group.title} (${group.memberCount} members)") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(group.leaderLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    Text("Total TP sales: ${BanglaFormatters.formatBdt(group.totalTpSales)}")
                    Text("Total TP+VAT sales: ${BanglaFormatters.formatBdt(group.totalTpPlusVat)}", fontWeight = FontWeight.Bold)
                    Text("Approved Deposits: ${BanglaFormatters.formatBdt(group.approvedJoma)}", color = IncomeEmerald, fontWeight = FontWeight.Bold)
                    if (group.pendingJoma > 0.0) {
                        Text("Pending Deposits: ${BanglaFormatters.formatBdt(group.pendingJoma)}", color = NagadAccent)
                    }
                    Text(
                        text = "Total Dues: ${BanglaFormatters.formatBdt(group.dues)}",
                        color = if (group.dues > 0.0) ExpenseCrimson else IncomeEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "Member Breakdown:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    group.members.forEach { memberItem ->
                        val ms = memberItem.summary
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "${ms.name}${if (ms.code.isNotBlank()) " (${ms.code})" else ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                val meta = listOfNotNull(
                                    memberItem.designation.takeIf { it.isNotBlank() },
                                    ms.area.takeIf { it.isNotBlank() },
                                    ms.depot.takeIf { it.isNotBlank() }
                                ).joinToString(" • ")
                                if (meta.isNotBlank()) {
                                    Text(meta, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(
                                    text = "TP+VAT sales: ${BanglaFormatters.formatBdt(ms.totalSales)} • Deposit: ${BanglaFormatters.formatBdt(ms.approvedJoma)} • Dues: ${BanglaFormatters.formatBdt(ms.dues)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectingGroup = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun EmptyReportNotice(message: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 18.dp, horizontal = 12.dp)
        )
    }
}

@Composable
private fun RTableHeaderCell(
    text: String,
    width: Dp,
    style: TextStyle,
    textAlign: TextAlign
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 5.dp, vertical = 10.dp),
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
private fun RTableCell(
    text: String,
    width: Dp,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 5.dp, vertical = 9.dp),
        contentAlignment = when (textAlign) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            textAlign = textAlign,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RVerticalBorder(color: Color) {
    VerticalDivider(
        thickness = 1.dp,
        color = color,
        modifier = Modifier.fillMaxHeight()
    )
}
