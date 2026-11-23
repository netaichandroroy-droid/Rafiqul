package com.example.data

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class DriveFileMeta(
    val id: String,
    val name: String,
    val modifiedTime: String = ""
)

class DriveUnauthorizedException(message: String) : IllegalStateException(message)

sealed interface DriveTokenResult {
    data class Success(val accessToken: String, val accountEmail: String) : DriveTokenResult
    data class NeedsConsent(val intent: Intent, val accountEmail: String) : DriveTokenResult
    data class Failure(val message: String, val isNetworkError: Boolean = false) : DriveTokenResult
}

object GoogleDriveBackupClient {

    const val BACKUP_FILE_NAME = "rafiqul-hisab-full-backup.json"
    const val GOOGLE_ACCOUNT_TYPE = "com.google"
    private const val SCOPE_DRIVE_FILE = "https://www.googleapis.com/auth/drive.file"
    private const val SCOPE_USER_EMAIL = "https://www.googleapis.com/auth/userinfo.email"
    private const val OAUTH2_SCOPE_STRING = "oauth2:$SCOPE_DRIVE_FILE $SCOPE_USER_EMAIL"

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(40, TimeUnit.SECONDS)
            .writeTimeout(40, TimeUnit.SECONDS)
            .build()
    }

    fun buildAuthorizationRequest(accountEmail: String? = null): AuthorizationRequest {
        val builder = AuthorizationRequest.builder()
            .setRequestedScopes(
                listOf(
                    Scope(SCOPE_DRIVE_FILE),
                    Scope(SCOPE_USER_EMAIL)
                )
            )
        val cleanEmail = accountEmail?.trim().orEmpty()
        if (cleanEmail.isNotEmpty()) {
            builder.setAccount(Account(cleanEmail, GOOGLE_ACCOUNT_TYPE))
        }
        return builder.build()
    }

    fun createGoogleAccountPickerIntent(selectedEmail: String? = null): Intent {
        val cleanEmail = selectedEmail?.trim().takeIf { !it.isNullOrEmpty() }
        return AccountManager.newChooseAccountIntent(
            cleanEmail?.let { Account(it, GOOGLE_ACCOUNT_TYPE) },
            null,
            arrayOf(GOOGLE_ACCOUNT_TYPE),
            null,
            null,
            null,
            null
        )
    }

    suspend fun acquireAccessTokenViaGoogleAuthUtil(
        context: Context,
        accountEmail: String
    ): DriveTokenResult {
        val cleanEmail = accountEmail.trim()
        if (cleanEmail.isEmpty()) {
            return DriveTokenResult.Failure("Gmail অ্যাকাউন্ট পাওয়া যায়নি।")
        }
        return withContext(Dispatchers.IO) {
            try {
                val account = Account(cleanEmail, GOOGLE_ACCOUNT_TYPE)
                val token = GoogleAuthUtil.getToken(
                    context.applicationContext,
                    account,
                    OAUTH2_SCOPE_STRING
                )
                if (token.isNotBlank()) {
                    DriveTokenResult.Success(token, cleanEmail)
                } else {
                    DriveTokenResult.Failure("Google Drive টোকেন পাওয়া যায়নি।")
                }
            } catch (recoverable: UserRecoverableAuthException) {
                val consentIntent = recoverable.intent
                if (consentIntent != null) {
                    DriveTokenResult.NeedsConsent(consentIntent, cleanEmail)
                } else {
                    DriveTokenResult.Failure("Google Drive অনুমোদন স্ক্রিন খোলা যায়নি।")
                }
            } catch (_: IOException) {
                DriveTokenResult.Failure(
                    message = "ইন্টারনেট সংযোগ পরীক্ষা করে আবার চেষ্টা করুন।",
                    isNetworkError = true
                )
            } catch (authErr: GoogleAuthException) {
                val detail = authErr.localizedMessage.orEmpty().trim()
                val msg = if (detail.isNotEmpty()) {
                    "Google Drive অনুমোদন ব্যর্থ হয়েছে: $detail"
                } else {
                    "Google Drive অনুমোদন ব্যর্থ হয়েছে।"
                }
                DriveTokenResult.Failure(msg)
            } catch (e: Exception) {
                DriveTokenResult.Failure(e.localizedMessage ?: "Google Drive সংযোগ ব্যর্থ হয়েছে।")
            }
        }
    }

    suspend fun clearCachedToken(context: Context, accessToken: String) {
        if (accessToken.isBlank()) return
        withContext(Dispatchers.IO) {
            runCatching {
                GoogleAuthUtil.clearToken(context.applicationContext, accessToken)
            }
        }
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
                    throw buildDriveException(response.code, body)
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
            val findRes = findBackupFile(accessToken)
            val findErr = findRes.exceptionOrNull()
            if (findErr is DriveUnauthorizedException) {
                throw findErr
            }
            val existingId = knownFileId?.trim()?.takeIf { it.isNotEmpty() }
                ?: findRes.getOrNull()?.id

            if (existingId != null) {
                val patchResult = uploadMultipart(
                    accessToken = accessToken,
                    fileId = existingId,
                    jsonPayload = jsonPayload
                )
                if (patchResult.isSuccess) {
                    return@runCatching patchResult.getOrThrow()
                }
                val patchErr = patchResult.exceptionOrNull()
                if (patchErr is DriveUnauthorizedException) {
                    throw patchErr
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
            val findRes = findBackupFile(accessToken)
            val findErr = findRes.exceptionOrNull()
            if (findErr is DriveUnauthorizedException) {
                throw findErr
            }
            val meta = findRes.getOrNull()
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
                    throw buildDriveException(response.code, body)
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
                    throw buildDriveException(response.code, body)
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

    private fun buildDriveException(code: Int, body: String): IllegalStateException {
        val message = extractDriveError(code, body)
        return if (code == 401) {
            DriveUnauthorizedException(message)
        } else {
            IllegalStateException(message)
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
