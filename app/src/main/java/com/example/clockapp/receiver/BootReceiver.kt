package com.example.clockapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.service.AlarmScheduler
import com.example.clockapp.service.DecemberReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Boot receiver
 * Reschedules all alarms after device reboot
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule all enabled alarms
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarms = AlarmDatabase.getDatabase(context)
                        .alarmDao()
                        .getEnabledAlarms()

                    alarms.forEach { alarm ->
                        AlarmScheduler.scheduleAlarm(context, alarm)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Schedule December reminder if in December
            DecemberReminderScheduler.getInstance(context).scheduleReminder()
        }
    }
}
