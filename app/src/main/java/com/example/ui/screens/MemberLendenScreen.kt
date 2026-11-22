package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
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
import com.example.data.MemberPhotoStorage
import com.example.model.BanglaFormatters
import com.example.model.HisabTransaction
import com.example.model.MemberTakaJoma
import com.example.model.TxType
import com.example.ui.components.SectionPanelCard
import com.example.ui.components.TransactionItemRow
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeEmeraldDark
import com.example.ui.theme.IncomeSurface

@Composable
fun MemberLendenScreen(
    submissions: List<MemberTakaJoma>,
    transactions: List<HisabTransaction>,
    onOpenTakaJomaForm: () -> Unit,
    onDeleteSubmission: (MemberTakaJoma) -> Unit,
    onAddPersonalTx: (TxType) -> Unit,
    onEditPersonalTx: (HisabTransaction) -> Unit,
    onDeletePersonalTx: (HisabTransaction) -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val tableHorizontalScroll = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var previewItem by remember { mutableStateOf<Pair<Int, MemberTakaJoma>?>(null) }
    var deletingItem by remember { mutableStateOf<MemberTakaJoma?>(null) }

    val currentMonth = remember { BanglaFormatters.currentYearMonth() }
    val monthTotalTaka = remember(submissions, currentMonth) {
        submissions.filter { it.date.startsWith(currentMonth) }.sumOf { it.amount }
    }
    val allTimeTaka = remember(submissions) { submissions.sumOf { it.amount } }

    val tableHeaderStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
    val tableBodyStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp
    )

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
        val widestDp = with(density) { maxOf(headerPx, maxValPx).toDp() } + 12.dp
        return widestDp.coerceIn(minWidth, maxWidth)
    }

    val baseSubmitW = remember(submissions, density) {
        val dateLabels = submissions.map { formatShortSubmitDate(it.date) }
        measureColumnWidth("Submit", dateLabels, minWidth = 56.dp, maxWidth = 90.dp)
    }
    val baseSlW = remember(submissions, density) {
        val slLabels = submissions.indices.map { (it + 1).toString().padStart(2, '0') }
        measureColumnWidth("SL", slLabels, minWidth = 30.dp, maxWidth = 44.dp)
    }
    val baseNameW = remember(submissions, density) {
        measureColumnWidth("Name", submissions.map { it.name }, minWidth = 66.dp, maxWidth = 170.dp)
    }
    val baseTakaW = remember(submissions, density) {
        val takaLabels = submissions.map { BanglaFormatters.formatBdt(it.amount) }
        measureColumnWidth("Taka", takaLabels, minWidth = 62.dp, maxWidth = 120.dp)
    }
    val slipColWidth = 54.dp

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionPanelCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_lenden_summary_card")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "মেম্বার লেনদেন (টাকা জমা)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "জমা দেওয়া সকল টাকা ও স্লিপের তালিকা",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = onOpenTakaJomaForm,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("member_lenden_add_joma_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("টাকা জমা")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LendenStatTile(
                        label = "মোট এন্ট্রি",
                        value = "${BanglaFormatters.toBanglaDigits(submissions.size.toString())} টি",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    LendenStatTile(
                        label = "চলতি মাস",
                        value = BanglaFormatters.formatBdt(monthTotalTaka),
                        containerColor = IncomeSurface,
                        contentColor = IncomeEmeraldDark,
                        modifier = Modifier.weight(1f)
                    )
                    LendenStatTile(
                        label = "সর্বমোট টাকা",
                        value = BanglaFormatters.formatBdt(allTimeTaka),
                        containerColor = IncomeSurface,
                        contentColor = IncomeEmerald,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("member_lenden_table_card"),
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                if (submissions.isEmpty()) {
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
                                imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "এখনো কোনো টাকা জমা এন্ট্রি নেই। 'টাকা জমা' ফর্ম থেকে জমা দিলে এখানে Submit, SL, Name, Taka ও Slip আকারে দেখাবে।",
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
                        val borderOverhead = 6.dp
                        val baseTotal = baseSubmitW + baseSlW + baseNameW + baseTakaW +
                            slipColWidth + borderOverhead
                        val extraWidth = (maxWidth - baseTotal).coerceAtLeast(0.dp)
                        val submitColWidth = baseSubmitW + (extraWidth * 0.20f)
                        val nameColWidth = baseNameW + (extraWidth * 0.48f)
                        val takaColWidth = baseTakaW + (extraWidth * 0.32f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(tableHorizontalScroll)
                        ) {
                            Column(
                                modifier = Modifier.border(1.dp, borderColor)
                            ) {
                                // Header Row: Submit | SL | Name | Taka | Slip
                                Row(
                                    modifier = Modifier
                                        .height(IntrinsicSize.Min)
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .testTag("member_lenden_table_header"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LendenHeaderCell("Submit", submitColWidth, tableHeaderStyle, textAlign = TextAlign.Center)
                                    LendenVerticalBorder(borderColor)
                                    LendenHeaderCell("SL", baseSlW, tableHeaderStyle, textAlign = TextAlign.Center)
                                    LendenVerticalBorder(borderColor)
                                    LendenHeaderCell("Name", nameColWidth, tableHeaderStyle)
                                    LendenVerticalBorder(borderColor)
                                    LendenHeaderCell("Taka", takaColWidth, tableHeaderStyle, textAlign = TextAlign.End)
                                    LendenVerticalBorder(borderColor)
                                    LendenHeaderCell("Slip", slipColWidth, tableHeaderStyle, textAlign = TextAlign.Center)
                                }

                                HorizontalDivider(thickness = 1.dp, color = borderColor)

                                submissions.forEachIndexed { index, entry ->
                                    val slNumber = index + 1
                                    val rowBg = if (index % 2 == 0) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                    }
                                    val slipBitmap = remember(entry.photoPath) {
                                        MemberPhotoStorage.loadBitmapFromPath(entry.photoPath)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .height(IntrinsicSize.Min)
                                            .background(rowBg)
                                            .clickable { previewItem = slNumber to entry }
                                            .testTag("member_lenden_row_${entry.id}"),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // 1. Submit
                                        Box(
                                            modifier = Modifier
                                                .width(submitColWidth)
                                                .fillMaxHeight()
                                                .padding(horizontal = 4.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = formatShortSubmitDate(entry.date),
                                                    style = tableBodyStyle.copy(
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "জমা",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                    color = IncomeEmerald,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        LendenVerticalBorder(borderColor)

                                        // 2. SL
                                        Box(
                                            modifier = Modifier
                                                .width(baseSlW)
                                                .fillMaxHeight()
                                                .padding(horizontal = 4.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = slNumber.toString().padStart(2, '0'),
                                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        LendenVerticalBorder(borderColor)

                                        // 3. Name
                                        Box(
                                            modifier = Modifier
                                                .width(nameColWidth)
                                                .fillMaxHeight()
                                                .padding(horizontal = 6.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            Column {
                                                Text(
                                                    text = entry.name,
                                                    style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                val subInfo = listOfNotNull(
                                                    entry.code.takeIf { it.isNotBlank() },
                                                    entry.bankName.takeIf { it.isNotBlank() }?.substringBefore(" (")
                                                ).joinToString(" • ")
                                                if (subInfo.isNotBlank()) {
                                                    Text(
                                                        text = subInfo,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        LendenVerticalBorder(borderColor)

                                        // 4. Taka
                                        Box(
                                            modifier = Modifier
                                                .width(takaColWidth)
                                                .fillMaxHeight()
                                                .padding(horizontal = 6.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Text(
                                                text = BanglaFormatters.formatBdt(entry.amount),
                                                style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                                color = IncomeEmerald,
                                                textAlign = TextAlign.End,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        LendenVerticalBorder(borderColor)

                                        // 5. Slip
                                        Box(
                                            modifier = Modifier
                                                .width(slipColWidth)
                                                .fillMaxHeight()
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (slipBitmap != null) {
                                                Image(
                                                    bitmap = slipBitmap.asImageBitmap(),
                                                    contentDescription = "${entry.name} স্লিপ",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .border(
                                                            0.75.dp,
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                            RoundedCornerShape(6.dp)
                                                        )
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(34.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Image,
                                                        contentDescription = "স্লিপ নেই",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (index < submissions.lastIndex) {
                                        HorizontalDivider(thickness = 1.dp, color = borderColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (transactions.isNotEmpty()) {
            item {
                SectionPanelCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ব্যক্তিগত আয়-ব্যয় লেনদেন",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        TextButton(onClick = { onAddPersonalTx(TxType.EXPENSE) }) {
                            Text("+ যোগ করুন")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    transactions.forEachIndexed { index, tx ->
                        TransactionItemRow(
                            transaction = tx,
                            onEdit = onEditPersonalTx,
                            onDelete = onDeletePersonalTx,
                            showDivider = index < transactions.lastIndex
                        )
                    }
                }
            }
        }
    }

    previewItem?.let { (sl, item) ->
        val bmp = remember(item.photoPath) { MemberPhotoStorage.loadBitmapFromPath(item.photoPath) }
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = {
                Text("SL ${sl.toString().padStart(2, '0')} • ${item.name}")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "${item.name} স্লিপ ছবি",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Text("Submit তারিখ: ${BanglaFormatters.formatBanglaDate(item.date)}")
                    Text("কোড (Code): ${item.code.ifBlank { "-" }}")
                    Text("নাম (Name): ${item.name}")
                    if (item.designation.isNotBlank()) {
                        Text("পদবি (Desig:): ${item.designation}")
                    }
                    if (item.area.isNotBlank()) {
                        Text("এরিয়া (Area): ${item.area}")
                    }
                    Text("ডিপো (Depot): ${item.depot.ifBlank { "-" }}")
                    if (item.bankName.isNotBlank()) {
                        Text("মাধ্যম / ব্যাংক: ${item.bankName}")
                    }
                    Text(
                        text = "টাকা (Taka): ${BanglaFormatters.formatBdt(item.amount)}",
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald
                    )
                    if (item.note.isNotBlank()) {
                        Text("মন্তব্য: ${item.note}")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { previewItem = null }) {
                    Text("বন্ধ করুন")
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
                    Text("মুছুন")
                }
            }
        )
    }

    deletingItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = { Text("এন্ট্রি মুছে ফেলবেন?") },
            text = {
                Text("${item.name} — ${BanglaFormatters.formatBdt(item.amount)} মুছে ফেলা হবে।")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSubmission(item)
                        deletingItem = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

private fun formatShortSubmitDate(isoDate: String): String {
    val cleaned = isoDate.trim()
    val parts = cleaned.split("-")
    if (parts.size == 3) {
        return BanglaFormatters.toBanglaDigits("${parts[2]}/${parts[1]}/${parts[0].takeLast(2)}")
    }
    return cleaned.ifBlank { "-" }
}

@Composable
private fun LendenHeaderCell(
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
private fun LendenVerticalBorder(color: Color) {
    VerticalDivider(
        thickness = 1.dp,
        color = color,
        modifier = Modifier.fillMaxHeight()
    )
}

@Composable
private fun LendenStatTile(
    label: String,
    value: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
