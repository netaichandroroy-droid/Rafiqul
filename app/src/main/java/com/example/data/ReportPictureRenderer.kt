package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.createBitmap
import com.example.R
import com.example.model.BanglaFormatters
import com.example.model.GroupedReportRow
import com.example.model.HisabTransaction
import com.example.model.MemberReportItem
import com.example.model.MonthlySalesRecord
import com.example.model.ReportCategory
import com.example.model.ReportPeriodScope
import com.example.model.ReportTotals
import com.example.model.TxType
import java.io.OutputStream

object ReportPictureRenderer {

    private const val IMAGE_WIDTH = 1080
    private const val MARGIN_X = 44f
    private const val BRAND_DEEP = 0xFF0A4A55.toInt()
    private const val BRAND_HERO_START = 0xFF0E6877.toInt()
    private const val BRAND_HERO_END = 0xFF083D47.toInt()
    private const val BG_COLOR = 0xFFF2F5F8.toInt()
    private const val CARD_WHITE = 0xFFFFFFFF.toInt()
    private const val BORDER_COLOR = 0xFFCBD5E1.toInt()
    private const val TEXT_PRIMARY = 0xFF162533.toInt()
    private const val TEXT_SECONDARY = 0xFF4E5D6D.toInt()
    private const val INCOME_GREEN = 0xFF15803D.toInt()
    private const val INCOME_BG = 0xFFDCFCE7.toInt()
    private const val EXPENSE_RED = 0xFFB91C1C.toInt()
    private const val EXPENSE_BG = 0xFFFEE2E2.toInt()
    private const val ROW_ALT_BG = 0xFFF8FAFC.toInt()

    private fun resolveBanglaTypeface(context: Context): Typeface {
        return runCatching {
            ResourcesCompat.getFont(context, R.font.plus_jakarta_sans)
        }.getOrNull() ?: Typeface.DEFAULT
    }

    fun renderHisabReportBitmap(
        context: Context,
        transactions: List<HisabTransaction>,
        selectedMonth: String
    ): Bitmap {
        val monthItems = transactions.filter { it.date.startsWith(selectedMonth) }
        val displayItems = if (monthItems.isNotEmpty()) monthItems else transactions
        val isMonthFiltered = monthItems.isNotEmpty()
        val maxRows = 30
        val shownRows = displayItems.take(maxRows)

        val monthIncome = displayItems.filter { it.resolvedType == TxType.INCOME }.sumOf { it.amount }
        val monthExpense = displayItems.filter { it.resolvedType == TxType.EXPENSE }.sumOf { it.amount }
        val netBalance = monthIncome - monthExpense

        val topCategories = displayItems
            .filter { it.resolvedType == TxType.EXPENSE }
            .groupBy { it.category }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
            .entries
            .sortedByDescending { it.value }
            .take(5)

        val headerHeight = 190f
        val statRowHeight = 148f
        val categoriesCardHeight = if (topCategories.isEmpty()) 0f else 90f + topCategories.size * 52f + 24f
        val tableHeaderHeight = 62f
        val tableRowHeight = 58f
        val emptyRowHeight = if (shownRows.isEmpty()) 110f else 0f
        val overflowNoteHeight = if (displayItems.size > maxRows) 44f else 0f
        val footerHeight = 96f

        val totalHeight = (
            headerHeight +
                28f +
                statRowHeight +
                (if (categoriesCardHeight > 0f) 24f + categoriesCardHeight else 0f) +
                28f +
                74f +
                tableHeaderHeight +
                (shownRows.size * tableRowHeight) +
                emptyRowHeight +
                overflowNoteHeight +
                footerHeight
            ).toInt().coerceAtLeast(880)

        val bitmap = createBitmap(IMAGE_WIDTH, totalHeight)
        val canvas = Canvas(bitmap)
        val baseTypeface = resolveBanglaTypeface(context)
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BG_COLOR }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), totalHeight.toFloat(), bgPaint)

        // Hero Header
        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, IMAGE_WIDTH.toFloat(), headerHeight,
                BRAND_HERO_START, BRAND_HERO_END, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), headerHeight, heroPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = Color.WHITE
            textSize = 40f
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            color = Color.argb(220, 255, 255, 255)
            textSize = 24f
        }

        canvas.drawText("Rafiqul Islam • Ledger Report", MARGIN_X, 74f, titlePaint)
        val periodLabel = if (isMonthFiltered) {
            "Month: ${BanglaFormatters.formatBanglaMonth(selectedMonth)}"
        } else {
            "All Transactions (No entries in ${BanglaFormatters.formatBanglaMonth(selectedMonth)})"
        }
        canvas.drawText(periodLabel, MARGIN_X, 118f, subtitlePaint)
        canvas.drawText(
            "Generated: ${BanglaFormatters.formatBanglaDate(BanglaFormatters.todayIso())} • Total ${displayItems.size} transactions",
            MARGIN_X,
            154f,
            subtitlePaint
        )

        var y = headerHeight + 28f

        // 3 Stat Cards
        val gap = 18f
        val statW = (IMAGE_WIDTH - 2 * MARGIN_X - 2 * gap) / 3f
        drawStatBox(
            canvas = canvas,
            rect = RectF(MARGIN_X, y, MARGIN_X + statW, y + statRowHeight),
            label = "Total Income",
            value = BanglaFormatters.formatBdt(monthIncome),
            bgColor = INCOME_BG,
            accentColor = INCOME_GREEN,
            baseTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )
        drawStatBox(
            canvas = canvas,
            rect = RectF(MARGIN_X + statW + gap, y, MARGIN_X + 2 * statW + gap, y + statRowHeight),
            label = "Total Expense",
            value = BanglaFormatters.formatBdt(monthExpense),
            bgColor = EXPENSE_BG,
            accentColor = EXPENSE_RED,
            baseTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )
        drawStatBox(
            canvas = canvas,
            rect = RectF(MARGIN_X + 2 * (statW + gap), y, IMAGE_WIDTH - MARGIN_X, y + statRowHeight),
            label = "Net Balance",
            value = BanglaFormatters.formatBdt(netBalance),
            bgColor = CARD_WHITE,
            accentColor = if (netBalance >= 0) BRAND_DEEP else EXPENSE_RED,
            baseTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )
        y += statRowHeight

        // Category breakdown card
        if (topCategories.isNotEmpty()) {
            y += 24f
            val cardRect = RectF(MARGIN_X, y, IMAGE_WIDTH - MARGIN_X, y + categoriesCardHeight)
            drawCard(canvas, cardRect)

            val secTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldTypeface
                color = TEXT_PRIMARY
                textSize = 28f
            }
            canvas.drawText("Expense Breakdown by Category", MARGIN_X + 24f, y + 48f, secTitlePaint)

            var cy = y + 88f
            val catPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = baseTypeface
                color = TEXT_PRIMARY
                textSize = 23f
            }
            val amtPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldTypeface
                color = EXPENSE_RED
                textSize = 23f
                textAlign = Paint.Align.RIGHT
            }
            val barTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE2E8F0.toInt() }
            val barFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = EXPENSE_RED }

            topCategories.forEach { (cat, amt) ->
                val ratio = if (monthExpense > 0.0) (amt / monthExpense).toFloat().coerceIn(0f, 1f) else 0f
                canvas.drawText(ellipsize(cat, catPaint, 240f), MARGIN_X + 24f, cy, catPaint)
                val barStart = MARGIN_X + 275f
                val barEnd = IMAGE_WIDTH - MARGIN_X - 220f
                canvas.drawRoundRect(
                    RectF(barStart, cy - 15f, barEnd, cy - 1f),
                    7f, 7f, barTrackPaint
                )
                val fillRight = barStart + (barEnd - barStart) * ratio
                if (fillRight > barStart + 2f) {
                    canvas.drawRoundRect(
                        RectF(barStart, cy - 15f, fillRight, cy - 1f),
                        7f, 7f, barFillPaint
                    )
                }
                canvas.drawText(BanglaFormatters.formatBdt(amt), IMAGE_WIDTH - MARGIN_X - 24f, cy, amtPaint)
                cy += 52f
            }
            y += categoriesCardHeight
        }

        // Ledger Table Section
        y += 28f
        val tableWidth = IMAGE_WIDTH - 2 * MARGIN_X
        val tableCardHeight = 74f + tableHeaderHeight +
            (shownRows.size * tableRowHeight) + emptyRowHeight + overflowNoteHeight
        val tableCardRect = RectF(MARGIN_X, y, IMAGE_WIDTH - MARGIN_X, y + tableCardHeight)
        drawCard(canvas, tableCardRect)

        val tableHeadingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = TEXT_PRIMARY
            textSize = 28f
        }
        canvas.drawText("Transaction List", MARGIN_X + 24f, y + 48f, tableHeadingPaint)
        y += 74f

        val colWidths = floatArrayOf(
            tableWidth * 0.21f, // Date
            tableWidth * 0.39f, // Details / Category
            tableWidth * 0.16f, // Account
            tableWidth * 0.24f  // Amount
        )
        val headers = listOf("Date", "Details & Category", "Method", "Amount")
        drawTableHeaderRow(canvas, MARGIN_X, y, tableWidth, tableHeaderHeight, colWidths, headers, boldTypeface)
        y += tableHeaderHeight

        if (shownRows.isEmpty()) {
            val emptyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = baseTypeface
                color = TEXT_SECONDARY
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No transactions found", IMAGE_WIDTH / 2f, y + 62f, emptyPaint)
            y += emptyRowHeight
        } else {
            shownRows.forEachIndexed { idx, tx ->
                val isAlt = idx % 2 == 1
                val rowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (isAlt) ROW_ALT_BG else CARD_WHITE
                }
                canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + tableRowHeight, rowBgPaint)

                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = BORDER_COLOR
                    strokeWidth = 1.2f
                }
                canvas.drawLine(MARGIN_X, y + tableRowHeight, MARGIN_X + tableWidth, y + tableRowHeight, linePaint)

                val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = baseTypeface
                    color = TEXT_PRIMARY
                    textSize = 21f
                }
                val isIncome = tx.resolvedType == TxType.INCOME
                val amtCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = boldTypeface
                    color = if (isIncome) INCOME_GREEN else EXPENSE_RED
                    textSize = 21f
                    textAlign = Paint.Align.RIGHT
                }

                val textY = y + 36f
                var cx = MARGIN_X
                canvas.drawText(
                    ellipsize(BanglaFormatters.formatBanglaDate(tx.date), cellPaint, colWidths[0] - 18f),
                    cx + 12f,
                    textY,
                    cellPaint
                )
                cx += colWidths[0]

                val title = tx.displayTitle
                canvas.drawText(
                    ellipsize(title, cellPaint, colWidths[1] - 18f),
                    cx + 12f,
                    textY,
                    cellPaint
                )
                cx += colWidths[1]

                canvas.drawText(
                    ellipsize(tx.resolvedAccount.bnShort, cellPaint, colWidths[2] - 18f),
                    cx + 12f,
                    textY,
                    cellPaint
                )
                cx += colWidths[2]

                canvas.drawText(
                    BanglaFormatters.formatSignedBdt(tx.amount, tx.resolvedType),
                    cx + colWidths[3] - 12f,
                    textY,
                    amtCellPaint
                )

                y += tableRowHeight
            }
        }

        if (displayItems.size > maxRows) {
            val morePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = baseTypeface
                color = TEXT_SECONDARY
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(
                "... ${displayItems.size - maxRows} more transactions",
                IMAGE_WIDTH / 2f,
                y + 28f,
                morePaint
            )
            y += overflowNoteHeight
        }

        drawFooter(canvas, y + 48f, baseTypeface)
        return bitmap
    }

    fun renderSalesReportBitmap(
        context: Context,
        salesRecords: List<MonthlySalesRecord>,
        selectedMonth: String
    ): Bitmap {
        val monthRecords = salesRecords.filter { it.yearMonth == selectedMonth }
        val totalTpSales = monthRecords.sumOf { it.tpSales }
        val totalTpPlusVat = monthRecords.sumOf { it.tpPlusVat }

        val headerHeight = 190f
        val statRowHeight = 148f
        val tableHeaderHeight = 62f
        val tableRowHeight = 58f
        val emptyRowHeight = if (monthRecords.isEmpty()) 110f else 0f
        val totalBarHeight = if (monthRecords.isNotEmpty()) 64f else 0f
        val footerHeight = 96f

        val totalHeight = (
            headerHeight +
                28f +
                statRowHeight +
                28f +
                74f +
                tableHeaderHeight +
                (monthRecords.size * tableRowHeight) +
                emptyRowHeight +
                totalBarHeight +
                footerHeight
            ).toInt().coerceAtLeast(780)

        val bitmap = createBitmap(IMAGE_WIDTH, totalHeight)
        val canvas = Canvas(bitmap)
        val baseTypeface = resolveBanglaTypeface(context)
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BG_COLOR }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), totalHeight.toFloat(), bgPaint)

        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, IMAGE_WIDTH.toFloat(), headerHeight,
                BRAND_HERO_START, BRAND_HERO_END, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), headerHeight, heroPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = Color.WHITE
            textSize = 40f
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            color = Color.argb(220, 255, 255, 255)
            textSize = 24f
        }

        canvas.drawText("Rafiqul Islam • Monthly Sales Report", MARGIN_X, 74f, titlePaint)
        canvas.drawText("Month: ${BanglaFormatters.formatBanglaMonth(selectedMonth)}", MARGIN_X, 118f, subtitlePaint)
        canvas.drawText(
            "Total rows: ${monthRecords.size} • Date: ${BanglaFormatters.formatBanglaDate(BanglaFormatters.todayIso())}",
            MARGIN_X,
            154f,
            subtitlePaint
        )

        var y = headerHeight + 28f

        val gap = 18f
        val statW = (IMAGE_WIDTH - 2 * MARGIN_X - gap) / 2f
        drawStatBox(
            canvas = canvas,
            rect = RectF(MARGIN_X, y, MARGIN_X + statW, y + statRowHeight),
            label = "Total TP sales",
            value = BanglaFormatters.formatBdt(totalTpSales),
            bgColor = CARD_WHITE,
            accentColor = BRAND_DEEP,
            baseTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )
        drawStatBox(
            canvas = canvas,
            rect = RectF(MARGIN_X + statW + gap, y, IMAGE_WIDTH - MARGIN_X, y + statRowHeight),
            label = "Total TP+VAT sales",
            value = BanglaFormatters.formatBdt(totalTpPlusVat),
            bgColor = INCOME_BG,
            accentColor = INCOME_GREEN,
            baseTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )
        y += statRowHeight + 28f

        val tableWidth = IMAGE_WIDTH - 2 * MARGIN_X
        val tableCardHeight = 74f + tableHeaderHeight +
            (monthRecords.size * tableRowHeight) + emptyRowHeight + totalBarHeight
        drawCard(canvas, RectF(MARGIN_X, y, IMAGE_WIDTH - MARGIN_X, y + tableCardHeight))

        val tableHeadingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = TEXT_PRIMARY
            textSize = 28f
        }
        canvas.drawText("Sales List (code, name, TP sales, TP+VAT sales)", MARGIN_X + 24f, y + 48f, tableHeadingPaint)
        y += 74f

        val colWidths = floatArrayOf(
            tableWidth * 0.09f, // SL
            tableWidth * 0.16f, // code
            tableWidth * 0.29f, // name
            tableWidth * 0.22f, // TP sales
            tableWidth * 0.24f  // TP+VAT sales
        )
        val headers = listOf("SL", "code", "name", "TP sales", "TP+VAT sales")
        drawTableHeaderRow(canvas, MARGIN_X, y, tableWidth, tableHeaderHeight, colWidths, headers, boldTypeface)
        y += tableHeaderHeight

        if (monthRecords.isEmpty()) {
            val emptyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = baseTypeface
                color = TEXT_SECONDARY
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No sales records for this month", IMAGE_WIDTH / 2f, y + 62f, emptyPaint)
            y += emptyRowHeight
        } else {
            monthRecords.forEachIndexed { idx, rec ->
                val rowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (idx % 2 == 1) ROW_ALT_BG else CARD_WHITE
                }
                canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + tableRowHeight, rowBgPaint)

                val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = BORDER_COLOR
                    strokeWidth = 1.2f
                }
                canvas.drawLine(MARGIN_X, y + tableRowHeight, MARGIN_X + tableWidth, y + tableRowHeight, linePaint)

                val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = baseTypeface
                    color = TEXT_PRIMARY
                    textSize = 21f
                }
                val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = boldTypeface
                    color = TEXT_PRIMARY
                    textSize = 21f
                    textAlign = Paint.Align.RIGHT
                }
                val vatPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = boldTypeface
                    color = INCOME_GREEN
                    textSize = 21f
                    textAlign = Paint.Align.RIGHT
                }

                val textY = y + 36f
                var cx = MARGIN_X
                canvas.drawText(
                    BanglaFormatters.toBanglaDigits((idx + 1).toString().padStart(2, '0')),
                    cx + 14f,
                    textY,
                    cellPaint
                )
                cx += colWidths[0]

                canvas.drawText(
                    ellipsize(rec.code.ifBlank { "-" }, cellPaint, colWidths[1] - 16f),
                    cx + 8f,
                    textY,
                    cellPaint
                )
                cx += colWidths[1]

                canvas.drawText(
                    ellipsize(rec.name, cellPaint, colWidths[2] - 18f),
                    cx + 8f,
                    textY,
                    cellPaint
                )
                cx += colWidths[2]

                canvas.drawText(
                    BanglaFormatters.formatBdt(rec.tpSales),
                    cx + colWidths[3] - 10f,
                    textY,
                    numPaint
                )
                cx += colWidths[3]

                canvas.drawText(
                    BanglaFormatters.formatBdt(rec.tpPlusVat),
                    cx + colWidths[4] - 12f,
                    textY,
                    vatPaint
                )

                y += tableRowHeight
            }

            // Total Row
            val totalRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INCOME_BG }
            canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + totalBarHeight, totalRowPaint)
            val totalLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldTypeface
                color = BRAND_DEEP
                textSize = 22f
            }
            val totalAmtPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldTypeface
                color = INCOME_GREEN
                textSize = 22f
                textAlign = Paint.Align.RIGHT
            }
            val ty = y + 40f
            canvas.drawText("Total", MARGIN_X + 16f, ty, totalLabelPaint)
            canvas.drawText(
                BanglaFormatters.formatBdt(totalTpSales),
                MARGIN_X + colWidths[0] + colWidths[1] + colWidths[2] + colWidths[3] - 10f,
                ty,
                totalAmtPaint
            )
            canvas.drawText(
                BanglaFormatters.formatBdt(totalTpPlusVat),
                MARGIN_X + tableWidth - 12f,
                ty,
                totalAmtPaint
            )
            y += totalBarHeight
        }

        drawFooter(canvas, y + 48f, baseTypeface)
        return bitmap
    }

    fun renderMpoWiseReportBitmap(
        context: Context,
        items: List<MemberReportItem>,
        totals: ReportTotals,
        selectedMonth: String,
        periodScope: ReportPeriodScope
    ): Bitmap {
        val periodText = when (periodScope) {
            ReportPeriodScope.SELECTED_MONTH -> "Period: ${BanglaFormatters.formatBanglaMonth(selectedMonth)}"
            ReportPeriodScope.ALL_TIME -> "Period: All Time (All Months)"
        }
        val rows = items.mapIndexed { idx, item ->
            val codePart = item.summary.code.takeIf { it.isNotBlank() }?.let { " ($it)" }.orEmpty()
            val areaPart = item.summary.area.takeIf { it.isNotBlank() }.orEmpty()
            RenderTableRow(
                sl = (idx + 1).toString().padStart(2, '0'),
                name = "${item.summary.name}$codePart",
                subLabel = areaPart.ifBlank { item.designation.ifBlank { "-" } },
                tpPlusVat = item.summary.totalSales,
                joma = item.summary.approvedJoma,
                dues = item.summary.dues
            )
        }
        return renderCategorizedReportBitmap(
            context = context,
            title = "Rafiqul Islam • ${ReportCategory.MPO_WISE.title}",
            periodText = periodText,
            countLabel = "Total MPO: ${items.size}",
            tableHeading = "MPO Sales, Deposit & Dues List",
            col2Header = "MPO & Area",
            rows = rows,
            totals = totals
        )
    }

    fun renderGroupedSummaryReportBitmap(
        context: Context,
        category: ReportCategory,
        groups: List<GroupedReportRow>,
        totals: ReportTotals,
        selectedMonth: String,
        periodScope: ReportPeriodScope
    ): Bitmap {
        val periodText = when (periodScope) {
            ReportPeriodScope.SELECTED_MONTH -> "Period: ${BanglaFormatters.formatBanglaMonth(selectedMonth)}"
            ReportPeriodScope.ALL_TIME -> "Period: All Time (All Months)"
        }
        val col2Header = when (category) {
            ReportCategory.ASM_DEPOT_WISE -> "Depot / ASM"
            ReportCategory.ASM_NAME_WISE -> "ASM Name"
            ReportCategory.AM_WISE -> "RSM / RSC / AM Area"
            ReportCategory.REGION_WISE -> "Region / Depot"
            else -> "Name"
        }
        val rows = groups.mapIndexed { idx, group ->
            RenderTableRow(
                sl = (idx + 1).toString().padStart(2, '0'),
                name = group.title,
                subLabel = "${group.leaderLabel} • ${group.memberCount} members",
                tpPlusVat = group.totalTpPlusVat,
                joma = group.approvedJoma,
                dues = group.dues
            )
        }
        return renderCategorizedReportBitmap(
            context = context,
            title = "Rafiqul Islam • ${category.title}",
            periodText = periodText,
            countLabel = "Total Groups: ${groups.size} • Members: ${totals.memberCount}",
            tableHeading = "${category.title} Summary",
            col2Header = col2Header,
            rows = rows,
            totals = totals
        )
    }

    private data class RenderTableRow(
        val sl: String,
        val name: String,
        val subLabel: String,
        val tpPlusVat: Double,
        val joma: Double,
        val dues: Double
    )

    private fun renderCategorizedReportBitmap(
        context: Context,
        title: String,
        periodText: String,
        countLabel: String,
        tableHeading: String,
        col2Header: String,
        rows: List<RenderTableRow>,
        totals: ReportTotals
    ): Bitmap {
        val maxRows = 45
        val shownRows = rows.take(maxRows)
        val headerHeight = 190f
        val statRowHeight = 148f
        val tableHeaderHeight = 62f
        val tableRowHeight = 64f
        val emptyRowHeight = if (shownRows.isEmpty()) 110f else 0f
        val totalBarHeight = if (shownRows.isNotEmpty()) 64f else 0f
        val overflowHeight = if (rows.size > maxRows) 44f else 0f
        val footerHeight = 96f

        val totalHeight = (
            headerHeight +
                28f +
                statRowHeight +
                28f +
                74f +
                tableHeaderHeight +
                (shownRows.size * tableRowHeight) +
                emptyRowHeight +
                totalBarHeight +
                overflowHeight +
                footerHeight
            ).toInt().coerceAtLeast(820)

        val bitmap = createBitmap(IMAGE_WIDTH, totalHeight)
        val canvas = Canvas(bitmap)
        val baseTypeface = resolveBanglaTypeface(context)
        val boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BG_COLOR }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), totalHeight.toFloat(), bgPaint)

        val heroPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, IMAGE_WIDTH.toFloat(), headerHeight,
                BRAND_HERO_START, BRAND_HERO_END, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, IMAGE_WIDTH.toFloat(), headerHeight, heroPaint)

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = Color.WHITE
            textSize = 38f
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            color = Color.argb(220, 255, 255, 255)
            textSize = 23f
        }
        canvas.drawText(title, MARGIN_X, 72f, titlePaint)
        canvas.drawText(periodText, MARGIN_X, 116f, subtitlePaint)
        canvas.drawText(
            "$countLabel • Date: ${BanglaFormatters.formatBanglaDate(BanglaFormatters.todayIso())}",
            MARGIN_X,
            152f,
            subtitlePaint
        )

        var y = headerHeight + 28f
        val gap = 18f
        val statW = (IMAGE_WIDTH - 2 * MARGIN_X - 2 * gap) / 3f
        drawStatBox(
            canvas = canvas,
            rect = RectF(MARGIN_X, y, MARGIN_X + statW, y + statRowHeight),
            label = "Total TP+VAT sales",
            value = BanglaFormatters.formatBdt(totals.totalTpPlusVat),
            bgColor = CARD_WHITE,
            accentColor = BRAND_DEEP,
            baseTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )
        drawStatBox(
            canvas = canvas,
            rect = RectF(MARGIN_X + statW + gap, y, MARGIN_X + 2 * statW + gap, y + statRowHeight),
            label = "Deposit",
            value = BanglaFormatters.formatBdt(totals.approvedJoma),
            bgColor = INCOME_BG,
            accentColor = INCOME_GREEN,
            baseTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )
        drawStatBox(
            canvas = canvas,
            rect = RectF(MARGIN_X + 2 * (statW + gap), y, IMAGE_WIDTH - MARGIN_X, y + statRowHeight),
            label = "Total Dues",
            value = BanglaFormatters.formatBdt(totals.dues),
            bgColor = if (totals.dues > 0.0) EXPENSE_BG else INCOME_BG,
            accentColor = if (totals.dues > 0.0) EXPENSE_RED else INCOME_GREEN,
            baseTypeface = baseTypeface,
            boldTypeface = boldTypeface
        )
        y += statRowHeight + 28f

        val tableWidth = IMAGE_WIDTH - 2 * MARGIN_X
        val tableCardHeight = 74f + tableHeaderHeight +
            (shownRows.size * tableRowHeight) + emptyRowHeight + totalBarHeight + overflowHeight
        drawCard(canvas, RectF(MARGIN_X, y, IMAGE_WIDTH - MARGIN_X, y + tableCardHeight))

        val tableHeadingPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = TEXT_PRIMARY
            textSize = 27f
        }
        canvas.drawText(tableHeading, MARGIN_X + 24f, y + 48f, tableHeadingPaint)
        y += 74f

        val colWidths = floatArrayOf(
            tableWidth * 0.07f,
            tableWidth * 0.34f,
            tableWidth * 0.22f,
            tableWidth * 0.19f,
            tableWidth * 0.18f
        )
        val headers = listOf("SL", col2Header, "TP+VAT sales", "Deposit", "Dues")
        drawTableHeaderRow(canvas, MARGIN_X, y, tableWidth, tableHeaderHeight, colWidths, headers, boldTypeface)
        y += tableHeaderHeight

        if (shownRows.isEmpty()) {
            val emptyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = baseTypeface
                color = TEXT_SECONDARY
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No report records found", IMAGE_WIDTH / 2f, y + 62f, emptyPaint)
            y += emptyRowHeight
        } else {
            val cellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = baseTypeface
                color = TEXT_PRIMARY
                textSize = 21f
            }
            val subCellPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = baseTypeface
                color = TEXT_SECONDARY
                textSize = 17f
            }
            val numPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldTypeface
                color = TEXT_PRIMARY
                textSize = 20f
                textAlign = Paint.Align.RIGHT
            }
            val jomaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldTypeface
                color = INCOME_GREEN
                textSize = 20f
                textAlign = Paint.Align.RIGHT
            }
            val duesPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldTypeface
                color = EXPENSE_RED
                textSize = 20f
                textAlign = Paint.Align.RIGHT
            }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = BORDER_COLOR
                strokeWidth = 1.2f
            }

            shownRows.forEachIndexed { idx, row ->
                val rowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (idx % 2 == 1) ROW_ALT_BG else CARD_WHITE
                }
                canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + tableRowHeight, rowBgPaint)
                canvas.drawLine(MARGIN_X, y + tableRowHeight, MARGIN_X + tableWidth, y + tableRowHeight, linePaint)

                val titleY = y + 28f
                val subY = y + 51f
                val numY = y + 39f
                var cx = MARGIN_X

                canvas.drawText(BanglaFormatters.toBanglaDigits(row.sl), cx + 12f, numY, cellPaint)
                cx += colWidths[0]

                canvas.drawText(ellipsize(row.name, cellPaint, colWidths[1] - 16f), cx + 8f, titleY, cellPaint)
                canvas.drawText(ellipsize(row.subLabel, subCellPaint, colWidths[1] - 16f), cx + 8f, subY, subCellPaint)
                cx += colWidths[1]

                canvas.drawText(BanglaFormatters.formatBdt(row.tpPlusVat), cx + colWidths[2] - 10f, numY, numPaint)
                cx += colWidths[2]

                canvas.drawText(BanglaFormatters.formatBdt(row.joma), cx + colWidths[3] - 10f, numY, jomaPaint)
                cx += colWidths[3]

                canvas.drawText(
                    BanglaFormatters.formatBdt(row.dues),
                    cx + colWidths[4] - 12f,
                    numY,
                    if (row.dues > 0.0) duesPaint else jomaPaint
                )

                y += tableRowHeight
            }

            val totalRowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = INCOME_BG }
            canvas.drawRect(MARGIN_X, y, MARGIN_X + tableWidth, y + totalBarHeight, totalRowPaint)
            val totalLabelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                typeface = boldTypeface
                color = BRAND_DEEP
                textSize = 21f
            }
            val ty = y + 40f
            canvas.drawText("Total", MARGIN_X + 16f, ty, totalLabelPaint)
            val col2End = MARGIN_X + colWidths[0] + colWidths[1] + colWidths[2]
            val col3End = col2End + colWidths[3]
            val col4End = MARGIN_X + tableWidth
            canvas.drawText(BanglaFormatters.formatBdt(totals.totalTpPlusVat), col2End - 10f, ty, numPaint)
            canvas.drawText(BanglaFormatters.formatBdt(totals.approvedJoma), col3End - 10f, ty, jomaPaint)
            canvas.drawText(
                BanglaFormatters.formatBdt(totals.dues),
                col4End - 12f,
                ty,
                if (totals.dues > 0.0) duesPaint else jomaPaint
            )
            y += totalBarHeight

            if (rows.size > maxRows) {
                val morePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    typeface = baseTypeface
                    color = TEXT_SECONDARY
                    textSize = 20f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(
                    "... ${rows.size - maxRows} more rows",
                    IMAGE_WIDTH / 2f,
                    y + 28f,
                    morePaint
                )
                y += overflowHeight
            }
        }

        drawFooter(canvas, y + 48f, baseTypeface)
        return bitmap
    }

    fun writeBitmapAsPng(bitmap: Bitmap, outputStream: OutputStream): Boolean {
        return try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        } finally {
            bitmap.recycle()
        }
    }

    private fun drawCard(canvas: Canvas, rect: RectF) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = CARD_WHITE }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BORDER_COLOR
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 18f, 18f, fillPaint)
        canvas.drawRoundRect(rect, 18f, 18f, strokePaint)
    }

    private fun drawStatBox(
        canvas: Canvas,
        rect: RectF,
        label: String,
        value: String,
        bgColor: Int,
        accentColor: Int,
        baseTypeface: Typeface,
        boldTypeface: Typeface
    ) {
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = bgColor }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BORDER_COLOR
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawRoundRect(rect, 16f, 16f, fillPaint)
        canvas.drawRoundRect(rect, 16f, 16f, strokePaint)

        val lblPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            color = TEXT_SECONDARY
            textSize = 22f
        }
        val valPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = accentColor
            textSize = 29f
        }
        canvas.drawText(label, rect.left + 20f, rect.top + 48f, lblPaint)
        canvas.drawText(
            ellipsize(value, valPaint, rect.width() - 40f),
            rect.left + 20f,
            rect.top + 104f,
            valPaint
        )
    }

    private fun drawTableHeaderRow(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        colWidths: FloatArray,
        headers: List<String>,
        boldTypeface: Typeface
    ) {
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BRAND_DEEP }
        canvas.drawRect(x, y, x + width, y + height, headerBg)

        val headerTextLeft = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = Color.WHITE
            textSize = 22f
        }
        val headerTextRight = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = boldTypeface
            color = Color.WHITE
            textSize = 22f
            textAlign = Paint.Align.RIGHT
        }

        var cx = x
        val textY = y + 39f
        headers.forEachIndexed { idx, title ->
            val cw = colWidths[idx]
            val isNumericRight = idx >= headers.size - 1 || (headers.size == 5 && idx >= 3)
            if (isNumericRight) {
                canvas.drawText(title, cx + cw - 12f, textY, headerTextRight)
            } else {
                canvas.drawText(title, cx + 12f, textY, headerTextLeft)
            }
            cx += cw
        }
    }

    private fun drawFooter(canvas: Canvas, y: Float, baseTypeface: Typeface) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = baseTypeface
            color = TEXT_SECONDARY
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            "Rafiqul Islam • Ledger Report Image (PNG)",
            IMAGE_WIDTH / 2f,
            y,
            paint
        )
    }

    private fun ellipsize(text: String, paint: TextPaint, maxWidth: Float): String {
        return TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString()
    }
}
