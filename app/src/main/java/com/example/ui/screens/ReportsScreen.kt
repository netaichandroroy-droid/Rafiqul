package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AreaManagerRoleFilter
import com.example.model.AsmNameReportScope
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.Member
import com.example.model.MemberSalesDuesCalculator
import com.example.model.MemberTakaJoma
import com.example.model.MonthlySalesRecord
import com.example.model.ReportAnalyticsCalculator
import com.example.model.ReportCategory
import com.example.model.ReportPeriodScope
import com.example.model.TakaJomaStatus
import com.example.model.TxType
import com.example.ui.components.GroupedWiseReportSection
import com.example.ui.components.MpoWiseReportSection
import com.example.ui.theme.BrandDeep
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandTint
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.NagadAccent

@Composable
fun ReportsScreen(
    transactions: List<HisabTransaction>,
    selectedMonth: String,
    onShiftMonth: (Int) -> Unit,
    onExportPicture: () -> Unit,
    modifier: Modifier = Modifier,
    members: List<Member> = emptyList(),
    monthlySales: List<MonthlySalesRecord> = emptyList(),
    memberTakaJomaList: List<MemberTakaJoma> = emptyList(),
    selectedCategory: ReportCategory = ReportCategory.MONTHLY,
    periodScope: ReportPeriodScope = ReportPeriodScope.SELECTED_MONTH,
    asmNameScope: AsmNameReportScope = AsmNameReportScope.DEPOT_TEAM,
    areaRoleFilter: AreaManagerRoleFilter = AreaManagerRoleFilter.ALL,
    onSelectCategory: (ReportCategory) -> Unit = {},
    onSelectPeriodScope: (ReportPeriodScope) -> Unit = {},
    onSelectAsmNameScope: (AsmNameReportScope) -> Unit = {},
    onSelectAreaRoleFilter: (AreaManagerRoleFilter) -> Unit = {},
    onOpenReportMenu: () -> Unit = {}
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

    val monthSalesList = remember(monthlySales, selectedMonth) {
        monthlySales.filter { it.yearMonth == selectedMonth }
    }
    val monthTpSales = remember(monthSalesList) { monthSalesList.sumOf { it.tpSales } }
    val monthTpPlusVat = remember(monthSalesList) { monthSalesList.sumOf { it.tpPlusVat } }
    val monthApprovedJoma = remember(memberTakaJomaList, selectedMonth) {
        memberTakaJomaList
            .filter { it.date.startsWith(selectedMonth) && it.resolvedStatus == TakaJomaStatus.APPROVED }
            .sumOf { it.amount }
    }
    val monthPendingJoma = remember(memberTakaJomaList, selectedMonth) {
        memberTakaJomaList
            .filter { it.date.startsWith(selectedMonth) && it.resolvedStatus == TakaJomaStatus.PENDING }
            .sumOf { it.amount }
    }

    var onlyMpoDesignation by remember { mutableStateOf(true) }
    var mpoSearchQuery by remember { mutableStateOf("") }
    var mpoAreaFilter by remember { mutableStateOf<String?>(null) }
    var asmDepotSearchQuery by remember { mutableStateOf("") }
    var asmNameSearchQuery by remember { mutableStateOf("") }
    var amSearchQuery by remember { mutableStateOf("") }
    var regionSearchQuery by remember { mutableStateOf("") }

    val mpoReportResult = remember(
        members,
        monthlySales,
        memberTakaJomaList,
        selectedMonth,
        periodScope,
        onlyMpoDesignation
    ) {
        ReportAnalyticsCalculator.buildMpoReport(
            members = members,
            monthlySales = monthlySales,
            submissions = memberTakaJomaList,
            selectedMonth = selectedMonth,
            periodScope = periodScope,
            onlyMpoDesignation = onlyMpoDesignation
        )
    }
    val mpoAvailableAreas = remember(mpoReportResult) {
        mpoReportResult.items
            .map { MemberSalesDuesCalculator.normalizeText(it.summary.area) }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase() }
            .sorted()
    }
    val effectiveMpoAreaFilter = if (mpoAreaFilter != null && mpoAvailableAreas.any {
            it.equals(mpoAreaFilter, ignoreCase = true)
        }
    ) {
        mpoAreaFilter
    } else {
        null
    }
    val filteredMpoItems = remember(mpoReportResult, mpoSearchQuery, effectiveMpoAreaFilter) {
        val q = mpoSearchQuery.trim()
        mpoReportResult.items.filter { item ->
            val areaOk = effectiveMpoAreaFilter == null ||
                MemberSalesDuesCalculator.normalizeText(item.summary.area)
                    .equals(effectiveMpoAreaFilter, ignoreCase = true)
            val queryOk = q.isEmpty() ||
                item.summary.name.contains(q, ignoreCase = true) ||
                item.summary.code.contains(q, ignoreCase = true) ||
                item.summary.area.contains(q, ignoreCase = true) ||
                item.summary.depot.contains(q, ignoreCase = true) ||
                item.designation.contains(q, ignoreCase = true)
            areaOk && queryOk
        }
    }
    val mpoTotals = remember(filteredMpoItems) {
        ReportAnalyticsCalculator.calculateMemberTotals(filteredMpoItems)
    }

    val asmDepotGroupsAll = remember(members, monthlySales, memberTakaJomaList, selectedMonth, periodScope) {
        ReportAnalyticsCalculator.buildAsmDepotWiseReport(
            members = members,
            monthlySales = monthlySales,
            submissions = memberTakaJomaList,
            selectedMonth = selectedMonth,
            periodScope = periodScope
        )
    }
    val filteredAsmDepotGroups = remember(asmDepotGroupsAll, asmDepotSearchQuery) {
        val q = asmDepotSearchQuery.trim()
        if (q.isEmpty()) asmDepotGroupsAll else asmDepotGroupsAll.filter { group ->
            group.title.contains(q, ignoreCase = true) ||
                group.leaderLabel.contains(q, ignoreCase = true) ||
                group.members.any {
                    it.summary.name.contains(q, ignoreCase = true) ||
                        it.summary.area.contains(q, ignoreCase = true)
                }
        }
    }
    val asmDepotTotals = remember(filteredAsmDepotGroups) {
        ReportAnalyticsCalculator.calculateGroupTotals(filteredAsmDepotGroups)
    }

    val asmNameReport = remember(
        members,
        monthlySales,
        memberTakaJomaList,
        selectedMonth,
        periodScope,
        asmNameScope
    ) {
        ReportAnalyticsCalculator.buildAsmNameWiseReport(
            members = members,
            monthlySales = monthlySales,
            submissions = memberTakaJomaList,
            selectedMonth = selectedMonth,
            periodScope = periodScope,
            scopeMode = asmNameScope
        )
    }
    val filteredAsmNameGroups = remember(asmNameReport, asmNameSearchQuery) {
        val q = asmNameSearchQuery.trim()
        if (q.isEmpty()) asmNameReport.rows else asmNameReport.rows.filter { group ->
            group.title.contains(q, ignoreCase = true) ||
                group.leaderLabel.contains(q, ignoreCase = true) ||
                group.members.any {
                    it.summary.name.contains(q, ignoreCase = true) ||
                        it.summary.depot.contains(q, ignoreCase = true)
                }
        }
    }
    val asmNameTotals = remember(filteredAsmNameGroups) {
        ReportAnalyticsCalculator.calculateGroupTotals(filteredAsmNameGroups)
    }

    val areaRoleReport = remember(
        members,
        monthlySales,
        memberTakaJomaList,
        selectedMonth,
        periodScope,
        areaRoleFilter
    ) {
        ReportAnalyticsCalculator.buildAreaRoleWiseReport(
            members = members,
            monthlySales = monthlySales,
            submissions = memberTakaJomaList,
            selectedMonth = selectedMonth,
            periodScope = periodScope,
            roleFilter = areaRoleFilter
        )
    }
    val filteredAmGroups = remember(areaRoleReport, amSearchQuery) {
        val q = amSearchQuery.trim()
        if (q.isEmpty()) areaRoleReport.rows else areaRoleReport.rows.filter { group ->
            group.title.contains(q, ignoreCase = true) ||
                group.leaderLabel.contains(q, ignoreCase = true) ||
                group.members.any { it.summary.name.contains(q, ignoreCase = true) }
        }
    }
    val amTotals = remember(filteredAmGroups) {
        ReportAnalyticsCalculator.calculateGroupTotals(filteredAmGroups)
    }

    val regionGroupsAll = remember(members, monthlySales, memberTakaJomaList, selectedMonth, periodScope) {
        ReportAnalyticsCalculator.buildRegionWiseReport(
            members = members,
            monthlySales = monthlySales,
            submissions = memberTakaJomaList,
            selectedMonth = selectedMonth,
            periodScope = periodScope
        )
    }
    val filteredRegionGroups = remember(regionGroupsAll, regionSearchQuery) {
        val q = regionSearchQuery.trim()
        if (q.isEmpty()) regionGroupsAll else regionGroupsAll.filter { group ->
            group.title.contains(q, ignoreCase = true) ||
                group.leaderLabel.contains(q, ignoreCase = true) ||
                group.members.any {
                    it.summary.area.contains(q, ignoreCase = true) ||
                        it.summary.name.contains(q, ignoreCase = true)
                }
        }
    }
    val regionTotals = remember(filteredRegionGroups) {
        ReportAnalyticsCalculator.calculateGroupTotals(filteredRegionGroups)
    }

    val incomeColor = IncomeEmerald
    val expenseColor = ExpenseCrimson

    Surface(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .testTag("report_full_page_card_${selectedCategory.name.lowercase()}"),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.36f),
                    Color.White.copy(alpha = 0.12f)
                )
            )
        ),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    )
                )
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            BrandHeroStart.copy(alpha = 0.16f),
                            Color.Transparent,
                            BrandTint.copy(alpha = 0.18f)
                        )
                    )
                )
        ) {
            // Dedicated Full Report Card Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenReportMenu,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("report_subpage_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "All Reports",
                        tint = BrandDeep
                    )
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = reportCategoryIcon(selectedCategory),
                        contentDescription = null,
                        tint = BrandDeep,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = selectedCategory.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.32f)),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onOpenReportMenu)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GridView,
                            contentDescription = "Switch Report",
                            tint = BrandDeep,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reports",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = BrandDeep
                        )
                    }
                }
            }

            HorizontalDivider(
                thickness = 0.8.dp,
                color = Color.White.copy(alpha = 0.14f)
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Month & Period Controls integrated inside the full card
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (selectedCategory != ReportCategory.MONTHLY) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ReportPeriodScope.entries.forEach { scope ->
                                    val isSelected = periodScope == scope
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { onSelectPeriodScope(scope) },
                                        label = { Text(scope.bnLabel) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("report_period_${scope.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        if (selectedCategory == ReportCategory.MONTHLY || periodScope == ReportPeriodScope.SELECTED_MONTH) {
                            if (selectedCategory != ReportCategory.MONTHLY) {
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.10f),
                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f)),
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
                                            contentDescription = "Previous Month"
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
                                            contentDescription = "Next Month"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                when (selectedCategory) {
                    ReportCategory.MONTHLY -> {
                        item {
                            Column(modifier = Modifier.fillMaxWidth()) {
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

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = stringResource(R.string.income_vs_expense),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                ComparisonBarRow(
                                    label = "Income",
                                    amount = BanglaFormatters.formatBdt(monthIncome),
                                    fraction = incomeRatio,
                                    barColor = BrandPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                ComparisonBarRow(
                                    label = "Expense",
                                    amount = BanglaFormatters.formatBdt(monthExpense),
                                    fraction = expenseRatio,
                                    barColor = expenseColor
                                )
                            }
                        }

                        item {
                            HorizontalDivider(
                                thickness = 0.8.dp,
                                color = Color.White.copy(alpha = 0.14f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Monthly Sales & Deposit Summary",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ReportStatBox(
                                    label = "TP sales",
                                    value = BanglaFormatters.formatBdt(monthTpSales),
                                    valueColor = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                ReportStatBox(
                                    label = "TP+VAT sales",
                                    value = BanglaFormatters.formatBdt(monthTpPlusVat),
                                    valueColor = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                ReportStatBox(
                                    label = "Approved Deposit",
                                    value = BanglaFormatters.formatBdt(monthApprovedJoma),
                                    valueColor = IncomeEmerald,
                                    modifier = Modifier.weight(1f)
                                )
                                ReportStatBox(
                                    label = "Pending Deposit",
                                    value = BanglaFormatters.formatBdt(monthPendingJoma),
                                    valueColor = NagadAccent,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (topExpenseCategories.isNotEmpty()) {
                            item {
                                HorizontalDivider(
                                    thickness = 0.8.dp,
                                    color = Color.White.copy(alpha = 0.14f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Expense Breakdown by Category",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                topExpenseCategories.forEach { (category, amount) ->
                                    val ratio = if (monthExpense > 0) (amount / monthExpense).toFloat() else 0f
                                    ComparisonBarRow(
                                        label = category,
                                        amount = BanglaFormatters.formatBdt(amount),
                                        fraction = ratio,
                                        barColor = expenseColor,
                                        labelWidthDp = 96
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                }
                            }
                        }

                        item {
                            ExportReportPictureButton(
                                buttonLabel = stringResource(R.string.download_picture),
                                onExportPicture = onExportPicture
                            )
                        }
                    }

                    ReportCategory.MPO_WISE -> {
                        item {
                            MpoWiseReportSection(
                                items = filteredMpoItems,
                                totals = mpoTotals,
                                hasExplicitMpoMembers = mpoReportResult.hasExplicitMpoMembers,
                                onlyMpoDesignation = onlyMpoDesignation,
                                onToggleOnlyMpo = { onlyMpoDesignation = it },
                                searchQuery = mpoSearchQuery,
                                onSearchQueryChange = { mpoSearchQuery = it },
                                selectedAreaFilter = effectiveMpoAreaFilter,
                                availableAreas = mpoAvailableAreas,
                                onSelectAreaFilter = { mpoAreaFilter = it },
                                embedInFullCard = true
                            )
                        }
                        item {
                            ExportReportPictureButton(
                                buttonLabel = "Download MPO Report (PNG)",
                                onExportPicture = onExportPicture
                            )
                        }
                    }

                    ReportCategory.ASM_DEPOT_WISE -> {
                        item {
                            GroupedWiseReportSection(
                                category = ReportCategory.ASM_DEPOT_WISE,
                                groups = filteredAsmDepotGroups,
                                totals = asmDepotTotals,
                                searchQuery = asmDepotSearchQuery,
                                onSearchQueryChange = { asmDepotSearchQuery = it },
                                embedInFullCard = true
                            )
                        }
                        item {
                            ExportReportPictureButton(
                                buttonLabel = "Download ASM Depot Report (PNG)",
                                onExportPicture = onExportPicture
                            )
                        }
                    }

                    ReportCategory.ASM_NAME_WISE -> {
                        item {
                            GroupedWiseReportSection(
                                category = ReportCategory.ASM_NAME_WISE,
                                groups = filteredAsmNameGroups,
                                totals = asmNameTotals,
                                searchQuery = asmNameSearchQuery,
                                onSearchQueryChange = { asmNameSearchQuery = it },
                                noticeBanner = asmNameReport.fallbackNotice,
                                headerFilterContent = if (asmNameReport.hasExplicitAsmMembers) {
                                    {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            AsmNameReportScope.entries.forEach { scopeOption ->
                                                val isSelected = asmNameScope == scopeOption
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = { onSelectAsmNameScope(scopeOption) },
                                                    label = { Text(scopeOption.bnLabel) },
                                                    colors = FilterChipDefaults.filterChipColors(
                                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                        selectedLabelColor = MaterialTheme.colorScheme.primary
                                                    ),
                                                    modifier = Modifier.testTag("asm_name_scope_${scopeOption.name.lowercase()}")
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    null
                                },
                                embedInFullCard = true
                            )
                        }
                        item {
                            ExportReportPictureButton(
                                buttonLabel = "Download ASM Name Report (PNG)",
                                onExportPicture = onExportPicture
                            )
                        }
                    }

                    ReportCategory.AM_WISE -> {
                        item {
                            val roleTitle = if (areaRoleFilter == AreaManagerRoleFilter.ALL) {
                                ReportCategory.AM_WISE.title
                            } else {
                                "${areaRoleFilter.roleTitle} Area Report"
                            }
                            val fallbackMsg = if (areaRoleReport.fallbackShowingAllAreas) {
                                "No members have '${areaRoleFilter.roleTitle}' assigned — showing all areas."
                            } else {
                                null
                            }
                            GroupedWiseReportSection(
                                category = ReportCategory.AM_WISE,
                                groups = filteredAmGroups,
                                totals = amTotals,
                                searchQuery = amSearchQuery,
                                onSearchQueryChange = { amSearchQuery = it },
                                customTitle = roleTitle,
                                noticeBanner = fallbackMsg,
                                headerFilterContent = {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        AreaManagerRoleFilter.entries.forEach { roleOption ->
                                            val isSelected = areaRoleFilter == roleOption
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { onSelectAreaRoleFilter(roleOption) },
                                                label = {
                                                    Text(
                                                        text = roleOption.bnLabel,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier.testTag("area_role_filter_${roleOption.name.lowercase()}")
                                            )
                                        }
                                    }
                                },
                                embedInFullCard = true
                            )
                        }
                        item {
                            ExportReportPictureButton(
                                buttonLabel = "Download ${areaRoleFilter.roleTitle} Area Report (PNG)",
                                onExportPicture = onExportPicture
                            )
                        }
                    }

                    ReportCategory.REGION_WISE -> {
                        item {
                            GroupedWiseReportSection(
                                category = ReportCategory.REGION_WISE,
                                groups = filteredRegionGroups,
                                totals = regionTotals,
                                searchQuery = regionSearchQuery,
                                onSearchQueryChange = { regionSearchQuery = it },
                                embedInFullCard = true
                            )
                        }
                        item {
                            ExportReportPictureButton(
                                buttonLabel = "Download Region Report (PNG)",
                                onExportPicture = onExportPicture
                            )
                        }
                    }
                }
            }
        }
    }
}

fun reportCategoryIcon(category: ReportCategory): ImageVector = when (category) {
    ReportCategory.MONTHLY -> Icons.Outlined.CalendarMonth
    ReportCategory.MPO_WISE -> Icons.Outlined.Badge
    ReportCategory.ASM_DEPOT_WISE -> Icons.Outlined.Storefront
    ReportCategory.ASM_NAME_WISE -> Icons.Outlined.ManageAccounts
    ReportCategory.AM_WISE -> Icons.Outlined.SupervisorAccount
    ReportCategory.REGION_WISE -> Icons.Outlined.Public
}

@Composable
private fun ExportReportPictureButton(
    buttonLabel: String,
    onExportPicture: () -> Unit
) {
    Button(
        onClick = onExportPicture,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("reports_export_picture_button"),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Image,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(buttonLabel)
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
        color = Color.White.copy(alpha = 0.10f),
        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f))
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
