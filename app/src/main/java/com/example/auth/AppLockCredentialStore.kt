package com.example.auth

import android.content.Context
import androidx.core.content.edit

sealed interface AppSession {
    data object LoggedOut : AppSession
    data object Admin : AppSession
    data class MemberUser(val memberId: String) : AppSession
}

class AppLockCredentialStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    init {
        ensureInitialized()
    }

    private fun ensureInitialized() {
        if (!prefs.contains(KEY_PASSWORD_HASH)) {
            val defaultHash = PasswordHasher.hashPassword(DEFAULT_PASSWORD, minLength = MIN_PASSWORD_LENGTH)
            prefs.edit(commit = true) {
                putString(KEY_LOGIN_ID, DEFAULT_LOGIN_ID)
                putString(KEY_PASSWORD_HASH, defaultHash)
                putBoolean(KEY_IS_DEFAULT_PASSWORD, true)
            }
        }
    }

    fun getLoginId(): String {
        return prefs.getString(KEY_LOGIN_ID, DEFAULT_LOGIN_ID) ?: DEFAULT_LOGIN_ID
    }

    fun isUsingDefaultPassword(): Boolean {
        return prefs.getBoolean(KEY_IS_DEFAULT_PASSWORD, true)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getActiveSession(): AppSession {
        if (!isLoggedIn()) return AppSession.LoggedOut
        val role = prefs.getString(KEY_SESSION_ROLE, ROLE_ADMIN) ?: ROLE_ADMIN
        if (role == ROLE_MEMBER) {
            val memberId = prefs.getString(KEY_SESSION_MEMBER_ID, null)
            if (!memberId.isNullOrBlank()) {
                return AppSession.MemberUser(memberId)
            }
            return AppSession.LoggedOut
        }
        return AppSession.Admin
    }

    fun verifyCredentials(loginId: String, rawPassword: String): Boolean {
        ensureInitialized()
        val storedId = getLoginId()
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null) ?: return false
        if (!loginId.trim().equals(storedId, ignoreCase = true)) return false
        return PasswordHasher.verifyPassword(rawPassword, storedHash)
    }

    fun login(loginId: String, rawPassword: String): Boolean {
        val verified = verifyCredentials(loginId, rawPassword)
        if (verified) {
            prefs.edit(commit = true) {
                putBoolean(KEY_IS_LOGGED_IN, true)
                putString(KEY_SESSION_ROLE, ROLE_ADMIN)
                remove(KEY_SESSION_MEMBER_ID)
            }
        }
        return verified
    }

    fun loginAsMember(memberId: String) {
        prefs.edit(commit = true) {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_SESSION_ROLE, ROLE_MEMBER)
            putString(KEY_SESSION_MEMBER_ID, memberId)
        }
    }

    fun logout() {
        prefs.edit(commit = true) {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_SESSION_ROLE)
            remove(KEY_SESSION_MEMBER_ID)
        }
    }

    fun changePassword(
        currentPassword: String,
        newPassword: String,
        newLoginId: String? = null
    ): Result<Unit> {
        ensureInitialized()
        val storedHash = prefs.getString(KEY_PASSWORD_HASH, null)
            ?: return Result.failure(IllegalStateException("লগইন তথ্য পাওয়া যায়নি।"))

        if (!PasswordHasher.verifyPassword(currentPassword, storedHash)) {
            return Result.failure(IllegalArgumentException("বর্তমান পাসওয়ার্ড সঠিক নয়।"))
        }

        if (newPassword.isBlank() || newPassword.length !in MIN_PASSWORD_LENGTH..128) {
            return Result.failure(
                IllegalArgumentException("নতুন পাসওয়ার্ড কমপক্ষে ৪ থেকে ১২৮ অক্ষরের মধ্যে হতে হবে।")
            )
        }

        val cleanedId = newLoginId?.trim()
        if (cleanedId != null && !LOGIN_ID_REGEX.matches(cleanedId)) {
            return Result.failure(
                IllegalArgumentException("লগইন আইডি ৩ থেকে ৩০ অক্ষরের মধ্যে হতে হবে (a-z, 0-9, _, ., -)।")
            )
        }

        val newHash = PasswordHasher.hashPassword(newPassword, minLength = MIN_PASSWORD_LENGTH)
        prefs.edit(commit = true) {
            if (cleanedId != null) {
                putString(KEY_LOGIN_ID, cleanedId)
            }
            putString(KEY_PASSWORD_HASH, newHash)
            putBoolean(KEY_IS_DEFAULT_PASSWORD, false)
        }
        return Result.success(Unit)
    }

    companion object {
        const val DEFAULT_LOGIN_ID = "rafiqul"
        private const val DEFAULT_PASSWORD = "rafiqul"
        private const val MIN_PASSWORD_LENGTH = 4
        private const val PREFS_NAME = "hisab_app_auth_prefs"
        private const val KEY_LOGIN_ID = "login_id"
        private const val KEY_PASSWORD_HASH = "password_hash"
        private const val KEY_IS_DEFAULT_PASSWORD = "is_default_password"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SESSION_ROLE = "session_role"
        private const val KEY_SESSION_MEMBER_ID = "session_member_id"
        private const val ROLE_ADMIN = "admin"
        private const val ROLE_MEMBER = "member"
        private val LOGIN_ID_REGEX = Regex("^[a-zA-Z0-9_.-]{3,30}$")
    }
}
