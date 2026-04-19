package com.example.clockapp.ui.alarm

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.example.clockapp.R
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.data.model.CalendarData
import com.example.clockapp.data.model.SpecialAlarmMode
import com.example.clockapp.data.repository.CalendarRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth

/**
 * Holiday calendar dialog - displays holidays and workdays
 * Supports navigation buttons to switch months
 */
class HolidayCalendarDialog(
    context: Context,
    private val year: Int,
    private val previewMode: com.example.clockapp.data.model.SpecialAlarmMode? = null,
    private val editingAlarmId: String? = null  // Current editing alarm ID, to show only its dates
) : BaseCalendarDialog(context) {

    private var calendarData: CalendarData? = null
    private var alarmDates: Set<LocalDate> = emptySet()
    private var currentYearMonth = YearMonth.of(year, LocalDate.now().monthValue)

    private lateinit var btnClear: Button
    private lateinit var btnCancel: Button
    private lateinit var btnConfirm: Button

    override fun getLayoutResId(): Int = R.layout.dialog_date_picker

    override fun initViews() {
        super.initViews()
        btnClear = findViewById(R.id.btnClear)
        btnCancel = findViewById(R.id.btnCancel)
        btnConfirm = findViewById(R.id.btnConfirm)

        // Hide unused buttons for holiday calendar
        btnClear.visibility = android.view.View.GONE
        btnCancel.visibility = android.view.View.GONE

        // Use confirm button as close button
        btnConfirm.text = "关闭"
        btnConfirm.setOnClickListener {
            dismiss()
        }
    }

    override fun onDialogCreated() {
        loadCalendarData()
    }

    private fun loadCalendarData() {
        val repository = CalendarRepository.getInstance(context)
        calendarData = repository.getCalendarDataSync(year)
        // Load alarm dates from database
        CoroutineScope(Dispatchers.IO).launch {
            val alarms = AlarmDatabase.getDatabase(context)
                .alarmDao()
                .getAllAlarmsSync()

            val dates = mutableSetOf<LocalDate>()
            val data = calendarData

            // If previewMode is provided (creating new alarm), calculate dates for preview
            // using the selected mode instead of saved mode
            if (previewMode != null && data != null) {
                // Creating new alarm: show preview of selected mode
                dates.addAll(calculateSpecialAlarmDatesForMode(data, previewMode))
            } else if (editingAlarmId != null) {
                // Editing specific alarm: show only that alarm's dates
                val editingAlarm = alarms.find { it.id == editingAlarmId }
                editingAlarm?.let {
                    dates.addAll(it.dates)
                }
            } else {
                // Viewing calendar from main page: show all enabled alarm dates
                alarms.filter { it.isEnabled }.forEach { alarm ->
                    dates.addAll(alarm.dates)
                }
            }

            withContext(Dispatchers.Main) {
                alarmDates = dates
                updateCalendar()
            }
        }
    }
    /**
     * Calculate all dates when a special alarm will ring within the year (for preview)
     */
    private fun calculateSpecialAlarmDatesForMode(
        data: CalendarData,
        mode: SpecialAlarmMode
    ): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        val startDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek.value  // 1=Monday, 7=Sunday

            when (mode) {
                SpecialAlarmMode.ALL_HOLIDAYS -> {
                    // All holidays = (legal holidays OR weekends) AND NOT workday adjustments
                    val isLegalHoliday = data.holidays.any { it == currentDate }
                    val isWeekend = dayOfWeek in listOf(6, 7)
                    val isWorkdayAdjustment = data.workdays.any { it == currentDate }
                    if ((isLegalHoliday || isWeekend) && !isWorkdayAdjustment) {
                        dates.add(currentDate)
                    }
                }
                SpecialAlarmMode.FIRST_WORKDAY_ONLY -> {
                    // Check if current day is a workday (not legal holiday, not weekend, OR is workday adjustment)
                    val isLegalHoliday = data.holidays.any { it == currentDate }
                    val isWeekend = dayOfWeek in listOf(6, 7)
                    val isWorkdayAdjustment = data.workdays.any { it == currentDate }
                    val isWorkday = (!isLegalHoliday && !isWeekend) || isWorkdayAdjustment

                    if (isWorkday) {
                        // Check if previous day was a holiday (legal holiday OR weekend, but NOT workday adjustment)
                        val prevDate = currentDate.minusDays(1)
                        val prevDayOfWeek = prevDate.dayOfWeek.value
                        val isPrevLegalHoliday = data.holidays.any { it == prevDate }
                        val isPrevWeekend = prevDayOfWeek in listOf(6, 7)
                        val isPrevWorkdayAdjustment = data.workdays.any { it == prevDate }
                        val isPrevDayHoliday = (isPrevLegalHoliday || isPrevWeekend) && !isPrevWorkdayAdjustment

                        if (isPrevDayHoliday) {
                            dates.add(currentDate)
                        }
                    }
                }
                SpecialAlarmMode.ALL_WORKDAYS -> {
                    // Use getAllWorkDates which handles workday adjustments correctly
                    dates.addAll(data.getAllWorkDates())
                }
            }
            currentDate = currentDate.plusDays(1)
        }

        return dates
    }

    override fun getCurrentYearMonth(): YearMonth = currentYearMonth

    override fun setCurrentYearMonth(yearMonth: YearMonth) {
        currentYearMonth = yearMonth
    }

    override fun canGoToPreviousMonth(): Boolean = currentYearMonth.monthValue > 1

    override fun canGoToNextMonth(): Boolean = currentYearMonth.monthValue < 12

    override fun updateCalendar() {
        tvMonth.text = currentYearMonth.format(monthFormatter)

        // Clear existing views
        gridDays.removeAllViews()

        val data = calendarData
        val firstDayOfMonth = currentYearMonth.atDay(1)

        // Get day of week (Monday=1, Sunday=7)
        // Convert to Sunday-first: Sunday=0, Monday=1, ..., Saturday=6
        val firstDayOfWeek = when (firstDayOfMonth.dayOfWeek.value) {
            7 -> 0  // Sunday
            1 -> 1  // Monday
            2 -> 2  // Tuesday
            3 -> 3  // Wednesday
            4 -> 4  // Thursday
            5 -> 5  // Friday
            6 -> 6  // Saturday
            else -> 0
        }

        val daysInMonth = currentYearMonth.lengthOfMonth()

        // Create rows, one row per week
        var currentRow: LinearLayout? = null

        // Add empty cells for days before the first day of month
        for (i in 0 until firstDayOfWeek) {
            if (i % 7 == 0) {
                currentRow = createWeekRow()
                gridDays.addView(currentRow)
            }
            val emptyCell = createDayCell("", null, false, false, data)
            currentRow?.addView(emptyCell)
        }

        // Add cells for each day of the month
        for (day in 1..daysInMonth) {
            val dayOfWeek = (firstDayOfWeek + day - 1) % 7
            if (dayOfWeek == 0) {
                currentRow = createWeekRow()
                gridDays.addView(currentRow)
            }

            val date = currentYearMonth.atDay(day)
            val hasAlarm = alarmDates.any { it == date }
            val dayCell = createDayCell(
                dayText = day.toString(),
                date = date,
                isSelected = false,
                isPast = false,
                calendarData = data,
                hasAlarm = hasAlarm
            )
            currentRow?.addView(dayCell)
        }

        // Add empty cells to complete the last week
        val lastDayOfWeek = (firstDayOfWeek + daysInMonth) % 7
        if (lastDayOfWeek != 0) {
            val remainingCells = 7 - lastDayOfWeek
            for (i in 0 until remainingCells) {
                val emptyCell = createDayCell("", null, false, false, data)
                currentRow?.addView(emptyCell)
            }
        }
    }
}
