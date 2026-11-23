package com.example.data

import android.content.Context
import androidx.core.content.edit
import com.example.auth.PasswordHasher
import com.example.model.Member
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed interface MemberLoginResult {
    data class Success(val member: Member) : MemberLoginResult
    data class Inactive(val member: Member) : MemberLoginResult
    data object WrongPassword : MemberLoginResult
    data object NotFound : MemberLoginResult
}

class MemberStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadAll(): List<Member> {
        val raw = prefs.getString(KEY_MEMBERS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val list = ArrayList<Member>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() }
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                list.add(
                    Member(
                        id = id,
                        name = name,
                        designation = obj.optString("designation", "").trim(),
                        phone = obj.optString("phone", "").trim(),
                        depot = obj.optString("depot", "").trim(),
                        code = obj.optString("code", "").trim(),
                        area = obj.optString("area", "").trim(),
                        isActive = obj.optBoolean("isActive", true),
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis()),
                        passwordHash = obj.optString("passwordHash", ""),
                        isDefaultPassword = obj.optBoolean("isDefaultPassword", true),
                        email = obj.optString("email", "").trim(),
                        address = obj.optString("address", "").trim(),
                        joiningDate = obj.optString("joiningDate", "").trim(),
                        profilePhotoPath = obj.optString("profilePhotoPath", "").trim(),
                        salesAmount = obj.optDouble("salesAmount", 0.0).takeIf { !it.isNaN() && it >= 0.0 } ?: 0.0
                    )
                )
            }
            list.sortedBy { it.createdAtMs }
        }.getOrDefault(emptyList())
    }

    fun findById(memberId: String): Member? {
        return loadAll().firstOrNull { it.id == memberId }
    }

    fun saveAll(members: List<Member>) {
        val array = JSONArray()
        for (member in members) {
            val obj = JSONObject().apply {
                put("id", member.id)
                put("name", member.name)
                put("designation", member.designation)
                put("phone", member.phone)
                put("depot", member.depot)
                put("code", member.code)
                put("area", member.area)
                put("isActive", member.isActive)
                put("createdAtMs", member.createdAtMs)
                put("passwordHash", member.passwordHash)
                put("isDefaultPassword", member.isDefaultPassword)
                put("email", member.email)
                put("address", member.address)
                put("joiningDate", member.joiningDate)
                put("profilePhotoPath", member.profilePhotoPath)
                put("salesAmount", member.salesAmount)
            }
            array.put(obj)
        }
        prefs.edit(commit = true) {
            putString(KEY_MEMBERS, array.toString())
        }
    }

    fun verifyMemberLogin(loginId: String, rawPassword: String): MemberLoginResult {
        val cleanedId = normalizeCredential(loginId)
        if (cleanedId.isEmpty() || rawPassword.isBlank()) return MemberLoginResult.NotFound

        val members = loadAll()
        val matched = members.firstOrNull {
            val memberCode = normalizeCredential(it.code)
            memberCode.isNotEmpty() && memberCode.equals(cleanedId, ignoreCase = true)
        } ?: return MemberLoginResult.NotFound

        if (!matched.isActive) {
            return MemberLoginResult.Inactive(matched)
        }

        val verified = verifyMemberPasswordInternal(matched, rawPassword)
        if (!verified) {
            return MemberLoginResult.WrongPassword
        }

        // Upgrade legacy or pre-migration default-password member so hash stays aligned with code
        val needsHashSync = matched.passwordHash.isBlank() ||
            (matched.isDefaultPassword && !PasswordHasher.verifyPassword(rawPassword, matched.passwordHash))
        if (needsHashSync) {
            val upgradedHash = PasswordHasher.hashPassword(rawPassword.trim(), minLength = 1)
            val upgradedMember = matched.copy(passwordHash = upgradedHash, isDefaultPassword = true)
            saveAll(members.map { if (it.id == matched.id) upgradedMember else it })
            return MemberLoginResult.Success(upgradedMember)
        }

        return MemberLoginResult.Success(matched)
    }

    fun changeMemberPassword(
        memberId: String,
        currentPassword: String,
        newPassword: String
    ): Result<Member> {
        val members = loadAll()
        val target = members.firstOrNull { it.id == memberId }
            ?: return Result.failure(IllegalStateException("Member record not found."))

        if (!verifyMemberPasswordInternal(target, currentPassword)) {
            return Result.failure(IllegalArgumentException("Current password is incorrect."))
        }

        if (newPassword.isBlank() || newPassword.length !in MIN_CUSTOM_PASSWORD_LENGTH..128) {
            return Result.failure(
                IllegalArgumentException("New password must be between 4 and 128 characters.")
            )
        }

        val newHash = PasswordHasher.hashPassword(newPassword, minLength = MIN_CUSTOM_PASSWORD_LENGTH)
        val updated = target.copy(
            passwordHash = newHash,
            isDefaultPassword = false
        )
        saveAll(members.map { if (it.id == memberId) updated else it })
        return Result.success(updated)
    }

    fun resetMemberPasswordToDefault(memberId: String): Member? {
        val members = loadAll()
        val target = members.firstOrNull { it.id == memberId } ?: return null
        val defaultPassword = normalizeCredential(target.code)
        if (defaultPassword.isEmpty()) return null
        val resetHash = PasswordHasher.hashPassword(defaultPassword, minLength = 1)
        val updated = target.copy(passwordHash = resetHash, isDefaultPassword = true)
        saveAll(members.map { if (it.id == memberId) updated else it })
        return updated
    }

    fun updateMemberProfile(
        memberId: String,
        phone: String,
        email: String,
        address: String,
        profilePhotoPath: String? = null
    ): Result<Member> {
        val members = loadAll()
        val target = members.firstOrNull { it.id == memberId }
            ?: return Result.failure(IllegalStateException("Member record not found."))

        val updated = target.copy(
            phone = phone.trim(),
            email = email.trim(),
            address = address.trim(),
            profilePhotoPath = profilePhotoPath ?: target.profilePhotoPath
        )
        saveAll(members.map { if (it.id == memberId) updated else it })
        return Result.success(updated)
    }

    fun updateMemberSales(
        memberId: String,
        salesAmount: Double
    ): Result<Member> {
        if (salesAmount.isNaN() || salesAmount < 0.0) {
            return Result.failure(IllegalArgumentException("Please enter a valid sales amount."))
        }
        val members = loadAll()
        val target = members.firstOrNull { it.id == memberId }
            ?: return Result.failure(IllegalStateException("Member record not found."))

        val updated = target.copy(salesAmount = salesAmount)
        saveAll(members.map { if (it.id == memberId) updated else it })
        return Result.success(updated)
    }

    private fun verifyMemberPasswordInternal(member: Member, rawPassword: String): Boolean {
        val normalizedMemberCode = normalizeCredential(member.code)
        if (member.isDefaultPassword || member.passwordHash.isBlank()) {
            val normalizedInput = normalizeCredential(rawPassword)
            if (normalizedMemberCode.isNotEmpty() &&
                normalizedInput.isNotEmpty() &&
                normalizedInput.equals(normalizedMemberCode, ignoreCase = true)
            ) {
                return true
            }
        }
        return if (member.passwordHash.isNotBlank()) {
            PasswordHasher.verifyPassword(rawPassword, member.passwordHash)
        } else {
            false
        }
    }

    private fun normalizeCredential(value: String): String {
        return value.trim().replace(Regex("\\s+"), " ")
    }

    companion object {
        private const val MIN_CUSTOM_PASSWORD_LENGTH = 4
        private const val PREFS_NAME = "hisab_members_prefs"
        private const val KEY_MEMBERS = "members_json"
    }
}
