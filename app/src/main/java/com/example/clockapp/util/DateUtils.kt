package com.example.clockapp.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit

/**
 * Date utility class providing thread-safe date operations using java.time API
 */
object DateUtils {
    
    /**
     * Formatter for "yyyy-MM-dd" pattern with strict parsing
     * Using DateTimeFormatter.ISO_LOCAL_DATE which is strict by default
     */
    private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    
    /**
     * Formatter for "yyyy年M月" pattern
     */
    private val YEAR_MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy年M月")
    
    /**
     * Formatter for "d" pattern (day of month)
     */
    private val DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("d")
    
    /**
     * Formatter for "HH:mm" pattern
     */
    private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    /**
     * Get current date time with system default time zone
     */
    fun now(): LocalDateTime = LocalDateTime.now(ZoneId.systemDefault())
    
    /**
     * Get current date
     */
    fun today(): LocalDate = LocalDate.now(ZoneId.systemDefault())

    /**
     * Parse date string in "yyyy-MM-dd" format to LocalDate
     */
    fun parseDate(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString, DATE_FORMATTER)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format LocalDate to "yyyy-MM-dd" string
     */
    fun formatDate(date: LocalDate): String {
        return date.format(DATE_FORMATTER)
    }
    
    /**
     * Format LocalDate to "yyyy年M月" string
     */
    fun formatYearMonth(date: LocalDate): String {
        return date.format(YEAR_MONTH_FORMATTER)
    }
    
    /**
     * Format LocalDate to "d" (day of month) string
     */
    fun formatDay(date: LocalDate): String {
        return date.format(DAY_FORMATTER)
    }
    
    /**
     * Format LocalTime to "HH:mm" string
     */
    fun formatTime(time: LocalTime): String {
        return time.format(TIME_FORMATTER)
    }
    
    /**
     * Format LocalDateTime to "HH:mm" string
     */
    fun formatTime(dateTime: LocalDateTime): String {
        return dateTime.format(TIME_FORMATTER)
    }

    /**
     * Check if two LocalDates are the same day
     */
    fun isSameDay(date1: LocalDate, date2: LocalDate): Boolean {
        return date1.isEqual(date2)
    }
    
    /**
     * Check if two Instants are the same day
     */
    fun isSameDay(instant1: Instant, instant2: Instant): Boolean {
        val zone = ZoneId.systemDefault()
        return instant1.atZone(zone).toLocalDate() == instant2.atZone(zone).toLocalDate()
    }
    
    /**
     * Get LocalDate with time set to start of day (00:00:00)
     */
    fun getDateOnly(date: LocalDateTime): LocalDate {
        return date.toLocalDate()
    }
    
    /**
     * Get start of day for a LocalDate
     */
    fun startOfDay(date: LocalDate): LocalDateTime {
        return date.atStartOfDay()
    }
    
    /**
     * Convert LocalDate to Instant (at start of day)
     */
    fun toInstant(localDate: LocalDate): Instant {
        return localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
    }
    
    /**
     * Convert Instant to LocalDate
     */
    fun toLocalDate(instant: Instant): LocalDate {
        return instant.atZone(ZoneId.systemDefault()).toLocalDate()
    }
    
    /**
     * Get day of week (1 = Monday, 7 = Sunday)
     */
    fun getDayOfWeek(localDate: LocalDate): Int {
        return localDate.dayOfWeek.value
    }
    
    /**
     * Add days to LocalDate
     */
    fun addDays(date: LocalDate, days: Int): LocalDate {
        return date.plusDays(days.toLong())
    }
    
    /**
     * Add days to LocalDateTime
     */
    fun addDays(dateTime: LocalDateTime, days: Int): LocalDateTime {
        return dateTime.plusDays(days.toLong())
    }
    
    /**
     * Check if date is before today
     */
    fun isBeforeToday(date: LocalDate): Boolean {
        return date.isBefore(today())
    }
    
    /**
     * Check if date is today
     */
    fun isToday(date: LocalDate): Boolean {
        return date.isEqual(today())
    }
    
    /**
     * Get days between two dates
     */
    fun daysBetween(start: LocalDate, end: LocalDate): Int {
        return ChronoUnit.DAYS.between(start, end).toInt()
    }
    
    /**
     * Get days between two dates (inclusive)
     */
    fun daysBetweenInclusive(start: LocalDate, end: LocalDate): Int {
        return ChronoUnit.DAYS.between(start, end).toInt() + 1
    }
    
    /**
     * Create LocalDateTime from date and time components
     */
    fun of(year: Int, month: Int, day: Int, hour: Int, minute: Int): LocalDateTime {
        return LocalDateTime.of(year, month, day, hour, minute)
    }
    
    /**
     * Create LocalDate from year, month, day
     */
    fun of(year: Int, month: Int, day: Int): LocalDate {
        return LocalDate.of(year, month, day)
    }
}
