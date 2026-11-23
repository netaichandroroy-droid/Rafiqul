package com.example.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class CloudSyncPrefsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val pendingLock = Any()

    fun isAutoSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_SYNC_ENABLED, true)
    }

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit(commit = true) {
            putBoolean(KEY_AUTO_SYNC_ENABLED, enabled)
        }
    }

    fun isExplicitGoogleSignOut(): Boolean {
        return prefs.getBoolean(KEY_EXPLICIT_GOOGLE_SIGN_OUT, false)
    }

    fun setExplicitGoogleSignOut(signedOut: Boolean) {
        prefs.edit(commit = true) {
            putBoolean(KEY_EXPLICIT_GOOGLE_SIGN_OUT, signedOut)
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

    fun markPendingUpsert(collection: String, docId: String) {
        val col = collection.trim()
        val id = docId.trim()
        if (col.isEmpty() || id.isEmpty()) return
        synchronized(pendingLock) {
            val upserts = readMapLocked(KEY_PENDING_UPSERTS)
            upserts.getOrPut(col) { LinkedHashSet() }.add(id)
            val deletes = readMapLocked(KEY_PENDING_DELETES)
            deletes[col]?.remove(id)
            if (deletes[col]?.isEmpty() == true) deletes.remove(col)
            writeMapsLocked(upserts, deletes)
        }
    }

    fun clearPendingUpsert(collection: String, docId: String) {
        val col = collection.trim()
        val id = docId.trim()
        if (col.isEmpty() || id.isEmpty()) return
        synchronized(pendingLock) {
            val upserts = readMapLocked(KEY_PENDING_UPSERTS)
            val removed = upserts[col]?.remove(id) == true
            if (upserts[col]?.isEmpty() == true) upserts.remove(col)
            if (removed) {
                writeSingleMapLocked(KEY_PENDING_UPSERTS, upserts)
            }
        }
    }

    fun getPendingUpserts(collection: String): Set<String> {
        val col = collection.trim()
        if (col.isEmpty()) return emptySet()
        synchronized(pendingLock) {
            return readMapLocked(KEY_PENDING_UPSERTS)[col]?.toSet() ?: emptySet()
        }
    }

    fun markPendingDelete(collection: String, docId: String) {
        val col = collection.trim()
        val id = docId.trim()
        if (col.isEmpty() || id.isEmpty()) return
        synchronized(pendingLock) {
            val deletes = readMapLocked(KEY_PENDING_DELETES)
            deletes.getOrPut(col) { LinkedHashSet() }.add(id)
            val upserts = readMapLocked(KEY_PENDING_UPSERTS)
            upserts[col]?.remove(id)
            if (upserts[col]?.isEmpty() == true) upserts.remove(col)
            writeMapsLocked(upserts, deletes)
        }
    }

    fun clearPendingDelete(collection: String, docId: String) {
        val col = collection.trim()
        val id = docId.trim()
        if (col.isEmpty() || id.isEmpty()) return
        synchronized(pendingLock) {
            val deletes = readMapLocked(KEY_PENDING_DELETES)
            val removed = deletes[col]?.remove(id) == true
            if (deletes[col]?.isEmpty() == true) deletes.remove(col)
            if (removed) {
                writeSingleMapLocked(KEY_PENDING_DELETES, deletes)
            }
        }
    }

    fun getPendingDeletes(collection: String): Set<String> {
        val col = collection.trim()
        if (col.isEmpty()) return emptySet()
        synchronized(pendingLock) {
            return readMapLocked(KEY_PENDING_DELETES)[col]?.toSet() ?: emptySet()
        }
    }

    fun hasPendingMutations(): Boolean {
        synchronized(pendingLock) {
            val upserts = readMapLocked(KEY_PENDING_UPSERTS)
            if (upserts.values.any { it.isNotEmpty() }) return true
            val deletes = readMapLocked(KEY_PENDING_DELETES)
            return deletes.values.any { it.isNotEmpty() }
        }
    }

    private fun readMapLocked(key: String): MutableMap<String, MutableSet<String>> {
        val raw = prefs.getString(key, null).orEmpty()
        if (raw.isBlank()) return LinkedHashMap()
        return runCatching {
            val obj = JSONObject(raw)
            val out = LinkedHashMap<String, MutableSet<String>>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val col = keys.next()
                val arr = obj.optJSONArray(col) ?: continue
                val set = LinkedHashSet<String>()
                for (i in 0 until arr.length()) {
                    val item = arr.optString(i, "").trim()
                    if (item.isNotEmpty()) set.add(item)
                }
                if (set.isNotEmpty()) {
                    out[col] = set
                }
            }
            out
        }.getOrElse { LinkedHashMap() }
    }

    private fun serializeMap(map: Map<String, Set<String>>): String {
        val obj = JSONObject()
        map.forEach { (col, ids) ->
            if (ids.isNotEmpty()) {
                val arr = JSONArray()
                ids.forEach { arr.put(it) }
                obj.put(col, arr)
            }
        }
        return obj.toString()
    }

    private fun writeSingleMapLocked(key: String, map: Map<String, Set<String>>) {
        val encoded = serializeMap(map)
        prefs.edit(commit = true) {
            putString(key, encoded)
        }
    }

    private fun writeMapsLocked(
        upserts: Map<String, Set<String>>,
        deletes: Map<String, Set<String>>
    ) {
        val encodedUpserts = serializeMap(upserts)
        val encodedDeletes = serializeMap(deletes)
        prefs.edit(commit = true) {
            putString(KEY_PENDING_UPSERTS, encodedUpserts)
            putString(KEY_PENDING_DELETES, encodedDeletes)
        }
    }

    companion object {
        private const val PREFS_NAME = "hisab_cloud_sync_prefs"
        private const val KEY_AUTO_SYNC_ENABLED = "auto_sync_enabled"
        private const val KEY_EXPLICIT_GOOGLE_SIGN_OUT = "explicit_google_sign_out"
        private const val KEY_LAST_SYNCED_AT_MS = "last_synced_at_ms"
        private const val KEY_SEEDED_PREFIX = "seeded_uid_"
        private const val KEY_PENDING_UPSERTS = "pending_upserts_json"
        private const val KEY_PENDING_DELETES = "pending_deletes_json"
    }
}
