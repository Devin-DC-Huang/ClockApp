package com.example.clockapp.ui.ring

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.clockapp.R
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.service.AlarmScheduler
import com.example.clockapp.service.AlarmService
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 闹钟响铃页面
 * 全屏显示，提供停止和贪睡功能
 * 支持队列式响铃：处理完当前闹钟后自动响下一个
 */
class RingActivity : AppCompatActivity() {

    private lateinit var tvTime: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvDate: TextView
    private lateinit var btnStop: Button
    private lateinit var btnSnooze: Button
    private lateinit var ripple1: View
    private lateinit var ripple2: View
    private lateinit var ripple3: View
    private lateinit var ivAlarmIcon: android.widget.ImageView

    private var alarmId: Int = -1
    private var alarmTitle: String = "闹钟"
    private var alarmUuid: String? = null
    private var isSnooze: Boolean = false
    private var snoozeEnabled: Boolean = true
    private var snoozeMinutes: Int = 5

    private val timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    private val dateFormat = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 设置全屏和保持屏幕亮起
        setupWindowFlags()
        
        setContentView(R.layout.activity_ring)

        // 拦截返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 禁止返回键，必须通过停止或贪睡按钮关闭
            }
        })

        // 获取传入参数
        alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
        alarmTitle = intent.getStringExtra(EXTRA_ALARM_TITLE) ?: "闹钟"
        alarmUuid = intent.getStringExtra(EXTRA_ALARM_UUID)
        isSnooze = intent.getBooleanExtra(EXTRA_IS_SNOOZE, false)
        snoozeEnabled = intent.getBooleanExtra(EXTRA_SNOOZE_ENABLED, true)
        snoozeMinutes = intent.getIntExtra(EXTRA_SNOOZE_MINUTES, 5)

        initViews()
        updateTime()
        startRippleAnimation()
        startAlarmIconAnimation()
    }

    /**
     * Start alarm icon shaking animation
     */
    private fun startAlarmIconAnimation() {
        val shakeAnimator = ObjectAnimator.ofFloat(ivAlarmIcon, View.ROTATION, -15f, 15f).apply {
            duration = 200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        shakeAnimator.start()
    }

    /**
     * Start ripple animation for alarm icon
     */
    private fun startRippleAnimation() {
        // Animate ripple1 - largest, slowest
        val animator1 = createRippleAnimator(ripple1, 0f, 1f, 2000, 0)
        
        // Animate ripple2 - medium
        val animator2 = createRippleAnimator(ripple2, 0f, 1f, 2000, 400)
        
        // Animate ripple3 - smallest, fastest
        val animator3 = createRippleAnimator(ripple3, 0f, 1f, 2000, 800)
        
        animator1.start()
        animator2.start()
        animator3.start()
    }

    private fun createRippleAnimator(view: View, startScale: Float, endScale: Float, duration: Long, delay: Long): AnimatorSet {
        // Scale animation
        val scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, startScale, endScale).apply {
            this.duration = duration
            startDelay = delay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        val scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, startScale, endScale).apply {
            this.duration = duration
            startDelay = delay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        
        // Alpha animation
        val alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 0.6f, 0f).apply {
            this.duration = duration
            startDelay = delay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
        }
        
        val animatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        return animatorSet
    }

    private fun setupWindowFlags() {
        // 保持屏幕亮起
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 显示在锁屏之上并尝试绕过键盘锁
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            
            // Android 8.1+ 尝试禁用键盘锁以允许免密操作
            try {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                if (keyguardManager.isKeyguardLocked) {
                    // 请求显示在键盘锁之上，允许用户无需解锁即可操作
                    keyguardManager.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                        override fun onDismissError() {
                            // 某些设备可能不支持，界面仍会显示
                        }
                        override fun onDismissSucceeded() {
                            // 成功，用户可以直接操作
                        }
                        override fun onDismissCancelled() {
                            // 用户取消或设备不支持
                        }
                    })
                }
            } catch (e: Exception) {
                // 某些设备可能不支持此方法
            }
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        
        // 确保窗口可以接收输入事件（即使锁屏）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.addFlags(WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON)
        }
    }

    private fun initViews() {
        tvTime = findViewById(R.id.tvTime)
        tvTitle = findViewById(R.id.tvTitle)
        tvDate = findViewById(R.id.tvDate)
        btnStop = findViewById(R.id.btnStop)
        btnSnooze = findViewById(R.id.btnSnooze)
        ripple1 = findViewById(R.id.ripple1)
        ripple2 = findViewById(R.id.ripple2)
        ripple3 = findViewById(R.id.ripple3)
        ivAlarmIcon = findViewById(R.id.ivAlarmIcon)

        tvTitle.text = alarmTitle
        tvDate.text = LocalDateTime.now().format(dateFormat)

        // Show/hide snooze button based on alarm settings
        if (!snoozeEnabled) {
            btnSnooze.visibility = android.view.View.GONE
        } else {
            // Update snooze button text with custom minutes
            btnSnooze.text = "贪睡 (${snoozeMinutes}分钟)"
            btnSnooze.setOnClickListener {
                snoozeAlarm()
            }
        }

        btnStop.setOnClickListener {
            stopCurrentAlarm()
        }
    }

    private fun updateTime() {
        tvTime.text = LocalDateTime.now().format(timeFormat)
        // Update time every second
        tvTime.postDelayed({ updateTime() }, 1000)
    }

    /**
     * 停止当前闹钟，继续响下一个
     */
    private fun stopCurrentAlarm() {
        // 如果不是贪睡闹钟，处理闹钟状态
        if (!isSnooze && alarmUuid != null) {
            lifecycleScope.launch {
                val alarm = AlarmDatabase.getDatabase(this@RingActivity)
                    .alarmDao()
                    .getAlarmById(alarmUuid!!)

                alarm?.let {
                    // 普通闹钟且永不重复（单次闹钟），关闭开关
                    if (it.isRegularAlarm && it.repeatDays.isEmpty()) {
                        disableOneTimeAlarm(it)
                    } else {
                        // 其他情况重新调度
                        AlarmScheduler.scheduleAlarm(this@RingActivity, it)
                    }
                }
                // 通知服务处理队列中的下一个闹钟
                AlarmService.stopCurrentAlarm()
                finish()
            }
            return
        }

        // 通知服务处理队列中的下一个闹钟
        AlarmService.stopCurrentAlarm()
        finish()
    }


    /**
     * 禁用单次闹钟
     */
    private suspend fun disableOneTimeAlarm(alarm: com.example.clockapp.data.model.Alarm) {
        alarm.isEnabled = false
        AlarmDatabase.getDatabase(this@RingActivity)
            .alarmDao()
            .insertAlarm(alarm)
    }

    /**
     * 贪睡功能 - 根据设置的贪睡时间后再次响铃
     */
    private fun snoozeAlarm() {
        // 设置贪睡闹钟，使用自定义贪睡时间
        AlarmScheduler.scheduleSnoozeAlarm(this, alarmId, alarmTitle, snoozeMinutes)

        // 通知服务处理队列中的下一个闹钟
        AlarmService.stopCurrentAlarm()

        finish()
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_ALARM_TITLE = "alarm_title"
        const val EXTRA_ALARM_UUID = "alarm_uuid"
        const val EXTRA_IS_SNOOZE = "is_snooze"
        const val EXTRA_SNOOZE_ENABLED = "snooze_enabled"
        const val EXTRA_SNOOZE_MINUTES = "snooze_minutes"
    }
}
