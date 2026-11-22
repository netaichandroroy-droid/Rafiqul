package com.example.data

import android.content.Context
import androidx.core.content.edit
import com.example.model.MemberTakaJoma
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class MemberTakaJomaStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadAll(): List<MemberTakaJoma> {
        val raw = prefs.getString(KEY_SUBMISSIONS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            val list = ArrayList<MemberTakaJoma>(array.length())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() }
                val name = obj.optString("name", "").trim()
                if (name.isEmpty()) continue
                list.add(
                    MemberTakaJoma(
                        id = id,
                        memberId = obj.optString("memberId", "").trim(),
                        name = name,
                        code = obj.optString("code", "").trim(),
                        depot = obj.optString("depot", "").trim(),
                        designation = obj.optString("designation", "").trim(),
                        area = obj.optString("area", "").trim(),
                        bankName = obj.optString("bankName", "").trim(),
                        amount = obj.optDouble("amount", 0.0).takeIf { !it.isNaN() && it >= 0.0 } ?: 0.0,
                        date = obj.optString("date", "").trim(),
                        note = obj.optString("note", "").trim(),
                        photoPath = obj.optString("photoPath", "").trim(),
                        slipHash = obj.optString("slipHash", "").trim(),
                        submittedByRole = obj.optString("submittedByRole", "MEMBER").ifBlank { "MEMBER" },
                        createdAtMs = obj.optLong("createdAtMs", System.currentTimeMillis())
                    )
                )
            }
            sortSubmissions(list)
        }.getOrDefault(emptyList())
    }

    fun loadForMember(memberId: String): List<MemberTakaJoma> {
        if (memberId.isBlank()) return emptyList()
        return loadAll().filter { it.memberId == memberId }
    }

    fun saveAll(submissions: List<MemberTakaJoma>) {
        val sorted = sortSubmissions(submissions)
        val array = JSONArray()
        for (item in sorted) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("memberId", item.memberId)
                put("name", item.name)
                put("code", item.code)
                put("depot", item.depot)
                put("designation", item.designation)
                put("area", item.area)
                put("bankName", item.bankName)
                put("amount", item.amount)
                put("date", item.date)
                put("note", item.note)
                put("photoPath", item.photoPath)
                put("slipHash", item.slipHash)
                put("submittedByRole", item.submittedByRole)
                put("createdAtMs", item.createdAtMs)
            }
            array.put(obj)
        }
        prefs.edit(commit = true) {
            putString(KEY_SUBMISSIONS, array.toString())
        }
    }

    fun isDuplicateSlip(slipHash: String, photoPath: String = ""): Boolean {
        val cleanHash = slipHash.trim()
        val cleanPath = photoPath.trim()
        if (cleanHash.isEmpty() && cleanPath.isEmpty()) return false
        return loadAll().any { existing ->
            (cleanHash.isNotEmpty() && existing.slipHash.equals(cleanHash, ignoreCase = true)) ||
                (cleanPath.isNotEmpty() && existing.photoPath == cleanPath)
        }
    }

    fun tryAddSubmission(entry: MemberTakaJoma): Result<List<MemberTakaJoma>> {
        val current = loadAll()
        if (current.any { it.id == entry.id }) {
            return Result.failure(
                IllegalStateException("এই জমা ফর্মটি ইতিমধ্যে সংরক্ষিত হয়েছে।")
            )
        }
        val cleanHash = entry.slipHash.trim()
        val cleanPath = entry.photoPath.trim()
        val hasDuplicateSlip = current.any { existing ->
            (cleanHash.isNotEmpty() && existing.slipHash.equals(cleanHash, ignoreCase = true)) ||
                (cleanPath.isNotEmpty() && existing.photoPath == cleanPath)
        }
        if (hasDuplicateSlip) {
            return Result.failure(
                IllegalStateException("এই স্লিপটি আগেও জমা দেওয়া হয়েছে। এক স্লিপ দুইবার জমা দেওয়া যাবে না।")
            )
        }
        val updated = sortSubmissions(listOf(entry) + current)
        saveAll(updated)
        return Result.success(updated)
    }

    fun addSubmission(entry: MemberTakaJoma): List<MemberTakaJoma> {
        return tryAddSubmission(entry).getOrElse { loadAll() }
    }

    fun deleteSubmission(
        submissionId: String,
        requesterMemberId: String? = null
    ): List<MemberTakaJoma> {
        val current = loadAll()
        val updated = current.filterNot { item ->
            item.id == submissionId && (requesterMemberId == null || item.memberId == requesterMemberId)
        }
        if (updated.size != current.size) {
            saveAll(updated)
        }
        return if (requesterMemberId != null) {
            updated.filter { it.memberId == requesterMemberId }
        } else {
            updated
        }
    }

    private fun sortSubmissions(list: List<MemberTakaJoma>): List<MemberTakaJoma> {
        return list.sortedWith(
            compareByDescending<MemberTakaJoma> { it.date }
                .thenByDescending { it.createdAtMs }
        )
    }

    companion object {
        private const val PREFS_NAME = "hisab_member_taka_joma_prefs"
        private const val KEY_SUBMISSIONS = "submissions_json"
    }
}
