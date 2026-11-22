package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.core.content.edit
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class MemberAppLinkStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDownloadUrl(): String {
        return prefs.getString(KEY_DOWNLOAD_URL, "")?.trim().orEmpty()
    }

    fun saveDownloadUrl(rawUrl: String): Result<String> {
        val cleaned = rawUrl.trim()
        if (cleaned.isEmpty()) {
            prefs.edit(commit = true) { remove(KEY_DOWNLOAD_URL) }
            return Result.success("")
        }
        if (cleaned.any { it.isWhitespace() }) {
            return Result.failure(
                IllegalArgumentException("অনুগ্রহ করে সঠিক http:// বা https:// ডাউনলোড লিঙ্ক দিন।")
            )
        }
        val hasExplicitScheme = HAS_SCHEME_REGEX.matches(cleaned) && !HOST_PORT_REGEX.matches(cleaned)
        val normalized = if (!hasExplicitScheme) {
            "https://$cleaned"
        } else {
            cleaned
        }
        if (!isValidHttpUrl(normalized)) {
            return Result.failure(
                IllegalArgumentException("অনুগ্রহ করে সঠিক http:// বা https:// ডাউনলোড লিঙ্ক দিন।")
            )
        }
        prefs.edit(commit = true) {
            putString(KEY_DOWNLOAD_URL, normalized)
        }
        return Result.success(normalized)
    }

    fun prepareApkShareUri(): Uri? {
        return runCatching {
            val sourcePath = appContext.applicationInfo.sourceDir ?: return null
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists() || !sourceFile.isFile) return null

            val shareDir = File(appContext.cacheDir, SHARE_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(shareDir, APK_SHARE_FILE_NAME).canonicalFile
            val canonicalShareDir = shareDir.canonicalFile
            if (!destFile.path.startsWith(canonicalShareDir.path + File.separator)) {
                return null
            }

            FileInputStream(sourceFile).use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                destFile
            )
        }.getOrNull()
    }

    companion object {
        private const val PREFS_NAME = "hisab_member_app_link_prefs"
        private const val KEY_DOWNLOAD_URL = "member_app_download_url"
        private const val SHARE_DIR_NAME = "share"
        private const val APK_SHARE_FILE_NAME = "Member_Mobile_App.apk"
        private val HAS_SCHEME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:.*")
        private val HOST_PORT_REGEX = Regex("^[a-zA-Z0-9.-]+:\\d+(/.*)?$")

        fun isValidHttpUrl(url: String): Boolean {
            val trimmed = url.trim()
            if (trimmed.isEmpty() || trimmed.any { it.isWhitespace() }) return false
            val parsed = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return false
            val scheme = parsed.scheme?.lowercase() ?: return false
            val host = parsed.host?.trim() ?: return false
            return (scheme == "https" || scheme == "http") &&
                host.isNotEmpty() &&
                !host.contains(':') &&
                (host.contains('.') || host.equals("localhost", ignoreCase = true))
        }

        fun openUrlSafely(context: Context, url: String): Boolean {
            val cleaned = url.trim()
            if (!isValidHttpUrl(cleaned)) return false
            return runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleaned)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }.getOrDefault(false)
        }

        fun shareApkOrLink(context: Context, apkUri: Uri?, downloadUrl: String): Boolean {
            return runCatching {
                val intent = if (apkUri != null) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "application/vnd.android.package-archive"
                        putExtra(Intent.EXTRA_STREAM, apkUri)
                        putExtra(
                            Intent.EXTRA_SUBJECT,
                            "মেম্বার মোবাইল অ্যাপ (APK)"
                        )
                        if (isValidHttpUrl(downloadUrl)) {
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "মেম্বার মোবাইল অ্যাপ ডাউনলোড লিঙ্ক: $downloadUrl"
                            )
                        }
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                } else if (isValidHttpUrl(downloadUrl)) {
                    Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "মেম্বার মোবাইল অ্যাপ ডাউনলোড লিঙ্ক: $downloadUrl"
                        )
                    }
                } else {
                    return false
                }
                val chooser = Intent.createChooser(intent, "মেম্বার মোবাইল অ্যাপ শেয়ার করুন").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                true
            }.getOrDefault(false)
        }
    }
}
