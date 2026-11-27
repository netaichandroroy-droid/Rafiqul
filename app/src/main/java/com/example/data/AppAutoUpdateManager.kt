package com.example.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.edit
import androidx.core.net.toUri
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

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

data class GitHubReleaseInfo(
    val tagName: String,
    val title: String,
    val versionCode: Int?,
    val versionName: String?,
    val htmlUrl: String,
    val apkDownloadUrl: String?,
    val apkSizeBytes: Long,
    val summary: String
)

data class AppUpdateUiState(
    val autoUpdateEnabled: Boolean = true,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val status: AppUpdateStatus = AppUpdateStatus.IDLE,
    val availableVersionCode: Int? = null,
    val availableVersionName: String? = null,
    val downloadUrl: String? = null,
    val releasePageUrl: String = GITHUB_RELEASES_PAGE_URL,
    val releaseNotes: String = "",
    val isGitHubUpdate: Boolean = false,
    val downloadProgressPercent: Int = 0,
    val lastCheckedAtMs: Long = 0L,
    val statusMessage: String = ""
) {
    companion object {
        const val GITHUB_OWNER = "netaichandroroy-droid"
        const val GITHUB_REPO = "Netai-"
        const val GITHUB_RELEASES_PAGE_URL =
            "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        const val GITHUB_ALL_RELEASES_PAGE_URL =
            "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases"
        const val GITHUB_LATEST_RELEASE_API_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        const val GITHUB_RELEASES_LIST_API_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases?per_page=1"
    }
}

object GitHubReleaseParser {
    private val TAG_BUILD_REGEX = Regex("(?:^|[-_])b(\\d+)", RegexOption.IGNORE_CASE)
    private val BODY_BUILD_REGEX = Regex("Build\\s*Code\\s*:\\s*(\\d+)", RegexOption.IGNORE_CASE)
    private val TITLE_BUILD_REGEX = Regex("Build\\s+(\\d+)", RegexOption.IGNORE_CASE)
    private val VERSION_NAME_REGEX = Regex("v?(\\d+(?:\\.\\d+)+)", RegexOption.IGNORE_CASE)
    private val COMMIT_LINE_REGEX = Regex("(?im)^Commit\\s*:\\s*(.+)$")

    fun parseReleaseResponse(rawJson: String): GitHubReleaseInfo? {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) return null

        val obj = runCatching {
            when {
                trimmed.startsWith("[") -> {
                    val arr = JSONArray(trimmed)
                    if (arr.length() == 0) return null
                    arr.optJSONObject(0)
                }
                trimmed.startsWith("{") -> JSONObject(trimmed)
                else -> null
            }
        }.getOrNull() ?: return null

        if (obj.optBoolean("draft", false)) return null

        val tagName = obj.optString("tag_name", "").trim()
        val title = obj.optString("name", "").trim().ifBlank { tagName }
        val body = obj.optString("body", "").trim()
        val htmlUrl = obj.optString("html_url", "").trim()
            .ifBlank { AppUpdateUiState.GITHUB_RELEASES_PAGE_URL }

        var apkUrl: String? = null
        var apkSize = 0L
        val assets = obj.optJSONArray("assets")
        if (assets != null) {
            for (i in 0 until assets.length()) {
                val asset = assets.optJSONObject(i) ?: continue
                val assetName = asset.optString("name", "")
                val assetDownloadUrl = asset.optString("browser_download_url", "").trim()
                if (assetName.endsWith(".apk", ignoreCase = true) && assetDownloadUrl.isNotEmpty()) {
                    apkUrl = assetDownloadUrl
                    apkSize = asset.optLong("size", 0L)
                    break
                }
            }
        }

        val versionCode = TAG_BUILD_REGEX.find(tagName)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: BODY_BUILD_REGEX.find(body)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: TITLE_BUILD_REGEX.find(title)?.groupValues?.getOrNull(1)?.toIntOrNull()

        val versionName = VERSION_NAME_REGEX.find(tagName)?.groupValues?.getOrNull(1)
            ?: VERSION_NAME_REGEX.find(title)?.groupValues?.getOrNull(1)

        val commitLine = COMMIT_LINE_REGEX.find(body)?.groupValues?.getOrNull(1)?.trim()
        val summary = commitLine?.takeIf { it.isNotBlank() } ?: title

        return GitHubReleaseInfo(
            tagName = tagName,
            title = title,
            versionCode = versionCode,
            versionName = versionName,
            htmlUrl = htmlUrl,
            apkDownloadUrl = apkUrl,
            apkSizeBytes = apkSize,
            summary = summary
        )
    }

    fun isNewerRelease(
        currentVersionCode: Int,
        currentVersionName: String,
        remoteVersionCode: Int?,
        remoteVersionName: String?
    ): Boolean {
        if (remoteVersionCode != null) {
            return remoteVersionCode > currentVersionCode
        }
        val remoteParts = parseVersionParts(remoteVersionName)
        val currentParts = parseVersionParts(currentVersionName)
        if (remoteParts.isEmpty() || currentParts.isEmpty()) return false
        val maxLen = maxOf(remoteParts.size, currentParts.size)
        for (i in 0 until maxLen) {
            val r = remoteParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    private fun parseVersionParts(raw: String?): List<Int> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.trim()
            .removePrefix("v")
            .removePrefix("V")
            .substringBefore('-')
            .split('.')
            .mapNotNull { it.toIntOrNull() }
    }
}

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
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeGitHubCheckJob: Job? = null

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build()
    }

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
                        isGitHubUpdate = false,
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
        if (!userInitiated && !prefs.isAutoUpdateEnabled()) return

        _state.update {
            it.copy(
                status = AppUpdateStatus.CHECKING,
                statusMessage = "Checking GitHub releases for updates..."
            )
        }

        val manager = appUpdateManager
        if (isInstalledFromPlayStore() && manager != null) {
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
                                        isGitHubUpdate = false,
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
                                        isGitHubUpdate = false,
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
                                        isGitHubUpdate = false,
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
                                checkGitHubRelease()
                            }
                        }
                    }
                    .addOnFailureListener { err ->
                        Log.i("AppAutoUpdate", "Play in-app update unavailable: ${err.message}")
                        checkGitHubRelease()
                    }
            }.onFailure { err ->
                Log.i("AppAutoUpdate", "AppUpdateInfo call skipped: ${err.message}")
                checkGitHubRelease()
            }
        } else {
            checkGitHubRelease()
        }
    }

    private fun checkGitHubRelease() {
        activeGitHubCheckJob?.cancel()
        activeGitHubCheckJob = controllerScope.launch {
            val result = withContext(Dispatchers.IO) {
                fetchLatestGitHubRelease()
            }
            val now = System.currentTimeMillis()
            prefs.setLastCheckedAtMs(now)

            when (result) {
                is GitHubCheckResult.Found -> {
                    val release = result.release
                    val isNewer = GitHubReleaseParser.isNewerRelease(
                        currentVersionCode = BuildConfig.VERSION_CODE,
                        currentVersionName = BuildConfig.VERSION_NAME,
                        remoteVersionCode = release.versionCode,
                        remoteVersionName = release.versionName
                    )

                    if (isNewer) {
                        val versionLabel = release.versionName?.let { "v$it" } ?: release.tagName
                        val buildLabel = release.versionCode?.let { " (Build $it)" }.orEmpty()
                        val targetDownloadUrl = release.apkDownloadUrl ?: release.htmlUrl
                        _state.update {
                            it.copy(
                                status = AppUpdateStatus.UPDATE_AVAILABLE,
                                availableVersionCode = release.versionCode,
                                availableVersionName = release.versionName ?: release.tagName,
                                downloadUrl = targetDownloadUrl,
                                releasePageUrl = release.htmlUrl,
                                releaseNotes = release.summary,
                                isGitHubUpdate = true,
                                lastCheckedAtMs = now,
                                statusMessage = "New GitHub update ready: $versionLabel$buildLabel. Tap Download Update to install."
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                status = AppUpdateStatus.UP_TO_DATE,
                                availableVersionCode = null,
                                availableVersionName = null,
                                downloadUrl = null,
                                releasePageUrl = release.htmlUrl,
                                releaseNotes = release.summary,
                                isGitHubUpdate = false,
                                lastCheckedAtMs = now,
                                statusMessage = "App is up to date (v${BuildConfig.VERSION_NAME}, Build ${BuildConfig.VERSION_CODE})."
                            )
                        }
                    }
                }

                GitHubCheckResult.NoReleaseYet -> {
                    _state.update {
                        it.copy(
                            status = AppUpdateStatus.PLAY_UNAVAILABLE,
                            availableVersionCode = null,
                            availableVersionName = null,
                            downloadUrl = null,
                            releasePageUrl = AppUpdateUiState.GITHUB_ALL_RELEASES_PAGE_URL,
                            releaseNotes = "",
                            isGitHubUpdate = false,
                            lastCheckedAtMs = now,
                            statusMessage = "No GitHub Release APK published yet. Push to main branch on GitHub to build a release, then check again."
                        )
                    }
                }

                is GitHubCheckResult.Failed -> {
                    _state.update {
                        it.copy(
                            status = AppUpdateStatus.ERROR,
                            lastCheckedAtMs = now,
                            statusMessage = result.message
                        )
                    }
                }
            }
        }
    }

    private fun fetchLatestGitHubRelease(): GitHubCheckResult {
        return try {
            val latestResponse = executeGitHubGet(AppUpdateUiState.GITHUB_LATEST_RELEASE_API_URL)
            if (latestResponse.code == 200 && latestResponse.body != null) {
                val parsed = GitHubReleaseParser.parseReleaseResponse(latestResponse.body)
                return if (parsed != null) {
                    GitHubCheckResult.Found(parsed)
                } else {
                    GitHubCheckResult.NoReleaseYet
                }
            }

            if (latestResponse.code == 404) {
                val listResponse = executeGitHubGet(AppUpdateUiState.GITHUB_RELEASES_LIST_API_URL)
                if (listResponse.code == 200 && listResponse.body != null) {
                    val parsed = GitHubReleaseParser.parseReleaseResponse(listResponse.body)
                    return if (parsed != null) {
                        GitHubCheckResult.Found(parsed)
                    } else {
                        GitHubCheckResult.NoReleaseYet
                    }
                }
                if (listResponse.code == 404) {
                    return GitHubCheckResult.NoReleaseYet
                }
                return GitHubCheckResult.Failed(
                    "GitHub update check returned HTTP ${listResponse.code}. Tap App Link to check Releases."
                )
            }

            GitHubCheckResult.Failed(
                if (latestResponse.code == 403) {
                    "GitHub API rate limit reached. Tap App Link to download from GitHub Releases."
                } else {
                    "GitHub update check returned HTTP ${latestResponse.code}. Please try again."
                }
            )
        } catch (err: Exception) {
            Log.w("AppAutoUpdate", "GitHub update check error: ${err.message}")
            GitHubCheckResult.Failed(
                "Could not reach GitHub updates. Check your internet connection and tap Check for Update."
            )
        }
    }

    private fun executeGitHubGet(url: String): HttpGetResult {
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Rafiqul-Islam-Android/${BuildConfig.VERSION_NAME}")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            return HttpGetResult(
                code = response.code,
                body = response.body?.string()
            )
        }
    }

    fun checkResumeState(
        activity: Activity? = null,
        updateLauncher: ActivityResultLauncher<IntentSenderRequest>? = null
    ) {
        if (!isInstalledFromPlayStore()) return
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

    fun completeUpdate(activity: Activity? = null) {
        val currentState = _state.value
        val externalUrl = currentState.downloadUrl
        if (currentState.status != AppUpdateStatus.DOWNLOADED && !externalUrl.isNullOrBlank()) {
            if (openExternalUrl(externalUrl, activity)) {
                _state.update {
                    it.copy(
                        statusMessage = "Opening update APK download in browser. Tap the downloaded APK to install."
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        status = AppUpdateStatus.ERROR,
                        statusMessage = "Could not open update download link."
                    )
                }
            }
            return
        }

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

    fun openReleasePage(activity: Activity? = null): Boolean {
        val url = _state.value.releasePageUrl.ifBlank {
            AppUpdateUiState.GITHUB_ALL_RELEASES_PAGE_URL
        }
        return openExternalUrl(url, activity)
    }

    private fun openExternalUrl(url: String, activity: Activity?): Boolean {
        val cleaned = url.trim()
        if (!cleaned.startsWith("https://") && !cleaned.startsWith("http://")) return false
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, cleaned.toUri()).apply {
                if (activity == null) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            (activity ?: appContext).startActivity(intent)
            true
        }.getOrDefault(false)
    }

    private fun isInstalledFromPlayStore(): Boolean {
        return runCatching {
            val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                appContext.packageManager.getInstallSourceInfo(appContext.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getInstallerPackageName(appContext.packageName)
            }
            installer == "com.android.vending"
        }.getOrDefault(false)
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
        activeGitHubCheckJob?.cancel()
        if (listenerRegistered) {
            val manager = appUpdateManager
            if (manager != null) {
                runCatching {
                    manager.unregisterListener(installStateListener)
                }
            }
            listenerRegistered = false
        }
    }

    fun dispose() {
        unregisterListener()
        controllerScope.cancel()
    }

    private data class HttpGetResult(val code: Int, val body: String?)

    private sealed interface GitHubCheckResult {
        data class Found(val release: GitHubReleaseInfo) : GitHubCheckResult
        data object NoReleaseYet : GitHubCheckResult
        data class Failed(val message: String) : GitHubCheckResult
    }
}
