package com.example.clockapp.data.validator

import com.example.clockapp.data.api.HolidayDay
import com.example.clockapp.data.api.HolidayResponse
import com.example.clockapp.data.model.CalendarData
import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate

/**
 * Unit tests for HolidayDataValidator
 */
class HolidayDataValidatorTest {

    // ==================== Schema Validation Tests ====================

    @Test
    fun `validate returns failure for null response`() {
        val result = HolidayDataValidator.validate(null, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertEquals("Response body is null", (result as HolidayDataValidator.ValidationResult.Failure).reason)
    }

    @Test
    fun `validate returns failure for zero year`() {
        val response = HolidayResponse(year = 0, days = emptyList())
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("Year field"))
    }

    @Test
    fun `validate returns failure for null days`() {
        val response = HolidayResponse(year = 2024, days = null)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertEquals("Days list is null", (result as HolidayDataValidator.ValidationResult.Failure).reason)
    }

    @Test
    fun `validate returns failure for too many days`() {
        val days = (1..400).map {
            HolidayDay(
                name = "Day $it",
                date = String.format("2024-%02d-%02d", (it % 12) + 1, (it % 28) + 1),
                isOffDay = true
            )
        }
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("exceeds maximum"))
    }

    @Test
    fun `validate returns failure for blank name`() {
        val days = listOf(
            HolidayDay(name = "", date = "2024-01-01", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("blank name"))
    }

    @Test
    fun `validate returns failure for name too long`() {
        val days = listOf(
            HolidayDay(name = "A".repeat(101), date = "2024-01-01", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("exceeding 100 characters"))
    }

    @Test
    fun `validate returns failure for blank date`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("blank date"))
    }

    @Test
    fun `validate returns failure for invalid date format`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "2024/01/01", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("invalid date format"))
    }

    // ==================== Business Logic Validation Tests ====================

    @Test
    fun `validate returns failure for year mismatch`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "2024-01-01", isOffDay = true)
        )
        val response = HolidayResponse(year = 2025, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("Year mismatch"))
    }

    @Test
    fun `validate returns failure for year too small`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "1999-01-01", isOffDay = true)
        )
        val response = HolidayResponse(year = 1999, days = days)
        val result = HolidayDataValidator.validate(response, 1999)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("out of valid range"))
    }

    @Test
    fun `validate returns failure for year too large`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "2101-01-01", isOffDay = true)
        )
        val response = HolidayResponse(year = 2101, days = days)
        val result = HolidayDataValidator.validate(response, 2101)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("out of valid range"))
    }

    @Test
    fun `validate returns failure for date outside expected year`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "2023-12-31", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("outside expected year"))
    }

    @Test
    fun `validate returns failure for duplicate dates`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "2024-01-01", isOffDay = true),
            HolidayDay(name = "Duplicate", date = "2024-01-01", isOffDay = false)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
        assertTrue((result as HolidayDataValidator.ValidationResult.Failure).reason.contains("Duplicate date"))
    }

    @Test
    fun `validate returns failure for date outside year range`() {
        val days = listOf(
            HolidayDay(name = "Invalid", date = "2024-13-01", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Failure)
    }

    // ==================== Success Cases ====================

    @Test
    fun `validate returns success for valid data`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "2024-01-01", isOffDay = true),
            HolidayDay(name = "Workday", date = "2024-02-04", isOffDay = false) // Sunday workday
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Success)
    }

    @Test
    fun `validate returns success for empty days list`() {
        val response = HolidayResponse(year = 2024, days = emptyList())
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Success)
    }

    @Test
    fun `validate returns success for workday on weekend`() {
        // isOffDay=false on Saturday - this is a makeup workday, should pass
        val days = listOf(
            HolidayDay(name = "Makeup Workday", date = "2024-02-03", isOffDay = false) // Saturday
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Success)
    }

    @Test
    fun `validate returns success for holiday on weekday`() {
        // isOffDay=true on Monday - this is a holiday, should pass
        val days = listOf(
            HolidayDay(name = "Holiday", date = "2024-01-01", isOffDay = true) // Monday
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Success)
    }

    @Test
    fun `validate handles boundary years`() {
        val days = listOf(
            HolidayDay(name = "New Year", date = "2000-01-01", isOffDay = true)
        )
        val response = HolidayResponse(year = 2000, days = days)
        val result = HolidayDataValidator.validate(response, 2000)
        assertTrue(result is HolidayDataValidator.ValidationResult.Success)
    }

    // ==================== CalendarData Validation Tests ====================

    @Test
    fun `validateCalendarData returns false for year mismatch`() {
        val calendarData = CalendarData(
            year = 2025,
            holidays = listOf(LocalDate.of(2024, 1, 1)),
            workdays = emptyList()
        )
        val result = HolidayDataValidator.validateCalendarData(calendarData, 2024)
        assertFalse(result)
    }

    @Test
    fun `validateCalendarData returns false for overlapping dates`() {
        val date = LocalDate.of(2024, 1, 1)
        val calendarData = CalendarData(
            year = 2024,
            holidays = listOf(date),
            workdays = listOf(date)
        )
        val result = HolidayDataValidator.validateCalendarData(calendarData, 2024)
        assertFalse(result)
    }

    @Test
    fun `validateCalendarData returns false for holiday outside year`() {
        val calendarData = CalendarData(
            year = 2024,
            holidays = listOf(LocalDate.of(2023, 12, 31)),
            workdays = emptyList()
        )
        val result = HolidayDataValidator.validateCalendarData(calendarData, 2024)
        assertFalse(result)
    }

    @Test
    fun `validateCalendarData returns false for workday outside year`() {
        val calendarData = CalendarData(
            year = 2024,
            holidays = emptyList(),
            workdays = listOf(LocalDate.of(2025, 1, 1))
        )
        val result = HolidayDataValidator.validateCalendarData(calendarData, 2024)
        assertFalse(result)
    }

    @Test
    fun `validateCalendarData returns true for valid data`() {
        val calendarData = CalendarData(
            year = 2024,
            holidays = listOf(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 5, 1)),
            workdays = listOf(LocalDate.of(2024, 2, 4), LocalDate.of(2024, 2, 18))
        )
        val result = HolidayDataValidator.validateCalendarData(calendarData, 2024)
        assertTrue(result)
    }

    @Test
    fun `validateCalendarData returns true for empty data`() {
        val calendarData = CalendarData(
            year = 2024,
            holidays = emptyList(),
            workdays = emptyList()
        )
        val result = HolidayDataValidator.validateCalendarData(calendarData, 2024)
        assertTrue(result)
    }

    // ==================== Edge Cases ====================

    @Test
    fun `validate handles leap year February 29`() {
        val days = listOf(
            HolidayDay(name = "Leap Day", date = "2024-02-29", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Success)
    }

    @Test
    fun `validate handles year boundary dates`() {
        val days = listOf(
            HolidayDay(name = "First Day", date = "2024-01-01", isOffDay = true),
            HolidayDay(name = "Last Day", date = "2024-12-31", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Success)
    }

    @Test
    fun `validate handles unicode characters in name`() {
        val days = listOf(
            HolidayDay(name = "春节", date = "2024-02-10", isOffDay = true),
            HolidayDay(name = "元旦节", date = "2024-01-01", isOffDay = true)
        )
        val response = HolidayResponse(year = 2024, days = days)
        val result = HolidayDataValidator.validate(response, 2024)
        assertTrue(result is HolidayDataValidator.ValidationResult.Success)
    }
}
