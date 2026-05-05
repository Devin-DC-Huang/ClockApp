package com.example.clockapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.UUID

/**
 * Special alarm mode - 特殊闹钟模式
 */
enum class SpecialAlarmMode {
    ALL_WORKDAYS,           // 所有工作日（周一到周五，跳过节假日）- 默认
    FIRST_WORKDAY_ONLY,     // 仅假期后第一个工作日（包括周末后的周一）
    ALL_HOLIDAYS            // 所有节假日（包括法定节假日和周末）
}

/**
 * Alarm data model using java.time API
 * Corresponds to SpecificDateAlarm in the reference project
 */
@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    var title: String = "闹钟",
    var hour: Int = 8,  // 0-23
    var minute: Int = 0,  // 0-59
    var dates: List<LocalDate> = emptyList(),  // List of specified dates (java.time)
    var isEnabled: Boolean = true,
    var ringtone: String = "default",
    var vibrate: Boolean = true,
    val createdAt: Instant = Instant.now(),  // java.time.Instant
    var isSpecialAlarm: Boolean = false,  // true=special alarm(auto fetch dates), false=manual date selection
    var isRegularAlarm: Boolean = false,  // true=regular alarm(weekly repeat), false=other types
    var year: Int? = null,  // Year for special alarm
    var snoozeEnabled: Boolean = true,  // Enable snooze function
    var snoozeMinutes: Int = 5,  // Snooze interval in minutes (default 5 min)
    var repeatDays: List<Int> = emptyList(),  // Weekly repeat days: 1=Monday, 2=Tuesday, ..., 7=Sunday
    var specialAlarmMode: SpecialAlarmMode = SpecialAlarmMode.ALL_WORKDAYS  // 特殊闹钟模式
) {
    /**
     * Get formatted time string
     */
    fun getTimeString(): String {
        val h = hour.toString().padStart(2, '0')
        val m = minute.toString().padStart(2, '0')
        return "$h:$m"
    }

    /**
     * Get type description
     */
    fun getTypeDescription(): String {
        return when {
            isSpecialAlarm -> year?.let { "Special Alarm (${it})" } ?: "Special Alarm"
            isRegularAlarm -> if (repeatDays.isEmpty()) "One-time Alarm" else "Weekly Repeat (${getRepeatDaysText()})"
            else -> if (dates.isEmpty()) "Specific Date" else "${dates.size} dates specified"
        }
    }

    /**
     * Get repeat days text description
     */
    private fun getRepeatDaysText(): String {
        if (repeatDays.isEmpty()) return "Never"
        val dayNames = mapOf(1 to "Mon", 2 to "Tue", 3 to "Wed", 4 to "Thu", 5 to "Fri", 6 to "Sat", 7 to "Sun")
        return repeatDays.sorted().map { dayNames[it] ?: "" }.joinToString(", ")
    }

    /**
     * Check if alarm time is after current time
     */
    private fun isTimeAfterNow(hour: Int, minute: Int, currentHour: Int, currentMinute: Int): Boolean {
        return hour > currentHour || (hour == currentHour && minute > currentMinute)
    }

    /**
     * Calculate ring time for repeat days
     * @return LocalDateTime of next ring time, or null if no upcoming alarm
     */
    private fun calculateRepeatDaysRingTime(
        today: LocalDate,
        currentDayOfWeek: Int,
        currentHour: Int,
        currentMinute: Int
    ): LocalDateTime? {
        if (repeatDays.isEmpty()) return null

        for (daysOffset in 0..13) {
            val checkDay = ((currentDayOfWeek - 1 + daysOffset) % 7) + 1
            if (checkDay in repeatDays) {
                val ringDate = today.plusDays(daysOffset.toLong())
                val ringTime = LocalDateTime.of(ringDate, LocalTime.of(hour, minute))

                if (daysOffset == 0) {
                    if (isTimeAfterNow(hour, minute, currentHour, currentMinute)) {
                        return ringTime
                    }
                } else {
                    return ringTime
                }
            }
        }
        return null
    }

    /**
     * Calculate one-time alarm ring time (today or tomorrow)
     */
    private fun calculateOneTimeRingTime(
        today: LocalDate,
        currentHour: Int,
        currentMinute: Int
    ): LocalDateTime {
        val ringDate = if (isTimeAfterNow(hour, minute, currentHour, currentMinute)) {
            today
        } else {
            today.plusDays(1)
        }
        return LocalDateTime.of(ringDate, LocalTime.of(hour, minute))
    }

    /**
     * Calculate next ring time
     * @return LocalDateTime of next ring time, or null if no upcoming alarm
     */
    fun calculateNextRingTime(): LocalDateTime? {
        if (!isEnabled) return null

        val now = LocalDateTime.now(ZoneId.systemDefault())
        val today = now.toLocalDate()
        val currentDayOfWeek = today.dayOfWeek.value  // 1=Monday, 7=Sunday

        // Special alarm: uses pre-calculated dates stored in alarm.dates
        if (isSpecialAlarm) {
            if (dates.isEmpty()) return null

            val sortedDates = dates.sorted()
            for (date in sortedDates) {
                val ringTime = LocalDateTime.of(date, LocalTime.of(hour, minute))
                if (ringTime.isAfter(now)) {
                    return ringTime
                }
            }
            return null
        }

        // Regular alarm: calculate based on repeat days only
        if (isRegularAlarm) {
            val currentHour = now.hour
            val currentMinute = now.minute

            calculateRepeatDaysRingTime(today, currentDayOfWeek, currentHour, currentMinute)?.let {
                return it
            }

            // One-time regular alarm (never repeat): schedule for today or tomorrow
            return calculateOneTimeRingTime(today, currentHour, currentMinute)
        }

        // Specific date alarm
        if (dates.isEmpty()) return null

        val sortedDates = dates.sorted()

        for (date in sortedDates) {
            val ringTime = LocalDateTime.of(date, LocalTime.of(hour, minute))
            if (ringTime.isAfter(now)) {
                return ringTime
            }
        }
        return null
    }

    /**
     * Get next ring time description
     */
    fun getNextRingDescription(): String {
        if (!isEnabled) return "Disabled"

        val nextTime = calculateNextRingTime()
        if (nextTime == null) {
            if (dates.isNotEmpty()) {
                val lastDate = dates.maxOrNull()
                val lastRingTime = lastDate?.let {
                    LocalDateTime.of(it, LocalTime.of(hour, minute))
                }
                if (lastRingTime != null && lastRingTime.isBefore(LocalDateTime.now())) {
                    return "All dates expired"
                }
            }
            return "No upcoming reminder"
        }

        val now = LocalDate.now()
        val nextDate = nextTime.toLocalDate()
        
        // Calculate days difference
        val days = java.time.temporal.ChronoUnit.DAYS.between(now, nextDate).toInt()

        return when {
            days == 0 -> "Today ${getTimeString()}"
            days == 1 -> "Tomorrow ${getTimeString()}"
            else -> "${nextTime.monthValue}/${nextTime.dayOfMonth} ${getTimeString()}"
        }
    }

    /**
     * Generate int ID for AlarmManager
     */
    fun getAlarmIntId(): Int {
        return id.hashCode() and 0x7FFFFFFF
    }
}
