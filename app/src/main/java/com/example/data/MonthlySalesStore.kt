package com.example.data

import android.content.Context
import androidx.core.content.edit
import com.example.model.MonthlySalesRecord
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MonthlySalesStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadAll(): List<MonthlySalesRecord> {
        val raw = prefs.getString(KEY_SALES_RECORDS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val list = ArrayList<MonthlySalesRecord>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() }
                val yearMonth = obj.optString("yearMonth", "").trim()
                val code = obj.optString("code", "").trim()
                val name = obj.optString("name", "").trim()
                if (yearMonth.isBlank() || (code.isBlank() && name.isBlank())) continue
                list.add(
                    MonthlySalesRecord(
                        id = id,
                        yearMonth = yearMonth,
                        code = code,
                        name = name.ifBlank { code },
                        tpSales = obj.optDouble("tpSales", 0.0).takeIf { !it.isNaN() && it >= 0.0 } ?: 0.0,
                        tpPlusVat = obj.optDouble("tpPlusVat", 0.0).takeIf { !it.isNaN() && it >= 0.0 } ?: 0.0,
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                    )
                )
            }
            sortRecords(list)
        }.getOrDefault(emptyList())
    }

    fun loadForMonth(yearMonth: String): List<MonthlySalesRecord> {
        return loadAll().filter { it.yearMonth == yearMonth }
    }

    fun saveAll(records: List<MonthlySalesRecord>) {
        val sorted = sortRecords(records)
        val array = JSONArray()
        for (item in sorted) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("yearMonth", item.yearMonth)
                put("code", item.code)
                put("name", item.name)
                put("tpSales", item.tpSales)
                put("tpPlusVat", item.tpPlusVat)
                put("createdAtMs", item.createdAtMs)
            }
            array.put(obj)
        }
        prefs.edit(commit = true) {
            putString(KEY_SALES_RECORDS, array.toString())
        }
    }

    fun uploadForMonth(
        yearMonth: String,
        newRecords: List<MonthlySalesRecord>,
        replaceMonth: Boolean
    ): List<MonthlySalesRecord> {
        val normalized = newRecords.map { record ->
            record.copy(
                id = record.id.ifBlank { UUID.randomUUID().toString() },
                yearMonth = yearMonth,
                code = record.code.trim(),
                name = record.name.trim().ifBlank { record.code.trim() },
                tpSales = record.tpSales.coerceAtLeast(0.0),
                tpPlusVat = record.tpPlusVat.coerceAtLeast(0.0)
            )
        }
        val current = loadAll()
        val combined = if (replaceMonth) {
            current.filterNot { it.yearMonth == yearMonth } + normalized
        } else {
            current + normalized
        }
        val sorted = sortRecords(combined)
        saveAll(sorted)
        return sorted
    }

    fun upsertRecord(record: MonthlySalesRecord): List<MonthlySalesRecord> {
        val current = loadAll()
        val existingIndex = current.indexOfFirst { it.id == record.id }
        val updated = if (existingIndex >= 0) {
            current.map { if (it.id == record.id) record else it }
        } else {
            current + record
        }
        val sorted = sortRecords(updated)
        saveAll(sorted)
        return sorted
    }

    fun deleteRecord(recordId: String): List<MonthlySalesRecord> {
        val current = loadAll()
        val updated = current.filterNot { it.id == recordId }
        if (updated.size != current.size) {
            saveAll(updated)
        }
        return updated
    }

    fun clearMonth(yearMonth: String): List<MonthlySalesRecord> {
        val current = loadAll()
        val updated = current.filterNot { it.yearMonth == yearMonth }
        if (updated.size != current.size) {
            saveAll(updated)
        }
        return updated
    }

    private fun sortRecords(list: List<MonthlySalesRecord>): List<MonthlySalesRecord> {
        return list.sortedWith(
            compareByDescending<MonthlySalesRecord> { it.yearMonth }
                .thenBy { it.createdAtMs }
        )
    }

    companion object {
        private const val PREFS_NAME = "hisab_monthly_sales_prefs"
        private const val KEY_SALES_RECORDS = "sales_records_json"
    }
}
