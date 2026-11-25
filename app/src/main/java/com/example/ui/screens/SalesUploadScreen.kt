package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.ReportPictureRenderer
import com.example.data.SalesSheetParser
import com.example.model.BanglaFormatters
import com.example.model.Member
import com.example.model.MonthlySalesRecord
import com.example.ui.components.MonthlySalesTableCard
import com.example.ui.components.SectionPanelCard
import com.example.ui.theme.IncomeEmerald
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SalesUploadScreen(
    salesRecords: List<MonthlySalesRecord>,
    selectedMonth: String,
    members: List<Member>,
    onShiftMonth: (Int) -> Unit,
    onUploadMonthSales: (yearMonth: String, records: List<MonthlySalesRecord>, replaceMonth: Boolean, onComplete: (Boolean) -> Unit) -> Unit,
    onSaveSingleRecord: (yearMonth: String, editingId: String?, code: String, name: String, tpSales: Double, tpPlusVat: Double, onComplete: (Boolean) -> Unit) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onClearMonth: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val monthRecords = remember(salesRecords, selectedMonth) {
        salesRecords.filter { it.yearMonth == selectedMonth }
    }
    val totalTpSales = remember(monthRecords) { monthRecords.sumOf { it.tpSales } }
    val totalTpPlusVat = remember(monthRecords) { monthRecords.sumOf { it.tpPlusVat } }

    var replaceOnUpload by remember { mutableStateOf(true) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var showManualDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<MonthlySalesRecord?>(null) }
    var selectedActionRecord by remember { mutableStateOf<MonthlySalesRecord?>(null) }
    var showClearMonthConfirm by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val (fileName, bytes) = withContext(Dispatchers.IO) {
                    val name = queryFileName(context, uri)
                    val content = runCatching {
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }.getOrNull()
                    name to content
                }
                if (bytes == null) {
                    onShowMessage("Could not read file")
                    return@launch
                }
                val parsed = SalesSheetParser.parseBytes(
                    bytes = bytes,
                    yearMonth = selectedMonth,
                    fileName = fileName
                )
                parsed.onSuccess { records ->
                    onUploadMonthSales(selectedMonth, records, replaceOnUpload) { }
                }.onFailure { err ->
                    onShowMessage(err.localizedMessage ?: "Could not parse file")
                }
            }
        }
    }

    val sampleCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            out.write(SalesSheetParser.buildSampleCsv(members).toByteArray(Charsets.UTF_8))
                        }
                    }.isSuccess
                }
                onShowMessage(
                    if (ok) "Sample CSV template saved" else "Failed to save template"
                )
            }
        }
    }

    val exportMonthPictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) {
                    runCatching {
                        val bitmap = ReportPictureRenderer.renderSalesReportBitmap(
                            context = context,
                            salesRecords = monthRecords,
                            selectedMonth = selectedMonth
                        )
                        context.contentResolver.openOutputStream(uri)?.use { out ->
                            ReportPictureRenderer.writeBitmapAsPng(bitmap, out)
                        } ?: run {
                            bitmap.recycle()
                            false
                        }
                    }.getOrDefault(false)
                }
                onShowMessage(
                    if (ok) "Monthly Sales PNG saved" else "Failed to save picture"
                )
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Month Selector & Summary Card
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Sales Upload",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
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
                                .testTag("sales_prev_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous month"
                            )
                        }
                        Text(
                            text = BanglaFormatters.formatBanglaMonth(selectedMonth),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.testTag("sales_selected_month_text")
                        )
                        IconButton(
                            onClick = { onShiftMonth(1) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sales_next_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next month"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Total TP sales",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BanglaFormatters.formatBdt(totalTpSales),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Total TP+VAT sales",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = BanglaFormatters.formatBdt(totalTpPlusVat),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = IncomeEmerald
                            )
                        }
                    }
                }
            }
        }

        // Upload & Entry Controls Card
        item {
            SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "File Upload & Entry",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = replaceOnUpload,
                        onClick = { replaceOnUpload = true },
                        label = { Text("Replace Month") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    FilterChip(
                        selected = !replaceOnUpload,
                        onClick = { replaceOnUpload = false },
                        label = { Text("Append") },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "text/csv",
                                "text/comma-separated-values",
                                "text/tab-separated-values",
                                "text/plain",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "*/*"
                            )
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("sales_upload_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FileUpload,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload CSV / Excel (.xlsx)")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPasteDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sales_paste_table_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentPaste,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste Table", maxLines = 1)
                    }
                    OutlinedButton(
                        onClick = {
                            editingRecord = null
                            showManualDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("sales_manual_add_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Manual", maxLines = 1)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { sampleCsvLauncher.launch("sales-template-$selectedMonth.csv") },
                        modifier = Modifier.testTag("sales_download_template_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sample Sheet")
                    }
                    if (monthRecords.isNotEmpty()) {
                        TextButton(
                            onClick = { exportMonthPictureLauncher.launch("sales-report-$selectedMonth.png") },
                            modifier = Modifier.testTag("sales_export_picture_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download PNG")
                        }
                        TextButton(
                            onClick = { showClearMonthConfirm = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("sales_clear_month_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Clear Month")
                        }
                    }
                }
            }
        }

        // Uploaded Sales Table Preview Card
        item {
            MonthlySalesTableCard(
                title = "${BanglaFormatters.formatBanglaMonth(selectedMonth)} — Sales List",
                records = monthRecords,
                emptyMessage = "No sales uploaded for ${BanglaFormatters.formatBanglaMonth(selectedMonth)} yet.",
                onRowClick = { record -> selectedActionRecord = record }
            )
        }
    }

    if (showPasteDialog) {
        PasteSalesTableDialog(
            yearMonth = selectedMonth,
            onDismiss = { showPasteDialog = false },
            onImport = { parsedRecords ->
                onUploadMonthSales(selectedMonth, parsedRecords, replaceOnUpload) { ok ->
                    if (ok) showPasteDialog = false
                }
            }
        )
    }

    if (showManualDialog) {
        SalesRecordEditorDialog(
            editingRecord = editingRecord,
            members = members,
            onDismiss = {
                showManualDialog = false
                editingRecord = null
            },
            onSave = { code, name, tpSales, tpPlusVat ->
                onSaveSingleRecord(
                    selectedMonth,
                    editingRecord?.id,
                    code,
                    name,
                    tpSales,
                    tpPlusVat
                ) { ok ->
                    if (ok) {
                        showManualDialog = false
                        editingRecord = null
                    }
                }
            }
        )
    }

    selectedActionRecord?.let { record ->
        AlertDialog(
            onDismissRequest = { selectedActionRecord = null },
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
                TextButton(
                    onClick = {
                        editingRecord = record
                        selectedActionRecord = null
                        showManualDialog = true
                    }
                ) {
                    Text("Edit")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            onDeleteRecord(record.id)
                            selectedActionRecord = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                    TextButton(onClick = { selectedActionRecord = null }) {
                        Text("Close")
                    }
                }
            }
        )
    }

    if (showClearMonthConfirm) {
        AlertDialog(
            onDismissRequest = { showClearMonthConfirm = false },
            title = { Text("Clear All Sales for This Month?") },
            text = {
                Text("${monthRecords.size} sales record(s) for ${BanglaFormatters.formatBanglaMonth(selectedMonth)} will be deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearMonth(selectedMonth)
                        showClearMonthConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearMonthConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PasteSalesTableDialog(
    yearMonth: String,
    onDismiss: () -> Unit,
    onImport: (List<MonthlySalesRecord>) -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Excel / CSV Data") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Paste data copied from Excel or Google Sheets (columns: code, name, TP sales, TP+VAT sales):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = rawText,
                    onValueChange = {
                        rawText = it
                        errorText = null
                    },
                    placeholder = {
                        Text("code,name,TP sales,TP+VAT sales\nC-101,Rafiqul Islam,50000,57500")
                    },
                    minLines = 5,
                    maxLines = 8,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_paste_text_input")
                )
                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = SalesSheetParser.parseDelimitedText(rawText, yearMonth)
                    result.onSuccess { parsed ->
                        onImport(parsed)
                    }.onFailure { err ->
                        errorText = err.localizedMessage ?: "Please enter data in a valid format."
                    }
                },
                modifier = Modifier.testTag("sales_paste_import_button")
            ) {
                Text("Upload")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun SalesRecordEditorDialog(
    editingRecord: MonthlySalesRecord?,
    members: List<Member>,
    onDismiss: () -> Unit,
    onSave: (code: String, name: String, tpSales: Double, tpPlusVat: Double) -> Unit
) {
    var code by remember(editingRecord) { mutableStateOf(editingRecord?.code ?: "") }
    var name by remember(editingRecord) { mutableStateOf(editingRecord?.name ?: "") }
    var tpSalesInput by remember(editingRecord) {
        mutableStateOf(editingRecord?.let { formatEditableAmount(it.tpSales) } ?: "")
    }
    var tpVatInput by remember(editingRecord) {
        mutableStateOf(editingRecord?.let { formatEditableAmount(it.tpPlusVat) } ?: "")
    }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (editingRecord == null) "Add Sales Record" else "Edit Sales Record")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (members.isNotEmpty() && editingRecord == null) {
                    Text(
                        text = "Auto-fill from Member:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        members.forEach { member ->
                            FilterChip(
                                selected = name.equals(member.name, ignoreCase = true) &&
                                    code.equals(member.code, ignoreCase = true),
                                onClick = {
                                    code = member.code
                                    name = member.name
                                    errorText = null
                                },
                                label = {
                                    val label = if (member.code.isNotBlank()) {
                                        "${member.code} - ${member.name}"
                                    } else {
                                        member.name
                                    }
                                    Text(label, maxLines = 1)
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        errorText = null
                    },
                    label = { Text("code") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_manual_code_input")
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorText = null
                    },
                    label = { Text("name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_manual_name_input")
                )

                OutlinedTextField(
                    value = tpSalesInput,
                    onValueChange = {
                        tpSalesInput = it
                        errorText = null
                    },
                    label = { Text("TP sales (৳)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_manual_tp_input")
                )

                OutlinedTextField(
                    value = tpVatInput,
                    onValueChange = {
                        tpVatInput = it
                        errorText = null
                    },
                    label = { Text("TP+VAT sales (৳)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sales_manual_tp_vat_input")
                )

                if (!errorText.isNullOrBlank()) {
                    Text(
                        text = errorText ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = name.trim()
                    val cleanCode = code.trim()
                    val tpSales = SalesSheetParser.parseAmount(tpSalesInput)
                    val tpVat = SalesSheetParser.parseAmount(tpVatInput)
                    if (cleanName.isEmpty() && cleanCode.isEmpty()) {
                        errorText = "Please enter a code or name."
                        return@Button
                    }
                    if (tpSales == null || tpVat == null) {
                        errorText = "Please enter valid TP sales and TP+VAT sales amounts."
                        return@Button
                    }
                    onSave(cleanCode, cleanName.ifBlank { cleanCode }, tpSales, tpVat)
                },
                modifier = Modifier.testTag("sales_manual_save_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatEditableAmount(value: Double): String {
    val asLong = value.toLong()
    return if (value == asLong.toDouble()) asLong.toString() else value.toString()
}

private fun queryFileName(context: android.content.Context, uri: Uri): String? {
    return runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
    }.getOrNull()
}
