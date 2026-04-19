package com.example.clockapp.ui.main

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.clockapp.R
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.data.model.Alarm
import com.example.clockapp.data.repository.CalendarRepository
import com.example.clockapp.service.AlarmScheduler
import com.example.clockapp.service.AlarmService
import com.example.clockapp.service.DecemberReminderManager
import com.example.clockapp.ui.alarm.AlarmDetailActivity
import com.example.clockapp.ui.ring.RingActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Main activity - alarm list
 */
class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AlarmAdapter
    private lateinit var emptyView: LinearLayout
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var fabMenu: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 优先检查是否有正在响铃的闹钟，确保用户能关闭闹钟
        if (checkAndShowRingingAlarm()) {
            return
        }

        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]

        checkExactAlarmPermission()
        initViews()
        observeData()

        // 处理从12月提醒通知点击进入的情况
        handleDecemberReminderIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleDecemberReminderIntent(it) }
    }

    /**
     * 处理从12月提醒通知点击进入的意图
     */
    private fun handleDecemberReminderIntent(intent: Intent) {
        if (intent.getBooleanExtra(DecemberReminderManager.EXTRA_FROM_REMINDER, false)) {
            // 取消通知
            DecemberReminderManager.getInstance(this).cancelReminder()
            // 标记该阶段提醒已显示
            CalendarRepository.getInstance(this).markDecemberReminderShown()
            // 打开新建指定日期闹钟页面
            createAlarm(isWorkdayAlarm = false)
        }
    }

    /**
     * 检查是否有正在响铃的闹钟，如果有则跳转到响铃页面
     * @return true 表示有闹钟正在响铃并已跳转
     */
    private fun checkAndShowRingingAlarm(): Boolean {
        val currentAlarm = AlarmService.getInstance()?.getCurrentAlarm()
        if (currentAlarm != null) {
            // 有闹钟正在响铃，跳转到响铃页面
            val intent = Intent(this, RingActivity::class.java).apply {
                putExtra(RingActivity.EXTRA_ALARM_ID, currentAlarm.id)
                putExtra(RingActivity.EXTRA_ALARM_TITLE, currentAlarm.title)
                putExtra(RingActivity.EXTRA_ALARM_UUID, currentAlarm.uuid)
                putExtra(RingActivity.EXTRA_IS_SNOOZE, currentAlarm.isSnooze)
                putExtra(RingActivity.EXTRA_SNOOZE_ENABLED, currentAlarm.snoozeEnabled)
                putExtra(RingActivity.EXTRA_SNOOZE_MINUTES, currentAlarm.snoozeMinutes)
                putExtra("queue_info", "")
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
            finish() // 关闭 MainActivity
            return true
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        // 只检查精确闹钟权限，不重复检查电池优化（避免重复弹窗）
        checkExactAlarmPermission(checkBatteryOpt = false)
        // 检查12月跨年提醒
        checkDecemberReminder()
    }

    /**
     * 检查12月跨年提醒
     */
    private fun checkDecemberReminder() {
        val repository = CalendarRepository.getInstance(this)
        val (phase, shouldShow, message) = repository.getDecemberReminderInfo()
        if (shouldShow && phase != CalendarRepository.DecemberPhase.NONE) {
            showDecemberReminderDialog(message, repository)
        }
    }

    /**
     * 显示12月跨年提醒对话框
     */
    private fun showDecemberReminderDialog(message: String, repository: CalendarRepository) {
        MaterialAlertDialogBuilder(this)
            .setTitle("设置下一年闹钟提醒")
            .setMessage(message)
            .setPositiveButton("去设置") { _, _ ->
                // 标记已显示
                repository.markDecemberReminderShown()
                // 打开新建指定日期闹钟页面
                createAlarm(isWorkdayAlarm = false)
            }
            .setNegativeButton("稍后提醒") { _, _ ->
                // 不标记，下次还会继续提示
            }
            .setNeutralButton("不再提醒") { _, _ ->
                // 禁用今年所有12月提醒
                repository.disableDecemberReminderForYear()
            }
            .setCancelable(true)
            .show()
    }

    companion object {
        private const val PREFS_NAME = "app_prefs"
        private const val KEY_BATTERY_OPTIMIZATION_SHOWN = "battery_optimization_shown"
    }

    /**
     * Check and request exact alarm permission for Android 12+
     * Optionally check battery optimization
     * @param checkBatteryOpt whether to check battery optimization after permission check
     */
    private fun checkExactAlarmPermission(checkBatteryOpt: Boolean = true) {
        // 1. 检查精确闹钟权限 (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val canSchedule = AlarmScheduler.canScheduleExactAlarms(this)
            android.util.Log.d("MainActivity", "Can schedule exact alarms: $canSchedule")
            if (!canSchedule) {
                showExactAlarmPermissionDialog()
                return
            }
        }

        // 2. 检查电池优化状态（仅在需要时）
        if (checkBatteryOpt) {
            checkBatteryOptimization()
        }
    }

    /**
     * 显示精确闹钟权限请求对话框
     */
    private fun showExactAlarmPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("需要权限")
            .setMessage("为了确保闹钟能在应用关闭后正常响铃，需要您手动授予精确闹钟权限。")
            .setPositiveButton("去设置") { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .setCancelable(false)
            .show()
    }

    /**
     * 检查电池优化状态并引导用户设置
     */
    private fun checkBatteryOptimization() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hintShown = prefs.getBoolean(KEY_BATTERY_OPTIMIZATION_SHOWN, false)

        // 如果已经提示过，不再重复提示
        if (hintShown) return

        // 检查是否在电池优化白名单中
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            powerManager.isIgnoringBatteryOptimizations(packageName)
        } else {
            true // Android 6.0 以下默认不优化
        }

        if (!isIgnoringBatteryOptimizations) {
            showBatteryOptimizationDialog(prefs)
        }
    }

    /**
     * 显示电池优化引导对话框
     */
    private fun showBatteryOptimizationDialog(prefs: android.content.SharedPreferences) {
        val message = buildString {
            appendLine("为了确保闹钟能在应用关闭后正常响铃，请允许后台活动。")
            appendLine()
            appendLine("设置路径：")
            appendLine("• 华为/荣耀：设置 > 电池 > 耗电管理 > 启动管理")
            appendLine("• 小米/红米：设置 > 省电与电池 > 应用智能省电")
            appendLine("• OPPO/一加：设置 > 电池 > 应用耗电管理")
            appendLine("• vivo/iQOO：设置 > 电池 > 后台耗电管理")
            appendLine("• 其他：设置 > 应用 > 电池优化")
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("需要允许后台活动")
            .setMessage(message)
            .setPositiveButton("去设置") { _, _ ->
                // 点击去设置后立即关闭提醒，下次不再显示
                prefs.edit().putBoolean(KEY_BATTERY_OPTIMIZATION_SHOWN, true).apply()
                openBatteryOptimizationSettings()
            }
            .setNegativeButton("稍后提醒") { _, _ ->
                // 不标记为已显示，下次还会继续提示
            }
            .setCancelable(true)
            .show()
    }

    /**
     * 打开电池优化设置页面
     */
    private fun openBatteryOptimizationSettings() {
        // 尝试打开电池设置页面（各品牌手机路径不同）
        val powerUsageIntent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
        if (powerUsageIntent.resolveActivity(packageManager) != null) {
            startActivity(powerUsageIntent)
        } else {
            // 如果打不开，打开应用详情页
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
        }
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.emptyView)
        fabAdd = findViewById(R.id.fabAdd)
        fabMenu = findViewById(R.id.fabMenu)

        // Setup RecyclerView
        adapter = AlarmAdapter(
            onItemClick = { alarm -> editAlarm(alarm) },
            onToggleChanged = { alarm, isEnabled ->
                viewModel.updateAlarmState(alarm, isEnabled)
            },
            onDeleteClick = { alarm -> showDeleteConfirm(alarm) },
            onCopyClick = { alarm -> copyAlarm(alarm) }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Add button
        fabAdd.setOnClickListener {
            showAddOptions()
        }

        // Menu button - show prefetch dialog
        fabMenu.setOnClickListener {
            showMenuDialog()
        }
    }

    /**
     * Show menu dialog with prefetch option
     */
    private fun showMenuDialog() {
        val repository = CalendarRepository.getInstance(this)
        val nextYear = repository.getNextYearForPrefetch()
        val hasData = repository.hasYearData(nextYear)
        val isDecember = repository.isPrefetchPeriod()

        val statusMessage = when {
            hasData -> getString(R.string.prefetch_status_prefetched, nextYear)
            isDecember -> getString(R.string.prefetch_status_available, nextYear)
            else -> getString(R.string.prefetch_status_not_published, nextYear)
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_prefetch_menu, null)
        val tvStatus = dialogView.findViewById<android.widget.TextView>(R.id.tvPrefetchMenuStatus)
        val btnPrefetch = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnPrefetch)

        tvStatus.text = statusMessage

        // Configure button state
        when {
            hasData -> {
                btnPrefetch.text = "已预取"
                btnPrefetch.isEnabled = false
            }
            !isDecember -> {
                btnPrefetch.text = "12月开放"
                btnPrefetch.isEnabled = false
            }
            else -> {
                btnPrefetch.text = "立即预取"
                btnPrefetch.isEnabled = true
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("预取下一年工作日数据")
            .setView(dialogView)
            .setPositiveButton("关闭", null)
            .create()

        btnPrefetch.setOnClickListener {
            dialog.dismiss()
            manualPrefetchNextYearData()
        }

        dialog.show()
    }

    /**
     * Manually prefetch next year's calendar data
     */
    private fun manualPrefetchNextYearData() {
        lifecycleScope.launch {
            try {
                val repository = CalendarRepository.getInstance(this@MainActivity)
                val success = repository.prefetchNextYearData()

                if (success) {
                    val nextYear = repository.getNextYearForPrefetch()
                    Toast.makeText(this@MainActivity, getString(R.string.prefetch_success), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "数据暂未发布，请稍后再试", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to prefetch data", e)
                Toast.makeText(this@MainActivity, getString(R.string.prefetch_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeData() {
        viewModel.alarms.observe(this) { alarms ->
            adapter.submitList(alarms)
            updateEmptyView(alarms.isEmpty())
        }

        // Load calendar data for workday alarm display
        lifecycleScope.launch {
            try {
                val repository = CalendarRepository.getInstance(this@MainActivity)
                val year = java.time.LocalDate.now().year
                val calendarData = repository.getCalendarData(year)
                adapter.updateCalendarData(calendarData)
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Failed to load calendar data", e)
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        if (isEmpty) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }

    /**
     * Show add options
     */
    private fun showAddOptions() {
        val bottomSheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_add_alarm, null)

        view.findViewById<MaterialButton>(R.id.btnSpecificDate).setOnClickListener {
            bottomSheet.dismiss()
            createAlarm(isWorkdayAlarm = false)
        }

        view.findViewById<MaterialButton>(R.id.btnWorkday).setOnClickListener {
            bottomSheet.dismiss()
            createAlarm(isWorkdayAlarm = true)
        }

        view.findViewById<MaterialButton>(R.id.btnRegular).setOnClickListener {
            bottomSheet.dismiss()
            createRegularAlarm()
        }

        bottomSheet.setContentView(view)
        bottomSheet.show()
    }

    /**
     * Create new alarm
     */
    private fun createAlarm(isWorkdayAlarm: Boolean) {
        val intent = Intent(this, AlarmDetailActivity::class.java).apply {
            putExtra(AlarmDetailActivity.EXTRA_IS_WORKDAY_ALARM, isWorkdayAlarm)
        }
        startActivity(intent)
    }

    /**
     * Create new regular alarm (weekly repeat)
     */
    private fun createRegularAlarm() {
        val intent = Intent(this, AlarmDetailActivity::class.java).apply {
            putExtra(AlarmDetailActivity.EXTRA_IS_REGULAR_ALARM, true)
        }
        startActivity(intent)
    }

    /**
     * Edit alarm
     */
    private fun editAlarm(alarm: Alarm) {
        val intent = Intent(this, AlarmDetailActivity::class.java).apply {
            putExtra(AlarmDetailActivity.EXTRA_ALARM_ID, alarm.id)
        }
        startActivity(intent)
    }

    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirm(alarm: Alarm) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除闹钟")
            .setMessage("确定要删除\"${alarm.title}\"吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteAlarm(alarm)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * Copy alarm
     */
    private fun copyAlarm(alarm: Alarm) {
        lifecycleScope.launch {
            val newAlarm = alarm.copy(
                id = UUID.randomUUID().toString(),
                title = alarm.title + " (复制)",
                isEnabled = false
            )

            AlarmDatabase.getDatabase(this@MainActivity)
                .alarmDao()
                .insertAlarm(newAlarm)

            Toast.makeText(this@MainActivity, getString(R.string.msg_alarm_copied), Toast.LENGTH_SHORT).show()
        }
    }
}
