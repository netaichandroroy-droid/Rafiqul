package com.example.data

import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class DriveFileMeta(
    val id: String,
    val name: String,
    val modifiedTime: String = ""
)

object GoogleDriveBackupClient {

    const val BACKUP_FILE_NAME = "rafiqul-hisab-full-backup.json"
    private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    private const val SCOPE_USER_EMAIL = "https://www.googleapis.com/auth/userinfo.email"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    fun buildAuthorizationRequest(): AuthorizationRequest {
        return AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(SCOPE_DRIVE_FILE),
                    Scope(SCOPE_USER_EMAIL)
                )
            )
            .build()
    }

    fun fetchUserEmail(accessToken: String): String? {
        if (accessToken.isBlank()) return null
        return runCatching {
            val request = Request.Builder()
                .url("https://www.googleapis.com/oauth2/v1/userinfo?alt=json")
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@use null
                JSONObject(body).optString("email", "").trim().takeIf { it.isNotEmpty() }
            }
        }.getOrNull()
    }

    fun findBackupFile(accessToken: String): Result<DriveFileMeta?> {
        return runCatching {
            val query = URLEncoder.encode(
                "name = '$BACKUP_FILE_NAME' and trashed = false",
                "UTF-8"
            )
            val url = "https://www.googleapis.com/drive/v3/files" +
                "?q=$query&spaces=drive&orderBy=modifiedTime%20desc&pageSize=1" +
                "&fields=files(id,name,modifiedTime)"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(extractDriveError(response.code, body))
                }
                val files = JSONObject(body).optJSONArray("files")
                if (files == null || files.length() == 0) {
                    null
                } else {
                    val obj = files.getJSONObject(0)
                    DriveFileMeta(
                        id = obj.getString("id"),
                        name = obj.optString("name", BACKUP_FILE_NAME),
                        modifiedTime = obj.optString("modifiedTime", "")
                    )
                }
            }
        }
    }

    fun uploadFullBackup(
        accessToken: String,
        knownFileId: String?,
        jsonPayload: String
    ): Result<DriveFileMeta> {
        return runCatching {
            val existingId = knownFileId?.trim()?.takeIf { it.isNotEmpty() }
                ?: findBackupFile(accessToken).getOrNull()?.id

            if (existingId != null) {
                val patchResult = uploadMultipart(
                    accessToken = accessToken,
                    fileId = existingId,
                    jsonPayload = jsonPayload
                )
                if (patchResult.isSuccess) {
                    return@runCatching patchResult.getOrThrow()
                }
            }

            uploadMultipart(
                accessToken = accessToken,
                fileId = null,
                jsonPayload = jsonPayload
            ).getOrThrow()
        }
    }

    fun downloadFullBackup(
        accessToken: String,
        knownFileId: String?
    ): Result<Pair<DriveFileMeta, String>> {
        return runCatching {
            val meta = findBackupFile(accessToken).getOrNull()
                ?: knownFileId?.trim()?.takeIf { it.isNotEmpty() }?.let {
                    DriveFileMeta(id = it, name = BACKUP_FILE_NAME)
                }
                ?: throw IllegalStateException("Google Drive-এ এখনো কোনো ব্যাকআপ ফাইল পাওয়া যায়নি।")

            val url = "https://www.googleapis.com/drive/v3/files/${meta.id}?alt=media"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(extractDriveError(response.code, body))
                }
                if (body.isBlank()) {
                    throw IllegalStateException("Google Drive-এর ব্যাকআপ ফাইলটি খালি।")
                }
                meta to body
            }
        }
    }

    private fun uploadMultipart(
        accessToken: String,
        fileId: String?,
        jsonPayload: String
    ): Result<DriveFileMeta> {
        return runCatching {
            val metadataJson = JSONObject().apply {
                put("name", BACKUP_FILE_NAME)
                put("mimeType", "application/json")
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType("multipart/related".toMediaType())
                .addPart(metadataJson.toRequestBody(JSON_MEDIA_TYPE))
                .addPart(jsonPayload.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            val url = if (fileId.isNullOrBlank()) {
                "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,modifiedTime"
            } else {
                "https://www.googleapis.com/upload/drive/v3/files/$fileId?uploadType=multipart&fields=id,name,modifiedTime"
            }

            val builder = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $accessToken")
            if (fileId.isNullOrBlank()) {
                builder.post(multipartBody)
            } else {
                builder.patch(multipartBody)
            }

            httpClient.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    throw IllegalStateException(extractDriveError(response.code, body))
                }
                val obj = JSONObject(body)
                DriveFileMeta(
                    id = obj.getString("id"),
                    name = obj.optString("name", BACKUP_FILE_NAME),
                    modifiedTime = obj.optString("modifiedTime", "")
                )
            }
        }
    }

    private fun extractDriveError(code: Int, body: String): String {
        val message = runCatching {
            JSONObject(body).optJSONObject("error")?.optString("message")
        }.getOrNull().orEmpty()
        return when (code) {
            401 -> "Google Drive অনুমোদনের মেয়াদ শেষ হয়েছে। অনুগ্রহ করে আবার সংযুক্ত করুন।"
            403 -> "Google Drive অনুমতি পাওয়া যায়নি। অনুগ্রহ করে আবার সংযুক্ত করুন।"
            404 -> "Google Drive-এ ব্যাকআপ ফাইল খুঁজে পাওয়া যায়নি।"
            else -> if (message.isNotBlank()) "Google Drive ত্রুটি ($code): $message" else "Google Drive সংযোগ ব্যর্থ হয়েছে ($code)।"
        }
    }
}
