package com.example.data

import android.content.Context
import androidx.core.content.edit
import com.example.model.AccountType
import com.example.model.HisabTransaction
import com.example.model.ImportedTransaction
import com.example.model.TxType
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class LocalTransactionStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadAll(): List<HisabTransaction> {
        val raw = prefs.getString(KEY_TRANSACTIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val list = ArrayList<HisabTransaction>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() }
                val type = TxType.fromRaw(obj.optString("type", "expense")).wireValue
                val amount = obj.optDouble("amount", 0.0)
                if (amount <= 0.0) continue
                val category = obj.optString("category", "")
                val note = obj.optString("note", "")
                val date = obj.optString("date", "")
                val account = AccountType.fromRaw(obj.optString("account", "cash")).wireValue
                val createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                list.add(
                    HisabTransaction(
                        id = id,
                        userId = LOCAL_USER_ID,
                        type = type,
                        amount = amount,
                        category = category,
                        note = note,
                        date = date,
                        account = account,
                        createdAt = Timestamp(createdAtMs / 1000, ((createdAtMs % 1000) * 1_000_000).toInt())
                    )
                )
            }
            list.sortedWith(
                compareByDescending<HisabTransaction> { it.date }
                    .thenByDescending { it.createdAt?.seconds ?: 0L }
            )
        }.getOrDefault(emptyList())
    }

    fun saveAll(items: List<HisabTransaction>) {
        val array = JSONArray()
        for (t in items) {
            val obj = JSONObject().apply {
                put("id", t.id)
                put("type", t.type)
                put("amount", t.amount)
                put("category", t.category)
                put("note", t.note)
                put("date", t.date)
                put("account", t.account)
                put("createdAtMs", t.createdAt?.toDate()?.time ?: System.currentTimeMillis())
            }
            array.put(obj)
        }
        prefs.edit(commit = true) {
            putString(KEY_TRANSACTIONS, array.toString())
        }
    }

    fun clearAll() {
        prefs.edit(commit = true) {
            remove(KEY_TRANSACTIONS)
        }
    }

    fun toImportedList(): List<ImportedTransaction> {
        return loadAll().map { tx ->
            ImportedTransaction(
                type = tx.resolvedType,
                amount = tx.amount,
                category = tx.category,
                note = tx.note,
                date = tx.date,
                account = tx.resolvedAccount
            )
        }
    }

    companion object {
        private const val PREFS_NAME = "hisab_local_transactions_prefs"
        private const val KEY_TRANSACTIONS = "transactions_json"
        const val LOCAL_USER_ID = "local_rafiqul"
    }
}
