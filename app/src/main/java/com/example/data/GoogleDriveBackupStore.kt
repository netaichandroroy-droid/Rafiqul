package com.example.data

import android.content.Context
import androidx.core.content.edit

data class GoogleDriveBackupStatus(
    val isConnected: Boolean = false,
    val connectedEmail: String = "",
    val driveFileId: String = "",
    val lastBackupAtMs: Long = 0L,
    val lastBackupSummary: String = "",
    val autoBackupEnabled: Boolean = true
)

class GoogleDriveBackupStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadStatus(): GoogleDriveBackupStatus {
        return GoogleDriveBackupStatus(
            isConnected = prefs.getBoolean(KEY_CONNECTED, false),
            connectedEmail = prefs.getString(KEY_EMAIL, "")?.trim().orEmpty(),
            driveFileId = prefs.getString(KEY_FILE_ID, "")?.trim().orEmpty(),
            lastBackupAtMs = prefs.getLong(KEY_LAST_BACKUP_MS, 0L),
            lastBackupSummary = prefs.getString(KEY_LAST_SUMMARY, "")?.trim().orEmpty(),
            autoBackupEnabled = prefs.getBoolean(KEY_AUTO_BACKUP, true)
        )
    }

    fun saveConnectedAccount(email: String) {
        prefs.edit(commit = true) {
            putBoolean(KEY_CONNECTED, true)
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

    fun setAutoBackupEnabled(enabled: Boolean) {
        prefs.edit(commit = true) {
            putBoolean(KEY_AUTO_BACKUP, enabled)
        }
    }

    fun disconnect() {
        prefs.edit(commit = true) {
            putBoolean(KEY_CONNECTED, false)
            remove(KEY_EMAIL)
            remove(KEY_FILE_ID)
            remove(KEY_LAST_BACKUP_MS)
            remove(KEY_LAST_SUMMARY)
        }
    }

    companion object {
        private const val PREFS_NAME = "hisab_google_drive_prefs"
        private const val KEY_CONNECTED = "drive_connected"
        private const val KEY_EMAIL = "drive_email"
        private const val KEY_FILE_ID = "drive_file_id"
        private const val KEY_LAST_BACKUP_MS = "drive_last_backup_ms"
        private const val KEY_LAST_SUMMARY = "drive_last_summary"
        private const val KEY_AUTO_BACKUP = "drive_auto_backup_enabled"
    }
}
