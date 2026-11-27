package com.example.data

import android.content.Context
import com.example.ai.FemaleAiVoiceOption
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class CallScheduleDay(
    val calendarDay: Int,
    val shortLabel: String,
    val fullLabel: String
) {
    SATURDAY(Calendar.SATURDAY, "Sat", "Saturday"),
    SUNDAY(Calendar.SUNDAY, "Sun", "Sunday"),
    MONDAY(Calendar.MONDAY, "Mon", "Monday"),
    TUESDAY(Calendar.TUESDAY, "Tue", "Tuesday"),
    WEDNESDAY(Calendar.WEDNESDAY, "Wed", "Wednesday"),
    THURSDAY(Calendar.THURSDAY, "Thu", "Thursday"),
    FRIDAY(Calendar.FRIDAY, "Fri", "Friday");

    companion object {
        fun fromCalendarDay(day: Int): CallScheduleDay? = entries.firstOrNull { it.calendarDay == day }
    }
}

enum class AiCallStatus(val label: String) {
    PENDING("Pending"),
    DIALING("Calling"),
    CALLED("Called"),
    NO_PHONE("No Phone"),
    SKIPPED("Skipped");

    companion object {
        fun fromWire(value: String?): AiCallStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: PENDING
        }
    }
}

data class AiCallRecord(
    val memberId: String,
    val memberName: String,
    val phone: String,
    val duesAmount: Double,
    val status: AiCallStatus,
    val updatedAtMs: Long,
    val scriptPreview: String = ""
)

data class AiCallScheduleConfig(
    val autoCallMondayEnabled: Boolean = true,
    val enabledDays: Set<Int> = setOf(Calendar.MONDAY),
    val scheduledHour: Int = 10,
    val scheduledMinute: Int = 0,
    val autoVoiceGreeting: Boolean = true,
    val alsoDialPhoneNumber: Boolean = false,
    val selectedVoice: FemaleAiVoiceOption = FemaleAiVoiceOption.AOEDE
)

class AiCallCenterPrefsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadConfig(): AiCallScheduleConfig {
        val voiceId = prefs.getString(KEY_VOICE_ID, FemaleAiVoiceOption.AOEDE.voiceId)
        val voice = FemaleAiVoiceOption.entries.firstOrNull {
            it.voiceId.equals(voiceId, ignoreCase = true)
        } ?: FemaleAiVoiceOption.AOEDE

        val enabledDays = if (prefs.contains(KEY_ENABLED_DAYS)) {
            val raw = prefs.getString(KEY_ENABLED_DAYS, "").orEmpty()
            raw.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
                .toSet()
        } else {
            setOf(Calendar.MONDAY)
        }

        return AiCallScheduleConfig(
            autoCallMondayEnabled = prefs.getBoolean(KEY_MONDAY_ENABLED, true),
            enabledDays = enabledDays,
            scheduledHour = prefs.getInt(KEY_SCHEDULE_HOUR, 10).coerceIn(0, 23),
            scheduledMinute = prefs.getInt(KEY_SCHEDULE_MINUTE, 0).coerceIn(0, 59),
            autoVoiceGreeting = prefs.getBoolean(KEY_AUTO_VOICE, true),
            alsoDialPhoneNumber = prefs.getBoolean(KEY_ALSO_DIAL_SIM, false),
            selectedVoice = voice
        )
    }

    fun saveConfig(config: AiCallScheduleConfig) {
        val serializedDays = CallScheduleDay.entries
            .map { it.calendarDay }
            .filter { it in config.enabledDays }
            .joinToString(",")
        prefs.edit()
            .putBoolean(KEY_MONDAY_ENABLED, config.autoCallMondayEnabled)
            .putString(KEY_ENABLED_DAYS, serializedDays)
            .putInt(KEY_SCHEDULE_HOUR, config.scheduledHour.coerceIn(0, 23))
            .putInt(KEY_SCHEDULE_MINUTE, config.scheduledMinute.coerceIn(0, 59))
            .putBoolean(KEY_AUTO_VOICE, config.autoVoiceGreeting)
            .putBoolean(KEY_ALSO_DIAL_SIM, config.alsoDialPhoneNumber)
            .putString(KEY_VOICE_ID, config.selectedVoice.voiceId)
            .apply()
    }

    fun getLastRunMondayKey(): String = prefs.getString(KEY_LAST_RUN_MONDAY, "").orEmpty()

    fun setLastRunMondayKey(key: String) {
        prefs.edit().putString(KEY_LAST_RUN_MONDAY, key).apply()
    }

    fun getPendingAutoStartMondayKey(): String = prefs.getString(KEY_PENDING_AUTO_START_MONDAY, "").orEmpty()

    fun setPendingAutoStartMondayKey(key: String) {
        prefs.edit().putString(KEY_PENDING_AUTO_START_MONDAY, key).apply()
    }

    fun clearRunMarkersForUpdatedSchedule() {
        prefs.edit()
            .putString(KEY_LAST_RUN_MONDAY, "")
            .putString(KEY_PENDING_AUTO_START_MONDAY, "")
            .apply()
    }

    fun isScheduledMondayAutoStartDue(nowMs: Long = System.currentTimeMillis()): Boolean {
        val config = loadConfig()
        if (!config.autoCallMondayEnabled || config.enabledDays.isEmpty()) return false
        if (!isDayEnabled(config.enabledDays, nowMs)) return false
        val slotKey = scheduleSlotKey(config, nowMs)
        val dayKey = currentCycleKey(nowMs)
        val lastRun = getLastRunMondayKey()
        if (lastRun == slotKey || lastRun == dayKey) return false
        val pendingKey = getPendingAutoStartMondayKey()
        if (pendingKey == slotKey || pendingKey == dayKey) return true
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val currentMinutes = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val scheduledMinutes = config.scheduledHour * 60 + config.scheduledMinute
        return currentMinutes >= scheduledMinutes
    }

    fun loadCallRecordsForCurrentCycle(nowMs: Long = System.currentTimeMillis()): Map<String, AiCallRecord> {
        val activeCycle = currentCycleKey(nowMs)
        val storedCycle = prefs.getString(KEY_RECORDS_CYCLE, "").orEmpty()
        if (storedCycle != activeCycle) {
            return emptyMap()
        }
        val raw = prefs.getString(KEY_RECORDS_JSON, "").orEmpty()
        if (raw.isBlank()) return emptyMap()
        return runCatching {
            val array = JSONArray(raw)
            val result = linkedMapOf<String, AiCallRecord>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val memberId = obj.optString("memberId").trim()
                if (memberId.isEmpty()) continue
                result[memberId] = AiCallRecord(
                    memberId = memberId,
                    memberName = obj.optString("memberName"),
                    phone = obj.optString("phone"),
                    duesAmount = obj.optDouble("duesAmount", 0.0),
                    status = AiCallStatus.fromWire(obj.optString("status")),
                    updatedAtMs = obj.optLong("updatedAtMs", 0L),
                    scriptPreview = obj.optString("scriptPreview")
                )
            }
            result
        }.getOrDefault(emptyMap())
    }

    fun upsertCallRecord(
        record: AiCallRecord,
        nowMs: Long = System.currentTimeMillis()
    ) {
        val activeCycle = currentCycleKey(nowMs)
        val current = loadCallRecordsForCurrentCycle(nowMs).toMutableMap()
        current[record.memberId] = record
        saveCycleRecords(activeCycle, current.values.toList())
    }

    fun clearCycleRecords(nowMs: Long = System.currentTimeMillis()) {
        saveCycleRecords(currentCycleKey(nowMs), emptyList())
    }

    private fun saveCycleRecords(cycleKey: String, records: List<AiCallRecord>) {
        val array = JSONArray()
        records.forEach { rec ->
            array.put(
                JSONObject().apply {
                    put("memberId", rec.memberId)
                    put("memberName", rec.memberName)
                    put("phone", rec.phone)
                    put("duesAmount", rec.duesAmount)
                    put("status", rec.status.name)
                    put("updatedAtMs", rec.updatedAtMs)
                    put("scriptPreview", rec.scriptPreview)
                }
            )
        }
        prefs.edit()
            .putString(KEY_RECORDS_CYCLE, cycleKey)
            .putString(KEY_RECORDS_JSON, array.toString())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "ai_call_center_prefs"
        private const val KEY_MONDAY_ENABLED = "monday_auto_call_enabled"
        private const val KEY_ENABLED_DAYS = "enabled_call_days_csv"
        private const val KEY_SCHEDULE_HOUR = "monday_schedule_hour"
        private const val KEY_SCHEDULE_MINUTE = "monday_schedule_minute"
        private const val KEY_AUTO_VOICE = "monday_auto_voice_greeting"
        private const val KEY_ALSO_DIAL_SIM = "monday_also_dial_sim"
        private const val KEY_VOICE_ID = "monday_voice_id"
        private const val KEY_LAST_RUN_MONDAY = "last_run_monday_key"
        private const val KEY_PENDING_AUTO_START_MONDAY = "pending_auto_start_monday_key"
        private const val KEY_RECORDS_CYCLE = "call_records_cycle_key"
        private const val KEY_RECORDS_JSON = "call_records_json"

        fun todayDayOfWeek(nowMs: Long = System.currentTimeMillis()): Int {
            val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
            return cal.get(Calendar.DAY_OF_WEEK)
        }

        fun isDayEnabled(enabledDays: Set<Int>, nowMs: Long = System.currentTimeMillis()): Boolean {
            return todayDayOfWeek(nowMs) in enabledDays
        }

        fun isMonday(nowMs: Long = System.currentTimeMillis()): Boolean {
            return todayDayOfWeek(nowMs) == Calendar.MONDAY
        }

        fun currentCycleKey(nowMs: Long = System.currentTimeMillis()): String {
            return SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date(nowMs))
        }

        fun scheduleSlotKey(
            config: AiCallScheduleConfig,
            nowMs: Long = System.currentTimeMillis()
        ): String {
            val dayKey = currentCycleKey(nowMs)
            return "$dayKey@${config.scheduledHour.coerceIn(0, 23)}:${config.scheduledMinute.coerceIn(0, 59)}"
        }

        fun nextScheduleTriggerMs(
            enabledDays: Set<Int>,
            hour: Int,
            minute: Int,
            nowMs: Long = System.currentTimeMillis()
        ): Long? {
            if (enabledDays.isEmpty()) return null
            val targetHour = hour.coerceIn(0, 23)
            val targetMinute = minute.coerceIn(0, 59)
            for (dayOffset in 0..7) {
                val candidate = Calendar.getInstance().apply {
                    timeInMillis = nowMs
                    add(Calendar.DAY_OF_YEAR, dayOffset)
                    set(Calendar.HOUR_OF_DAY, targetHour)
                    set(Calendar.MINUTE, targetMinute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val dayOfWeek = candidate.get(Calendar.DAY_OF_WEEK)
                if (dayOfWeek in enabledDays && candidate.timeInMillis > nowMs) {
                    return candidate.timeInMillis
                }
            }
            return null
        }

        fun nextMondayTriggerMs(
            hour: Int,
            minute: Int,
            nowMs: Long = System.currentTimeMillis()
        ): Long {
            return nextScheduleTriggerMs(
                enabledDays = setOf(Calendar.MONDAY),
                hour = hour,
                minute = minute,
                nowMs = nowMs
            ) ?: (nowMs + 7L * 24L * 60L * 60L * 1000L)
        }

        fun formatNextScheduleLabel(
            enabledDays: Set<Int>,
            hour: Int,
            minute: Int,
            autoEnabled: Boolean = true,
            nowMs: Long = System.currentTimeMillis()
        ): String {
            if (!autoEnabled) return "Paused"
            if (enabledDays.isEmpty()) return "No day selected (turn ON any day)"
            val trigger = nextScheduleTriggerMs(enabledDays, hour, minute, nowMs)
                ?: return "No day selected"
            return SimpleDateFormat("EEE, dd MMM • hh:mm a", Locale.ENGLISH).format(Date(trigger))
        }

        fun formatNextMondayLabel(
            hour: Int,
            minute: Int,
            nowMs: Long = System.currentTimeMillis()
        ): String {
            return formatNextScheduleLabel(
                enabledDays = setOf(Calendar.MONDAY),
                hour = hour,
                minute = minute,
                nowMs = nowMs
            )
        }

        fun formatTimeSlot(hour: Int, minute: Int): String {
            val h = hour.coerceIn(0, 23)
            val m = minute.coerceIn(0, 59)
            val displayHour = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            val minStr = m.toString().padStart(2, '0')
            val suffix = if (h < 12) "AM" else "PM"
            return "$displayHour:$minStr $suffix"
        }

        fun formatHourSlot(hour: Int): String = formatTimeSlot(hour, 0)

        fun formatEnabledDaysSummary(enabledDays: Set<Int>): String {
            if (enabledDays.isEmpty()) return "No day active"
            val ordered = CallScheduleDay.entries.filter { it.calendarDay in enabledDays }
            return if (ordered.size == CallScheduleDay.entries.size) {
                "Every Day"
            } else {
                ordered.joinToString(", ") { it.shortLabel }
            }
        }

        fun sanitizeDialNumber(rawPhone: String): String {
            val trimmed = rawPhone.trim()
            if (trimmed.isEmpty()) return ""
            val cleaned = buildString {
                trimmed.forEachIndexed { idx, ch ->
                    if (ch.isDigit() || (ch == '+' && idx == 0)) append(ch)
                }
            }
            val digitCount = cleaned.count { it.isDigit() }
            return if (digitCount >= 6) cleaned else ""
        }
    }
}
