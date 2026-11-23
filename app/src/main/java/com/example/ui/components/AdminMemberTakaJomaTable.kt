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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.minimumInteractiveComponentSize
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
import com.example.model.MemberTakaJoma
import com.example.model.TakaJomaStatus
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent
import com.example.ui.theme.NagadSurface

private enum class TakaJomaStatusFilter {
    ALL,
    PENDING,
    APPROVED
}

@Composable
fun AdminMemberTakaJomaTableCard(
    submissions: List<MemberTakaJoma>,
    onDeleteSubmission: (MemberTakaJoma) -> Unit,
    onApproveSubmission: ((MemberTakaJoma) -> Unit)? = null,
    onRejectSubmission: ((MemberTakaJoma) -> Unit)? = null,
    onOpenForm: (() -> Unit)? = null,
    pendingOnlyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val scrollState = rememberScrollState()
    val pillsScrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var statusFilter by remember { mutableStateOf(TakaJomaStatusFilter.ALL) }
    var previewItem by remember { mutableStateOf<Pair<Int, MemberTakaJoma>?>(null) }
    var deletingItem by remember { mutableStateOf<MemberTakaJoma?>(null) }

    val pendingEntries = remember(submissions) {
        submissions.filter { it.resolvedStatus == TakaJomaStatus.PENDING }
    }
    val approvedEntries = remember(submissions) {
        submissions.filter { it.resolvedStatus == TakaJomaStatus.APPROVED }
    }
    val approvedAmount = remember(approvedEntries) { approvedEntries.sumOf { it.amount } }
    val pendingAmount = remember(pendingEntries) { pendingEntries.sumOf { it.amount } }

    val visibleSubmissions = remember(submissions, statusFilter, pendingOnlyMode) {
        if (pendingOnlyMode) {
            submissions.filter { it.resolvedStatus == TakaJomaStatus.PENDING }
        } else {
            when (statusFilter) {
                TakaJomaStatusFilter.ALL -> submissions
                TakaJomaStatusFilter.PENDING -> submissions.filter { it.resolvedStatus == TakaJomaStatus.PENDING }
                TakaJomaStatusFilter.APPROVED -> submissions.filter { it.resolvedStatus == TakaJomaStatus.APPROVED }
            }
        }
    }

    val tableHeaderStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.sp,
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

    val submitColW = remember(visibleSubmissions, density) {
        measureCol("Submit", visibleSubmissions.map { formatAdminShortDate(it.date) }, 56.dp, 84.dp)
    }
    val slColW = remember(visibleSubmissions, density) {
        measureCol("SL", visibleSubmissions.indices.map { (it + 1).toString().padStart(2, '0') }, 32.dp, 46.dp)
    }
    val codeColW = remember(visibleSubmissions, density) {
        measureCol("Code", visibleSubmissions.map { it.code }, 52.dp, 96.dp)
    }
    val nameColW = remember(visibleSubmissions, density) {
        measureCol("Name", visibleSubmissions.map { it.name }, 74.dp, 150.dp)
    }
    val desigColW = remember(visibleSubmissions, density) {
        measureCol("Desig:", visibleSubmissions.map { it.designation }, 52.dp, 120.dp)
    }
    val areaColW = remember(visibleSubmissions, density) {
        measureCol("Area", visibleSubmissions.map { it.area }, 56.dp, 130.dp)
    }
    val depotColW = remember(visibleSubmissions, density) {
        measureCol("Depot", visibleSubmissions.map { it.depot }, 60.dp, 110.dp)
    }
    val methodColW = remember(visibleSubmissions, density) {
        val methods = visibleSubmissions.map { it.bankName.substringBefore(" (").ifBlank { "-" } }
        measureCol("Method", methods, 64.dp, 128.dp)
    }
    val takaColW = remember(visibleSubmissions, density) {
        measureCol("Taka", visibleSubmissions.map { BanglaFormatters.formatBdt(it.amount) }, 64.dp, 116.dp)
    }
    val statusColW = if (onApproveSubmission != null) 104.dp else 80.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("admin_member_taka_joma_card"),
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (pendingOnlyMode) {
                            "Pending Deposits (Approval)"
                        } else {
                            "Member Deposits"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (pendingOnlyMode) {
                            "Member deposit submissions appear here until approved"
                        } else {
                            "Submitted member deposits and approval status"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onOpenForm != null) {
                    TextButton(
                        onClick = onOpenForm,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("admin_lenden_open_taka_joma")
                    ) {
                        Text("+ Add Entry")
                    }
                }
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
                    color = IncomeSurface
                ) {
                    Text(
                        text = "Approved: ${BanglaFormatters.formatBdt(approvedAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (pendingEntries.isNotEmpty()) NagadSurface else MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Pending: ${pendingEntries.size} (${BanglaFormatters.formatBdt(pendingAmount)})",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingEntries.isNotEmpty()) NagadAccent else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            if (!pendingOnlyMode && submissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = statusFilter == TakaJomaStatusFilter.ALL,
                        onClick = { statusFilter = TakaJomaStatusFilter.ALL },
                        label = { Text("All (${submissions.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("admin_taka_joma_filter_all")
                    )
                    FilterChip(
                        selected = statusFilter == TakaJomaStatusFilter.PENDING,
                        onClick = { statusFilter = TakaJomaStatusFilter.PENDING },
                        label = { Text("Pending (${pendingEntries.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NagadSurface,
                            selectedLabelColor = NagadAccent
                        ),
                        modifier = Modifier.testTag("admin_taka_joma_filter_pending")
                    )
                    FilterChip(
                        selected = statusFilter == TakaJomaStatusFilter.APPROVED,
                        onClick = { statusFilter = TakaJomaStatusFilter.APPROVED },
                        label = { Text("Approved (${approvedEntries.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeSurface,
                            selectedLabelColor = IncomeEmerald
                        ),
                        modifier = Modifier.testTag("admin_taka_joma_filter_approved")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (visibleSubmissions.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            pendingOnlyMode -> "No pending deposits awaiting approval."
                            submissions.isEmpty() -> "Member deposit submissions will appear in this table."
                            else -> "No deposit entries match this filter."
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
                    val borderOverhead = 11.dp
                    val baseTotal = submitColW + slColW + codeColW + nameColW + desigColW +
                        areaColW + depotColW + methodColW + takaColW + statusColW + borderOverhead
                    val extraWidth = (maxWidth - baseTotal).coerceAtLeast(0.dp)
                    val resolvedNameW = nameColW + (extraWidth * 0.30f)
                    val resolvedAreaW = areaColW + (extraWidth * 0.20f)
                    val resolvedMethodW = methodColW + (extraWidth * 0.25f)
                    val resolvedTakaW = takaColW + (extraWidth * 0.25f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        Column(
                            modifier = Modifier.border(1.dp, borderColor)
                        ) {
                            // Header row matching form headers + Status
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("admin_member_taka_joma_table_header"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AdminTableHeaderCell("Submit", submitColW, tableHeaderStyle, TextAlign.Center)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("SL", slColW, tableHeaderStyle, TextAlign.Center)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Code", codeColW, tableHeaderStyle, TextAlign.Center)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Name", resolvedNameW, tableHeaderStyle)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Desig:", desigColW, tableHeaderStyle)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Area", resolvedAreaW, tableHeaderStyle)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Depot", depotColW, tableHeaderStyle)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Method", resolvedMethodW, tableHeaderStyle)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Amount", resolvedTakaW, tableHeaderStyle, TextAlign.End)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Status", statusColW, tableHeaderStyle, TextAlign.Center)
                            }

                            HorizontalDivider(thickness = 1.dp, color = borderColor)

                            visibleSubmissions.forEachIndexed { index, entry ->
                                val sl = index + 1
                                val status = entry.resolvedStatus
                                val rowBg = when {
                                    status == TakaJomaStatus.PENDING -> NagadSurface.copy(alpha = 0.35f)
                                    index % 2 == 0 -> MaterialTheme.colorScheme.surface
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                }
                                val methodLabel = entry.bankName.substringBefore(" (").ifBlank { "-" }

                                Row(
                                    modifier = Modifier
                                        .height(IntrinsicSize.Min)
                                        .background(rowBg)
                                        .clickable { previewItem = sl to entry }
                                        .testTag("admin_member_taka_joma_row_${entry.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Submit date
                                    Box(
                                        modifier = Modifier
                                            .width(submitColW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = formatAdminShortDate(entry.date),
                                                style = tableBodyStyle.copy(
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "Deposit",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                                                color = IncomeEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // SL
                                    Box(
                                        modifier = Modifier
                                            .width(slColW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sl.toString().padStart(2, '0'),
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Code
                                    Box(
                                        modifier = Modifier
                                            .width(codeColW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 5.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = entry.code.ifBlank { "-" },
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Name
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedNameW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = entry.name,
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Desig:
                                    Box(
                                        modifier = Modifier
                                            .width(desigColW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = entry.designation.ifBlank { "-" },
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Area
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedAreaW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = entry.area.ifBlank { "-" },
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Depot
                                    Box(
                                        modifier = Modifier
                                            .width(depotColW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = entry.depot.ifBlank { "-" },
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Payment method (Cash / Nagad / bKash / Bank)
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedMethodW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = methodLabel,
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Taka
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedTakaW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = BanglaFormatters.formatBdt(entry.amount),
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            color = when (status) {
                                                TakaJomaStatus.APPROVED -> IncomeEmerald
                                                TakaJomaStatus.PENDING -> NagadAccent
                                                TakaJomaStatus.REJECTED -> ExpenseCrimson
                                            },
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Status & Quick Approve
                                    Box(
                                        modifier = Modifier
                                            .width(statusColW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 4.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (status == TakaJomaStatus.PENDING && onApproveSubmission != null) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = IncomeEmerald,
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onApproveSubmission(entry) }
                                                    .testTag("approve_taka_joma_${entry.id}")
                                            ) {
                                                Text(
                                                    text = "Approve",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                                )
                                            }
                                        } else {
                                            TakaJomaStatusPill(status = status)
                                        }
                                    }
                                }

                                if (index < visibleSubmissions.lastIndex) {
                                    HorizontalDivider(thickness = 1.dp, color = borderColor)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewItem?.let { (sl, item) ->
        val status = item.resolvedStatus
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = { Text("SL ${sl.toString().padStart(2, '0')} • ${item.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Status:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TakaJomaStatusPill(status = status)
                    }
                    Text("Submit Date: ${BanglaFormatters.formatBanglaDate(item.date)}")
                    Text("Code: ${item.code.ifBlank { "-" }}")
                    Text("Name: ${item.name}")
                    if (item.designation.isNotBlank()) {
                        Text("Designation: ${item.designation}")
                    }
                    if (item.area.isNotBlank()) {
                        Text("Area: ${item.area}")
                    }
                    Text("Depot: ${item.depot.ifBlank { "-" }}")
                    if (item.bankName.isNotBlank()) {
                        Text("Method / Bank: ${item.bankName}")
                    }
                    Text(
                        text = "Amount: ${BanglaFormatters.formatBdt(item.amount)}",
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald
                    )
                    if (onApproveSubmission != null || onRejectSubmission != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (status != TakaJomaStatus.APPROVED && onApproveSubmission != null) {
                                Button(
                                    onClick = {
                                        onApproveSubmission(item)
                                        previewItem = null
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = IncomeEmerald),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("dialog_approve_taka_joma_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Approve", maxLines = 1)
                                }
                            }
                            if (status != TakaJomaStatus.REJECTED && onRejectSubmission != null) {
                                OutlinedButton(
                                    onClick = {
                                        onRejectSubmission(item)
                                        previewItem = null
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseCrimson),
                                    border = BorderStroke(1.dp, ExpenseCrimson.copy(alpha = 0.5f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("dialog_reject_taka_joma_${item.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Reject", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewItem = null }) {
                    Text("Close")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        previewItem = null
                        deletingItem = item
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("Delete deposit entry?") },
            text = {
                Text("${item.name} (${item.code}) — ${BanglaFormatters.formatBdt(item.amount)} will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSubmission(item)
                        deletingItem = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TakaJomaStatusPill(
    status: TakaJomaStatus,
    modifier: Modifier = Modifier
) {
    val bgColor = when (status) {
        TakaJomaStatus.APPROVED -> IncomeSurface
        TakaJomaStatus.PENDING -> NagadSurface
        TakaJomaStatus.REJECTED -> ExpenseSurface
    }
    val textColor = when (status) {
        TakaJomaStatus.APPROVED -> IncomeEmerald
        TakaJomaStatus.PENDING -> NagadAccent
        TakaJomaStatus.REJECTED -> ExpenseCrimson
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = bgColor
    ) {
        Text(
            text = status.bnLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

private fun formatAdminShortDate(isoDate: String): String {
    val cleaned = isoDate.trim()
    val parts = cleaned.split("-")
    if (parts.size == 3) {
        return BanglaFormatters.toBanglaDigits("${parts[2]}/${parts[1]}/${parts[0].takeLast(2)}")
    }
    return cleaned.ifBlank { "-" }
}

@Composable
private fun AdminTableHeaderCell(
    text: String,
    width: Dp,
    style: TextStyle,
    textAlign: TextAlign = TextAlign.Start
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
private fun AdminTableVerticalBorder(color: Color) {
    VerticalDivider(
        thickness = 1.dp,
        color = color,
        modifier = Modifier.fillMaxHeight()
    )
}
