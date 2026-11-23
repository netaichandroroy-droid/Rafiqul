package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BangladeshBankDirectory
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.model.PaymentMethodCategory
import com.example.model.TakaJomaStatus
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.BkashAccent
import com.example.ui.theme.BkashSurface
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface
import com.example.ui.theme.NagadAccent
import com.example.ui.theme.NagadSurface
import java.util.UUID

@Composable
fun MemberTakaJomaScreen(
    submissions: List<MemberTakaJoma>,
    currentMember: Member? = null,
    adminMembers: List<Member> = emptyList(),
    isAdminMode: Boolean = false,
    onSubmit: (
        submissionId: String,
        memberId: String,
        name: String,
        code: String,
        depot: String,
        bankName: String,
        amount: Double,
        date: String,
        note: String,
        photoPath: String,
        slipHash: String,
        designation: String,
        area: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    onDeleteSubmission: (MemberTakaJoma) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingSubmissionId by remember { mutableStateOf(UUID.randomUUID().toString()) }
    var selectedMemberId by remember(currentMember?.id) {
        mutableStateOf(currentMember?.id ?: "")
    }
    var name by remember(currentMember?.id) {
        mutableStateOf(currentMember?.name.orEmpty())
    }
    var code by remember(currentMember?.id) {
        mutableStateOf(currentMember?.code.orEmpty())
    }
    var designation by remember(currentMember?.id) {
        mutableStateOf(currentMember?.designation.orEmpty())
    }
    var area by remember(currentMember?.id) {
        mutableStateOf(currentMember?.area.orEmpty())
    }
    var depot by remember(currentMember?.id) {
        mutableStateOf(currentMember?.depot.orEmpty())
    }
    var selectedBank by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var selectedCategoryFilter by remember { mutableStateOf<PaymentMethodCategory?>(null) }
    var bankSearchQuery by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf(BanglaFormatters.todayIso()) }
    var formError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun applyMemberAutoFill(member: Member) {
        selectedMemberId = member.id
        name = member.name
        code = member.code
        designation = member.designation
        area = member.area
        depot = member.depot
        formError = null
    }

    LaunchedEffect(
        currentMember?.id,
        currentMember?.name,
        currentMember?.code,
        currentMember?.designation,
        currentMember?.area,
        currentMember?.depot
    ) {
        if (currentMember != null) {
            applyMemberAutoFill(currentMember)
        }
    }

    LaunchedEffect(isAdminMode, adminMembers) {
        if (isAdminMode) {
            val selectedExisting = adminMembers.firstOrNull { it.id == selectedMemberId }
            if (selectedExisting != null) {
                applyMemberAutoFill(selectedExisting)
            } else if (selectedMemberId.isBlank() &&
                name.isBlank() &&
                code.isBlank() &&
                depot.isBlank()
            ) {
                val activeMembers = adminMembers.filter { it.isActive }
                if (activeMembers.size == 1) {
                    applyMemberAutoFill(activeMembers.first())
                } else if (adminMembers.size == 1) {
                    applyMemberAutoFill(adminMembers.first())
                }
            }
        }
    }

    val filteredBanks = remember(bankSearchQuery, selectedCategoryFilter) {
        val base = BangladeshBankDirectory.filterPaymentMethods(bankSearchQuery)
        if (selectedCategoryFilter == null) base
        else base.filter { it.category == selectedCategoryFilter }
    }

    var previewSubmission by remember { mutableStateOf<MemberTakaJoma?>(null) }
    var deletingSubmission by remember { mutableStateOf<MemberTakaJoma?>(null) }

    val approvedAmount = remember(submissions) {
        submissions.filter { it.resolvedStatus == TakaJomaStatus.APPROVED }.sumOf { it.amount }
    }
    val pendingAmount = remember(submissions) {
        submissions.filter { it.resolvedStatus == TakaJomaStatus.PENDING }.sumOf { it.amount }
    }

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
                            text = "Member Deposit Form",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Enter name, code, depot, Cash/Nagad/bKash/Bank & amount",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                            contentDescription = "Member Deposit Form",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Total: ${submissions.size}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
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
                    if (pendingAmount > 0.0) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = NagadSurface
                        ) {
                            Text(
                                text = "Pending: ${BanglaFormatters.formatBdt(pendingAmount)}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = NagadAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Deposit Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (currentMember != null || selectedMemberId.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = IncomeSurface
                        ) {
                            Text(
                                text = "Auto-filled",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeEmerald,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                if (isAdminMode && adminMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select from Member List (Auto-fill)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val areaSortedMembers = com.example.model.MemberSalesDuesCalculator.sortMembersByArea(adminMembers)
                        items(areaSortedMembers, key = { it.id }) { member ->
                            val isSelected = selectedMemberId == member.id
                            val chipLabel = if (member.code.isNotBlank()) {
                                "${member.code} • ${member.name}"
                            } else {
                                member.name
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { applyMemberAutoFill(member) },
                                label = { Text(chipLabel) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("taka_joma_member_chip_${member.id}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { input ->
                        name = input
                        formError = null
                        if (isAdminMode) {
                            val trimmed = input.trim()
                            val matched = if (trimmed.isNotEmpty()) {
                                adminMembers.firstOrNull { m ->
                                    m.name.trim().equals(trimmed, ignoreCase = true)
                                }
                            } else null
                            if (matched != null) {
                                selectedMemberId = matched.id
                                code = matched.code
                                designation = matched.designation
                                area = matched.area
                                depot = matched.depot
                            }
                        }
                    },
                    label = { Text("Name *") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("taka_joma_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { input ->
                            code = input
                            formError = null
                            if (isAdminMode) {
                                val trimmed = input.trim()
                                val matched = if (trimmed.isNotEmpty()) {
                                    adminMembers.firstOrNull { m ->
                                        m.code.isNotBlank() && m.code.trim().equals(trimmed, ignoreCase = true)
                                    }
                                } else null
                                if (matched != null) {
                                    selectedMemberId = matched.id
                                    name = matched.name
                                    designation = matched.designation
                                    area = matched.area
                                    depot = matched.depot
                                }
                            }
                        },
                        label = { Text("Code *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.QrCode,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("taka_joma_code_input")
                    )

                    OutlinedTextField(
                        value = depot,
                        onValueChange = {
                            depot = it
                            formError = null
                        },
                        label = { Text("Depot *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("taka_joma_depot_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = designation,
                        onValueChange = {
                            designation = it
                            formError = null
                        },
                        label = { Text("Designation") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.WorkOutline,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("taka_joma_designation_input")
                    )

                    OutlinedTextField(
                        value = area,
                        onValueChange = {
                            area = it
                            formError = null
                        },
                        label = { Text("Area") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("taka_joma_area_input")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = {
                            amountText = it
                            formError = null
                        },
                        label = { Text("Amount (৳) *") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Badge,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("taka_joma_amount_input")
                    )

                    OutlinedTextField(
                        value = dateText,
                        onValueChange = { dateText = it },
                        label = { Text("Date") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("taka_joma_date_input")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment Method (Cash / Nagad / bKash / Bank) *",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                val selectedBankItem = remember(selectedBank) {
                    BangladeshBankDirectory.ALL_PAYMENT_METHODS.firstOrNull {
                        it.displayTitle == selectedBank
                    }
                }
                val dropdownIcon = when (selectedBankItem?.category) {
                    PaymentMethodCategory.CASH -> Icons.Outlined.Payments
                    PaymentMethodCategory.MFS -> Icons.Outlined.Smartphone
                    else -> Icons.Outlined.AccountBalance
                }
                val dropdownAccent = when (selectedBankItem?.shortCode) {
                    "CASH" -> IncomeEmerald
                    "NAGAD" -> NagadAccent
                    "BKASH" -> BkashAccent
                    else -> MaterialTheme.colorScheme.primary
                }
                val dropdownBadgeBg = when (selectedBankItem?.shortCode) {
                    "CASH" -> IncomeSurface
                    "NAGAD" -> NagadSurface
                    "BKASH" -> BkashSurface
                    else -> MaterialTheme.colorScheme.primaryContainer
                }

                // Dropdown Anchor Field
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        width = if (dropdownExpanded || selectedBank.isNotBlank()) 1.5.dp else 1.dp,
                        color = if (dropdownExpanded) {
                            MaterialTheme.colorScheme.primary
                        } else if (selectedBank.isNotBlank()) {
                            dropdownAccent
                        } else {
                            MaterialTheme.colorScheme.outline
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { dropdownExpanded = !dropdownExpanded }
                        .testTag("taka_joma_payment_dropdown")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(
                                    if (selectedBank.isNotBlank()) dropdownBadgeBg
                                    else MaterialTheme.colorScheme.primaryContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = dropdownIcon,
                                contentDescription = "Payment method",
                                tint = if (selectedBank.isNotBlank()) dropdownAccent else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedBank.ifBlank { "Select Cash, Nagad, bKash or Bank" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selectedBank.isNotBlank()) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (selectedBank.isNotBlank()) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (selectedBankItem?.category) {
                                    PaymentMethodCategory.CASH -> "Cash • Cash in hand"
                                    PaymentMethodCategory.MFS -> "${selectedBankItem.enName} • Mobile Banking"
                                    PaymentMethodCategory.BANK -> "${selectedBankItem.enName} • ${selectedBankItem.shortCode}"
                                    null -> "Tap to open payment methods (${BangladeshBankDirectory.ALL_PAYMENT_METHODS.size} options)"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (selectedBank.isNotBlank()) {
                            IconButton(
                                onClick = { selectedBank = "" },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear selection",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = if (dropdownExpanded) {
                                Icons.Outlined.KeyboardArrowUp
                            } else {
                                Icons.Outlined.KeyboardArrowDown
                            },
                            contentDescription = if (dropdownExpanded) "Collapse dropdown" else "Expand dropdown",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = dropdownExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        tonalElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
                            .testTag("taka_joma_dropdown_menu")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        ) {
                            // Category filter pills inside dropdown
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == null,
                                        onClick = { selectedCategoryFilter = null },
                                        label = { Text("All (${BangladeshBankDirectory.ALL_PAYMENT_METHODS.size})") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("dropdown_filter_all")
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == PaymentMethodCategory.CASH,
                                        onClick = { selectedCategoryFilter = PaymentMethodCategory.CASH },
                                        label = { Text("Cash") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = IncomeSurface,
                                            selectedLabelColor = IncomeEmerald
                                        ),
                                        modifier = Modifier.testTag("dropdown_filter_cash")
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == PaymentMethodCategory.MFS,
                                        onClick = { selectedCategoryFilter = PaymentMethodCategory.MFS },
                                        label = { Text("Nagad / bKash") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = BkashSurface,
                                            selectedLabelColor = BkashAccent
                                        ),
                                        modifier = Modifier.testTag("dropdown_filter_mfs")
                                    )
                                }
                                item {
                                    FilterChip(
                                        selected = selectedCategoryFilter == PaymentMethodCategory.BANK,
                                        onClick = { selectedCategoryFilter = PaymentMethodCategory.BANK },
                                        label = { Text("All Bank") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.testTag("dropdown_filter_bank")
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            OutlinedTextField(
                                value = bankSearchQuery,
                                onValueChange = { bankSearchQuery = it },
                                placeholder = { Text("Search (Cash, Nagad, bKash, Sonali...)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Search,
                                        contentDescription = "Search methods",
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                trailingIcon = {
                                    if (bankSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { bankSearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Outlined.Close,
                                                contentDescription = "Clear search",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("taka_joma_bank_search_input")
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(240.dp)
                                    .testTag("taka_joma_all_bank_list")
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(filteredBanks, key = { it.enName }) { item ->
                                        val fullLabel = item.displayTitle
                                        val isSelected = selectedBank == fullLabel
                                        val itemAccent = when (item.shortCode) {
                                            "CASH" -> IncomeEmerald
                                            "NAGAD" -> NagadAccent
                                            "BKASH" -> BkashAccent
                                            else -> MaterialTheme.colorScheme.primary
                                        }
                                        val itemSurface = when (item.shortCode) {
                                            "CASH" -> IncomeSurface
                                            "NAGAD" -> NagadSurface
                                            "BKASH" -> BkashSurface
                                            else -> MaterialTheme.colorScheme.primaryContainer
                                        }
                                        val itemIcon = when (item.category) {
                                            PaymentMethodCategory.CASH -> Icons.Outlined.Payments
                                            PaymentMethodCategory.MFS -> Icons.Outlined.Smartphone
                                            PaymentMethodCategory.BANK -> Icons.Outlined.AccountBalance
                                        }
                                        val subtitle = when (item.category) {
                                            PaymentMethodCategory.CASH -> "${item.enName} • Cash in hand"
                                            PaymentMethodCategory.MFS -> "${item.enName} • Mobile Banking"
                                            PaymentMethodCategory.BANK -> "${item.enName} • ${item.shortCode}"
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .minimumInteractiveComponentSize()
                                                .background(
                                                    if (isSelected) {
                                                        itemSurface.copy(alpha = 0.65f)
                                                    } else {
                                                        Color.Transparent
                                                    }
                                                )
                                                .clickable {
                                                    selectedBank = fullLabel
                                                    dropdownExpanded = false
                                                    formError = null
                                                }
                                                .padding(horizontal = 10.dp, vertical = 9.dp)
                                                .testTag("bank_row_${item.shortCode}"),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(RoundedCornerShape(9.dp))
                                                    .background(if (isSelected) itemAccent else itemSurface),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = itemIcon,
                                                    contentDescription = item.bnName,
                                                    tint = if (isSelected) Color.White else itemAccent,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.bnName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = subtitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isSelected) {
                                                    Icons.Filled.CheckCircle
                                                } else {
                                                    Icons.Outlined.RadioButtonUnchecked
                                                },
                                                contentDescription = if (isSelected) "Selected" else "Select",
                                                tint = if (isSelected) {
                                                    itemAccent
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                },
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        HorizontalDivider(
                                            thickness = 0.75.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
                                        )
                                    }

                                    if (filteredBanks.isEmpty() && bankSearchQuery.isNotBlank()) {
                                        item {
                                            val customBank = bankSearchQuery.trim()
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .minimumInteractiveComponentSize()
                                                    .clickable {
                                                        selectedBank = customBank
                                                        dropdownExpanded = false
                                                        formError = null
                                                    }
                                                    .padding(horizontal = 14.dp, vertical = 14.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.AccountBalance,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = "Use \"$customBank\" as payment method",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!formError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formError ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    enabled = !isSubmitting,
                    onClick = {
                        val cleanName = name.trim()
                        val cleanCode = code.trim()
                        val cleanDepot = depot.trim()
                        val cleanBank = selectedBank.trim()
                        val parsedAmount = amountText.trim().toDoubleOrNull()
                        val cleanDate = dateText.trim().ifBlank { BanglaFormatters.todayIso() }

                        when {
                            cleanName.isEmpty() -> formError = "Please enter a name."
                            cleanCode.isEmpty() -> formError = "Please enter a code."
                            cleanDepot.isEmpty() -> formError = "Please enter a depot name."
                            cleanBank.isEmpty() -> formError = "Please select Cash, Nagad, bKash or a bank from the dropdown."
                            parsedAmount == null || parsedAmount <= 0.0 -> {
                                formError = "Please enter a valid amount."
                            }
                            else -> {
                                isSubmitting = true
                                val cleanDesignation = designation.trim()
                                val cleanArea = area.trim()
                                val resolvedMemberId = currentMember?.id
                                    ?: selectedMemberId.ifBlank { "admin_manual" }
                                onSubmit(
                                    pendingSubmissionId,
                                    resolvedMemberId,
                                    cleanName,
                                    cleanCode,
                                    cleanDepot,
                                    cleanBank,
                                    parsedAmount,
                                    cleanDate,
                                    "",
                                    "",
                                    "",
                                    cleanDesignation,
                                    cleanArea
                                ) { ok ->
                                    isSubmitting = false
                                    if (ok) {
                                        pendingSubmissionId = UUID.randomUUID().toString()
                                        amountText = ""
                                        selectedBank = ""
                                        dropdownExpanded = false
                                        selectedCategoryFilter = null
                                        bankSearchQuery = ""
                                        if (currentMember != null) {
                                            applyMemberAutoFill(currentMember)
                                        } else {
                                            val activeMembers = adminMembers.filter { it.isActive }
                                            if (activeMembers.size == 1) {
                                                applyMemberAutoFill(activeMembers.first())
                                            } else if (adminMembers.size == 1) {
                                                applyMemberAutoFill(adminMembers.first())
                                            } else {
                                                name = ""
                                                code = ""
                                                designation = ""
                                                area = ""
                                                depot = ""
                                                selectedMemberId = ""
                                            }
                                        }
                                        formError = null
                                    } else {
                                        formError = "Could not save deposit. Please try again."
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("taka_joma_submit_button")
                ) {
                    Text(if (isSubmitting) "Saving..." else "Save Deposit")
                }
            }
        }

        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Submitted Deposits",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (submissions.isEmpty()) {
                        "No deposit records saved yet."
                    } else {
                        "Tap a row to view details (balance updates after admin approval)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (submissions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                    )
                    submissions.forEachIndexed { index, entry ->
                        TakaJomaListRow(
                            entry = entry,
                            canDelete = isAdminMode,
                            onPreview = { previewSubmission = entry },
                            onDelete = { deletingSubmission = entry }
                        )
                        if (index < submissions.lastIndex) {
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                            )
                        }
                    }
                }
            }
        }
    }

    previewSubmission?.let { item ->
        AlertDialog(
            onDismissRequest = { previewSubmission = null },
            title = { Text("${item.name} • ${BanglaFormatters.formatBdt(item.amount)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        com.example.ui.components.TakaJomaStatusPill(status = item.resolvedStatus)
                    }
                    Text("Code: ${item.code.ifBlank { "-" }}")
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
                    Text("Date: ${BanglaFormatters.formatBanglaDate(item.date)}")
                    if (item.note.isNotBlank()) {
                        Text("Note: ${item.note}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewSubmission = null }) {
                    Text("Close")
                }
            }
        )
    }

    if (isAdminMode) {
        deletingSubmission?.let { item ->
            AlertDialog(
                onDismissRequest = { deletingSubmission = null },
                title = { Text("Delete Deposit Record?") },
                text = {
                    Text("${item.name} (Code: ${item.code}) — ${BanglaFormatters.formatBdt(item.amount)} will be deleted.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onDeleteSubmission(item)
                            deletingSubmission = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingSubmission = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
private fun TakaJomaListRow(
    entry: MemberTakaJoma,
    canDelete: Boolean,
    onPreview: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onPreview)
            .padding(vertical = 10.dp)
            .testTag("taka_joma_row_${entry.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(IncomeSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                contentDescription = null,
                tint = IncomeEmerald,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val memberLine = listOfNotNull(
                "Code: ${entry.code.ifBlank { "-" }}",
                entry.designation.takeIf { it.isNotBlank() },
                entry.area.takeIf { it.isNotBlank() },
                "Depot: ${entry.depot.ifBlank { "-" }}"
            ).joinToString(" • ")
            Text(
                text = memberLine,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (entry.bankName.isNotBlank()) {
                Text(
                    text = "Method / Bank: ${entry.bankName}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val meta = listOfNotNull(
                BanglaFormatters.formatBanglaDate(entry.date).takeIf { entry.date.isNotBlank() },
                entry.note.takeIf { it.isNotBlank() }
            ).joinToString(" • ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = BanglaFormatters.formatBdt(entry.amount),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = IncomeEmerald
            )
            Spacer(modifier = Modifier.height(3.dp))
            com.example.ui.components.TakaJomaStatusPill(status = entry.resolvedStatus)
        }

        if (canDelete) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_taka_joma_${entry.id}")
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "Delete",
                    tint = ExpenseCrimson,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
