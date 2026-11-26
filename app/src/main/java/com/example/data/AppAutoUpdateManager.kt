package com.example.data

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.edit
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppUpdateStatus {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    DOWNLOADING,
    DOWNLOADED,
    PLAY_UNAVAILABLE,
    ERROR
}

data class AppUpdateUiState(
    val autoUpdateEnabled: Boolean = true,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val status: AppUpdateStatus = AppUpdateStatus.IDLE,
    val availableVersionCode: Int? = null,
    val downloadProgressPercent: Int = 0,
    val lastCheckedAtMs: Long = 0L,
    val statusMessage: String = ""
)

class AppUpdatePrefsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun isAutoUpdateEnabled(): Boolean = prefs.getBoolean(KEY_AUTO_UPDATE_ENABLED, true)

    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.edit(commit = true) {
            putBoolean(KEY_AUTO_UPDATE_ENABLED, enabled)
        }
    }

    fun getLastCheckedAtMs(): Long = prefs.getLong(KEY_LAST_CHECKED_AT_MS, 0L)

    fun setLastCheckedAtMs(timestampMs: Long) {
        prefs.edit(commit = true) {
            putLong(KEY_LAST_CHECKED_AT_MS, timestampMs)
        }
    }

    companion object {
        private const val PREFS_NAME = "hisab_app_update_prefs"
        private const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        private const val KEY_LAST_CHECKED_AT_MS = "last_checked_at_ms"
    }
}

class AppAutoUpdateController(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = AppUpdatePrefsStore(appContext)
    private val appUpdateManager: AppUpdateManager? = runCatching {
        AppUpdateManagerFactory.create(appContext)
    }.getOrNull()

    private var listenerRegistered = false

    private val _state = MutableStateFlow(
        AppUpdateUiState(
            autoUpdateEnabled = prefs.isAutoUpdateEnabled(),
            currentVersionName = BuildConfig.VERSION_NAME,
            currentVersionCode = BuildConfig.VERSION_CODE,
            lastCheckedAtMs = prefs.getLastCheckedAtMs()
        )
    )
    val state: StateFlow<AppUpdateUiState> = _state.asStateFlow()

    private val installStateListener = InstallStateUpdatedListener { installState ->
        when (installState.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val total = installState.totalBytesToDownload()
                val downloaded = installState.bytesDownloaded()
                val pct = if (total > 0L) {
                    ((downloaded * 100L) / total).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                _state.update {
                    it.copy(
                        status = AppUpdateStatus.DOWNLOADING,
                        downloadProgressPercent = pct,
                        statusMessage = "Downloading update ($pct%)..."
                    )
                }
            }

            InstallStatus.DOWNLOADED -> {
                _state.update {
                    it.copy(
                        status = AppUpdateStatus.DOWNLOADED,
                        downloadProgressPercent = 100,
                        statusMessage = "Update downloaded. Tap Restart & Install to finish."
                    )
                }
            }

            InstallStatus.INSTALLED -> {
                unregisterListener()
                _state.update {
                    it.copy(
                        status = AppUpdateStatus.UP_TO_DATE,
                        downloadProgressPercent = 100,
                        statusMessage = "App updated successfully."
                    )
                }
            }

            InstallStatus.CANCELED -> {
                unregisterListener()
                _state.update {
                    it.copy(
                        status = AppUpdateStatus.UPDATE_AVAILABLE,
                        statusMessage = "Update postponed. You can update anytime from Settings."
                    )
                }
            }

            InstallStatus.FAILED -> {
                unregisterListener()
                _state.update {
                    it.copy(
                        status = AppUpdateStatus.ERROR,
                        statusMessage = "Update download failed. Please try again."
                    )
                }
            }

            else -> Unit
        }
    }

    fun setAutoUpdateEnabled(enabled: Boolean) {
        prefs.setAutoUpdateEnabled(enabled)
        _state.update {
            it.copy(
                autoUpdateEnabled = enabled,
                statusMessage = if (enabled) {
                    "Auto-update check enabled."
                } else {
                    "Auto-update paused. You can still check manually."
                }
            )
        }
    }

    fun checkForUpdates(
        activity: Activity? = null,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null,
        userInitiated: Boolean = false
    ) {
        val manager = appUpdateManager
        if (manager == null) {
            val now = System.currentTimeMillis()
            prefs.setLastCheckedAtMs(now)
            _state.update {
                it.copy(
                    status = AppUpdateStatus.PLAY_UNAVAILABLE,
                    lastCheckedAtMs = now,
                    statusMessage = "Auto-update is active (v${BuildConfig.VERSION_NAME})."
                )
            }
            return
        }

        if (!userInitiated && !prefs.isAutoUpdateEnabled()) return

        _state.update {
            it.copy(
                status = AppUpdateStatus.CHECKING,
                statusMessage = "Checking for app updates..."
            )
        }

        runCatching {
            manager.appUpdateInfo
                .addOnSuccessListener { info ->
                    val now = System.currentTimeMillis()
                    prefs.setLastCheckedAtMs(now)

                    when {
                        info.installStatus() == InstallStatus.DOWNLOADED -> {
                            _state.update {
                                it.copy(
                                    status = AppUpdateStatus.DOWNLOADED,
                                    downloadProgressPercent = 100,
                                    lastCheckedAtMs = now,
                                    statusMessage = "Update downloaded and ready to install."
                                )
                            }
                        }

                        info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                            _state.update {
                                it.copy(
                                    status = AppUpdateStatus.DOWNLOADING,
                                    availableVersionCode = info.availableVersionCode(),
                                    lastCheckedAtMs = now,
                                    statusMessage = "Resuming app update..."
                                )
                            }
                            if (activity != null && updateLauncher != null &&
                                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                            ) {
                                runCatching {
                                    manager.startUpdateFlowForResult(
                                        info,
                                        updateLauncher,
                                        AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                                    )
                                }
                            }
                        }

                        info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                            val nextCode = info.availableVersionCode()
                            _state.update {
                                it.copy(
                                    status = AppUpdateStatus.UPDATE_AVAILABLE,
                                    availableVersionCode = nextCode,
                                    lastCheckedAtMs = now,
                                    statusMessage = "New update available (Build $nextCode)."
                                )
                            }
                            val canStartFlexible = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
                            val canStartImmediate = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                            val updateType = when {
                                canStartFlexible -> AppUpdateType.FLEXIBLE
                                canStartImmediate -> AppUpdateType.IMMEDIATE
                                else -> null
                            }
                            if (updateType != null && activity != null && updateLauncher != null) {
                                if (updateType == AppUpdateType.FLEXIBLE) {
                                    registerListener()
                                }
                                runCatching {
                                    manager.startUpdateFlowForResult(
                                        info,
                                        updateLauncher,
                                        AppUpdateOptions.newBuilder(updateType).build()
                                    )
                                }.onFailure { err ->
                                    Log.w("AppAutoUpdate", "Failed to start update flow: ${err.message}")
                                }
                            }
                        }

                        else -> {
                            _state.update {
                                it.copy(
                                    status = AppUpdateStatus.UP_TO_DATE,
                                    availableVersionCode = null,
                                    lastCheckedAtMs = now,
                                    statusMessage = "App is up to date (v${BuildConfig.VERSION_NAME})."
                                )
                            }
                        }
                    }
                }
                .addOnFailureListener { err ->
                    val now = System.currentTimeMillis()
                    prefs.setLastCheckedAtMs(now)
                    Log.i("AppAutoUpdate", "Play in-app update unavailable: ${err.message}")
                    _state.update {
                        it.copy(
                            status = AppUpdateStatus.PLAY_UNAVAILABLE,
                            lastCheckedAtMs = now,
                            statusMessage = "Running v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}). Auto-updates apply automatically when an update is released."
                        )
                    }
                }
        }.onFailure { err ->
            val now = System.currentTimeMillis()
            prefs.setLastCheckedAtMs(now)
            Log.i("AppAutoUpdate", "AppUpdateInfo call skipped: ${err.message}")
            _state.update {
                it.copy(
                    status = AppUpdateStatus.PLAY_UNAVAILABLE,
                    lastCheckedAtMs = now,
                    statusMessage = "Running v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})."
                )
            }
        }
    }

    fun checkResumeState(
        activity: Activity? = null,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    ) {
        val manager = appUpdateManager ?: return
        runCatching {
            manager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) {
                    _state.update {
                        it.copy(
                            status = AppUpdateStatus.DOWNLOADED,
                            downloadProgressPercent = 100,
                            statusMessage = "Update downloaded and ready to install."
                        )
                    }
                } else if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                    activity != null &&
                    updateLauncher != null &&
                    info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                ) {
                    runCatching {
                        manager.startUpdateFlowForResult(
                            info,
                            updateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                    }
                }
            }
        }
    }

    fun handleUpdateFlowResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_CANCELED) {
            unregisterListener()
            _state.update {
                it.copy(
                    status = AppUpdateStatus.UPDATE_AVAILABLE,
                    statusMessage = "Update postponed."
                )
            }
        }
    }

    fun completeUpdate() {
        val manager = appUpdateManager ?: return
        runCatching {
            manager.completeUpdate()
        }.onFailure { err ->
            _state.update {
                it.copy(
                    status = AppUpdateStatus.ERROR,
                    statusMessage = err.localizedMessage ?: "Could not complete update."
                )
            }
        }
    }

    private fun registerListener() {
        if (listenerRegistered) return
        val manager = appUpdateManager ?: return
        runCatching {
            manager.registerListener(installStateListener)
            listenerRegistered = true
        }
    }

    fun unregisterListener() {
        if (!listenerRegistered) return
        val manager = appUpdateManager ?: return
        runCatching {
            manager.unregisterListener(installStateListener)
        }
        listenerRegistered = false
    }
}
