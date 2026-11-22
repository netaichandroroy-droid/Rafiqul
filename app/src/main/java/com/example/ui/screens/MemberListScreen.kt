package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.ExpenseSurface
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface

@Composable
fun MemberListScreen(
    members: List<Member>,
    onSaveMember: (
        editingId: String?,
        name: String,
        designation: String,
        phone: String,
        depot: String,
        isActive: Boolean,
        email: String,
        address: String,
        joiningDate: String,
        code: String,
        area: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onToggleActive: (memberId: String, isActive: Boolean) -> Unit,
    onDeleteMember: (Member) -> Unit,
    onResetPassword: ((Member) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val activeBg = IncomeSurface
    val activeFg = IncomeEmerald
    val inactiveBg = ExpenseSurface
    val inactiveFg = ExpenseCrimson
    val borderColor = MaterialTheme.colorScheme.outline

    var showEditorSheet by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<Member?>(null) }
    var viewingProfileMember by remember { mutableStateOf<Member?>(null) }
    var deletingMember by remember { mutableStateOf<Member?>(null) }

    val activeCount = remember(members) { members.count { it.isActive } }
    val inactiveCount = members.size - activeCount
    val tableHorizontalScroll = rememberScrollState()

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val tableHeaderStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
    val tableBodyStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp
    )
    val tableRowHeight = 38.dp

    fun measureColumnWidth(
        header: String,
        values: List<String>,
        minWidth: Dp,
        maxWidth: Dp
    ): Dp {
        val headerPx = textMeasurer.measure(header, style = tableHeaderStyle).size.width
        val maxValPx = values.maxOfOrNull { text ->
            textMeasurer.measure(text.ifBlank { "-" }, style = tableBodyStyle).size.width
        } ?: 0
        val widestDp = with(density) { maxOf(headerPx, maxValPx).toDp() } + 10.dp
        return widestDp.coerceIn(minWidth, maxWidth)
    }

    val baseSlW = remember(members, density) {
        val slValues = members.indices.map { (it + 1).toString().padStart(2, '0') }
        measureColumnWidth("SL", slValues, minWidth = 28.dp, maxWidth = 40.dp)
    }
    val baseCodeW = remember(members, density) {
        measureColumnWidth("Code", members.map { it.code }, minWidth = 42.dp, maxWidth = 100.dp)
    }
    val baseNameW = remember(members, density) {
        measureColumnWidth("Name", members.map { it.name }, minWidth = 52.dp, maxWidth = 200.dp)
    }
    val baseDesigW = remember(members, density) {
        measureColumnWidth("Desig:", members.map { it.designation }, minWidth = 44.dp, maxWidth = 150.dp)
    }
    val baseAreaW = remember(members, density) {
        measureColumnWidth("Area", members.map { it.area }, minWidth = 46.dp, maxWidth = 160.dp)
    }
    val baseDepotW = remember(members, density) {
        measureColumnWidth("Depot", members.map { it.depot }, minWidth = 46.dp, maxWidth = 150.dp)
    }
    val activeColWidth = 46.dp
    val actionColWidth = 56.dp

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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Member List",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "মোট ${BanglaFormatters.toBanglaDigits(members.size.toString())} জন • সক্রিয় ${BanglaFormatters.toBanglaDigits(activeCount.toString())} জন",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = {
                            editingMember = null
                            showEditorSheet = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("add_member_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("সদস্য যোগ")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MemberStatChip(
                        label = "মোট: ${BanglaFormatters.toBanglaDigits(members.size.toString())}",
                        bg = MaterialTheme.colorScheme.primaryContainer,
                        fg = MaterialTheme.colorScheme.primary
                    )
                    MemberStatChip(
                        label = "সক্রিয়: ${BanglaFormatters.toBanglaDigits(activeCount.toString())}",
                        bg = activeBg,
                        fg = activeFg
                    )
                    MemberStatChip(
                        label = "নিষ্ক্রিয়: ${BanglaFormatters.toBanglaDigits(inactiveCount.toString())}",
                        bg = inactiveBg,
                        fg = inactiveFg
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (members.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Groups,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "এখনো কোনো সদস্য যোগ করা হয়নি। উপরে 'সদস্য যোগ' বাটনে চাপ দিয়ে তালিকা তৈরি করুন।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 10.dp)
                    ) {
                        val borderOverhead = 9.dp
                        val baseTotal = baseSlW + baseCodeW + baseNameW + baseDesigW + baseAreaW +
                            baseDepotW + activeColWidth + actionColWidth + borderOverhead
                        val extraWidth = (maxWidth - baseTotal).coerceAtLeast(0.dp)
                        val codeColWidth = baseCodeW + (extraWidth * 0.12f)
                        val nameColWidth = baseNameW + (extraWidth * 0.28f)
                        val designationColWidth = baseDesigW + (extraWidth * 0.16f)
                        val areaColWidth = baseAreaW + (extraWidth * 0.22f)
                        val depotColWidth = baseDepotW + (extraWidth * 0.22f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableHorizontalScroll)
                        ) {
                            Column(
                                modifier = Modifier.border(1.dp, borderColor)
                            ) {
                                // All-bordered Header Row
                                Row(
                                    modifier = Modifier
                                        .height(tableRowHeight)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TableHeaderCell("SL", baseSlW, tableHeaderStyle, textAlign = TextAlign.Center)
                                    TableVerticalBorder(borderColor)
                                    TableHeaderCell("Code", codeColWidth, tableHeaderStyle)
                                    TableVerticalBorder(borderColor)
                                    TableHeaderCell("Name", nameColWidth, tableHeaderStyle)
                                    TableVerticalBorder(borderColor)
                                    TableHeaderCell("Desig:", designationColWidth, tableHeaderStyle)
                                    TableVerticalBorder(borderColor)
                                    TableHeaderCell("Area", areaColWidth, tableHeaderStyle)
                                    TableVerticalBorder(borderColor)
                                    TableHeaderCell("Depot", depotColWidth, tableHeaderStyle)
                                    TableVerticalBorder(borderColor)
                                    TableHeaderCell("Active", activeColWidth, tableHeaderStyle, textAlign = TextAlign.Center)
                                    TableVerticalBorder(borderColor)
                                    TableHeaderCell("Action", actionColWidth, tableHeaderStyle, textAlign = TextAlign.Center)
                                }

                                HorizontalDivider(thickness = 1.dp, color = borderColor)

                                // All-bordered Member Rows with exact equal height to Header Row
                                members.forEachIndexed { index, member ->
                                    val rowBg = if (index % 2 == 0) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .height(tableRowHeight)
                                            .background(rowBg)
                                            .testTag("member_row_${member.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TableTextCell(
                                            text = (index + 1).toString().padStart(2, '0'),
                                            width = baseSlW,
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        TableVerticalBorder(borderColor)

                                        TableTextCell(
                                            text = member.code.ifBlank { "-" },
                                            width = codeColWidth,
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        TableVerticalBorder(borderColor)

                                        Box(
                                            modifier = Modifier
                                                .width(nameColWidth)
                                                .fillMaxHeight()
                                                .clickable { viewingProfileMember = member }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                .testTag("view_member_profile_${member.id}"),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Text(
                                                text = member.name,
                                                style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        TableVerticalBorder(borderColor)

                                        TableTextCell(
                                            text = member.designation.ifBlank { "-" },
                                            width = designationColWidth,
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TableVerticalBorder(borderColor)

                                        TableTextCell(
                                            text = member.area.ifBlank { "-" },
                                            width = areaColWidth,
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        TableVerticalBorder(borderColor)

                                        TableTextCell(
                                            text = member.depot.ifBlank { "-" },
                                            width = depotColWidth,
                                            style = tableBodyStyle,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        TableVerticalBorder(borderColor)

                                        Box(
                                            modifier = Modifier
                                                .width(activeColWidth)
                                                .fillMaxHeight()
                                                .clickable { onToggleActive(member.id, !member.isActive) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Box(
                                                modifier = Modifier.wrapContentSize(unbounded = true),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Switch(
                                                    checked = member.isActive,
                                                    onCheckedChange = { checked ->
                                                        onToggleActive(member.id, checked)
                                                    },
                                                    modifier = Modifier
                                                        .scale(0.65f)
                                                        .testTag("member_active_switch_${member.id}")
                                                )
                                            }
                                        }
                                        TableVerticalBorder(borderColor)

                                        Row(
                                            modifier = Modifier
                                                .width(actionColWidth)
                                                .fillMaxHeight()
                                                .padding(horizontal = 2.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        editingMember = member
                                                        showEditorSheet = true
                                                    }
                                                    .testTag("edit_member_${member.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Edit,
                                                    contentDescription = "সম্পাদনা",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .clickable { deletingMember = member }
                                                    .testTag("delete_member_${member.id}"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.DeleteOutline,
                                                    contentDescription = "মুছুন",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (index < members.lastIndex) {
                                        HorizontalDivider(thickness = 1.dp, color = borderColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    viewingProfileMember?.let { target ->
        val latest = members.firstOrNull { it.id == target.id } ?: target
        MemberProfileDetailSheet(
            member = latest,
            onDismiss = { viewingProfileMember = null },
            onToggleActive = { checked -> onToggleActive(latest.id, checked) },
            onEdit = {
                viewingProfileMember = null
                editingMember = latest
                showEditorSheet = true
            },
            onResetPassword = onResetPassword?.let { resetFn ->
                { resetFn(latest) }
            },
            onDelete = {
                viewingProfileMember = null
                deletingMember = latest
            }
        )
    }

    if (showEditorSheet) {
        MemberEditorModalSheet(
            editingMember = editingMember,
            onDismiss = { showEditorSheet = false },
            onResetPassword = onResetPassword,
            onSave = { editingId, code, name, designation, area, phone, depot, isActive, email, address, joiningDate ->
                onSaveMember(
                    editingId,
                    name,
                    designation,
                    phone,
                    depot,
                    isActive,
                    email,
                    address,
                    joiningDate,
                    code,
                    area
                ) { ok ->
                    if (ok) showEditorSheet = false
                }
            }
        )
    }

    deletingMember?.let { target ->
        AlertDialog(
            onDismissRequest = { deletingMember = null },
            title = { Text("সদস্য মুছবেন?") },
            text = { Text("${target.name} কে মেম্বার লিস্ট থেকে স্থায়ীভাবে মুছে ফেলা হবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteMember(target)
                        deletingMember = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_member_button")
                ) {
                    Text("মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingMember = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun TableVerticalBorder(color: Color) {
    VerticalDivider(
        modifier = Modifier.fillMaxHeight(),
        thickness = 1.dp,
        color = color
    )
}

@Composable
private fun TableHeaderCell(
    text: String,
    width: Dp,
    style: TextStyle,
    textAlign: TextAlign = TextAlign.Start
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = if (textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart
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
private fun TableTextCell(
    text: String,
    width: Dp,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign = TextAlign.Start
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        contentAlignment = if (textAlign == TextAlign.Center) Alignment.Center else Alignment.CenterStart
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
private fun MemberStatChip(
    label: String,
    bg: Color,
    fg: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberProfileDetailSheet(
    member: Member,
    onDismiss: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onResetPassword: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MemberAvatarView(
                    name = member.name,
                    photoPath = member.profilePhotoPath,
                    size = 60.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = member.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = listOfNotNull(
                            member.designation.takeIf { it.isNotBlank() },
                            member.depot.takeIf { it.isNotBlank() }
                        ).joinToString(" • ").ifBlank { "সদস্য প্রোফাইল" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = member.isActive,
                    onCheckedChange = onToggleActive
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))

            ProfileSheetFieldRow("Code", member.code.ifBlank { "-" })
            ProfileSheetFieldRow("Login ID", member.name)
            ProfileSheetFieldRow(
                "পাসওয়ার্ড",
                if (member.isDefaultPassword) "${member.name} (ডিফল্ট)" else "সদস্য কর্তৃক পরিবর্তিত"
            )
            ProfileSheetFieldRow("Desig:", member.designation.ifBlank { "-" })
            ProfileSheetFieldRow("Area", member.area.ifBlank { "-" })
            ProfileSheetFieldRow("Depot", member.depot.ifBlank { "-" })
            ProfileSheetFieldRow("Phone", member.phone.ifBlank { "-" })
            ProfileSheetFieldRow("Email", member.email.ifBlank { "-" })
            ProfileSheetFieldRow("Address", member.address.ifBlank { "-" })
            ProfileSheetFieldRow("Sales", BanglaFormatters.formatBdt(member.salesAmount))
            ProfileSheetFieldRow(
                "Joining Date",
                if (member.joiningDate.isNotBlank()) BanglaFormatters.formatBanglaDate(member.joiningDate) else "-"
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onEdit,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("সম্পাদনা")
                }
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("মুছুন")
                }
            }

            if (!member.isDefaultPassword && onResetPassword != null) {
                TextButton(
                    onClick = {
                        onResetPassword()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("পাসওয়ার্ড ডিফল্ট (${member.name}) এ রিসেট করুন")
                }
            }
        }
    }
}

@Composable
private fun ProfileSheetFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MemberEditorModalSheet(
    editingMember: Member?,
    onDismiss: () -> Unit,
    onResetPassword: ((Member) -> Unit)? = null,
    onSave: (
        editingId: String?,
        code: String,
        name: String,
        designation: String,
        area: String,
        phone: String,
        depot: String,
        isActive: Boolean,
        email: String,
        address: String,
        joiningDate: String
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var code by remember(editingMember) { mutableStateOf(editingMember?.code ?: "") }
    var name by remember(editingMember) { mutableStateOf(editingMember?.name ?: "") }
    var designation by remember(editingMember) { mutableStateOf(editingMember?.designation ?: "") }
    var area by remember(editingMember) { mutableStateOf(editingMember?.area ?: "") }
    var phone by remember(editingMember) { mutableStateOf(editingMember?.phone ?: "") }
    var depot by remember(editingMember) { mutableStateOf(editingMember?.depot ?: "") }
    var email by remember(editingMember) { mutableStateOf(editingMember?.email ?: "") }
    var address by remember(editingMember) { mutableStateOf(editingMember?.address ?: "") }
    var joiningDate by remember(editingMember) {
        mutableStateOf(editingMember?.joiningDate?.ifBlank { BanglaFormatters.todayIso() } ?: BanglaFormatters.todayIso())
    }
    var isActive by remember(editingMember) { mutableStateOf(editingMember?.isActive ?: true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = if (editingMember == null) "নতুন সদস্য যোগ করুন" else "সদস্যের প্রোফাইল ও তথ্য সম্পাদনা",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = code,
                onValueChange = {
                    code = it
                    errorMessage = null
                },
                label = { Text("কোড (Code)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_code_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("নাম (Name)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_name_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = designation,
                onValueChange = {
                    designation = it
                    errorMessage = null
                },
                label = { Text("পদবি (Designation)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_designation_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = area,
                onValueChange = {
                    area = it
                    errorMessage = null
                },
                label = { Text("এরিয়া (Area)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_area_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = depot,
                onValueChange = {
                    depot = it
                    errorMessage = null
                },
                label = { Text("ডিপো (Depot)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_depot_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = phone,
                onValueChange = {
                    phone = it
                    errorMessage = null
                },
                label = { Text("ফোন নম্বর (Phone - ঐচ্ছিক)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_phone_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("ইমেইল (Email - ঐচ্ছিক)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_email_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("ঠিকানা (Address - ঐচ্ছিক)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_address_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = joiningDate,
                onValueChange = { joiningDate = it },
                label = { Text("যোগদানের তারিখ (YYYY-MM-DD)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_joining_date_input")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Status",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isActive) "সক্রিয় (ON)" else "নিষ্ক্রিয় (OFF)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isActive,
                    onCheckedChange = { isActive = it },
                    modifier = Modifier.testTag("member_editor_active_switch")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            val previewCredential = name.trim().ifBlank { "সদস্যের নাম" }
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "মেম্বার মোবাইল অ্যাপ লগইন তথ্য",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Login ID: $previewCredential  •  Password: $previewCredential",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (editingMember != null && !editingMember.isDefaultPassword && onResetPassword != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                onResetPassword(editingMember)
                                onDismiss()
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("reset_member_password_button")
                        ) {
                            Text("পাসওয়ার্ড নামের সমান (ডিফল্ট) রিসেট করুন")
                        }
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "অনুগ্রহ করে সদস্যের নাম লিখুন।"
                        return@Button
                    }
                    onSave(
                        editingMember?.id,
                        code,
                        name,
                        designation,
                        area,
                        phone,
                        depot,
                        isActive,
                        email,
                        address,
                        joiningDate
                    )
                },
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_member_button")
            ) {
                Text(if (editingMember == null) "সদস্য সংরক্ষণ করুন" else "হালনাগাদ করুন")
            }
        }
    }
}
