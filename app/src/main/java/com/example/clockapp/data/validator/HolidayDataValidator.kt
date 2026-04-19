package com.example.clockapp.data.validator

import android.util.Log
import com.example.clockapp.data.api.HolidayDay
import com.example.clockapp.data.api.HolidayResponse
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

// Prevent Log calls from crashing in unit tests
private fun logError(tag: String, message: String) {
    try {
        Log.e(tag, message)
    } catch (e: RuntimeException) {
        // Log class not available in unit tests, print to stderr
        System.err.println("[$tag] ERROR: $message")
    }
}

private fun logWarning(tag: String, message: String) {
    try {
        Log.w(tag, message)
    } catch (e: RuntimeException) {
        // Log class not available in unit tests, print to stderr
        System.err.println("[$tag] WARN: $message")
    }
}

/**
 * Holiday data validator
 * Validates API response data for security and business logic correctness
 */
object HolidayDataValidator {

    private const val TAG = "HolidayDataValidator"
    private const val MAX_DAYS_IN_YEAR = 366
    private const val MIN_YEAR = 2000
    private const val MAX_YEAR = 2100

    private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Validation result sealed class
     */
    sealed class ValidationResult {
        data class Success(val data: HolidayResponse) : ValidationResult()
        data class Failure(val reason: String) : ValidationResult()
    }

    /**
     * Validate complete holiday response
     * @param response The API response to validate
     * @param expectedYear The year that was requested
     * @return ValidationResult indicating success or failure with reason
     */
    fun validate(response: HolidayResponse?, expectedYear: Int): ValidationResult {
        if (response == null) {
            return ValidationResult.Failure("Response body is null")
        }

        // Schema validation
        val schemaResult = validateSchema(response)
        if (schemaResult is ValidationResult.Failure) {
            return schemaResult
        }

        // Business logic validation
        val businessResult = validateBusinessLogic(response, expectedYear)
        if (businessResult is ValidationResult.Failure) {
            return businessResult
        }

        return ValidationResult.Success(response)
    }

    /**
     * Schema validation - check data structure and format
     */
    private fun validateSchema(response: HolidayResponse): ValidationResult {
        // Check year field exists and is valid
        if (response.year == 0) {
            return ValidationResult.Failure("Year field is missing or zero")
        }

        // Check days list exists (can be empty but not null)
        if (response.days == null) {
            return ValidationResult.Failure("Days list is null")
        }

        // Check days list size is reasonable
        if (response.days.size > MAX_DAYS_IN_YEAR) {
            return ValidationResult.Failure(
                "Days list size (${response.days.size}) exceeds maximum allowed ($MAX_DAYS_IN_YEAR)"
            )
        }

        // Validate each day's schema
        response.days.forEachIndexed { index, day ->
            val dayResult = validateDaySchema(day, index)
            if (dayResult is ValidationResult.Failure) {
                return dayResult
            }
        }

        return ValidationResult.Success(response)
    }

    /**
     * Validate individual holiday day schema
     */
    private fun validateDaySchema(day: HolidayDay, index: Int): ValidationResult {
        // Check name field
        if (day.name.isBlank()) {
            return ValidationResult.Failure("Day at index $index has blank name")
        }

        // Check name length (prevent potential attacks with extremely long strings)
        if (day.name.length > 100) {
            return ValidationResult.Failure(
                "Day at index $index has name exceeding 100 characters"
            )
        }

        // Check date field format (yyyy-MM-dd)
        if (day.date.isBlank()) {
            return ValidationResult.Failure("Day at index $index has blank date")
        }

        // Validate date format
        try {
            LocalDate.parse(day.date, DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            return ValidationResult.Failure(
                "Day at index $index has invalid date format: ${day.date}"
            )
        }

        // isOffDay is Boolean and cannot be null in Kotlin, so no need to check

        return ValidationResult.Success(HolidayResponse(0, emptyList()))
    }

    /**
     * Business logic validation
     */
    private fun validateBusinessLogic(
        response: HolidayResponse,
        expectedYear: Int
    ): ValidationResult {
        // Validate year matches expected year
        if (response.year != expectedYear) {
            return ValidationResult.Failure(
                "Year mismatch: expected $expectedYear, got ${response.year}"
            )
        }

        // Validate year is in reasonable range
        if (response.year < MIN_YEAR || response.year > MAX_YEAR) {
            return ValidationResult.Failure(
                "Year ${response.year} is out of valid range ($MIN_YEAR-$MAX_YEAR)"
            )
        }

        // Validate each day's business logic
        val seenDates = mutableSetOf<String>()
        response.days?.forEachIndexed { index, day ->
            val dayResult = validateDayBusinessLogic(day, expectedYear, seenDates, index)
            if (dayResult is ValidationResult.Failure) {
                return dayResult
            }
        }

        return ValidationResult.Success(response)
    }

    /**
     * Validate individual day's business logic
     */
    private fun validateDayBusinessLogic(
        day: HolidayDay,
        expectedYear: Int,
        seenDates: MutableSet<String>,
        index: Int
    ): ValidationResult {
        val date = try {
            LocalDate.parse(day.date, DATE_FORMATTER)
        } catch (e: DateTimeParseException) {
            return ValidationResult.Failure(
                "Day at index $index has unparseable date: ${day.date}"
            )
        }

        // Check date is in expected year
        if (date.year != expectedYear) {
            return ValidationResult.Failure(
                "Day at index $index has date ${day.date} outside expected year $expectedYear"
            )
        }

        // Check for duplicate dates
        if (seenDates.contains(day.date)) {
            return ValidationResult.Failure(
                "Duplicate date found: ${day.date} at index $index"
            )
        }
        seenDates.add(day.date)

        // Check date is in valid range (January 1 to December 31 of expected year)
        val yearStart = LocalDate.of(expectedYear, 1, 1)
        val yearEnd = LocalDate.of(expectedYear, 12, 31)
        if (date.isBefore(yearStart) || date.isAfter(yearEnd)) {
            return ValidationResult.Failure(
                "Date ${day.date} is outside valid range for year $expectedYear"
            )
        }

        // Validate workday (isOffDay=false) is actually a weekend
        if (!day.isOffDay) {
            val dayOfWeek = date.dayOfWeek
            if (dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY) {
                logWarning(TAG, "Warning: Workday ${day.date} (${day.name}) is not a weekend, " +
                        "it's ${dayOfWeek.name}")
                // This is a warning but we don't fail validation
                // because sometimes the government may adjust weekdays
            }
        }

        return ValidationResult.Success(HolidayResponse(0, emptyList()))
    }

    /**
     * Validate CalendarData after conversion
     * @param calendarData The converted calendar data
     * @param expectedYear The expected year
     * @return true if valid, false otherwise
     */
    fun validateCalendarData(calendarData: com.example.clockapp.data.model.CalendarData, expectedYear: Int): Boolean {
        // Check year matches
        if (calendarData.year != expectedYear) {
            logError(TAG, "CalendarData year mismatch: expected $expectedYear, got ${calendarData.year}")
            return false
        }

        // Check no date is duplicated between holidays and workdays
        val intersection = calendarData.holidays.intersect(calendarData.workdays.toSet())
        if (intersection.isNotEmpty()) {
            logError(TAG, "Dates found in both holidays and workdays: $intersection")
            return false
        }

        // Validate all dates are in expected year
        val yearStart = LocalDate.of(expectedYear, 1, 1)
        val yearEnd = LocalDate.of(expectedYear, 12, 31)

        calendarData.holidays.forEach { date ->
            if (date.isBefore(yearStart) || date.isAfter(yearEnd)) {
                logError(TAG, "Holiday date $date is outside year $expectedYear")
                return false
            }
        }

        calendarData.workdays.forEach { date ->
            if (date.isBefore(yearStart) || date.isAfter(yearEnd)) {
                logError(TAG, "Workday date $date is outside year $expectedYear")
                return false
            }
        }

        return true
    }
}
