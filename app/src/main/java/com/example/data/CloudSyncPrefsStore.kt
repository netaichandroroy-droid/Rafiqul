package com.example.data

import android.content.Context
import androidx.core.content.edit

class CloudSyncPrefsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit(commit = true) {
            putBoolean(KEY_AUTO_SYNC_ENABLED, enabled)
        }
    }

    fun getLastSyncedAtMs(): Long {
        return prefs.getLong(KEY_LAST_SYNCED_AT_MS, 0L)
    }

    fun setLastSyncedAtMs(timestampMs: Long) {
        prefs.edit(commit = true) {
            putLong(KEY_LAST_SYNCED_AT_MS, timestampMs)
        }
    }

    fun isSeededForUid(uid: String): Boolean {
        if (uid.isBlank()) return false
        return prefs.getBoolean("$KEY_SEEDED_PREFIX$uid", false)
    }

    fun markSeededForUid(uid: String) {
        if (uid.isBlank()) return
        prefs.edit(commit = true) {
            putBoolean("$KEY_SEEDED_PREFIX$uid", true)
        }
    }

    companion object {
        private const val PREFS_NAME = "hisab_cloud_sync_prefs"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_LAST_SYNCED_AT_MS = "last_synced_at_ms"
        private const val KEY_SEEDED_PREFIX = "seeded_uid_"
    }
}
