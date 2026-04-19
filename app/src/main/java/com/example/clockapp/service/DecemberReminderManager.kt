package com.example.clockapp.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.clockapp.R
import com.example.clockapp.data.repository.CalendarRepository
import com.example.clockapp.receiver.DecemberReminderReceiver
import com.example.clockapp.ui.main.MainActivity

/**
 * Manager for December reminder notifications
 * Handles showing notifications to remind users to set next year's alarms
 */
class DecemberReminderManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Check and show December reminder notification
     * @return true if notification was shown
     */
    fun checkAndShowReminder(): Boolean {
        val repository = CalendarRepository.getInstance(context)
        val (phase, shouldShow, message) = repository.getDecemberReminderInfo()

        if (shouldShow && phase != CalendarRepository.DecemberPhase.NONE) {
            showNotification(message, phase)
            return true
        }
        return false
    }

    /**
     * Show December reminder notification with actions
     */
    private fun showNotification(message: String, phase: CalendarRepository.DecemberPhase) {
        // Create intent to open MainActivity (去设置)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_FROM_REMINDER, true)
            putExtra(EXTRA_REMINDER_PHASE, phase.name)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            REMINDER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create "不再提醒" action
        val disableIntent = Intent(context, DecemberReminderReceiver::class.java).apply {
            action = ACTION_DISABLE_REMINDER
        }
        val disablePendingIntent = PendingIntent.getBroadcast(
            context,
            DISABLE_REQUEST_CODE,
            disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification with actions
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("设置下一年闹钟提醒")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_alarm, "去设置", pendingIntent)
            .addAction(R.drawable.ic_alarm_off, "不再提醒", disablePendingIntent)
            .build()

        notificationManager.notify(REMINDER_NOTIFICATION_ID, notification)
    }

    /**
     * Cancel reminder notification
     */
    fun cancelReminder() {
        notificationManager.cancel(REMINDER_NOTIFICATION_ID)
    }

    companion object {
        const val CHANNEL_ID = "december_reminder_channel"
        const val REMINDER_NOTIFICATION_ID = 2001
        const val DISABLE_REQUEST_CODE = 2002
        const val EXTRA_FROM_REMINDER = "from_december_reminder"
        const val EXTRA_REMINDER_PHASE = "reminder_phase"
        const val ACTION_DISABLE_REMINDER = "com.example.clockapp.DISABLE_DECEMBER_REMINDER"

        @Volatile
        private var INSTANCE: DecemberReminderManager? = null

        fun getInstance(context: Context): DecemberReminderManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DecemberReminderManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}
