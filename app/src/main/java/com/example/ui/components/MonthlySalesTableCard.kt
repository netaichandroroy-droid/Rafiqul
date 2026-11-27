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
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
import com.example.model.MonthlySalesRecord
import com.example.ui.theme.IncomeEmerald
import com.example.ui.theme.IncomeSurface

@Composable
fun MonthlySalesTableCard(
    title: String,
    subtitle: String = "",
    records: List<MonthlySalesRecord>,
    emptyMessage: String,
    highlightedMemberCode: String = "",
    highlightedMemberName: String = "",
    headerAction: (@Composable () -> Unit)? = null,
    filterContent: (@Composable () -> Unit)? = null,
    onRowClick: ((MonthlySalesRecord) -> Unit)? = null,
    embeddedInOuterCard: Boolean = false,
    modifier: Modifier = Modifier
) {
    val borderColor = MaterialTheme.colorScheme.outline
    val scrollState = rememberScrollState()
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val totalTpSales = remember(records) { records.sumOf { it.tpSales } }
    val totalTpPlusVat = remember(records) { records.sumOf { it.tpPlusVat } }

    val tableHeaderStyle = MaterialTheme.typography.labelMedium.copy(
        fontSize = 12.5.sp,
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

    val codeColW = remember(records, density) {
        measureCol("Code", records.map { it.code }, 56.dp, 100.dp)
    }
    val nameColW = remember(records, density) {
        measureCol("Name", records.map { it.name }, 92.dp, 180.dp)
    }
    val tpSalesColW = remember(records, totalTpSales, density) {
        measureCol(
            "TP sales",
            records.map { BanglaFormatters.formatBdt(it.tpSales) } + BanglaFormatters.formatBdt(totalTpSales),
            80.dp,
            130.dp
        )
    }
    val tpVatColW = remember(records, totalTpPlusVat, density) {
        measureCol(
            "TP+VAT sales",
            records.map { BanglaFormatters.formatBdt(it.tpPlusVat) } + BanglaFormatters.formatBdt(totalTpPlusVat),
            90.dp,
            146.dp
        )
    }

    val tableBorderColor = if (embeddedInOuterCard) borderColor.copy(alpha = 0.55f) else borderColor

    val tableBody: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = if (embeddedInOuterCard) 4.dp else 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!embeddedInOuterCard && subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                headerAction?.invoke()
            }

            if (filterContent != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                    filterContent()
                }
            }

            if (!embeddedInOuterCard) {
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
                            text = "Entries: ${records.size}",
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
                            text = "Total TP+VAT sales: ${BanglaFormatters.formatBdt(totalTpPlusVat)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IncomeEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (records.isEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = emptyMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (embeddedInOuterCard) 14.dp else 6.dp)
                ) {
                    val borderOverhead = 5.dp
                    val baseTotal = codeColW + nameColW + tpSalesColW + tpVatColW + borderOverhead
                    val extraWidth = (maxWidth - baseTotal).coerceAtLeast(0.dp)
                    val resolvedCodeW = codeColW + (extraWidth * 0.14f)
                    val resolvedNameW = nameColW + (extraWidth * 0.36f)
                    val resolvedTpSalesW = tpSalesColW + (extraWidth * 0.25f)
                    val resolvedTpVatW = tpVatColW + (extraWidth * 0.25f)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scrollState)
                    ) {
                        val tableShape = RoundedCornerShape(if (embeddedInOuterCard) 10.dp else 0.dp)
                        Column(
                            modifier = Modifier
                                .clip(tableShape)
                                .border(if (embeddedInOuterCard) 0.8.dp else 1.dp, tableBorderColor, tableShape)
                        ) {
                            // Header row with requested columns: code, name, TP sales, TP+Vat
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .testTag("monthly_sales_table_header"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SalesTableHeaderCell("Code", resolvedCodeW, tableHeaderStyle, TextAlign.Center)
                                SalesTableVerticalBorder(borderColor)
                                SalesTableHeaderCell("Name", resolvedNameW, tableHeaderStyle, TextAlign.Start)
                                SalesTableVerticalBorder(borderColor)
                                SalesTableHeaderCell("TP sales", resolvedTpSalesW, tableHeaderStyle, TextAlign.End)
                                SalesTableVerticalBorder(borderColor)
                                SalesTableHeaderCell("TP+VAT sales", resolvedTpVatW, tableHeaderStyle, TextAlign.End)
                            }

                            HorizontalDivider(thickness = 1.dp, color = borderColor)

                            records.forEachIndexed { index, record ->
                                val isMine = isMatchingMember(
                                    record = record,
                                    memberCode = highlightedMemberCode,
                                    memberName = highlightedMemberName
                                )
                                val rowBg = when {
                                    isMine -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
                                    index % 2 == 0 -> MaterialTheme.colorScheme.surface
                                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                                }

                                Row(
                                    modifier = Modifier
                                        .height(IntrinsicSize.Min)
                                        .background(rowBg)
                                        .then(
                                            if (onRowClick != null) {
                                                Modifier.clickable { onRowClick(record) }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .testTag("monthly_sales_row_${record.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // code
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedCodeW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = record.code.ifBlank { "-" },
                                            style = tableBodyStyle.copy(
                                                fontWeight = if (isMine) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isMine) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    SalesTableVerticalBorder(borderColor)

                                    // name
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedNameW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = record.name,
                                            style = tableBodyStyle.copy(
                                                fontWeight = if (isMine) FontWeight.Bold else FontWeight.SemiBold
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    SalesTableVerticalBorder(borderColor)

                                    // TP sales
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedTpSalesW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = BanglaFormatters.formatBdt(record.tpSales),
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Medium),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    SalesTableVerticalBorder(borderColor)

                                    // TP+Vat
                                    Box(
                                        modifier = Modifier
                                            .width(resolvedTpVatW)
                                            .fillMaxHeight()
                                            .padding(horizontal = 6.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = BanglaFormatters.formatBdt(record.tpPlusVat),
                                            style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                            color = IncomeEmerald,
                                            textAlign = TextAlign.End,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                HorizontalDivider(thickness = 1.dp, color = borderColor)
                            }

                            // Footer total row
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(resolvedCodeW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Total",
                                        style = tableHeaderStyle,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                SalesTableVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedNameW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = "${records.size} members",
                                        style = tableBodyStyle.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                SalesTableVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedTpSalesW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = BanglaFormatters.formatBdt(totalTpSales),
                                        style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                SalesTableVerticalBorder(borderColor)
                                Box(
                                    modifier = Modifier
                                        .width(resolvedTpVatW)
                                        .fillMaxHeight()
                                        .padding(horizontal = 6.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = BanglaFormatters.formatBdt(totalTpPlusVat),
                                        style = tableBodyStyle.copy(fontWeight = FontWeight.Bold),
                                        color = IncomeEmerald,
                                        textAlign = TextAlign.End,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (embeddedInOuterCard) {
        Box(modifier = modifier.fillMaxWidth().testTag("monthly_sales_table_card")) {
            tableBody()
        }
    } else {
        GlassCardSurface(
            modifier = modifier.testTag("monthly_sales_table_card"),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            tableBody()
        }
    }
}

fun isMatchingMember(
    record: MonthlySalesRecord,
    memberCode: String,
    memberName: String
): Boolean {
    val cleanCode = memberCode.trim()
    val cleanName = memberName.trim().replace(Regex("\\s+"), " ")
    val recCode = record.code.trim()
    val recName = record.name.trim().replace(Regex("\\s+"), " ")
    val codeMatch = cleanCode.isNotEmpty() && recCode.equals(cleanCode, ignoreCase = true)
    val nameMatch = cleanName.isNotEmpty() && recName.equals(cleanName, ignoreCase = true)
    return codeMatch || nameMatch
}

@Composable
private fun SalesTableHeaderCell(
    text: String,
    width: Dp,
    style: TextStyle,
    textAlign: TextAlign
) {
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 6.dp, vertical = 7.dp),
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
private fun SalesTableVerticalBorder(color: Color) {
    VerticalDivider(
        thickness = 1.dp,
        color = color,
        modifier = Modifier.fillMaxHeight()
    )
}
