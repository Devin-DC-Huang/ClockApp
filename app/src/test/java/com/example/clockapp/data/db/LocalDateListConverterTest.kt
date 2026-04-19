package com.example.clockapp.data.db

import org.junit.Test
import org.junit.Assert.*
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit tests for LocalDateListConverter
 */
class LocalDateListConverterTest {

    private val converter = LocalDateListConverter()

    @Test
    fun `fromLocalDateList converts empty list to empty json array`() {
        val result = converter.fromLocalDateList(emptyList())
        assertEquals("[]", result)
    }

    @Test
    fun `fromLocalDateList converts date list to json array of timestamps`() {
        val date1 = LocalDate.of(2021, 1, 1)
        val date2 = LocalDate.of(2021, 1, 2)

        val result = converter.fromLocalDateList(listOf(date1, date2))
        val expected1 = date1.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val expected2 = date2.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals("[$expected1,$expected2]", result)
    }

    @Test
    fun `toLocalDateList converts empty json array to empty list`() {
        val result = converter.toLocalDateList("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toLocalDateList converts json array to date list`() {
        val date1 = LocalDate.of(2021, 1, 1)
        val date2 = LocalDate.of(2021, 1, 2)
        val ts1 = date1.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val ts2 = date2.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val jsonString = "[$ts1,$ts2]"

        val result = converter.toLocalDateList(jsonString)
        assertEquals(2, result.size)
        assertEquals(date1, result[0])
        assertEquals(date2, result[1])
    }

    @Test
    fun `toLocalDateList handles single date`() {
        val date = LocalDate.of(2021, 1, 1)
        val ts = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        
        val result = converter.toLocalDateList("[$ts]")
        assertEquals(1, result.size)
        assertEquals(date, result[0])
    }

    @Test
    fun `round trip conversion preserves data`() {
        val originalDates = listOf(
            LocalDate.of(2021, 1, 1),
            LocalDate.of(2021, 1, 2),
            LocalDate.of(2021, 1, 3)
        )

        val stringValue = converter.fromLocalDateList(originalDates)
        val convertedDates = converter.toLocalDateList(stringValue)

        assertEquals(originalDates.size, convertedDates.size)
        for (i in originalDates.indices) {
            assertEquals(originalDates[i], convertedDates[i])
        }
    }

    @Test
    fun `fromLocalDateList handles single date`() {
        val date = LocalDate.of(2021, 1, 1)
        val result = converter.fromLocalDateList(listOf(date))
        val ts = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        assertEquals("[$ts]", result)
    }
}
