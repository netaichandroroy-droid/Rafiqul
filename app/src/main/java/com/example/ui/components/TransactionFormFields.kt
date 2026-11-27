package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.AccountType
import com.example.model.BanglaFormatters
import com.example.model.HisabCategories
import com.example.model.TxType
import com.example.ui.theme.BrandHeroStart
import com.example.ui.theme.CardInnerTileSolid
import com.example.ui.theme.ExpenseCrimson
import com.example.ui.theme.GlassTextPrimary
import com.example.ui.theme.GlassTextSecondary
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.MetallicSilverRim
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionFormFields(
    selectedType: TxType,
    onTypeChange: (TxType) -> Unit,
    amountText: String,
    onAmountChange: (String) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    noteText: String,
    onNoteChange: (String) -> Unit,
    dateIso: String,
    onDateChange: (String) -> Unit,
    selectedAccount: AccountType,
    onAccountChange: (AccountType) -> Unit,
    validationError: String?,
    isSaving: Boolean,
    submitLabel: String,
    tagPrefix: String,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val categories = remember(selectedType) { HisabCategories.forType(selectedType) }
    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0

    val isIncome = selectedType == TxType.INCOME
    val previewAccent = if (isIncome) NeonEmerald else ExpenseCrimson
    val previewFg = if (isIncome) IncomeEmerald else ExpenseCrimson
    val formAccent = if (isIncome) NeonEmerald else NeonCyan

    val segmentedColors = SegmentedButtonDefaults.colors(
        activeContainerColor = formAccent.copy(alpha = 0.24f),
        activeContentColor = Color.White,
        activeBorderColor = formAccent.copy(alpha = 0.90f),
        inactiveContainerColor = CardInnerTileSolid,
        inactiveContentColor = GlassTextSecondary,
        inactiveBorderColor = MetallicSilverRim.copy(alpha = 0.35f)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Live BDT (৳) Currency Display Banner
        GlossyInnerMetricTile(
            accentColor = previewAccent,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${tagPrefix}_bdt_preview")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isIncome) "Daily Income (BDT)" else "Daily Expense (BDT)",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTextSecondary
                    )
                    Text(
                        text = BanglaFormatters.formatSignedBdt(parsedAmount, selectedType),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = previewFg
                    )
                }
                Text(
                    text = BanglaFormatters.formatBanglaDate(dateIso),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = previewFg
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .hisab3DSegmentedShell(accentColor = formAccent)
        ) {
            TxType.entries.forEachIndexed { index, txType ->
                SegmentedButton(
                    selected = selectedType == txType,
                    onClick = { onTypeChange(txType) },
                    shape = SegmentedButtonDefaults.itemShape(index, TxType.entries.size),
                    colors = segmentedColors,
                    modifier = Modifier.testTag("${tagPrefix}_type_${txType.wireValue}")
                ) {
                    Text(txType.bnLabel)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        onAmountChange(filtered)
                    }
                },
                label = { Text("Amount (BDT ৳)") },
                prefix = {
                    Text(
                        text = "৳ ",
                        fontWeight = FontWeight.Bold,
                        color = previewFg
                    )
                },
                placeholder = { Text("0") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(12.dp),
                colors = hisabFormFieldColors(accentColor = formAccent),
                modifier = Modifier
                    .weight(1.2f)
                    .hisab3DFieldDepth(accentColor = formAccent)
                    .testTag("${tagPrefix}_amount_input")
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = CardInnerTileSolid,
                    contentColor = GlassTextPrimary
                ),
                border = BorderStroke(1.dp, MetallicSilverRim.copy(alpha = 0.42f)),
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 6.dp)
                    .height(52.dp)
                    .hisab3DButtonSurface(accentColor = NeonCyan, cornerRadius = 12.dp)
                    .testTag("${tagPrefix}_date_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = "Select Date",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = BanglaFormatters.formatBanglaDate(dateIso),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Category",
            style = MaterialTheme.typography.labelLarge,
            color = GlassTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategoryChange(cat) },
                    label = { Text(cat) },
                    colors = hisabFilterChipColors(accentColor = formAccent),
                    border = hisabFilterChipBorder(selected = isSelected, accentColor = formAccent)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = noteText,
            onValueChange = { if (it.length <= 200) onNoteChange(it) },
            label = { Text("Notes / Description") },
            placeholder = { Text("e.g., Office supplies, salary, travel...") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = hisabFormFieldColors(accentColor = formAccent),
            modifier = Modifier
                .fillMaxWidth()
                .hisab3DFieldDepth(accentColor = formAccent)
                .testTag("${tagPrefix}_note_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Account",
            style = MaterialTheme.typography.labelLarge,
            color = GlassTextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .hisab3DSegmentedShell(accentColor = formAccent)
        ) {
            AccountType.entries.forEachIndexed { index, account ->
                SegmentedButton(
                    selected = selectedAccount == account,
                    onClick = { onAccountChange(account) },
                    shape = SegmentedButtonDefaults.itemShape(index, AccountType.entries.size),
                    colors = segmentedColors,
                    modifier = Modifier.testTag("${tagPrefix}_account_${account.wireValue}")
                ) {
                    Text(account.bnShort)
                }
            }
        }

        if (!validationError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = validationError,
                style = MaterialTheme.typography.bodyMedium,
                color = ExpenseCrimson
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            enabled = !isSaving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandHeroStart,
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .hisab3DButtonSurface(accentColor = BrandHeroStart, cornerRadius = 14.dp)
                .testTag("${tagPrefix}_save_button")
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(submitLabel, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = remember(dateIso) {
            BanglaFormatters.isoDateToUtcMillis(dateIso) ?: System.currentTimeMillis()
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChange(BanglaFormatters.millisToIsoDate(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.cancel_action))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
