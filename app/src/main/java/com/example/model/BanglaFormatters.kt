package com.example.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BanglaFormatters {

    private val englishMonths = listOf(
        "January",
        "February",
        "March",
        "April",
        "May",
        "June",
        "July",
        "August",
        "September",
        "October",
        "November",
        "December"
    )

    private val shortEnglishMonths = listOf(
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec"
    )

    private val numberFormat = DecimalFormat("#,##,##0.##", DecimalFormatSymbols(Locale.US))

    fun toBanglaDigits(input: String): String = input

    fun formatBdt(amount: Double): String {
        val isNegative = amount < 0
        val formatted = numberFormat.format(kotlin.math.abs(amount))
        return if (isNegative) "-৳ $formatted" else "৳ $formatted"
    }

    fun formatSignedBdt(amount: Double, type: TxType): String {
        val formatted = numberFormat.format(kotlin.math.abs(amount))
        return if (type == TxType.INCOME) "+৳ $formatted" else "-৳ $formatted"
    }

    fun todayIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun currentYearMonth(): String {
        return todayIso().substring(0, 7)
    }

    fun millisToIsoDate(utcMillis: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return sdf.format(Date(utcMillis))
    }

    fun isoDateToUtcMillis(isoDate: String): Long? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }
            sdf.parse(isoDate)?.time
        } catch (_: Exception) {
            null
        }
    }

    fun formatBanglaDate(isoDate: String): String {
        val parts = isoDate.split("-")
        if (parts.size != 3) return isoDate
        val year = parts[0]
        val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: return isoDate
        val day = parts[2].toIntOrNull() ?: return isoDate
        if (monthIdx !in shortEnglishMonths.indices) return isoDate
        return "$day ${shortEnglishMonths[monthIdx]} $year"
    }

    fun formatBanglaMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return yearMonth
        val year = parts[0]
        val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: return yearMonth
        if (monthIdx !in englishMonths.indices) return yearMonth
        return "${englishMonths[monthIdx]} $year"
    }

    fun shiftYearMonth(yearMonth: String, deltaMonths: Int): String {
        val parts = yearMonth.split("-")
        val year = parts.getOrNull(0)?.toIntOrNull() ?: return currentYearMonth()
        val month = parts.getOrNull(1)?.toIntOrNull() ?: return currentYearMonth()
        val cal = Calendar.getInstance(Locale.US).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, (month - 1).coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, deltaMonths)
        }
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", y, m)
    }

    fun formatUpdateTime(timestampMs: Long = System.currentTimeMillis()): String {
        val safeMs = if (timestampMs > 0L) timestampMs else System.currentTimeMillis()
        val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)
        return sdf.format(Date(safeMs))
    }
}
