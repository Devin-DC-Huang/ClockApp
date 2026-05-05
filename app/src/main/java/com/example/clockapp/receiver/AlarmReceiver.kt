package com.example.clockapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.service.AlarmService
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

/**
 * Alarm broadcast receiver
 * Receives alarm trigger events from AlarmManager
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        val alarmTitle = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "闹钟"
        val alarmUuid = intent.getStringExtra(EXTRA_ALARM_UUID)
        val vibrate = intent.getBooleanExtra(EXTRA_VIBRATE, true)
        val isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        val snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)
        val snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)

        if (alarmId == -1) return

        // For special alarms: verify today is in pre-calculated dates
        if (alarmUuid != null && !isSnooze) {
            val shouldRing = runBlocking {
                try {
                    val alarm = AlarmDatabase.getDatabase(context).alarmDao().getAlarmById(alarmUuid)
                    if (alarm?.isSpecialAlarm == true) {
                        val today = LocalDate.now()
                        alarm.dates.contains(today)
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to verify special alarm dates", e)
                    true // Fail-safe: ring on error
                }
            }
            if (!shouldRing) {
                Log.d(TAG, "Skipping special alarm $alarmId, today not in pre-calculated dates")
                return
            }
        }

        // Acquire wake lock to ensure device stays awake (点亮屏幕)
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.FULL_WAKE_LOCK or
            PowerManager.ACQUIRE_CAUSES_WAKEUP or
            PowerManager.ON_AFTER_RELEASE,
            "ClockApp::AlarmWakeLock"
        )
        wakeLock.acquire(60000) // Hold for 60 seconds

        // Start alarm ringing service
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, alarmTitle)
            putExtra(EXTRA_ALARM_UUID, alarmUuid)
            putExtra(EXTRA_VIBRATE, vibrate)
            putExtra(EXTRA_IS_SNOOZE, isSnooze)
            putExtra(EXTRA_SNOOZE_ENABLED, snoozeEnabled)
            putExtra(EXTRA_SNOOZE_MINUTES, snoozeMinutes)
        }

        // 在后台启动前台服务（APP被杀死时也能启动）
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            // Android 12+ 可能需要特殊处理
            try {
                context.startService(serviceIntent)
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_TITLE = "alarm_title"
        const val EXTRA_ALARM_UUID = "alarm_uuid"
        const val EXTRA_VIBRATE = "vibrate"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_SNOOZE_ENABLED = "snooze_enabled"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }
}
