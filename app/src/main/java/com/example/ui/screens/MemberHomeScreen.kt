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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.model.MemberTakaJoma
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.BrandHeroEnd
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.IncomeEmerald

@Composable
fun MemberHomeScreen(
    member: Member,
    submissions: List<MemberTakaJoma>,
    onUpdateSales: (Double) -> Unit,
    onOpenTakaJomaForm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSubmitTaka = remember(submissions) { submissions.sumOf { it.amount } }
    val salesAmount = member.salesAmount
    val duesAmount = (salesAmount - totalSubmitTaka).coerceAtLeast(0.0)
    val advanceAmount = (totalSubmitTaka - salesAmount).coerceAtLeast(0.0)

    val currentMonth = remember { BanglaFormatters.currentYearMonth() }
    val monthSubmissions = remember(submissions, currentMonth) {
        submissions.filter { it.date.startsWith(currentMonth) }
    }
    val monthSubmitTotal = remember(monthSubmissions) { monthSubmissions.sumOf { it.amount } }

    var showSalesDialog by remember { mutableStateOf(false) }

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
                            title = "Sales",
                            subtitle = "মোট বিক্রয়",
                            value = BanglaFormatters.formatBdt(salesAmount),
                            highlightColor = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("member_home_sales_box")
                        )
                        HeroSummaryMetricTile(
                            title = "Dues",
                            subtitle = if (advanceAmount > 0.0) "অগ্রিম আছে" else "বকেয়া",
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
                            subtitle = "মোট জমা",
                            value = BanglaFormatters.formatBdt(totalSubmitTaka),
                            highlightColor = Color(0xFFC8F7DC),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("member_home_submit_taka_box")
                        )
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

        // Quick Actions Card for updating Sales & submitting Taka Joma
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "হিসাব ও দ্রুত কার্যক্রম",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sales যোগ বা আপডেট করুন এবং নতুন টাকা জমা ফর্ম দাখিল করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showSalesDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("member_update_sales_button")
                    ) {
                        Icon(
                            imageVector = if (salesAmount > 0.0) Icons.Outlined.Edit else Icons.Outlined.TrendingUp,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sales আপডেট", maxLines = 1)
                    }

                    Button(
                        onClick = onOpenTakaJomaForm,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("member_home_go_taka_joma_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("টাকা জমা ফর্ম", maxLines = 1)
                    }
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

                MemberMetricListRow(
                    label = "মোট সেলস (Sales)",
                    value = BanglaFormatters.formatBdt(salesAmount),
                    valueColor = MaterialTheme.colorScheme.onSurface
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "মোট জমা টাকা (Total Submit Taka)",
                    value = BanglaFormatters.formatBdt(totalSubmitTaka),
                    valueColor = IncomeEmerald
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "বর্তমান বকেয়া (Dues)",
                    value = BanglaFormatters.formatBdt(duesAmount),
                    valueColor = if (duesAmount > 0.0) ExpenseCrimson else IncomeEmerald
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
                )
                MemberMetricListRow(
                    label = "চলতি মাসে জমা (${BanglaFormatters.toBanglaDigits(monthSubmissions.size.toString())}টি)",
                    value = BanglaFormatters.formatBdt(monthSubmitTotal),
                    valueColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showSalesDialog) {
        UpdateMemberSalesDialog(
            currentSales = salesAmount,
            totalSubmitTaka = totalSubmitTaka,
            onDismiss = { showSalesDialog = false },
            onSave = { updatedSales ->
                onUpdateSales(updatedSales)
                showSalesDialog = false
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

@Composable
private fun UpdateMemberSalesDialog(
    currentSales: Double,
    totalSubmitTaka: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var addMode by remember { mutableStateOf(true) }
    var amountInput by remember(addMode) {
        mutableStateOf(
            if (addMode || currentSales <= 0.0) "" else currentSales.toLong().toString()
        )
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val parsed = amountInput.trim().toDoubleOrNull() ?: 0.0
    val previewSales = if (addMode) (currentSales + parsed) else parsed
    val previewDues = (previewSales - totalSubmitTaka).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sales হিসাব আপডেট") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = addMode,
                        onClick = { addMode = true },
                        label = { Text("নতুন যোগ (+)") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    FilterChip(
                        selected = !addMode,
                        onClick = { addMode = false },
                        label = { Text("মোট সেট করুন") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        errorMessage = null
                    },
                    label = {
                        Text(if (addMode) "নতুন Sales টাকার পরিমাণ (৳)" else "সর্বমোট Sales পরিমাণ (৳)")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("member_sales_amount_input")
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "আপডেটের পর মোট Sales: ${BanglaFormatters.formatBdt(previewSales)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "বর্তমান Total Submit: ${BanglaFormatters.formatBdt(totalSubmitTaka)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "হিসাবকৃত Dues: ${BanglaFormatters.formatBdt(previewDues)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (previewDues > 0.0) ExpenseCrimson else IncomeEmerald
                        )
                    }
                }

                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amountInput.trim().toDoubleOrNull()
                    if (value == null || value < 0.0) {
                        errorMessage = "অনুগ্রহ করে সঠিক পরিমাণ লিখুন।"
                        return@Button
                    }
                    val targetSales = if (addMode) currentSales + value else value
                    onSave(targetSales)
                },
                modifier = Modifier.testTag("member_sales_save_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
