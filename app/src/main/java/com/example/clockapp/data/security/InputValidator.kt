package com.example.clockapp.data.security

import com.example.clockapp.data.model.Alarm
import java.util.UUID

/**
 * Input validator for user inputs and external data
 * Prevents injection attacks and ensures data integrity
 */
object InputValidator {

    /**
     * Validation result sealed class
     */
    sealed class ValidationResult {
        data class Success(val value: String) : ValidationResult()
        data class Failure(val reason: String) : ValidationResult()
    }

    /**
     * Sanitize and validate alarm title
     * @param title Raw input title
     * @return ValidationResult with sanitized value or failure reason
     */
    fun validateAlarmTitle(title: String?): ValidationResult {
        if (title == null) {
            return ValidationResult.Failure("Title cannot be null")
        }

        // Trim whitespace
        val trimmed = title.trim()

        // Check empty
        if (trimmed.isEmpty()) {
            return ValidationResult.Failure("Title cannot be empty")
        }

        // Check length
        if (trimmed.length > SecurityConfig.MAX_ALARM_TITLE_LENGTH) {
            return ValidationResult.Failure(
                "Title exceeds maximum length of ${SecurityConfig.MAX_ALARM_TITLE_LENGTH}"
            )
        }

        // Check allowed characters (alphanumeric, spaces, Chinese characters, common punctuation)
        if (!SecurityConfig.ALLOWED_TITLE_PATTERN.matches(trimmed)) {
            return ValidationResult.Failure("Title contains invalid characters")
        }

        // Check for potential script injection
        if (containsScriptInjection(trimmed)) {
            return ValidationResult.Failure("Title contains potentially dangerous content")
        }

        return ValidationResult.Success(trimmed)
    }

    /**
     * Validate alarm ID format
     * @param id Alarm ID string
     * @return true if valid, false otherwise
     */
    fun validateAlarmId(id: String?): Boolean {
        if (id == null || id.isBlank()) {
            return false
        }

        if (id.length > SecurityConfig.MAX_ID_LENGTH) {
            return false
        }

        // Check if it's a valid UUID format or matches allowed pattern
        return try {
            UUID.fromString(id)
            true
        } catch (e: IllegalArgumentException) {
            // If not UUID, check allowed characters
            SecurityConfig.ALLOWED_ID_PATTERN.matches(id)
        }
    }

    /**
     * Validate hour value
     * @param hour Hour value
     * @return true if valid (0-23)
     */
    fun validateHour(hour: Int): Boolean {
        return hour in 0..23
    }

    /**
     * Validate minute value
     * @param minute Minute value
     * @return true if valid (0-59)
     */
    fun validateMinute(minute: Int): Boolean {
        return minute in 0..59
    }

    /**
     * Validate year value
     * @param year Year value
     * @return true if valid (2000-2100)
     */
    fun validateYear(year: Int?): Boolean {
        if (year == null) return true // null is valid (optional field)
        return year in 2000..2100
    }

    /**
     * Validate repeat days list
     * @param days List of day values (1=Monday, 7=Sunday)
     * @return true if valid
     */
    fun validateRepeatDays(days: List<Int>): Boolean {
        if (days.size > 7) return false
        return days.all { it in 1..7 }
    }

    /**
     * Sanitize string for safe display
     * Removes control characters and normalizes whitespace
     */
    fun sanitizeForDisplay(input: String): String {
        return input
            // Remove control characters except newline and tab
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
            // Normalize multiple spaces to single space
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Check for potential script injection patterns
     */
    private fun containsScriptInjection(input: String): Boolean {
        val dangerousPatterns = listOf(
            "<script",
            "javascript:",
            "onerror=",
            "onload=",
            "onclick=",
            "eval(",
            "expression(",
            "${'$'}{",  // Template injection
            "<%",       // JSP/ASP tags
            "<?php",    // PHP tags
            "data:text/html",
            "vbscript:",
            "mocha:",
            "livescript:"
        )

        val lowerInput = input.lowercase()
        return dangerousPatterns.any { lowerInput.contains(it) }
    }

    /**
     * Validate complete Alarm object
     * @param alarm Alarm to validate
     * @return ValidationResult indicating success or failure
     */
    fun validateAlarm(alarm: Alarm): ValidationResult {
        // Validate ID
        if (!validateAlarmId(alarm.id)) {
            return ValidationResult.Failure("Invalid alarm ID: ${alarm.id}")
        }

        // Validate title
        val titleResult = validateAlarmTitle(alarm.title)
        if (titleResult is ValidationResult.Failure) {
            return titleResult
        }

        // Validate time
        if (!validateHour(alarm.hour)) {
            return ValidationResult.Failure("Invalid hour: ${alarm.hour}")
        }
        if (!validateMinute(alarm.minute)) {
            return ValidationResult.Failure("Invalid minute: ${alarm.minute}")
        }

        // Validate year if present
        if (!validateYear(alarm.year)) {
            return ValidationResult.Failure("Invalid year: ${alarm.year}")
        }

        // Validate repeat days
        if (!validateRepeatDays(alarm.repeatDays)) {
            return ValidationResult.Failure("Invalid repeat days: ${alarm.repeatDays}")
        }

        // Validate snooze minutes (reasonable range)
        if (alarm.snoozeMinutes !in 1..60) {
            return ValidationResult.Failure("Invalid snooze minutes: ${alarm.snoozeMinutes}")
        }

        return ValidationResult.Success(alarm.title)
    }
}
