package com.example.clockapp.ui.alarm

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import com.example.clockapp.R
import com.example.clockapp.data.model.CalendarData
import com.example.clockapp.data.repository.CalendarRepository
import java.time.LocalDate
import java.time.YearMonth

/**
 * Date picker dialog - with holiday and workday markers
 * Supports swipe left/right to switch months
 */
class DatePickerDialog(
    private val activity: androidx.fragment.app.FragmentActivity,
    private val initialDates: List<LocalDate>,
    private val onDatesSelected: (List<LocalDate>) -> Unit
) : BaseCalendarDialog(activity) {

    private val selectedDates = mutableSetOf<LocalDate>()
    private var currentYearMonth = YearMonth.now()

    private lateinit var btnClear: Button
    private lateinit var btnConfirm: Button
    private lateinit var btnCancel: Button

    override fun getLayoutResId(): Int = R.layout.dialog_date_picker

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize selected dates before super.onCreate
        initialDates.forEach { date ->
            selectedDates.add(date)
        }
        super.onCreate(savedInstanceState)
    }

    override fun initViews() {
        super.initViews()
        btnClear = findViewById(R.id.btnClear)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnCancel = findViewById(R.id.btnCancel)

        btnClear.setOnClickListener {
            selectedDates.clear()
            updateCalendar()
            updateConfirmButton()
        }

        btnConfirm.setOnClickListener {
            onDatesSelected(selectedDates.toList().sorted())
            dismiss()
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDialogCreated() {
        updateCalendar()
    }

    override fun getCurrentYearMonth(): YearMonth = currentYearMonth

    override fun setCurrentYearMonth(yearMonth: YearMonth) {
        currentYearMonth = yearMonth
    }

    override fun canGoToPreviousMonth(): Boolean = true

    override fun canGoToNextMonth(): Boolean = true

    override fun updateCalendar() {
        tvMonth.text = currentYearMonth.format(monthFormatter)

        // Clear existing views
        gridDays.removeAllViews()

        val calendarData = getCalendarDataForYear(currentYearMonth.year)
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
        val today = LocalDate.now()

        // Create rows, one row per week
        var currentRow: LinearLayout? = null

        // Add empty cells for days before the first day of month
        for (i in 0 until firstDayOfWeek) {
            if (i % 7 == 0) {
                currentRow = createWeekRow()
                gridDays.addView(currentRow)
            }
            val emptyCell = createDayCell("", null, false, false, calendarData)
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
            val isSelected = selectedDates.contains(date)
            val isPast = date.isBefore(today)
            val dayCell = createDayCell(
                dayText = day.toString(),
                date = date,
                isSelected = isSelected,
                isPast = isPast,
                calendarData = calendarData,
                hasAlarm = isSelected,
                onDateClick = if (!isPast) { clickedDate ->
                    if (selectedDates.contains(clickedDate)) {
                        selectedDates.remove(clickedDate)
                    } else {
                        selectedDates.add(clickedDate)
                    }
                    updateCalendar()
                    updateConfirmButton()
                } else null
            )
            currentRow?.addView(dayCell)
        }

        // Add empty cells to complete the last week
        val lastDayOfWeek = (firstDayOfWeek + daysInMonth) % 7
        if (lastDayOfWeek != 0) {
            val remainingCells = 7 - lastDayOfWeek
            for (i in 0 until remainingCells) {
                val emptyCell = createDayCell("", null, false, false, calendarData)
                currentRow?.addView(emptyCell)
            }
        }
    }

    private fun getCalendarDataForYear(year: Int): CalendarData? {
        val repository = CalendarRepository.getInstance(activity)
        return try {
            repository.getCalendarDataSync(year)
        } catch (e: Exception) {
            null
        }
    }

    private fun updateConfirmButton() {
        btnConfirm.text = "确定 (${selectedDates.size})"
    }
}
