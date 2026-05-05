package com.example.clockapp.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.clockapp.data.model.Alarm
import com.example.clockapp.receiver.AlarmReceiver
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Alarm scheduler service
 * Manages alarm ringing using AlarmManager
 */
object AlarmScheduler {

    /**
     * Check if app can schedule exact alarms
     */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Schedule a single alarm
     */
    fun scheduleAlarm(context: Context, alarm: Alarm): Boolean {
        // Always cancel any existing alarm with the same ID first
        cancelAlarm(context, alarm)
        
        if (!alarm.isEnabled) {
            Log.d("AlarmScheduler", "Alarm ${alarm.id} is disabled, not scheduling")
            return false
        }

        val nextRingTime = alarm.calculateNextRingTime()
        if (nextRingTime == null) {
            Log.d("AlarmScheduler", "Alarm ${alarm.id} has no next ring time, not scheduling")
            return false
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createAlarmIntent(context, alarm)
        
        Log.d("AlarmScheduler", "Scheduling alarm: id=${alarm.id}, intId=${alarm.getAlarmIntId()}, time=$nextRingTime")

        // Convert LocalDateTime to epoch milliseconds
        val triggerTime = nextRingTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // Set exact alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ requires SCHEDULE_EXACT_ALARM permission
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                // Use setExact as fallback (more reliable than setAndAllowWhileIdle)
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }

        return true
    }

    /**
     * Cancel a single alarm
     */
    fun cancelAlarm(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = createAlarmIntent(context, alarm)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d("AlarmScheduler", "Cancelled alarm: id=${alarm.id}, intId=${alarm.getAlarmIntId()}")
    }

    /**
     * Cancel all alarms
     */
    suspend fun cancelAllAlarms(context: Context, alarms: List<Alarm>) {
        alarms.forEach { cancelAlarm(context, it) }
    }

    /**
     * Schedule snooze alarm with custom snooze minutes
     */
    fun scheduleSnoozeAlarm(context: Context, alarmId: Int, title: String, snoozeMinutes: Int = 5) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeTime = Instant.now().plus(snoozeMinutes.toLong(), ChronoUnit.MINUTES).toEpochMilli()

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId + 100000)
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, "$title (Snooze)")
            putExtra(AlarmReceiver.EXTRA_IS_SNOOZE, true)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId + 100000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                snoozeTime,
                pendingIntent
            )
        }
    }

    /**
     * Create alarm intent
     */
    private fun createAlarmIntent(context: Context, alarm: Alarm): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.example.clockapp.ALARM_TRIGGER"
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarm.getAlarmIntId())
            putExtra(AlarmReceiver.EXTRA_ALARM_TITLE, alarm.title)
            putExtra(AlarmReceiver.EXTRA_ALARM_UUID, alarm.id)
            putExtra(AlarmReceiver.EXTRA_VIBRATE, alarm.vibrate)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, alarm.snoozeEnabled)
            putExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, alarm.snoozeMinutes)
        }

        return PendingIntent.getBroadcast(
            context,
            alarm.getAlarmIntId(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
