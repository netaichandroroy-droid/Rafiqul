package com.example.data

import android.content.Context
import androidx.core.content.edit
import com.example.model.OnlinePaymentAccount
import com.example.model.OnlinePaymentConfig
import com.example.model.PaymentChannel
import org.json.JSONArray
import org.json.JSONObject

class OnlinePaymentStore(
    context: Context,
    ownerScope: String = ADMIN_SCOPE
) {

    private val prefsName = if (ownerScope == ADMIN_SCOPE) {
        PREFS_NAME
    } else {
        "${PREFS_NAME}_${ownerScope}"
    }

    private val prefs = context.applicationContext.getSharedPreferences(
        prefsName,
        Context.MODE_PRIVATE
    )

    fun load(): OnlinePaymentConfig {
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        val rawAccounts = prefs.getString(KEY_ACCOUNTS, null)
        if (rawAccounts.isNullOrBlank()) {
            return OnlinePaymentConfig(
                isOnlinePaymentEnabled = enabled,
                accounts = OnlinePaymentConfig.defaultAccounts()
            )
        }
        val parsed = runCatching {
            val array = JSONArray(rawAccounts)
            val byId = LinkedHashMap<String, OnlinePaymentAccount>()
            OnlinePaymentConfig.defaultAccounts().forEach { byId[it.id] = it }
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").trim()
                val channelName = obj.optString("channel", "")
                val channel = PaymentChannel.entries.firstOrNull { it.name == channelName } ?: continue
                if (id.isEmpty()) continue
                byId[id] = OnlinePaymentAccount(
                    id = id,
                    channel = channel,
                    accountNumber = obj.optString("accountNumber", "").trim(),
                    accountType = obj.optString("accountType", "Personal").trim().ifBlank { "Personal" },
                    isEnabled = obj.optBoolean("isEnabled", false)
                )
            }
            byId.values.toList()
        }.getOrElse { OnlinePaymentConfig.defaultAccounts() }

        return OnlinePaymentConfig(
            isOnlinePaymentEnabled = enabled,
            accounts = parsed
        )
    }

    fun save(config: OnlinePaymentConfig) {
        val array = JSONArray()
        for (acc in config.accounts) {
            val obj = JSONObject().apply {
                put("id", acc.id)
                put("channel", acc.channel.name)
                put("accountNumber", acc.accountNumber)
                put("accountType", acc.accountType)
                put("isEnabled", acc.isEnabled)
            }
            array.put(obj)
        }
        prefs.edit(commit = true) {
            putBoolean(KEY_ENABLED, config.isOnlinePaymentEnabled)
            putString(KEY_ACCOUNTS, array.toString())
        }
    }

    companion object {
        const val ADMIN_SCOPE = "admin"
        private const val PREFS_NAME = "hisab_online_payment_prefs"
        private const val KEY_ENABLED = "online_payment_enabled"
        private const val KEY_ACCOUNTS = "online_payment_accounts_json"
    }
}
