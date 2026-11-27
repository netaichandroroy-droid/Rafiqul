package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ai.FemaleAiVoiceOption
import com.example.data.AiCallCenterPrefsStore
import com.example.data.AiCallScheduleConfig
import com.example.data.CallScheduleDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiCallCenterScheduleTest {

    @Test
    fun `persists 7-day enabled set, custom call time, and male speaker voices`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AiCallCenterPrefsStore(context)

        assertEquals(7, CallScheduleDay.entries.size)

        val selectedDays = setOf(Calendar.WEDNESDAY, Calendar.FRIDAY)
        store.saveConfig(
            AiCallScheduleConfig(
                autoCallMondayEnabled = true,
                enabledDays = selectedDays,
                scheduledHour = 15,
                scheduledMinute = 45,
                selectedVoice = FemaleAiVoiceOption.NITAI
            )
        )

        val loaded = store.loadConfig()
        assertEquals(selectedDays, loaded.enabledDays)
        assertEquals(15, loaded.scheduledHour)
        assertEquals(45, loaded.scheduledMinute)
        assertEquals(FemaleAiVoiceOption.NITAI, loaded.selectedVoice)
        assertEquals("Nitai Chondro Roy", loaded.selectedVoice.agentName)
        assertTrue(loaded.selectedVoice.isMale)

        assertEquals("Md.Rafiqul", FemaleAiVoiceOption.RAFIQUL.agentName)
        assertEquals("Nikhil Chondro Roy", FemaleAiVoiceOption.NIKHIL.agentName)
    }

    @Test
    fun `calculates next trigger for any enabled day and custom time`() {
        // Thursday 2026-11-26 at 10:00 AM
        val thursdayMorning = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 26, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Enabled Friday at 2:30 PM -> next trigger must be Friday Nov 27, 14:30
        val nextFri = AiCallCenterPrefsStore.nextScheduleTriggerMs(
            enabledDays = setOf(Calendar.FRIDAY),
            hour = 14,
            minute = 30,
            nowMs = thursdayMorning
        )
        assertNotNull(nextFri)
        val friCal = Calendar.getInstance().apply { timeInMillis = nextFri!! }
        assertEquals(Calendar.FRIDAY, friCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(14, friCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, friCal.get(Calendar.MINUTE))

        // Enabled Thursday (same day) at 4:15 PM -> next trigger must be today Nov 26, 16:15
        val nextSameDay = AiCallCenterPrefsStore.nextScheduleTriggerMs(
            enabledDays = setOf(Calendar.THURSDAY),
            hour = 16,
            minute = 15,
            nowMs = thursdayMorning
        )
        assertNotNull(nextSameDay)
        val sameDayCal = Calendar.getInstance().apply { timeInMillis = nextSameDay!! }
        assertEquals(Calendar.THURSDAY, sameDayCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(26, sameDayCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(16, sameDayCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, sameDayCal.get(Calendar.MINUTE))
    }

    @Test
    fun `auto-start triggers only on enabled days at or after updated call time`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = AiCallCenterPrefsStore(context)

        // Thursday 2026-11-26 at 11:20 AM
        val thursdayMidday = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 26, 11, 20, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Only Monday is ON -> Thursday should NOT be due
        store.saveConfig(
            AiCallScheduleConfig(
                autoCallMondayEnabled = true,
                enabledDays = setOf(Calendar.MONDAY),
                scheduledHour = 10,
                scheduledMinute = 0
            )
        )
        assertFalse(store.isScheduledMondayAutoStartDue(thursdayMidday))

        // Turn Thursday ON at 11:15 AM -> due immediately
        store.saveConfig(
            AiCallScheduleConfig(
                autoCallMondayEnabled = true,
                enabledDays = setOf(Calendar.THURSDAY),
                scheduledHour = 11,
                scheduledMinute = 15
            )
        )
        assertTrue(store.isScheduledMondayAutoStartDue(thursdayMidday))

        // Update call time to 14:30 -> not due yet at 11:20
        store.saveConfig(
            AiCallScheduleConfig(
                autoCallMondayEnabled = true,
                enabledDays = setOf(Calendar.THURSDAY),
                scheduledHour = 14,
                scheduledMinute = 30
            )
        )
        assertFalse(store.isScheduledMondayAutoStartDue(thursdayMidday))
    }
}
