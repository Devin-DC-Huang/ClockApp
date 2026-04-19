package com.example.clockapp.data.model

import org.junit.Test
import org.junit.Assert.*
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Unit tests for CalendarData model
 */
class CalendarDataTest {

    private fun createDate(year: Int, month: Int, day: Int): LocalDate {
        return LocalDate.of(year, month, day)
    }

    @Test
    fun `getAllWorkDates returns correct workdays for regular week`() {
        // January 2024: 1st is Monday, no holidays
        val calendarData = CalendarData(
            year = 2024,
            holidays = emptyList(),
            workdays = emptyList()
        )

        val workDates = calendarData.getAllWorkDates()

        // Should have approximately 261 workdays in a year (365 - 104 weekends)
        assertTrue(workDates.size in 250..270)

        // All returned dates should be weekdays (Mon-Fri)
        for (date in workDates) {
            val dayOfWeek = date.dayOfWeek
            assertTrue(
                "Date $date should be weekday",
                dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY
            )
        }
    }

    @Test
    fun `getAllWorkDates excludes holidays`() {
        // Create a Monday holiday
        val mondayHoliday = createDate(2024, 1, 1) // New Year's Day 2024 is Monday

        val calendarData = CalendarData(
            year = 2024,
            holidays = listOf(mondayHoliday),
            workdays = emptyList()
        )

        val workDates = calendarData.getAllWorkDates()

        // Verify the holiday is not in work dates
        val isHolidayExcluded = workDates.none { it == mondayHoliday }
        assertTrue("Holiday should be excluded from work dates", isHolidayExcluded)
    }

    @Test
    fun `getAllWorkDates includes makeup workdays on weekends`() {
        // Create a Saturday makeup workday
        val saturdayWorkday = createDate(2024, 2, 3) // This is a Saturday

        val calendarData = CalendarData(
            year = 2024,
            holidays = emptyList(),
            workdays = listOf(saturdayWorkday)
        )

        val workDates = calendarData.getAllWorkDates()

        // Verify the Saturday workday is included
        val isWorkdayIncluded = workDates.any { it == saturdayWorkday }
        assertTrue("Saturday workday should be included", isWorkdayIncluded)
    }

    @Test
    fun `getAllWorkDates returns sorted dates`() {
        val calendarData = CalendarData(
            year = 2024,
            holidays = emptyList(),
            workdays = emptyList()
        )

        val workDates = calendarData.getAllWorkDates()

        // Verify dates are sorted
        for (i in 1 until workDates.size) {
            assertTrue(
                "Work dates should be sorted",
                !workDates[i].isBefore(workDates[i - 1])
            )
        }
    }

    @Test
    fun `getAllWorkDates handles holiday on weekend correctly`() {
        // Sunday holiday - should not affect workdays count
        val sundayHoliday = createDate(2024, 1, 7) // Sunday

        val calendarData = CalendarData(
            year = 2024,
            holidays = listOf(sundayHoliday),
            workdays = emptyList()
        )

        val workDates = calendarData.getAllWorkDates()

        // Sunday is already not a workday, so holiday on Sunday shouldn't change anything
        var expectedWorkdays = 0
        var currentDate = createDate(2024, 1, 1)
        val yearEnd = createDate(2024, 12, 31)

        // Count manually
        while (!currentDate.isAfter(yearEnd)) {
            val dayOfWeek = currentDate.dayOfWeek
            if (dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY) {
                expectedWorkdays++
            }
            currentDate = currentDate.plusDays(1)
        }

        assertEquals(expectedWorkdays, workDates.size)
    }

    @Test
    fun `getAllWorkDates handles leap year correctly`() {
        val calendarData = CalendarData(
            year = 2024, // Leap year
            holidays = emptyList(),
            workdays = emptyList()
        )

        val workDates = calendarData.getAllWorkDates()

        // 2024 is a leap year with 366 days
        // Expected workdays: approximately 366 - 104 weekend days = 262 (if no holidays)
        // Allow some variance due to year starting/ending on different weekdays
        assertTrue("Expected around 262 workdays but got ${workDates.size}",
            workDates.size in 261..263)
    }

    @Test
    fun `getAllWorkDates handles non-leap year correctly`() {
        val calendarData = CalendarData(
            year = 2023, // Non-leap year
            holidays = emptyList(),
            workdays = emptyList()
        )

        val workDates = calendarData.getAllWorkDates()

        // 2023 has 365 days
        // Expected workdays: approximately 365 - 104 weekend days = 261 (if no holidays)
        // Allow some variance due to year starting/ending on different weekdays
        assertTrue("Expected around 261 workdays but got ${workDates.size}",
            workDates.size in 260..262)
    }

    @Test
    fun `getAllWorkDates with both holidays and makeup days`() {
        // Scenario: Holiday on Monday, makeup on Saturday
        val mondayHoliday = createDate(2024, 1, 1) // Monday
        val saturdayWorkday = createDate(2024, 1, 6) // Saturday

        val calendarData = CalendarData(
            year = 2024,
            holidays = listOf(mondayHoliday),
            workdays = listOf(saturdayWorkday)
        )

        val workDates = calendarData.getAllWorkDates()

        // Verify Monday is excluded
        val isMondayExcluded = workDates.none { it == mondayHoliday }
        assertTrue("Monday holiday should be excluded", isMondayExcluded)

        // Verify Saturday is included
        val isSaturdayIncluded = workDates.any { it == saturdayWorkday }
        assertTrue("Saturday makeup day should be included", isSaturdayIncluded)
    }

}
