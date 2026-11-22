package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
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
import com.example.model.MemberTakaJoma
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface

@Composable
fun AdminMemberTakaJomaTableCard(
    submissions: List<MemberTakaJoma>,
    onDeleteSubmission: (MemberTakaJoma) -> Unit,
    onOpenForm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    var previewItem by remember { mutableStateOf<Pair<Int, MemberTakaJoma>?>(null) }
    var deletingItem by remember { mutableStateOf<MemberTakaJoma?>(null) }

    val totalAmount = remember(submissions) { submissions.sumOf { it.amount } }
    val tableHeaderStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
    val tableBodyStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = 12.sp
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

    val submitColW = remember(submissions, density) {
        measureCol("Submit", submissions.map { formatAdminShortDate(it.date) }, 56.dp, 84.dp)
    }
    val slColW = remember(submissions, density) {
        measureCol("SL", submissions.indices.map { (it + 1).toString().padStart(2, '0') }, 32.dp, 46.dp)
    }
    val codeColW = remember(submissions, density) {
        measureCol("Code", submissions.map { it.code }, 52.dp, 96.dp)
    }
    val nameColW = remember(submissions, density) {
        measureCol("Name", submissions.map { it.name }, 74.dp, 150.dp)
    }
    val desigColW = remember(submissions, density) {
        measureCol("Desig:", submissions.map { it.designation }, 52.dp, 120.dp)
    }
    val areaColW = remember(submissions, density) {
        measureCol("Area", submissions.map { it.area }, 56.dp, 130.dp)
    }
    val depotColW = remember(submissions, density) {
        measureCol("Depot", submissions.map { it.depot }, 60.dp, 110.dp)
    }
    val methodColW = remember(submissions, density) {
        val methods = submissions.map { it.bankName.substringBefore(" (").ifBlank { "-" } }
        measureCol("মাধ্যম", methods, 64.dp, 128.dp)
    }
    val takaColW = remember(submissions, density) {
        measureCol("Taka", submissions.map { BanglaFormatters.formatBdt(it.amount) }, 64.dp, 116.dp)
    }
    val slipColW = 54.dp

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
                        text = "মেম্বার টাকা জমা (ফর্ম অনুযায়ী)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "মেম্বারদের দাখিলকৃত টাকা ও স্লিপের তালিকা",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onOpenForm != null) {
                    TextButton(
                        onClick = onOpenForm,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("admin_lenden_open_taka_joma")
                    ) {
                        Text("+ ফর্ম")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "মোট এন্ট্রি: ${BanglaFormatters.toBanglaDigits(submissions.size.toString())} টি",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IncomeSurface
                ) {
                    Text(
                        text = "মোট জমা: ${BanglaFormatters.formatBdt(totalAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = IncomeEmerald,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (submissions.isEmpty()) {
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
                        text = "কোনো মেম্বার টাকা জমা দিলে ফর্মের হেডার অনুযায়ী এখানে যুক্ত হবে।",
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
                        areaColW + depotColW + methodColW + takaColW + slipColW + borderOverhead
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
                            // Header row matching form headers
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
                                AdminTableHeaderCell("মাধ্যম", resolvedMethodW, tableHeaderStyle)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Taka", resolvedTakaW, tableHeaderStyle, TextAlign.End)
                                AdminTableVerticalBorder(borderColor)
                                AdminTableHeaderCell("Slip", slipColW, tableHeaderStyle, TextAlign.Center)
                            }

                            HorizontalDivider(thickness = 1.dp, color = borderColor)

                            submissions.forEachIndexed { index, entry ->
                                val sl = index + 1
                                val rowBg = if (index % 2 == 0) {
                                    MaterialTheme.colorScheme.surface
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                }
                                val slipBitmap = remember(entry.photoPath) {
                                    MemberPhotoStorage.loadBitmapFromPath(entry.photoPath)
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
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                ),
                                                color = MaterialTheme.colorScheme.onSurface,
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
                                            color = IncomeEmerald,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AdminTableVerticalBorder(borderColor)

                                    // Slip
                                    Box(
                                        modifier = Modifier
                                            .width(slipColW)
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

    previewItem?.let { (sl, item) ->
        val bmp = remember(item.photoPath) { MemberPhotoStorage.loadBitmapFromPath(item.photoPath) }
        AlertDialog(
            onDismissRequest = { previewItem = null },
            title = { Text("SL ${sl.toString().padStart(2, '0')} • ${item.name}") },
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
            title = { Text("মেম্বার জমা মুছে ফেলবেন?") },
            text = {
                Text("${item.name} (${item.code}) — ${BanglaFormatters.formatBdt(item.amount)} মুছে ফেলা হবে।")
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
