package com.example.clockapp.ui.alarm

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.clockapp.R
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.data.model.Alarm
import com.example.clockapp.data.model.CalendarData
import com.example.clockapp.data.model.SpecialAlarmMode
import com.example.clockapp.data.repository.CalendarRepository
import com.example.clockapp.service.AlarmScheduler
import com.google.android.material.appbar.MaterialToolbar
import androidx.appcompat.widget.SwitchCompat
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID

/**
 * Alarm detail page - create/edit alarm
 */
class AlarmDetailActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var etHour: EditText
    private lateinit var etMinute: EditText
    private lateinit var etTitle: EditText
    private lateinit var layoutDates: LinearLayout
    private lateinit var tvDateCount: TextView
    private lateinit var layoutViewCalendar: LinearLayout
    private lateinit var layoutRepeatDays: LinearLayout
    private lateinit var tvRepeatDays: TextView
    private lateinit var switchEnabled: SwitchCompat
    private lateinit var switchVibrate: SwitchCompat
    
    // Special alarm mode selection
    private lateinit var layoutSpecialAlarmMode: LinearLayout
    private lateinit var layoutAllWorkdaysOption: LinearLayout
    private lateinit var layoutFirstWorkdayOption: LinearLayout
    private lateinit var layoutAllHolidaysOption: LinearLayout
    private lateinit var rbAllWorkdaysMode: android.widget.RadioButton
    private lateinit var rbFirstWorkdayMode: android.widget.RadioButton
    private lateinit var rbAllHolidaysMode: android.widget.RadioButton

    // Quick date selection
    private lateinit var layoutQuickSelectHeader: LinearLayout
    private lateinit var layoutQuickSelectContent: LinearLayout
    private lateinit var ivExpandIcon: android.widget.ImageView
    private lateinit var spinnerYear: Spinner
    private lateinit var chipGroupWeekDays: ChipGroup
    private lateinit var chipMonday: Chip
    private lateinit var chipTuesday: Chip
    private lateinit var chipWednesday: Chip
    private lateinit var chipThursday: Chip
    private lateinit var chipFriday: Chip
    private lateinit var chipSaturday: Chip
    private lateinit var chipSunday: Chip
    private lateinit var cbSkipHolidays: CheckBox
    private lateinit var tvSkipHolidaysHint: TextView
    private lateinit var btnApplyQuickSelect: MaterialButton
    private var selectedYear: Int = LocalDate.now().year
    private var availableYears: List<Int> = emptyList()
    private var yearToCalendarData: MutableMap<Int, CalendarData?> = mutableMapOf()
    private var isQuickSelectExpanded: Boolean = false

    private var alarmId: String? = null
    private var isSpecialAlarm: Boolean = false
    private var isRegularAlarm: Boolean = false
    private var selectedDates: MutableList<LocalDate> = mutableListOf()
    private var repeatDays: MutableList<Int> = mutableListOf()
    private var specialAlarmMode: SpecialAlarmMode = SpecialAlarmMode.ALL_WORKDAYS

    private var existingAlarm: Alarm? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_detail)

        // Get parameters
        alarmId = intent.getStringExtra(EXTRA_ALARM_ID)
        isSpecialAlarm = intent.getBooleanExtra(EXTRA_IS_SPECIAL_ALARM, false)
        isRegularAlarm = intent.getBooleanExtra(EXTRA_IS_REGULAR_ALARM, false)

        initViews()
        setupToolbar()

        if (alarmId != null) {
            loadAlarmData(alarmId!!)
        } else {
            // New alarm - set default title based on alarm type
            etTitle.setText(getDefaultAlarmTitle())
        }
    }

    /**
     * Get default alarm title based on alarm type and special alarm mode
     */
    private fun getDefaultAlarmTitle(): String {
        return when {
            isSpecialAlarm -> getSpecialAlarmModeTitle()
            isRegularAlarm -> getString(R.string.alarm_type_regular)
            else -> getString(R.string.alarm_type_specific)
        }
    }

    /**
     * Get title for special alarm based on current mode
     */
    private fun getSpecialAlarmModeTitle(): String {
        return when (specialAlarmMode) {
            SpecialAlarmMode.ALL_WORKDAYS -> getString(R.string.alarm_name_all_workdays)
            SpecialAlarmMode.FIRST_WORKDAY_ONLY -> getString(R.string.alarm_name_first_workday)
            SpecialAlarmMode.ALL_HOLIDAYS -> getString(R.string.alarm_name_all_holidays)
        }
    }

    /**
     * Check if user has edited the title (not using default)
     */
    private fun isUserEditedTitle(): Boolean {
        val currentTitle = etTitle.text.toString().trim()
        // Check against all possible default titles
        val defaultTitles = setOf(
            getString(R.string.alarm_name_all_workdays),
            getString(R.string.alarm_name_first_workday),
            getString(R.string.alarm_name_all_holidays),
            getString(R.string.alarm_type_workday),  // Legacy default
            getString(R.string.alarm_type_regular),
            getString(R.string.alarm_type_specific)
        )
        return currentTitle.isNotEmpty() && currentTitle !in defaultTitles
    }

    /**
     * Update default title if user hasn't edited it
     */
    private fun updateDefaultTitleIfNeeded() {
        if (isSpecialAlarm && !isUserEditedTitle()) {
            etTitle.setText(getSpecialAlarmModeTitle())
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        etHour = findViewById(R.id.etHour)
        etMinute = findViewById(R.id.etMinute)
        etTitle = findViewById(R.id.etTitle)
        layoutDates = findViewById(R.id.layoutDates)
        tvDateCount = findViewById(R.id.tvDateCount)
        layoutViewCalendar = findViewById(R.id.layoutViewCalendar)
        layoutRepeatDays = findViewById(R.id.layoutRepeatDays)
        tvRepeatDays = findViewById(R.id.tvRepeatDays)
        switchEnabled = findViewById(R.id.switchEnabled)
        switchVibrate = findViewById(R.id.switchVibrate)
        
        // Special alarm mode selection
        layoutSpecialAlarmMode = findViewById(R.id.layoutSpecialAlarmMode)
        layoutAllWorkdaysOption = findViewById(R.id.layoutAllWorkdaysOption)
        layoutFirstWorkdayOption = findViewById(R.id.layoutFirstWorkdayOption)
        layoutAllHolidaysOption = findViewById(R.id.layoutAllHolidaysOption)
        rbAllWorkdaysMode = findViewById(R.id.rbAllWorkdaysMode)
        rbFirstWorkdayMode = findViewById(R.id.rbFirstWorkdayMode)
        rbAllHolidaysMode = findViewById(R.id.rbAllHolidaysMode)

        // Quick date selection
        layoutQuickSelectHeader = findViewById(R.id.layoutQuickSelectHeader)
        layoutQuickSelectContent = findViewById(R.id.layoutQuickSelectContent)
        ivExpandIcon = findViewById(R.id.ivExpandIcon)
        spinnerYear = findViewById(R.id.spinnerYear)
        chipGroupWeekDays = findViewById(R.id.chipGroupWeekDays)
        chipMonday = findViewById(R.id.chipMonday)
        chipTuesday = findViewById(R.id.chipTuesday)
        chipWednesday = findViewById(R.id.chipWednesday)
        chipThursday = findViewById(R.id.chipThursday)
        chipFriday = findViewById(R.id.chipFriday)
        chipSaturday = findViewById(R.id.chipSaturday)
        chipSunday = findViewById(R.id.chipSunday)
        cbSkipHolidays = findViewById(R.id.cbSkipHolidays)
        tvSkipHolidaysHint = findViewById(R.id.tvSkipHolidaysHint)
        btnApplyQuickSelect = findViewById(R.id.btnApplyQuickSelect)

        // Time input listeners with validation
        setupTimeInput(etHour, 0, 23) { hour ->
            etHour.setText(hour.toString().padStart(2, '0'))
            etMinute.requestFocus()
        }
        setupTimeInput(etMinute, 0, 59) { minute ->
            etMinute.setText(minute.toString().padStart(2, '0'))
        }

        // Remove purple highlight, show cursor only when selected
        etHour.highlightColor = android.graphics.Color.TRANSPARENT
        etMinute.highlightColor = android.graphics.Color.TRANSPARENT

        // Date selection
        findViewById<LinearLayout>(R.id.layoutSelectDates).setOnClickListener {
            if (!isSpecialAlarm) {
                showDatePicker()
            }
        }

        // Hide date selection for workday alarms and regular alarms
        if (isSpecialAlarm) {
            layoutDates.visibility = LinearLayout.GONE
            layoutRepeatDays.visibility = LinearLayout.GONE
            // Setup Special alarm mode selection
            setupSpecialAlarmModeSelection()
            // Calendar view button is only shown when editing existing alarm
            layoutViewCalendar.visibility = if (alarmId != null) LinearLayout.VISIBLE else LinearLayout.GONE
        } else if (isRegularAlarm) {
            // Regular alarm: hide date selection, show repeat days
            layoutDates.visibility = LinearLayout.GONE
            layoutViewCalendar.visibility = LinearLayout.GONE
            layoutRepeatDays.visibility = LinearLayout.VISIBLE
            layoutSpecialAlarmMode.visibility = LinearLayout.GONE
            layoutRepeatDays.setOnClickListener {
                showRepeatDaysPicker()
            }
        }

        switchEnabled.isChecked = true
        switchVibrate.isChecked = true

        // Setup quick date selection (only for specific date alarm)
        if (!isSpecialAlarm && !isRegularAlarm) {
            setupQuickDateSelection()
        }
    }

    /**
     * Setup quick date selection UI and logic
     */
    private fun setupQuickDateSelection() {
        // Setup expand/collapse
        layoutQuickSelectHeader.setOnClickListener {
            toggleQuickSelectExpansion()
        }

        // Initialize year spinner
        val currentYear = LocalDate.now().year
        availableYears = (currentYear..currentYear + 10).toList()
        val yearAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, availableYears)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerYear.adapter = yearAdapter
        spinnerYear.setSelection(0)

        spinnerYear.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedYear = availableYears[position]
                checkCalendarDataForYear(selectedYear)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Apply button click
        btnApplyQuickSelect.setOnClickListener {
            applyQuickDateSelection()
        }

        // Load calendar data for available years
        loadCalendarDataForYears()
    }

    /**
     * Toggle quick select section expansion
     */
    private fun toggleQuickSelectExpansion() {
        isQuickSelectExpanded = !isQuickSelectExpanded
        if (isQuickSelectExpanded) {
            layoutQuickSelectContent.visibility = View.VISIBLE
            ivExpandIcon.rotation = 90f
        } else {
            layoutQuickSelectContent.visibility = View.GONE
            ivExpandIcon.rotation = 0f
        }
    }

    /**
     * Load calendar data for all available years
     */
    private fun loadCalendarDataForYears() {
        lifecycleScope.launch {
            try {
                val repository = CalendarRepository.getInstance(this@AlarmDetailActivity)
                availableYears.forEach { year ->
                    try {
                        // Use hasYearData to check if valid data exists before fetching
                        if (repository.hasYearData(year)) {
                            val data = repository.getCalendarData(year)
                            yearToCalendarData[year] = data
                        } else {
                            yearToCalendarData[year] = null
                        }
                    } catch (e: Exception) {
                        yearToCalendarData[year] = null
                    }
                }
                // Update UI for current selection
                checkCalendarDataForYear(selectedYear)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load calendar data", e)
            }
        }
    }

    /**
     * Check if valid calendar data exists for the selected year and update UI
     * Data is considered invalid if it's null or contains no holidays and no workdays
     */
    private fun checkCalendarDataForYear(year: Int) {
        val calendarData = yearToCalendarData[year]
        // Data is valid only if it exists and has at least some holidays or workdays
        val hasValidData = calendarData != null && 
            (calendarData.holidays.isNotEmpty() || calendarData.workdays.isNotEmpty())
        
        cbSkipHolidays.isEnabled = hasValidData
        if (!hasValidData) {
            cbSkipHolidays.isChecked = false
            tvSkipHolidaysHint.visibility = View.VISIBLE
        } else {
            tvSkipHolidaysHint.visibility = View.GONE
        }
    }

    /**
     * Get selected week days from chips
     */
    private fun getSelectedWeekDays(): List<Int> {
        val selectedDays = mutableListOf<Int>()
        if (chipMonday.isChecked) selectedDays.add(1)
        if (chipTuesday.isChecked) selectedDays.add(2)
        if (chipWednesday.isChecked) selectedDays.add(3)
        if (chipThursday.isChecked) selectedDays.add(4)
        if (chipFriday.isChecked) selectedDays.add(5)
        if (chipSaturday.isChecked) selectedDays.add(6)
        if (chipSunday.isChecked) selectedDays.add(7)
        return selectedDays
    }

    /**
     * Apply quick date selection
     * Clears existing dates and generates new ones based on selection
     */
    private fun applyQuickDateSelection() {
        val weekDays = getSelectedWeekDays()
        if (weekDays.isEmpty()) {
            Toast.makeText(this, "请至少选择一个星期", Toast.LENGTH_SHORT).show()
            return
        }

        // Generate dates for selected year and week days
        val generatedDates = mutableListOf<LocalDate>()
        val startDate = LocalDate.of(selectedYear, 1, 1)
        val endDate = LocalDate.of(selectedYear, 12, 31)

        var date = startDate
        while (!date.isAfter(endDate)) {
            val dayOfWeek = date.dayOfWeek.value // 1=Monday, 7=Sunday
            if (dayOfWeek in weekDays) {
                generatedDates.add(date)
            }
            date = date.plusDays(1)
        }

        // Skip holidays if checked and data available
        val calendarData = yearToCalendarData[selectedYear]
        val finalDates = if (cbSkipHolidays.isChecked && calendarData != null) {
            val holidays = calendarData.holidays.toSet()
            generatedDates.filter { it !in holidays }
        } else {
            generatedDates
        }

        // Clear existing dates and set new ones
        selectedDates.clear()
        selectedDates.addAll(finalDates)
        selectedDates.sort()

        updateDateDisplay()

        val skipInfo = if (cbSkipHolidays.isChecked && calendarData != null) "（已跳过节假日）" else ""
        Toast.makeText(this, "已生成 ${finalDates.size} 个日期$skipInfo", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Setup Special alarm mode selection
     * In create mode: show three selectable options
     * In edit mode: show only current mode as read-only text
     */
    private fun setupSpecialAlarmModeSelection() {
        // Mode selection is only enabled in create mode (alarmId == null)
        val isCreateMode = alarmId == null

        if (isCreateMode) {
            // Create mode: show mode selection layout
            layoutSpecialAlarmMode.visibility = LinearLayout.VISIBLE
            updateSpecialAlarmModeSelection()

            // Workday option click (ALL_WORKDAYS: All workdays)
            layoutAllWorkdaysOption.setOnClickListener {
                specialAlarmMode = SpecialAlarmMode.ALL_WORKDAYS
                updateSpecialAlarmModeSelection()
                updateDefaultTitleIfNeeded()
            }

            // First workday option click (FIRST_WORKDAY_ONLY)
            layoutFirstWorkdayOption.setOnClickListener {
                specialAlarmMode = SpecialAlarmMode.FIRST_WORKDAY_ONLY
                updateSpecialAlarmModeSelection()
                updateDefaultTitleIfNeeded()
            }

            // All holidays option click (ALL_HOLIDAYS)
            layoutAllHolidaysOption.setOnClickListener {
                specialAlarmMode = SpecialAlarmMode.ALL_HOLIDAYS
                updateSpecialAlarmModeSelection()
                updateDefaultTitleIfNeeded()
            }

            // Radio button change listeners
            rbAllWorkdaysMode.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    specialAlarmMode = SpecialAlarmMode.ALL_WORKDAYS
                    updateSpecialAlarmModeSelection()
                    updateDefaultTitleIfNeeded()
                }
            }

            rbFirstWorkdayMode.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    specialAlarmMode = SpecialAlarmMode.FIRST_WORKDAY_ONLY
                    updateSpecialAlarmModeSelection()
                    updateDefaultTitleIfNeeded()
                }
            }

            rbAllHolidaysMode.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    specialAlarmMode = SpecialAlarmMode.ALL_HOLIDAYS
                    updateSpecialAlarmModeSelection()
                    updateDefaultTitleIfNeeded()
                }
            }
        } else {
            // Edit mode: show only the selected mode (not all three options)
            layoutSpecialAlarmMode.visibility = LinearLayout.VISIBLE
            showSelectedModeOnly()
        }
    }

    /**
     * In edit mode, show only the selected mode without radio buttons
     */
    private fun showSelectedModeOnly() {
        // Hide all radio buttons
        rbAllWorkdaysMode.visibility = View.GONE
        rbFirstWorkdayMode.visibility = View.GONE
        rbAllHolidaysMode.visibility = View.GONE

        // Hide unselected options, show only selected one
        when (specialAlarmMode) {
            SpecialAlarmMode.ALL_WORKDAYS -> {
                layoutFirstWorkdayOption.visibility = View.GONE
                layoutAllHolidaysOption.visibility = View.GONE
                layoutAllWorkdaysOption.visibility = View.VISIBLE
                layoutAllWorkdaysOption.isClickable = false
            }
            SpecialAlarmMode.FIRST_WORKDAY_ONLY -> {
                layoutAllWorkdaysOption.visibility = View.GONE
                layoutAllHolidaysOption.visibility = View.GONE
                layoutFirstWorkdayOption.visibility = View.VISIBLE
                layoutFirstWorkdayOption.isClickable = false
            }
            SpecialAlarmMode.ALL_HOLIDAYS -> {
                layoutAllWorkdaysOption.visibility = View.GONE
                layoutFirstWorkdayOption.visibility = View.GONE
                layoutAllHolidaysOption.visibility = View.VISIBLE
                layoutAllHolidaysOption.isClickable = false
            }
        }
    }

    /**
     * Update radio button states based on current mode
     */
    private fun updateSpecialAlarmModeSelection() {
        rbAllWorkdaysMode.isChecked = specialAlarmMode == SpecialAlarmMode.ALL_WORKDAYS
        rbFirstWorkdayMode.isChecked = specialAlarmMode == SpecialAlarmMode.FIRST_WORKDAY_ONLY
        rbAllHolidaysMode.isChecked = specialAlarmMode == SpecialAlarmMode.ALL_HOLIDAYS
    }

    /**
     * Show repeat days picker dialog
     */
    private fun showRepeatDaysPicker() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_repeat_days, null)
        val cbMonday = dialogView.findViewById<android.widget.CheckBox>(R.id.cbMonday)
        val cbTuesday = dialogView.findViewById<android.widget.CheckBox>(R.id.cbTuesday)
        val cbWednesday = dialogView.findViewById<android.widget.CheckBox>(R.id.cbWednesday)
        val cbThursday = dialogView.findViewById<android.widget.CheckBox>(R.id.cbThursday)
        val cbFriday = dialogView.findViewById<android.widget.CheckBox>(R.id.cbFriday)
        val cbSaturday = dialogView.findViewById<android.widget.CheckBox>(R.id.cbSaturday)
        val cbSunday = dialogView.findViewById<android.widget.CheckBox>(R.id.cbSunday)
        val cbFirstWorkdayOnly = dialogView.findViewById<android.widget.CheckBox>(R.id.cbFirstWorkdayOnly)
        val layoutDaySelection = dialogView.findViewById<LinearLayout>(R.id.layoutDaySelection)

        val checkBoxes = listOf(cbMonday, cbTuesday, cbWednesday, cbThursday, cbFriday, cbSaturday, cbSunday)

        // Set current selection
        repeatDays.forEach { day ->
            if (day in 1..7) {
                checkBoxes[day - 1].isChecked = true
            }
        }
        cbFirstWorkdayOnly.isChecked = specialAlarmMode == SpecialAlarmMode.FIRST_WORKDAY_ONLY
        
        // For regular alarm: hide "first workday only" option, only show day selection
        // For workday alarm (special alarm): only show "first workday only" option
        if (isRegularAlarm) {
            // Regular alarm: show day selection, hide "first workday only"
            cbFirstWorkdayOnly.visibility = View.GONE
            layoutDaySelection.visibility = View.VISIBLE
        } else if (isSpecialAlarm) {
            // Special alarm: only show "first workday only" option, hide day selection
            // Special alarm has two modes:
            // 1. First workday after holiday only (checked)
            // 2. All workdays Mon-Fri, skip holidays (unchecked - default)
            cbFirstWorkdayOnly.visibility = View.VISIBLE
            layoutDaySelection.visibility = View.GONE
            
            // Update text to be more descriptive
            cbFirstWorkdayOnly.text = getString(R.string.first_workday_after_holiday_only)
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.action_confirm)) { _, _ ->
                repeatDays.clear()
                checkBoxes.forEachIndexed { index, checkBox ->
                    if (checkBox.isChecked) {
                        repeatDays.add(index + 1) // 1=Monday, 7=Sunday
                    }
                }
                specialAlarmMode = if (cbFirstWorkdayOnly.isChecked) SpecialAlarmMode.FIRST_WORKDAY_ONLY else SpecialAlarmMode.ALL_WORKDAYS
                updateRepeatDaysDisplay()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    /**
     * Update repeat days display text
     */
    private fun updateRepeatDaysDisplay() {
        tvRepeatDays.text = when {
            // Special alarm modes
            isSpecialAlarm && specialAlarmMode == SpecialAlarmMode.ALL_HOLIDAYS -> "所有节假日（包括周末）"
            isSpecialAlarm && specialAlarmMode == SpecialAlarmMode.FIRST_WORKDAY_ONLY -> getString(R.string.first_workday_after_holiday_only)
            isSpecialAlarm -> "所有工作日（跳过节假日）"
            // Regular alarm: only never or custom days
            repeatDays.isEmpty() -> getString(R.string.repeat_never)
            else -> {
                val dayNames = mapOf(
                    1 to getString(R.string.repeat_day_mon),
                    2 to getString(R.string.repeat_day_tue),
                    3 to getString(R.string.repeat_day_wed),
                    4 to getString(R.string.repeat_day_thu),
                    5 to getString(R.string.repeat_day_fri),
                    6 to getString(R.string.repeat_day_sat),
                    7 to getString(R.string.repeat_day_sun)
                )
                repeatDays.sorted().map { dayNames[it] }.joinToString(", ")
            }
        }
    }

    /**
     * Show holiday calendar dialog
     */
    private fun showHolidayCalendar() {
        lifecycleScope.launch {
            try {
                val repository = CalendarRepository.getInstance(this@AlarmDetailActivity)
                val year = LocalDate.now().year
                val data = repository.getCalendarData(year)

                val dialog = HolidayCalendarDialog(
                    this@AlarmDetailActivity,
                    data.year,
                    if (alarmId == null) specialAlarmMode else null,  // Pass mode for preview in create mode
                    alarmId  // Pass alarm ID to show only this alarm's dates when editing
                )
                dialog.show()
            } catch (e: CalendarRepository.WorkdayDataException) {
                Toast.makeText(this@AlarmDetailActivity, e.message, Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@AlarmDetailActivity, getString(R.string.msg_load_holiday_data_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Setup time input with validation
     */
    private fun setupTimeInput(editText: EditText, min: Int, max: Int, onValid: (Int) -> Unit) {
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                validateTimeInput(editText, min, max, onValid)
            }
        }
    }

    /**
     * Validate time input and format
     */
    private fun validateTimeInput(editText: EditText, min: Int, max: Int, onValid: (Int) -> Unit) {
        val text = editText.text.toString()
        val value = text.toIntOrNull()
        if (value != null) {
            val clamped = value.coerceIn(min, max)
            onValid(clamped)
        } else {
            editText.setText(min.toString().padStart(2, '0'))
        }
    }

    /**
     * Get current hour value
     */
    private fun getHour(): Int {
        return etHour.text.toString().toIntOrNull()?.coerceIn(0, 23) ?: 8
    }

    /**
     * Get current minute value
     */
    private fun getMinute(): Int {
        return etMinute.text.toString().toIntOrNull()?.coerceIn(0, 59) ?: 0
    }

    /**
     * Set hour value
     */
    private fun setHour(hour: Int) {
        etHour.setText(hour.coerceIn(0, 23).toString().padStart(2, '0'))
    }

    /**
     * Set minute value
     */
    private fun setMinute(minute: Int) {
        etMinute.setText(minute.coerceIn(0, 59).toString().padStart(2, '0'))
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (alarmId != null) {
            getString(R.string.title_edit_alarm)
        } else {
            when {
                isSpecialAlarm -> getString(R.string.title_new_workday_alarm)
                isRegularAlarm -> getString(R.string.title_new_regular_alarm)
                else -> getString(R.string.title_new_specific_alarm)
            }
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadAlarmData(id: String) {
        lifecycleScope.launch {
            val alarm = AlarmDatabase.getDatabase(this@AlarmDetailActivity)
                .alarmDao()
                .getAlarmById(id)

            alarm?.let {
                existingAlarm = it
                isSpecialAlarm = it.isSpecialAlarm
                isRegularAlarm = it.isRegularAlarm
                setHour(it.hour)
                setMinute(it.minute)
                selectedDates = it.dates.toMutableList()
                repeatDays = it.repeatDays.toMutableList()
                specialAlarmMode = it.specialAlarmMode

                etTitle.setText(it.title)
                switchEnabled.isChecked = it.isEnabled
                switchVibrate.isChecked = it.vibrate

                updateDateDisplay()
                updateRepeatDaysDisplay()

                when {
                    isSpecialAlarm -> {
                        layoutDates.visibility = LinearLayout.GONE
                        // Show calendar view button for workday alarm when editing
                        layoutViewCalendar.visibility = LinearLayout.VISIBLE
                        layoutViewCalendar.setOnClickListener {
                            showHolidayCalendar()
                        }
                        // Setup workday mode display (read-only in edit mode)
                        setupSpecialAlarmModeSelection()
                    }
                    isRegularAlarm -> {
                        // Regular alarm: hide date selection, show repeat days
                        layoutDates.visibility = LinearLayout.GONE
                        layoutViewCalendar.visibility = LinearLayout.GONE
                        layoutRepeatDays.visibility = LinearLayout.VISIBLE
                        layoutSpecialAlarmMode.visibility = LinearLayout.GONE
                        layoutRepeatDays.setOnClickListener {
                            showRepeatDaysPicker()
                        }
                    }
                }
            }
        }
    }

    private fun showDatePicker() {
        val dialog = DatePickerDialog(this, selectedDates) { dates ->
            selectedDates = dates.toMutableList()
            updateDateDisplay()
        }
        dialog.show()
    }

    private fun updateDateDisplay() {
        tvDateCount.text = "${selectedDates.size} 个日期"
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_alarm_detail, menu)
        // Only show delete and copy buttons in edit mode
        menu?.findItem(R.id.action_delete)?.isVisible = alarmId != null
        menu?.findItem(R.id.action_copy)?.isVisible = alarmId != null
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveAlarm()
                true
            }
            R.id.action_delete -> {
                showDeleteConfirm()
                true
            }
            R.id.action_copy -> {
                copyAlarm()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveAlarm() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            Toast.makeText(this, getString(R.string.msg_enter_alarm_name), Toast.LENGTH_SHORT).show()
            return
        }

        // Validate time inputs before saving
        validateTimeInput(etHour, 0, 23) { hour ->
            etHour.setText(hour.toString().padStart(2, '0'))
        }
        validateTimeInput(etMinute, 0, 59) { minute ->
            etMinute.setText(minute.toString().padStart(2, '0'))
        }

        val hour = getHour()
        val minute = getMinute()

        lifecycleScope.launch {
            try {
                val dates = when {
                    isSpecialAlarm -> {
                        val existing = existingAlarm
                        if (alarmId != null && existing != null) {
                            // Edit mode: mode is read-only in UI, keep all existing dates
                            existing.dates
                        } else {
                            // Create mode: calculate workday dates
                            loadWorkdayDates()
                        }
                    }
                    isRegularAlarm -> {
                        // Regular alarm: no specific dates needed
                        emptyList()
                    }
                    else -> {
                        if (selectedDates.isEmpty()) {
                            Toast.makeText(this@AlarmDetailActivity, getString(R.string.msg_select_at_least_one_date), Toast.LENGTH_SHORT).show()
                            return@launch
                        }
                        selectedDates
                    }
                }

                // For regular alarm editing, auto-enable the alarm
                val isEnabled = if (isRegularAlarm && alarmId != null) {
                    true // Auto-enable when editing regular alarm
                } else {
                    switchEnabled.isChecked
                }

                // If editing existing alarm, cancel the old schedule first
                existingAlarm?.let { oldAlarm ->
                    AlarmScheduler.cancelAlarm(this@AlarmDetailActivity, oldAlarm)
                }

                val alarm = Alarm(
                    id = alarmId ?: UUID.randomUUID().toString(),
                    title = title,
                    hour = hour,
                    minute = minute,
                    dates = dates,
                    isEnabled = isEnabled,
                    vibrate = switchVibrate.isChecked,
                    isSpecialAlarm = isSpecialAlarm,
                    isRegularAlarm = isRegularAlarm,
                    year = if (isSpecialAlarm) {
                        existingAlarm?.year ?: LocalDate.now().year
                    } else null,
                    snoozeEnabled = existingAlarm?.snoozeEnabled ?: true,
                    snoozeMinutes = existingAlarm?.snoozeMinutes ?: 5,
                    repeatDays = repeatDays.toList(),
                    specialAlarmMode = specialAlarmMode
                )

                // Save to database
                AlarmDatabase.getDatabase(this@AlarmDetailActivity)
                    .alarmDao()
                    .insertAlarm(alarm)

                // Schedule alarm (if disabled, scheduleAlarm will cancel any existing pending intent)
                try {
                    AlarmScheduler.scheduleAlarm(this@AlarmDetailActivity, alarm)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule alarm: ${e.message}", e)
                    Toast.makeText(this@AlarmDetailActivity, getString(R.string.msg_alarm_saved_schedule_failed, e.message), Toast.LENGTH_LONG).show()
                }

                finish()
            } catch (e: CalendarRepository.WorkdayDataException) {
                // Show specific error for workday data fetch failure
                Toast.makeText(this@AlarmDetailActivity, getString(R.string.msg_workday_data_error), Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@AlarmDetailActivity, getString(R.string.msg_save_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun loadWorkdayDates(): List<LocalDate> {
        Log.d(TAG, "Loading workday dates for mode: $specialAlarmMode")
        val repository = CalendarRepository.getInstance(this)
        val year = LocalDate.now().year
        Log.d(TAG, "Current year: $year")

        val calendarData = repository.getCalendarData(year)
        Log.d(TAG, "Got calendar data: year=${calendarData.year}, holidays=${calendarData.holidays.size}, workdays=${calendarData.workdays.size}")

        // Calculate dates based on selected special alarm mode
        val dates = when (specialAlarmMode) {
            SpecialAlarmMode.ALL_HOLIDAYS -> {
                // All holidays (official holidays + weekends)
                calculateAllHolidays(calendarData)
            }
            SpecialAlarmMode.FIRST_WORKDAY_ONLY -> {
                // First workday after holidays (including weekends)
                calculateFirstWorkdays(calendarData)
            }
            SpecialAlarmMode.ALL_WORKDAYS -> {
                // All workdays (Mon-Fri, skip holidays)
                calendarData.getAllWorkDates()
            }
        }

        Log.d(TAG, "Calculated dates count: ${dates.size} for mode: $specialAlarmMode")

        if (dates.isEmpty()) {
            Log.w(TAG, "Dates list is empty for mode: $specialAlarmMode")
        }

        return dates
    }

    private fun calculateAllHolidays(calendarData: CalendarData): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val startDate = LocalDate.of(calendarData.year, 1, 1)
        val endDate = LocalDate.of(calendarData.year, 12, 31)

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek.value
            // Holiday = (legal holiday OR weekend) AND NOT workday adjustment
            val isLegalHoliday = calendarData.holidays.any { it == currentDate }
            val isWeekend = dayOfWeek in listOf(6, 7)
            val isWorkdayAdjustment = calendarData.workdays.any { it == currentDate }

            if ((isLegalHoliday || isWeekend) && !isWorkdayAdjustment) {
                dates.add(currentDate)
            }
            currentDate = currentDate.plusDays(1)
        }
        return dates
    }

    private fun calculateFirstWorkdays(calendarData: CalendarData): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val startDate = LocalDate.of(calendarData.year, 1, 1)
        val endDate = LocalDate.of(calendarData.year, 12, 31)

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek.value

            // Check if current day is a workday (not legal holiday, not weekend, OR is workday adjustment)
            val isLegalHoliday = calendarData.holidays.any { it == currentDate }
            val isWeekend = dayOfWeek in listOf(6, 7)
            val isWorkdayAdjustment = calendarData.workdays.any { it == currentDate }
            val isWorkday = (!isLegalHoliday && !isWeekend) || isWorkdayAdjustment

            if (isWorkday) {
                // Check if previous day was a holiday (legal holiday OR weekend, but NOT workday adjustment)
                val prevDate = currentDate.minusDays(1)
                val prevDayOfWeek = prevDate.dayOfWeek.value
                val isPrevLegalHoliday = calendarData.holidays.any { it == prevDate }
                val isPrevWeekend = prevDayOfWeek in listOf(6, 7)
                val isPrevWorkdayAdjustment = calendarData.workdays.any { it == prevDate }
                val isPrevDayHoliday = (isPrevLegalHoliday || isPrevWeekend) && !isPrevWorkdayAdjustment

                if (isPrevDayHoliday) {
                    dates.add(currentDate)
                }
            }
            currentDate = currentDate.plusDays(1)
        }
        return dates
    }

    private fun showDeleteConfirm() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.msg_delete_title))
            .setMessage(getString(R.string.msg_delete_confirm))
            .setPositiveButton(getString(R.string.action_delete)) { _, _ ->
                deleteAlarm()
            }
            .setNegativeButton(getString(R.string.action_cancel), null)
            .show()
    }

    private fun deleteAlarm() {
        alarmId?.let { id ->
            lifecycleScope.launch {
                existingAlarm?.let { alarm ->
                    AlarmScheduler.cancelAlarm(this@AlarmDetailActivity, alarm)
                }

                AlarmDatabase.getDatabase(this@AlarmDetailActivity)
                    .alarmDao()
                    .deleteAlarmById(id)

                finish()
            }
        }
    }

    private fun copyAlarm() {
        existingAlarm?.let { alarm ->
            lifecycleScope.launch {
                val newAlarm = alarm.copy(
                    id = UUID.randomUUID().toString(),
                    title = alarm.title + " (复制)",
                    isEnabled = false
                )

                AlarmDatabase.getDatabase(this@AlarmDetailActivity)
                    .alarmDao()
                    .insertAlarm(newAlarm)

                Toast.makeText(this@AlarmDetailActivity, getString(R.string.msg_alarm_copied), Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmDetailActivity"
        const val EXTRA_ALARM_ID = "alarm_id"
        const val EXTRA_IS_SPECIAL_ALARM = "is_special_alarm"
        const val EXTRA_IS_REGULAR_ALARM = "is_regular_alarm"
    }
}
