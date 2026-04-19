package com.example.clockapp.util

import org.junit.Test
import org.junit.Assert.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests for DateUtils
 */
class DateUtilsTest {

    @Test
    fun `parseDate returns LocalDate for valid date string`() {
        val result = DateUtils.parseDate("2024-01-15")
        assertNotNull(result)
        assertEquals(2024, result!!.year)
        assertEquals(1, result.monthValue)
        assertEquals(15, result.dayOfMonth)
    }

    @Test
    fun `parseDate returns null for invalid date string`() {
        assertNull(DateUtils.parseDate("invalid"))
        assertNull(DateUtils.parseDate(""))
        assertNull(DateUtils.parseDate("2024/01/15"))
    }

    @Test
    fun `formatDate returns correct string format`() {
        val date = LocalDate.of(2024, 1, 15)
        assertEquals("2024-01-15", DateUtils.formatDate(date))
    }

    @Test
    fun `formatYearMonth returns correct Chinese format`() {
        val date = LocalDate.of(2024, 1, 15)
        assertEquals("2024年1月", DateUtils.formatYearMonth(date))
    }

    @Test
    fun `formatDay returns day of month`() {
        val date = LocalDate.of(2024, 1, 15)
        assertEquals("15", DateUtils.formatDay(date))
    }

    @Test
    fun `formatTime returns correct time format`() {
        val time = LocalTime.of(14, 30)
        assertEquals("14:30", DateUtils.formatTime(time))
    }

    @Test
    fun `formatTime for LocalDateTime returns correct format`() {
        val dateTime = LocalDateTime.of(2024, 1, 15, 14, 30)
        assertEquals("14:30", DateUtils.formatTime(dateTime))
    }

    @Test
    fun `isSameDay for LocalDate returns true for same day`() {
        val date1 = LocalDate.of(2024, 1, 15)
        val date2 = LocalDate.of(2024, 1, 15)
        assertTrue(DateUtils.isSameDay(date1, date2))
    }

    @Test
    fun `isSameDay for LocalDate returns false for different days`() {
        val date1 = LocalDate.of(2024, 1, 15)
        val date2 = LocalDate.of(2024, 1, 16)
        assertFalse(DateUtils.isSameDay(date1, date2))
    }

    @Test
    fun `isSameDay for Instant returns true for same day`() {
        val instant1 = Instant.parse("2024-01-15T10:00:00Z")
        val instant2 = Instant.parse("2024-01-15T14:00:00Z")
        assertTrue(DateUtils.isSameDay(instant1, instant2))
    }

    @Test
    fun `isSameDay for Instant returns false for different days`() {
        val instant1 = Instant.parse("2024-01-15T10:00:00Z")
        val instant2 = Instant.parse("2024-01-16T10:00:00Z")
        assertFalse(DateUtils.isSameDay(instant1, instant2))
    }

    @Test
    fun `addDays adds correct number of days to LocalDate`() {
        val date = LocalDate.of(2024, 1, 15)
        val result = DateUtils.addDays(date, 5)
        assertEquals(LocalDate.of(2024, 1, 20), result)
    }

    @Test
    fun `addDays subtracts days when negative`() {
        val date = LocalDate.of(2024, 1, 15)
        val result = DateUtils.addDays(date, -5)
        assertEquals(LocalDate.of(2024, 1, 10), result)
    }

    @Test
    fun `addDays adds correct number of days to LocalDateTime`() {
        val dateTime = LocalDateTime.of(2024, 1, 15, 10, 30)
        val result = DateUtils.addDays(dateTime, 5)
        assertEquals(LocalDateTime.of(2024, 1, 20, 10, 30), result)
    }

    @Test
    fun `isBeforeToday returns true for past date`() {
        val yesterday = LocalDate.now().minusDays(1)
        assertTrue(DateUtils.isBeforeToday(yesterday))
    }

    @Test
    fun `isBeforeToday returns false for today`() {
        val today = LocalDate.now()
        assertFalse(DateUtils.isBeforeToday(today))
    }

    @Test
    fun `isBeforeToday returns false for future date`() {
        val tomorrow = LocalDate.now().plusDays(1)
        assertFalse(DateUtils.isBeforeToday(tomorrow))
    }

    @Test
    fun `isToday returns true for today`() {
        val today = LocalDate.now()
        assertTrue(DateUtils.isToday(today))
    }

    @Test
    fun `isToday returns false for other dates`() {
        val yesterday = LocalDate.now().minusDays(1)
        val tomorrow = LocalDate.now().plusDays(1)
        assertFalse(DateUtils.isToday(yesterday))
        assertFalse(DateUtils.isToday(tomorrow))
    }

    @Test
    fun `daysBetween calculates correct difference`() {
        val start = LocalDate.of(2024, 1, 15)
        val end = LocalDate.of(2024, 1, 20)
        assertEquals(5, DateUtils.daysBetween(start, end))
    }

    @Test
    fun `daysBetween returns negative for reversed order`() {
        val start = LocalDate.of(2024, 1, 20)
        val end = LocalDate.of(2024, 1, 15)
        assertEquals(-5, DateUtils.daysBetween(start, end))
    }

    @Test
    fun `daysBetweenInclusive includes both start and end`() {
        val start = LocalDate.of(2024, 1, 15)
        val end = LocalDate.of(2024, 1, 20)
        assertEquals(6, DateUtils.daysBetweenInclusive(start, end))
    }

    @Test
    fun `getDayOfWeek returns correct value`() {
        // 2024-01-15 is Monday (1)
        val monday = LocalDate.of(2024, 1, 15)
        assertEquals(1, DateUtils.getDayOfWeek(monday))

        // 2024-01-21 is Sunday (7)
        val sunday = LocalDate.of(2024, 1, 21)
        assertEquals(7, DateUtils.getDayOfWeek(sunday))
    }

    @Test
    fun `toInstant converts LocalDate correctly`() {
        val date = LocalDate.of(2024, 1, 15)
        val instant = DateUtils.toInstant(date)
        val convertedBack = DateUtils.toLocalDate(instant)
        assertEquals(date, convertedBack)
    }

    @Test
    fun `of creates LocalDateTime with correct values`() {
        val result = DateUtils.of(2024, 1, 15, 14, 30)
        assertEquals(2024, result.year)
        assertEquals(1, result.monthValue)
        assertEquals(15, result.dayOfMonth)
        assertEquals(14, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun `of creates LocalDate with correct values`() {
        val result = DateUtils.of(2024, 1, 15)
        assertEquals(2024, result.year)
        assertEquals(1, result.monthValue)
        assertEquals(15, result.dayOfMonth)
    }

    @Test
    fun `startOfDay returns midnight time`() {
        val date = LocalDate.of(2024, 1, 15)
        val result = DateUtils.startOfDay(date)
        assertEquals(LocalDateTime.of(2024, 1, 15, 0, 0, 0), result)
    }

    @Test
    fun `getDateOnly extracts date from LocalDateTime`() {
        val dateTime = LocalDateTime.of(2024, 1, 15, 14, 30)
        val result = DateUtils.getDateOnly(dateTime)
        assertEquals(LocalDate.of(2024, 1, 15), result)
    }

    // ==================== Boundary Value Tests ====================

}
