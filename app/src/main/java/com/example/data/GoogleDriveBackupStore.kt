package com.example.data

import android.content.Context
import androidx.core.content.edit

data class GoogleDriveBackupStatus(
    val isConnected: Boolean = false,
    val connectedEmail: String = "",
    val driveFileId: String = "",
    val lastBackupAtMs: Long = 0L,
    val lastBackupSummary: String = "",
    val autoBackupEnabled: Boolean = true,
    val oauthConnected: Boolean = false,
    val safDocumentUri: String = "",
    val safDisplayName: String = ""
)

class GoogleDriveBackupStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadStatus(): GoogleDriveBackupStatus {
        val legacyConnected = prefs.getBoolean(KEY_CONNECTED, false)
        val safUri = prefs.getString(KEY_SAF_URI, "")?.trim().orEmpty()
        val safName = prefs.getString(KEY_SAF_NAME, "")?.trim().orEmpty()
        val oauthConnected = if (prefs.contains(KEY_OAUTH_CONNECTED)) {
            prefs.getBoolean(KEY_OAUTH_CONNECTED, false)
        } else {
            legacyConnected && safUri.isEmpty()
        }
        val anyConnected = oauthConnected || safUri.isNotEmpty()
        return GoogleDriveBackupStatus(
            isConnected = anyConnected,
            connectedEmail = prefs.getString(KEY_EMAIL, "")?.trim().orEmpty(),
            driveFileId = prefs.getString(KEY_FILE_ID, "")?.trim().orEmpty(),
            lastBackupAtMs = prefs.getLong(KEY_LAST_BACKUP_MS, 0L),
            lastBackupSummary = prefs.getString(KEY_LAST_SUMMARY, "")?.trim().orEmpty(),
            autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP, true),
            oauthConnected = oauthConnected,
            safDocumentUri = safUri,
            safDisplayName = safName
        )
    }

    fun saveConnectedAccount(email: String) {
        prefs.edit(commit = true) {
            putBoolean(KEY_CONNECTED, true)
            putBoolean(KEY_OAUTH_CONNECTED, true)
            remove(KEY_AUTO_LINK_SKIPPED_EMAIL)
            if (email.isNotBlank()) {
                putString(KEY_EMAIL, email.trim())
            }
        }
    }

    fun updateBackupMetadata(
        fileId: String,
        timestampMs: Long,
        summary: String,
        email: String = ""
    ) {
        prefs.edit(commit = true) {
            putBoolean(KEY_CONNECTED, true)
            putBoolean(KEY_OAUTH_CONNECTED, true)
            remove(KEY_AUTO_LINK_SKIPPED_EMAIL)
            if (email.isNotBlank()) {
                putString(KEY_EMAIL, email.trim())
            }
            if (fileId.isNotBlank()) {
                putString(KEY_FILE_ID, fileId.trim())
            }
            putLong(KEY_LAST_BACKUP_MS, timestampMs)
            putString(KEY_LAST_SUMMARY, summary.trim())
        }
    }

    fun saveSafDocument(
        uriString: String,
        displayName: String,
        email: String = ""
    ) {
        val cleanUri = uriString.trim()
        if (cleanUri.isEmpty()) return
        prefs.edit(commit = true) {
            putBoolean(KEY_CONNECTED, true)
            putString(KEY_SAF_URI, cleanUri)
            putString(KEY_SAF_NAME, displayName.trim())
            remove(KEY_AUTO_LINK_SKIPPED_EMAIL)
            if (email.isNotBlank() && prefs.getString(KEY_EMAIL, "").isNullOrBlank()) {
                putString(KEY_EMAIL, email.trim())
            }
        }
    }

    fun updateSafBackupMetadata(
        uriString: String,
        displayName: String,
        timestampMs: Long,
        summary: String,
        email: String = ""
    ) {
        val cleanUri = uriString.trim()
        if (cleanUri.isEmpty()) return
        prefs.edit(commit = true) {
            putBoolean(KEY_CONNECTED, true)
            putString(KEY_SAF_URI, cleanUri)
            if (displayName.isNotBlank()) {
                putString(KEY_SAF_NAME, displayName.trim())
            }
            remove(KEY_AUTO_LINK_SKIPPED_EMAIL)
            if (email.isNotBlank() && prefs.getString(KEY_EMAIL, "").isNullOrBlank()) {
                putString(KEY_EMAIL, email.trim())
            }
            putLong(KEY_LAST_BACKUP_MS, timestampMs)
            putString(KEY_LAST_SUMMARY, summary.trim())
        }
    }

    fun clearSafDocument() {
        val status = loadStatus()
        prefs.edit(commit = true) {
            remove(KEY_SAF_URI)
            remove(KEY_SAF_NAME)
            putBoolean(KEY_CONNECTED, status.oauthConnected)
        }
    }

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit(commit = true) {
            putBoolean(KEY_AUTO_BACKUP, enabled)
        }
    }

    fun markAutoLinkSkipped(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) return
        prefs.edit(commit = true) {
            putString(KEY_AUTO_LINK_SKIPPED_EMAIL, cleanEmail)
        }
    }

    fun wasAutoLinkSkippedFor(email: String): Boolean {
        val cleanEmail = email.trim()
        if (cleanEmail.isEmpty()) return false
        val skipped = prefs.getString(KEY_AUTO_LINK_SKIPPED_EMAIL, "")?.trim().orEmpty()
        return skipped.equals(cleanEmail, ignoreCase = true)
    }

    fun clearAutoLinkSkipped() {
        prefs.edit(commit = true) {
            remove(KEY_AUTO_LINK_SKIPPED_EMAIL)
        }
    }

    fun disconnect() {
        prefs.edit(commit = true) {
            putBoolean(KEY_CONNECTED, false)
            putBoolean(KEY_OAUTH_CONNECTED, false)
            remove(KEY_EMAIL)
            remove(KEY_FILE_ID)
            remove(KEY_SAF_URI)
            remove(KEY_SAF_NAME)
            remove(KEY_LAST_BACKUP_MS)
            remove(KEY_LAST_SUMMARY)
        }
    }

    fun applySyncedDriveConfig(
        isConnected: Boolean,
        connectedEmail: String,
        driveFileId: String,
        autoBackupEnabled: Boolean,
        lastBackupAtMs: Long,
        lastBackupSummary: String
    ) {
        val cleanEmail = connectedEmail.trim()
        val cleanFileId = driveFileId.trim()
        val cleanSummary = lastBackupSummary.trim()
        val currentSafUri = prefs.getString(KEY_SAF_URI, "")?.trim().orEmpty()
        if (!isConnected) {
            if (currentSafUri.isNotEmpty()) {
                // Keep local SAF link if user has linked a Drive file on this device
                prefs.edit(commit = true) {
                    putBoolean(KEY_OAUTH_CONNECTED, false)
                    putBoolean(KEY_CONNECTED, true)
                    putBoolean(KEY_AUTO_BACKUP, autoBackupEnabled)
                    remove(KEY_FILE_ID)
                }
                return
            }
            prefs.edit(commit = true) {
                putBoolean(KEY_CONNECTED, false)
                putBoolean(KEY_OAUTH_CONNECTED, false)
                putBoolean(KEY_AUTO_BACKUP, autoBackupEnabled)
                remove(KEY_EMAIL)
                remove(KEY_FILE_ID)
                remove(KEY_LAST_BACKUP_MS)
                remove(KEY_LAST_SUMMARY)
            }
            return
        }
        val existingLastMs = prefs.getLong(KEY_LAST_BACKUP_MS, 0L)
        val existingFileId = prefs.getString(KEY_FILE_ID, "")?.trim().orEmpty()
        val shouldAcceptBackupMeta = lastBackupAtMs >= existingLastMs || existingFileId.isEmpty()
        val hasOauthInfo = cleanFileId.isNotEmpty() || (currentSafUri.isEmpty() && cleanEmail.isNotEmpty())
        prefs.edit(commit = true) {
            putBoolean(KEY_CONNECTED, true)
            if (hasOauthInfo) {
                putBoolean(KEY_OAUTH_CONNECTED, true)
            }
            putBoolean(KEY_AUTO_BACKUP, autoBackupEnabled)
            remove(KEY_AUTO_LINK_SKIPPED_EMAIL)
            if (cleanEmail.isNotEmpty()) {
                putString(KEY_EMAIL, cleanEmail)
            }
            if (shouldAcceptBackupMeta) {
                if (cleanFileId.isNotEmpty()) {
                    putString(KEY_FILE_ID, cleanFileId)
                }
                if (lastBackupAtMs > 0L) {
                    putLong(KEY_LAST_BACKUP_MS, lastBackupAtMs)
                }
                if (cleanSummary.isNotEmpty()) {
                    putString(KEY_LAST_SUMMARY, cleanSummary)
                }
            }
        }
    }

    companion object {
        private const val PREFS_NAME = "hisab_google_drive_prefs"
        private const val KEY_CONNECTED = "drive_connected"
        private const val KEY_OAUTH_CONNECTED = "drive_oauth_connected"
        private const val KEY_EMAIL = "drive_email"
        private const val KEY_FILE_ID = "drive_file_id"
        private const val KEY_SAF_URI = "drive_saf_uri"
        private const val KEY_SAF_NAME = "drive_saf_display_name"
        private const val KEY_LAST_BACKUP_MS = "drive_last_backup_ms"
        private const val KEY_LAST_SUMMARY = "drive_last_summary"
        private const val KEY_AUTO_BACKUP = "drive_auto_backup_enabled"
        private const val KEY_AUTO_LINK_SKIPPED_EMAIL = "drive_auto_link_skipped_email"
    }
}
