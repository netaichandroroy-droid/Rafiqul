package com.example.ai

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AiCallCenterPrefsStore
import com.example.data.MemberStore
import com.example.data.MemberTakaJomaStore
import com.example.data.MonthlySalesStore
import com.example.model.BanglaFormatters
import com.example.model.MemberSalesDuesCalculator

object MondayAutoCallScheduler {

    const val ACTION_MONDAY_AUTO_CALL = "com.example.ACTION_MONDAY_AUTO_CALL"
    const val EXTRA_OPEN_AI_CALL_CENTER = "open_ai_call_center"
    const val EXTRA_AUTO_START_MONDAY_CALLS = "auto_start_monday_calls"
    private const val REQUEST_CODE = 7104
    private const val CHANNEL_ID = "ai_call_center_monday"
    private const val NOTIFICATION_ID = 7105

    fun syncSchedule(context: Context) {
        val appContext = context.applicationContext
        val prefs = AiCallCenterPrefsStore(appContext)
        val config = prefs.loadConfig()
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildAlarmPendingIntent(appContext)

        if (!config.autoCallMondayEnabled) {
            runCatching { alarmManager.cancel(pendingIntent) }
            return
        }

        val triggerAtMs = AiCallCenterPrefsStore.nextMondayTriggerMs(
            hour = config.scheduledHour,
            minute = config.scheduledMinute
        )
        runCatching {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMs,
                pendingIntent
            )
        }
    }

    fun showMondayCallQueueNotification(context: Context, dueCount: Int, totalDues: Double) {
        if (dueCount <= 0) return
        val appContext = context.applicationContext
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Monday AI Call Center",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when the Monday due-member auto-call queue is ready."
            }
            manager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_AI_CALL_CENTER, true)
            putExtra(EXTRA_AUTO_START_MONDAY_CALLS, true)
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            NOTIFICATION_ID,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedTotal = BanglaFormatters.formatBdt(totalDues)
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Monday Free AI Call Ready")
            .setContentText("$dueCount member(s) have dues ($formattedTotal). Tap to start female AI calls.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildAlarmPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MondayAutoCallReceiver::class.java).apply {
            action = ACTION_MONDAY_AUTO_CALL
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class MondayAutoCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val prefs = AiCallCenterPrefsStore(appContext)
        val config = prefs.loadConfig()
        if (!config.autoCallMondayEnabled) return

        val members = MemberStore(appContext).loadAll()
        val monthlySales = MonthlySalesStore(appContext).loadAll()
        val submissions = MemberTakaJomaStore(appContext).loadAll()
        val dueSummaries = MemberSalesDuesCalculator.buildAllMemberSummaries(
            members = members,
            monthlySales = monthlySales,
            submissions = submissions
        ).filter { it.dues > 0.0 }

        if (dueSummaries.isNotEmpty()) {
            prefs.setPendingAutoStartMondayKey(AiCallCenterPrefsStore.currentCycleKey())
            val totalDues = dueSummaries.sumOf { it.dues }
            MondayAutoCallScheduler.showMondayCallQueueNotification(
                context = appContext,
                dueCount = dueSummaries.size,
                totalDues = totalDues
            )
        }

        // Schedule next Monday automatically
        MondayAutoCallScheduler.syncSchedule(appContext)
    }
}
