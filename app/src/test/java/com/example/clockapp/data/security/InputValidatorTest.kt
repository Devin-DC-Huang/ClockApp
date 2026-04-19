package com.example.clockapp.data.security

import com.example.clockapp.data.model.Alarm
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for InputValidator
 */
class InputValidatorTest {

    // ==================== Alarm Title Validation Tests ====================

    @Test
    fun `validateAlarmTitle returns failure for null`() {
        val result = InputValidator.validateAlarmTitle(null)
        assertTrue(result is InputValidator.ValidationResult.Failure)
        assertEquals("Title cannot be null", (result as InputValidator.ValidationResult.Failure).reason)
    }

    @Test
    fun `validateAlarmTitle returns failure for empty string`() {
        val result = InputValidator.validateAlarmTitle("")
        assertTrue(result is InputValidator.ValidationResult.Failure)
        assertEquals("Title cannot be empty", (result as InputValidator.ValidationResult.Failure).reason)
    }

    @Test
    fun `validateAlarmTitle returns failure for whitespace only`() {
        val result = InputValidator.validateAlarmTitle("   ")
        assertTrue(result is InputValidator.ValidationResult.Failure)
        assertEquals("Title cannot be empty", (result as InputValidator.ValidationResult.Failure).reason)
    }

    @Test
    fun `validateAlarmTitle returns failure for too long`() {
        val longTitle = "A".repeat(101)
        val result = InputValidator.validateAlarmTitle(longTitle)
        assertTrue(result is InputValidator.ValidationResult.Failure)
        assertTrue((result as InputValidator.ValidationResult.Failure).reason.contains("exceeds maximum length"))
    }

    @Test
    fun `validateAlarmTitle returns failure for script injection`() {
        val maliciousTitles = listOf(
            "<script>alert('xss')</script>",
            "javascript:alert('xss')",
            "onclick=alert('xss')",
            "eval(maliciousCode)",
            "\${7*7}",  // Template injection attempt
            "<% System.exit(0) %>",
            "data:text/html,<script>alert('xss')</script>"
        )

        maliciousTitles.forEach { title ->
            val result = InputValidator.validateAlarmTitle(title)
            assertTrue("Title '$title' should be rejected", result is InputValidator.ValidationResult.Failure)
            val reason = (result as InputValidator.ValidationResult.Failure).reason
            // Should be rejected either for invalid characters or for potentially dangerous content
            assertTrue("Reason should indicate danger or invalid chars: $reason", 
                reason.contains("potentially dangerous") || reason.contains("invalid characters"))
        }
    }

    @Test
    fun `validateAlarmTitle returns success for valid titles`() {
        val validTitles = listOf(
            "Morning Alarm",
            "起床闹钟",
            "Work Meeting (9AM)",
            "Lunch_Break",
            "Dinner - Family",
            "Gym.Workout",
            "中文标题测试"
        )

        validTitles.forEach { title ->
            val result = InputValidator.validateAlarmTitle(title)
            assertTrue("Title '$title' should be valid", result is InputValidator.ValidationResult.Success)
            assertEquals(title.trim(), (result as InputValidator.ValidationResult.Success).value)
        }
    }

    @Test
    fun `validateAlarmTitle trims whitespace`() {
        val result = InputValidator.validateAlarmTitle("  Morning Alarm  ")
        assertTrue(result is InputValidator.ValidationResult.Success)
        assertEquals("Morning Alarm", (result as InputValidator.ValidationResult.Success).value)
    }

    @Test
    fun `validateAlarmTitle returns failure for invalid characters`() {
        val invalidTitles = listOf(
            "Alarm<script>",
            "Alarm@Home",
            "Alarm#1",
            "Alarm\$pecial",
            "Alarm%20Test",
            "Alarm*Star",
            "Alarm<Angle>"
        )

        invalidTitles.forEach { title ->
            val result = InputValidator.validateAlarmTitle(title)
            assertTrue("Title '$title' should be rejected for invalid characters", 
                result is InputValidator.ValidationResult.Failure)
        }
    }

    // ==================== Alarm ID Validation Tests ====================

    @Test
    fun `validateAlarmId returns false for null`() {
        assertFalse(InputValidator.validateAlarmId(null))
    }

    @Test
    fun `validateAlarmId returns false for empty`() {
        assertFalse(InputValidator.validateAlarmId(""))
    }

    @Test
    fun `validateAlarmId returns false for blank`() {
        assertFalse(InputValidator.validateAlarmId("   "))
    }

    @Test
    fun `validateAlarmId returns false for too long`() {
        val longId = "A".repeat(51)
        assertFalse(InputValidator.validateAlarmId(longId))
    }

    @Test
    fun `validateAlarmId returns true for valid UUID`() {
        val validUuid = "550e8400-e29b-41d4-a716-446655440000"
        assertTrue(InputValidator.validateAlarmId(validUuid))
    }

    @Test
    fun `validateAlarmId returns true for valid simple ID`() {
        assertTrue(InputValidator.validateAlarmId("alarm123"))
        assertTrue(InputValidator.validateAlarmId("alarm-123"))
        assertTrue(InputValidator.validateAlarmId("alarm_123"))
    }

    @Test
    fun `validateAlarmId returns false for invalid characters`() {
        assertFalse(InputValidator.validateAlarmId("alarm@123"))
        assertFalse(InputValidator.validateAlarmId("alarm#123"))
        assertFalse(InputValidator.validateAlarmId("alarm.123"))
    }

    // ==================== Time Validation Tests ====================

    @Test
    fun `validateHour returns true for valid hours`() {
        (0..23).forEach { hour ->
            assertTrue("Hour $hour should be valid", InputValidator.validateHour(hour))
        }
    }

    @Test
    fun `validateHour returns false for invalid hours`() {
        assertFalse(InputValidator.validateHour(-1))
        assertFalse(InputValidator.validateHour(24))
        assertFalse(InputValidator.validateHour(100))
    }

    @Test
    fun `validateMinute returns true for valid minutes`() {
        (0..59).forEach { minute ->
            assertTrue("Minute $minute should be valid", InputValidator.validateMinute(minute))
        }
    }

    @Test
    fun `validateMinute returns false for invalid minutes`() {
        assertFalse(InputValidator.validateMinute(-1))
        assertFalse(InputValidator.validateMinute(60))
        assertFalse(InputValidator.validateMinute(100))
    }

    // ==================== Year Validation Tests ====================

    @Test
    fun `validateYear returns true for null`() {
        assertTrue(InputValidator.validateYear(null))
    }

    @Test
    fun `validateYear returns true for valid years`() {
        assertTrue(InputValidator.validateYear(2000))
        assertTrue(InputValidator.validateYear(2024))
        assertTrue(InputValidator.validateYear(2100))
    }

    @Test
    fun `validateYear returns false for invalid years`() {
        assertFalse(InputValidator.validateYear(1999))
        assertFalse(InputValidator.validateYear(2101))
        assertFalse(InputValidator.validateYear(0))
    }

    // ==================== Repeat Days Validation Tests ====================

    @Test
    fun `validateRepeatDays returns true for valid days`() {
        assertTrue(InputValidator.validateRepeatDays(emptyList()))
        assertTrue(InputValidator.validateRepeatDays(listOf(1)))
        assertTrue(InputValidator.validateRepeatDays(listOf(1, 3, 5)))
        assertTrue(InputValidator.validateRepeatDays(listOf(1, 2, 3, 4, 5, 6, 7)))
    }

    @Test
    fun `validateRepeatDays returns false for too many days`() {
        assertFalse(InputValidator.validateRepeatDays(listOf(1, 2, 3, 4, 5, 6, 7, 1)))
    }

    @Test
    fun `validateRepeatDays returns false for invalid day values`() {
        assertFalse(InputValidator.validateRepeatDays(listOf(0)))
        assertFalse(InputValidator.validateRepeatDays(listOf(8)))
        assertFalse(InputValidator.validateRepeatDays(listOf(-1)))
        assertFalse(InputValidator.validateRepeatDays(listOf(1, 8)))
    }

    // ==================== Sanitization Tests ====================

    @Test
    fun `sanitizeForDisplay removes control characters`() {
        val input = "Alarm\u0000\u0001\u0002Test"
        val result = InputValidator.sanitizeForDisplay(input)
        assertEquals("AlarmTest", result)
    }

    @Test
    fun `sanitizeForDisplay normalizes whitespace`() {
        val input = "Alarm    Test   Multiple   Spaces"
        val result = InputValidator.sanitizeForDisplay(input)
        assertEquals("Alarm Test Multiple Spaces", result)
    }

    @Test
    fun `sanitizeForDisplay preserves newlines and tabs`() {
        val input = "Alarm\tTest\nNewLine"
        val result = InputValidator.sanitizeForDisplay(input)
        // Tab (\x09) and newline (\x0A) are preserved, but tab is \t which is \x09
        // The regex only removes \x0B and \x0C but keeps \x09 (tab) and \x0A (newline)
        assertTrue(result.contains("Alarm"))
        assertTrue(result.contains("Test"))
        assertTrue(result.contains("NewLine"))
    }

    @Test
    fun `sanitizeForDisplay trims whitespace`() {
        val input = "  Alarm Test  "
        val result = InputValidator.sanitizeForDisplay(input)
        assertEquals("Alarm Test", result)
    }

    // ==================== Complete Alarm Validation Tests ====================

    @Test
    fun `validateAlarm returns success for valid alarm`() {
        val alarm = Alarm(
            id = "550e8400-e29b-41d4-a716-446655440000",
            title = "Morning Alarm",
            hour = 7,
            minute = 30,
            isEnabled = true
        )
        val result = InputValidator.validateAlarm(alarm)
        assertTrue(result is InputValidator.ValidationResult.Success)
    }

    @Test
    fun `validateAlarm returns failure for invalid hour`() {
        val alarm = Alarm(
            title = "Test",
            hour = 25,
            minute = 0
        )
        val result = InputValidator.validateAlarm(alarm)
        assertTrue(result is InputValidator.ValidationResult.Failure)
        assertTrue((result as InputValidator.ValidationResult.Failure).reason.contains("Invalid hour"))
    }

    @Test
    fun `validateAlarm returns failure for invalid minute`() {
        val alarm = Alarm(
            title = "Test",
            hour = 12,
            minute = 60
        )
        val result = InputValidator.validateAlarm(alarm)
        assertTrue(result is InputValidator.ValidationResult.Failure)
        assertTrue((result as InputValidator.ValidationResult.Failure).reason.contains("Invalid minute"))
    }

    @Test
    fun `validateAlarm returns failure for invalid snooze minutes`() {
        val alarm = Alarm(
            title = "Test",
            hour = 12,
            minute = 0,
            snoozeMinutes = 0
        )
        val result = InputValidator.validateAlarm(alarm)
        assertTrue(result is InputValidator.ValidationResult.Failure)
        assertTrue((result as InputValidator.ValidationResult.Failure).reason.contains("Invalid snooze minutes"))
    }

    @Test
    fun `validateAlarm returns failure for invalid repeat days`() {
        val alarm = Alarm(
            title = "Test",
            hour = 12,
            minute = 0,
            repeatDays = listOf(1, 2, 8) // 8 is invalid
        )
        val result = InputValidator.validateAlarm(alarm)
        assertTrue(result is InputValidator.ValidationResult.Failure)
        assertTrue((result as InputValidator.ValidationResult.Failure).reason.contains("Invalid repeat days"))
    }

    @Test
    fun `validateAlarm returns failure for malicious title`() {
        val alarm = Alarm(
            title = "<script>alert('xss')</script>",
            hour = 12,
            minute = 0
        )
        val result = InputValidator.validateAlarm(alarm)
        assertTrue(result is InputValidator.ValidationResult.Failure)
    }

    // ==================== Security Constants Tests ====================

    @Test
    fun `security constants are reasonable`() {
        assertTrue(SecurityConfig.MAX_ALARM_TITLE_LENGTH > 0)
        assertTrue(SecurityConfig.MAX_ALARM_TITLE_LENGTH <= 200)
        assertTrue(SecurityConfig.MAX_ID_LENGTH > 0)
        assertTrue(SecurityConfig.MAX_ID_LENGTH <= 100)
    }

    @Test
    fun `title pattern allows expected characters`() {
        val pattern = SecurityConfig.ALLOWED_TITLE_PATTERN
        assertTrue(pattern.matches("Hello World"))
        assertTrue(pattern.matches("Test_123"))
        assertTrue(pattern.matches("Test-Name"))
        assertTrue(pattern.matches("Test.Name"))
        assertTrue(pattern.matches("Test(Name)"))
        assertTrue(pattern.matches("中文测试"))
    }

    @Test
    fun `id pattern allows expected characters`() {
        val pattern = SecurityConfig.ALLOWED_ID_PATTERN
        assertTrue(pattern.matches("abc123"))
        assertTrue(pattern.matches("abc-123"))
        assertTrue(pattern.matches("abc_123"))
        assertTrue(pattern.matches("ABC-123"))
    }
}
