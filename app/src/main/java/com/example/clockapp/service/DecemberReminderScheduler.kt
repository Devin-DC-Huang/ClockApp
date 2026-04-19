package com.example.clockapp.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.clockapp.receiver.DecemberReminderReceiver
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Scheduler for December reminder notifications
 * Schedules daily checks in December to show reminder notifications
 */
class DecemberReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule December reminder checks
     * Only schedules if current date is in December
     */
    fun scheduleReminder() {
        val today = LocalDate.now()

        // Only schedule in December
        if (today.monthValue != 12) {
            Log.d(TAG, "Not December, skipping reminder schedule")
            return
        }

        val intent = Intent(context, DecemberReminderReceiver::class.java).apply {
            action = DecemberReminderReceiver.ACTION_DECEMBER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule for 9:00 AM every day
        var triggerDateTime = today.atTime(9, 0)
        val now = LocalDateTime.now()
        
        // If 9 AM passed, start tomorrow
        if (triggerDateTime.isBefore(now)) {
            triggerDateTime = triggerDateTime.plusDays(1)
        }
        
        val triggerTime = triggerDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Check if we can schedule exact alarms
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    // Fall back to inexact alarm
                    alarmManager.setAndAllowWhileIdle(
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
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            Log.d(TAG, "December reminder scheduled for ${triggerDateTime.format(formatter)}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule December reminder", e)
        }
    }

    /**
     * Cancel scheduled reminder
     */
    fun cancelReminder() {
        val intent = Intent(context, DecemberReminderReceiver::class.java).apply {
            action = DecemberReminderReceiver.ACTION_DECEMBER_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "December reminder cancelled")
    }

    companion object {
        private const val TAG = "DecemberReminderScheduler"
        const val REMINDER_REQUEST_CODE = 2001

        @Volatile
        private var INSTANCE: DecemberReminderScheduler? = null

        fun getInstance(context: Context): DecemberReminderScheduler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DecemberReminderScheduler(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
