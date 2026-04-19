package com.example.clockapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.service.AlarmScheduler
import com.example.clockapp.service.DecemberReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Application entry point
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Create notification channel
        createNotificationChannel()

        // Reschedule all alarms (handle reboot scenario)
        rescheduleAlarms()

        // Schedule December reminder if in December
        scheduleDecemberReminder()
    }

    /**
     * Schedule December reminder notification
     */
    private fun scheduleDecemberReminder() {
        val scheduler = DecemberReminderScheduler.getInstance(this)
        scheduler.scheduleReminder()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            // Alarm ringing channel
            val alarmChannel = NotificationChannel(
                "alarm_channel",
                "Alarm Reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alarm ringing notification"
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(alarmChannel)

            // December reminder channel
            val reminderChannel = NotificationChannel(
                "december_reminder_channel",
                "Year-end Reminder",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to set alarms for the next year in December"
            }
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    private fun rescheduleAlarms() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarms = AlarmDatabase.getDatabase(this@App)
                    .alarmDao()
                    .getEnabledAlarms()

                alarms.forEach { alarm ->
                    AlarmScheduler.scheduleAlarm(this@App, alarm)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
