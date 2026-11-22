package com.example.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BanglaFormatters {

    private val banglaDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')

    private val banglaMonths = listOf(
        "জানুয়ারি",
        "ফেব্রুয়ারি",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টেম্বর",
        "অক্টোবর",
        "নভেম্বর",
        "ডিসেম্বর"
    )

    private val numberFormat = DecimalFormat("#,##,##0.##", DecimalFormatSymbols(Locale.US))

    fun toBanglaDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(banglaDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun formatBdt(amount: Double): String {
        val isNegative = amount < 0
        val formatted = numberFormat.format(kotlin.math.abs(amount))
        val bnFormatted = toBanglaDigits(formatted)
        return if (isNegative) "−৳ $bnFormatted" else "৳ $bnFormatted"
    }

    fun formatSignedBdt(amount: Double, type: TxType): String {
        val formatted = toBanglaDigits(numberFormat.format(kotlin.math.abs(amount)))
        return if (type == TxType.INCOME) "+৳ $formatted" else "−৳ $formatted"
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
        if (parts.size != 3) return toBanglaDigits(isoDate)
        val year = parts[0]
        val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: return toBanglaDigits(isoDate)
        val day = parts[2].toIntOrNull() ?: return toBanglaDigits(isoDate)
        if (monthIdx !in banglaMonths.indices) return toBanglaDigits(isoDate)
        return "${toBanglaDigits(day.toString())} ${banglaMonths[monthIdx]} ${toBanglaDigits(year)}"
    }

    fun formatBanglaMonth(yearMonth: String): String {
        val parts = yearMonth.split("-")
        if (parts.size != 2) return toBanglaDigits(yearMonth)
        val year = parts[0]
        val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: return toBanglaDigits(yearMonth)
        if (monthIdx !in banglaMonths.indices) return toBanglaDigits(yearMonth)
        return "${banglaMonths[monthIdx]} ${toBanglaDigits(year)}"
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
}
