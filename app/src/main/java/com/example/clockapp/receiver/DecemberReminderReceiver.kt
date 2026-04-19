package com.example.clockapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.clockapp.data.repository.CalendarRepository
import com.example.clockapp.service.DecemberReminderManager

/**
 * Broadcast receiver for December reminder alarms
 * Shows notification when triggered
 */
class DecemberReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_DECEMBER_REMINDER -> handleReminderTrigger(context)
            DecemberReminderManager.ACTION_DISABLE_REMINDER -> handleDisableReminder(context)
        }
    }

    /**
     * Handle reminder alarm trigger - show notification
     */
    private fun handleReminderTrigger(context: Context) {
        val reminderManager = DecemberReminderManager.getInstance(context)
        val repository = CalendarRepository.getInstance(context)

        // Check if we should show reminder
        val (phase, shouldShow, _) = repository.getDecemberReminderInfo()

        if (shouldShow && phase != CalendarRepository.DecemberPhase.NONE) {
            // Show notification
            reminderManager.checkAndShowReminder()
        }
    }

    /**
     * Handle disable reminder action - disable for current year
     */
    private fun handleDisableReminder(context: Context) {
        val repository = CalendarRepository.getInstance(context)
        val reminderManager = DecemberReminderManager.getInstance(context)

        // Disable reminder for current year
        repository.disableDecemberReminderForYear()

        // Cancel current notification
        reminderManager.cancelReminder()
    }

    companion object {
        const val ACTION_DECEMBER_REMINDER = "com.example.clockapp.DECEMBER_REMINDER"
    }
}
