package com.example.data

import com.example.model.AccountType
import com.example.model.HisabTransaction
import com.example.model.ImportedTransaction
import com.example.model.TxType
import org.json.JSONArray
import org.json.JSONObject

object BackupExporter {

    private val dateRegex = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

    fun buildCsv(transactions: List<HisabTransaction>): String {
        val sb = StringBuilder("\uFEFF")
        sb.append("Type,Amount,Category,Note,Date,Account\n")
        for (t in transactions) {
            val fields = listOf(
                t.type,
                t.amount.toString(),
                t.category,
                t.note,
                t.date,
                t.account
            )
            sb.append(fields.joinToString(",") { value ->
                "\"" + value.replace("\"", "\"\"") + "\""
            })
            sb.append('\n')
        }
        return sb.toString()
    }

    fun buildJsonBackup(transactions: List<HisabTransaction>): String {
        val array = JSONArray()
        for (t in transactions) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("amount", t.amount)
                put("category", t.category)
                put("note", t.note)
                put("date", t.date)
                put("account", t.account)
                put("createdAt", t.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            }
            array.put(obj)
        }
        return array.toString(2)
    }

    fun parseJsonBackup(jsonText: String): Result<List<ImportedTransaction>> {
        return try {
            val array = JSONArray(jsonText.trim())
            val parsed = mutableListOf<ImportedTransaction>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val amount = obj.optDouble("amount", -1.0)
                val date = obj.optString("date", "").trim()
                val rawType = obj.optString("type", "expense")
                val rawAccount = obj.optString("account", "cash")
                val category = obj.optString("category", "").trim()
                val note = obj.optString("note", "").trim()

                if (amount <= 0.0 || amount > 1_000_000_000.0) continue
                if (!dateRegex.matches(date)) continue

                val txType = TxType.fromRaw(rawType)
                val accType = AccountType.fromRaw(rawAccount)
                val safeCategory = category.take(60).ifBlank { txType.bnLabel }
                val safeNote = note.take(200).ifBlank { safeCategory }

                parsed.add(
                    ImportedTransaction(
                        type = txType,
                        amount = amount,
                        category = safeCategory,
                        note = safeNote,
                        date = date,
                        account = accType
                    )
                )
            }
            if (parsed.isEmpty()) {
                Result.failure(IllegalArgumentException("সঠিক JSON ব্যাকআপ ফাইল নির্বাচন করুন।"))
            } else {
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(IllegalArgumentException("সঠিক JSON ব্যাকআপ নির্বাচন করুন।", e))
        }
    }
}
