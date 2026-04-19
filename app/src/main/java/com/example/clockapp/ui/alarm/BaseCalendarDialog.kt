package com.example.clockapp.ui.alarm

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.example.clockapp.R
import com.example.clockapp.data.model.CalendarData
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Base calendar dialog with common functionality
 * Supports navigation buttons to switch months
 */
abstract class BaseCalendarDialog(
    context: Context
) : Dialog(context) {

    protected val monthFormatter = DateTimeFormatter.ofPattern("yyyy年M月", Locale.getDefault())
    protected lateinit var tvMonth: TextView
    protected lateinit var btnPrev: ImageButton
    protected lateinit var btnNext: ImageButton
    protected lateinit var gridDays: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())

        // Set dialog window background to transparent for rounded corners
        window?.setBackgroundDrawableResource(android.R.color.transparent)

        initViews()
        setupNavigationButtons()
        onDialogCreated()
    }

    protected abstract fun getLayoutResId(): Int

    protected open fun initViews() {
        tvMonth = findViewById(R.id.tvMonth)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
        gridDays = findViewById(R.id.gridDays)
    }

    private fun setupNavigationButtons() {
        btnPrev.setOnClickListener {
            if (canGoToPreviousMonth()) {
                setCurrentYearMonth(getCurrentYearMonth().minusMonths(1))
                updateCalendar()
            }
        }

        btnNext.setOnClickListener {
            if (canGoToNextMonth()) {
                setCurrentYearMonth(getCurrentYearMonth().plusMonths(1))
                updateCalendar()
            }
        }
    }

    protected abstract fun onDialogCreated()

    protected abstract fun getCurrentYearMonth(): YearMonth

    protected abstract fun setCurrentYearMonth(yearMonth: YearMonth)

    protected abstract fun canGoToPreviousMonth(): Boolean

    protected abstract fun canGoToNextMonth(): Boolean

    protected abstract fun updateCalendar()

    protected fun createWeekRow(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                40.dpToPx()
            )
            orientation = LinearLayout.HORIZONTAL
        }
    }

    protected fun Int.dpToPx(): Int {
        return (this * context.resources.displayMetrics.density).toInt()
    }

    protected fun createDayCell(
        dayText: String,
        date: LocalDate?,
        isSelected: Boolean,
        isPast: Boolean,
        calendarData: CalendarData?,
        hasAlarm: Boolean = false,
        onDateClick: ((LocalDate) -> Unit)? = null
    ): LinearLayout {
        val container = LinearLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER
            setPadding(4, 4, 4, 2)

            // Enable click for date selection if needed
            if (date != null && onDateClick != null && !isPast) {
                isClickable = true
                setOnClickListener { onDateClick(date) }
            }
        }

        if (date != null) {
            val isHoliday = calendarData?.holidays?.any { it == date } ?: false
            val isWorkday = calendarData?.workdays?.any { it == date } ?: false

            // Create frame for day number with top-right mark
            val dayFrame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Create day number text
            val dayNumberView = TextView(context).apply {
                text = dayText
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // Create top-right mark view (休/班)
            val topMarkView = when {
                isHoliday || isWorkday -> {
                    TextView(context).apply {
                        text = when {
                            isHoliday -> "休"
                            isWorkday -> "班"
                            else -> ""
                        }
                        textSize = 9f
                        setTypeface(null, android.graphics.Typeface.BOLD)
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.WRAP_CONTENT,
                            FrameLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = android.view.Gravity.TOP or android.view.Gravity.END
                            topMargin = 0
                            rightMargin = 2
                        }
                        setTextColor(when {
                            isHoliday -> context.getColor(android.R.color.holo_green_dark)
                            isWorkday -> context.getColor(android.R.color.holo_red_light)
                            else -> context.getColor(android.R.color.white)
                        })
                    }
                }
                else -> null
            }

            dayFrame.addView(dayNumberView)
            topMarkView?.let { dayFrame.addView(it) }

            // Create alarm icon view (below date)
            val alarmIconView = when {
                hasAlarm -> {
                    ImageView(context).apply {
                        setImageResource(R.drawable.ic_alarm_mark)
                        layoutParams = LinearLayout.LayoutParams(32, 32).apply {
                            topMargin = 0
                        }
                    }
                }
                else -> {
                    View(context).apply {
                        layoutParams = LinearLayout.LayoutParams(32, 28).apply {
                            topMargin = 0
                        }
                        visibility = View.INVISIBLE
                    }
                }
            }

            // Set background and day number color
            when {
                isSelected -> {
                    // Use border style (purple border, transparent fill) for all selected dates
                    container.setBackgroundResource(R.drawable.bg_date_selected_border)
                    dayNumberView.setTextColor(context.getColor(android.R.color.black))
                }
                isPast -> {
                    container.setBackgroundResource(R.drawable.bg_date_normal)
                    dayNumberView.setTextColor(context.getColor(android.R.color.darker_gray))
                }
                else -> {
                    container.setBackgroundResource(R.drawable.bg_date_normal)
                    dayNumberView.setTextColor(context.getColor(android.R.color.black))
                }
            }

            container.addView(dayFrame)
            container.addView(alarmIconView)
        } else {
            val emptyView = TextView(context).apply {
                text = dayText
                textSize = 14f
                gravity = android.view.Gravity.CENTER
            }
            container.addView(emptyView)
        }

        return container
    }
}
