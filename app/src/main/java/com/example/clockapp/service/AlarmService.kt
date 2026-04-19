package com.example.clockapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.CombinedVibration
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.clockapp.R
import com.example.clockapp.receiver.AlarmReceiver
import com.example.clockapp.ui.ring.RingActivity

/**
 * Alarm ringing service
 * 业界标准实现：同时只响一个闹钟，新的闹钟触发时跳过（或替换）
 * 参考：Google Clock、Samsung Clock 等主流闹钟应用
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    
    // 当前正在响铃的闹钟（业界做法：只保留一个）
    private var currentAlarm: AlarmData? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra(AlarmReceiver.EXTRA_ALARM_ID, -1) ?: -1
        val alarmTitle = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_TITLE) ?: "闹钟"
        val alarmUuid = intent?.getStringExtra(AlarmReceiver.EXTRA_ALARM_UUID)
        val vibrate = intent?.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATE, true) ?: true
        val isSnooze = intent?.getBooleanExtra(AlarmReceiver.EXTRA_IS_SNOOZE, false) ?: false
        val snoozeEnabled = intent?.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE_ENABLED, true) ?: true
        val snoozeMinutes = intent?.getIntExtra(AlarmReceiver.EXTRA_SNOOZE_MINUTES, 5) ?: 5

        Log.d(TAG, "AlarmService started for alarmId=$alarmId, title=$alarmTitle")

        if (alarmId == -1) {
            Log.e(TAG, "Invalid alarm ID, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        // 检查是否是同一个闹钟重复触发（避免重复响铃）
        if (currentAlarm?.id == alarmId) {
            Log.d(TAG, "Alarm $alarmId is already ringing, ignoring duplicate")
            return START_STICKY
        }

        // 如果已经有闹钟在响铃，跳过新的闹钟（业界标准做法）
        if (currentAlarm != null) {
            Log.d(TAG, "Another alarm is ringing (id=${currentAlarm?.id}), skipping alarm $alarmId")
            return START_STICKY
        }

        // 开始响铃
        val alarmData = AlarmData(alarmId, alarmTitle, alarmUuid, isSnooze, snoozeEnabled, snoozeMinutes, vibrate)
        currentAlarm = alarmData
        startRinging(alarmData)

        return START_STICKY
    }

    /**
     * 开始响铃
     */
    private fun startRinging(alarmData: AlarmData) {
        Log.d(TAG, "Starting to ring alarm ${alarmData.id}")

        // Start foreground service
        try {
            val notification = createNotification(alarmData)
            startForeground(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service, continuing without notification", e)
        }

        // Play ringtone
        playRingtone()

        // Vibrate
        if (alarmData.vibrate) {
            startVibration()
        }

        // Launch ringing activity
        launchRingActivity(alarmData)
    }

    /**
     * 启动响铃界面
     */
    private fun launchRingActivity(alarmData: AlarmData) {
        val ringIntent = Intent(this, RingActivity::class.java).apply {
            putExtra("alarm_id", alarmData.id)
            putExtra("alarm_title", alarmData.title)
            putExtra("alarm_uuid", alarmData.uuid)
            putExtra("is_snooze", alarmData.isSnooze)
            putExtra("snooze_enabled", alarmData.snoozeEnabled)
            putExtra("snooze_minutes", alarmData.snoozeMinutes)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    alarmData.id,
                    ringIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent.send()
            } else {
                startActivity(ringIntent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch RingActivity", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        currentAlarm = null
        stopRingtone()
        stopVibration()
    }

    /**
     * 停止当前响铃
     */
    fun stopCurrentAlarm() {
        Log.d(TAG, "Stopping alarm ${currentAlarm?.id}")
        currentAlarm = null
        stopRingtone()
        stopVibration()
        stopSelf()
    }

    /**
     * 获取当前正在响铃的闹钟
     */
    fun getCurrentAlarm(): AlarmData? = currentAlarm

    /**
     * Create notification channel
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /**
     * Create foreground notification
     */
    private fun createNotification(alarmData: AlarmData): Notification {
        val intent = Intent(this, RingActivity::class.java).apply {
            putExtra("alarm_id", alarmData.id)
            putExtra("alarm_title", alarmData.title)
            putExtra("alarm_uuid", alarmData.uuid)
            putExtra("snooze_enabled", alarmData.snoozeEnabled)
            putExtra("snooze_minutes", alarmData.snoozeMinutes)
            setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            alarmData.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(alarmData.title)
            .setContentText(getString(R.string.notification_alarm_ringing))
            .setSmallIcon(R.drawable.ic_alarm)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    /**
     * Play ringtone
     */
    private fun playRingtone() {
        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AlarmService, ringtoneUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Stop ringtone
     */
    private fun stopRingtone() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    /**
     * Start vibration
     */
    private fun startVibration() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            val vibrationEffect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
            vibratorManager?.vibrate(CombinedVibration.createParallel(vibrationEffect))
        } else {
            @Suppress("DEPRECATION")
            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val vibrationEffect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
                vibrator?.vibrate(vibrationEffect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(VIBRATION_PATTERN, 0)
            }
        }
    }

    /**
     * Stop vibration
     */
    private fun stopVibration() {
        vibrator?.cancel()
        vibrator = null
    }

    companion object {
        private const val TAG = "AlarmService"
        private const val CHANNEL_ID = "alarm_channel"
        private const val NOTIFICATION_ID = 1001
        private val VIBRATION_PATTERN = longArrayOf(0, 500, 500, 500, 500, 500)

        @Volatile
        private var instance: AlarmService? = null

        fun getInstance(): AlarmService? = instance

        /**
         * 停止当前闹钟
         */
        fun stopCurrentAlarm() {
            instance?.stopCurrentAlarm()
        }
    }
}

/**
 * 闹钟数据类
 */
data class AlarmData(
    val id: Int,
    val title: String,
    val uuid: String?,
    val isSnooze: Boolean,
    val snoozeEnabled: Boolean,
    val snoozeMinutes: Int,
    val vibrate: Boolean
)
