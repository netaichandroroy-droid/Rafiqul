package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.scale
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

data class SavedSlipPhoto(
    val path: String,
    val sha256: String
)

object MemberPhotoStorage {

    private const val AVATAR_DIR_NAME = "member_avatars"
    private const val TAKA_JOMA_DIR_NAME = "taka_joma_photos"
    private const val MAX_AVATAR_PX = 512
    private const val MAX_RECEIPT_PX = 1024

    fun saveAvatarFromUri(context: Context, memberId: String, uri: Uri): String? {
        return saveImageFromUri(
            context = context,
            dirName = AVATAR_DIR_NAME,
            fileBaseName = memberId,
            maxPx = MAX_AVATAR_PX,
            uri = uri
        )
    }

    fun saveTakaJomaPhotoFromUri(context: Context, submissionId: String, uri: Uri): String? {
        return saveTakaJomaPhotoWithHashFromUri(context, submissionId, uri)?.path
    }

    fun saveTakaJomaPhotoWithHashFromUri(
        context: Context,
        submissionId: String,
        uri: Uri
    ): SavedSlipPhoto? {
        val originalHash = computeUriSha256(context, uri)
        val savedPath = saveImageFromUri(
            context = context,
            dirName = TAKA_JOMA_DIR_NAME,
            fileBaseName = submissionId,
            maxPx = MAX_RECEIPT_PX,
            uri = uri
        ) ?: return null
        val effectiveHash = originalHash.ifBlank { computeFileSha256(savedPath) }
        return SavedSlipPhoto(path = savedPath, sha256 = effectiveHash)
    }

    fun computeFileSha256(filePath: String): String {
        if (filePath.isBlank()) return ""
        return runCatching {
            val file = File(filePath)
            if (!file.exists() || !file.isFile) return ""
            FileInputStream(file).use { streamSha256(it) }
        }.getOrDefault("")
    }

    private fun computeUriSha256(context: Context, uri: Uri): String {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { streamSha256(it) } ?: ""
        }.getOrDefault("")
    }

    private fun streamSha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8192)
        var read: Int
        while (input.read(buffer).also { read = it } > 0) {
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }

    fun deleteStoredPhoto(context: Context, path: String) {
        if (path.isBlank()) return
        runCatching {
            val target = File(path).canonicalFile
            val avatarDir = File(context.filesDir, AVATAR_DIR_NAME).canonicalFile
            val receiptDir = File(context.filesDir, TAKA_JOMA_DIR_NAME).canonicalFile
            val inAllowedDir = target.path.startsWith(avatarDir.path + File.separator) ||
                target.path.startsWith(receiptDir.path + File.separator)
            if (inAllowedDir && target.exists() && target.isFile) {
                target.delete()
            }
        }
    }

    private fun saveImageFromUri(
        context: Context,
        dirName: String,
        fileBaseName: String,
        maxPx: Int,
        uri: Uri
    ): String? {
        val safeId = fileBaseName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        if (safeId.isBlank()) return null

        return runCatching {
            val resolver = context.contentResolver
            val decoded = resolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: return null

            val scaled = scaleDownIfNeeded(decoded, maxPx)
            val dir = File(context.filesDir, dirName).apply {
                if (!exists()) mkdirs()
            }
            val destFile = File(dir, "$safeId.jpg").canonicalFile
            val canonicalDir = dir.canonicalFile
            if (!destFile.path.startsWith(canonicalDir.path + File.separator)) {
                return null
            }

            FileOutputStream(destFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 86, out)
            }
            if (scaled !== decoded) {
                scaled.recycle()
            }
            decoded.recycle()
            destFile.absolutePath
        }.getOrNull()
    }

    fun loadBitmapFromPath(path: String): Bitmap? {
        if (path.isBlank()) return null
        return runCatching {
            val file = File(path)
            if (!file.exists() || !file.isFile) return null
            BitmapFactory.decodeFile(file.absolutePath)
        }.getOrNull()
    }

    fun createTempSlipCaptureFile(context: Context, submissionId: String): File {
        val safeId = submissionId.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "slip" }
        val dir = File(context.cacheDir, "slip_captures").apply {
            if (!exists()) mkdirs()
        }
        return File(dir, "capture_$safeId.jpg")
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        if (w <= maxEdge && h <= maxEdge) return bitmap
        val ratio = minOf(maxEdge.toFloat() / w.toFloat(), maxEdge.toFloat() / h.toFloat())
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return bitmap.scale(targetW, targetH)
    }
}
