package com.example.clockapp.data.db

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for IntListConverter
 */
class IntListConverterTest {

    private val converter = IntListConverter()

    @Test
    fun `fromIntList converts empty list to empty json array`() {
        val result = converter.fromIntList(emptyList())
        assertEquals("[]", result)
    }

    @Test
    fun `fromIntList converts single element list`() {
        val result = converter.fromIntList(listOf(1))
        assertEquals("[1]", result)
    }

    @Test
    fun `fromIntList converts multiple elements list`() {
        val result = converter.fromIntList(listOf(1, 2, 3, 4, 5))
        assertEquals("[1,2,3,4,5]", result)
    }

    @Test
    fun `toIntList converts empty json array to empty list`() {
        val result = converter.toIntList("[]")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `toIntList converts json array to int list`() {
        val result = converter.toIntList("[1,2,3]")
        assertEquals(listOf(1, 2, 3), result)
    }

    @Test
    fun `toIntList converts single element json array`() {
        val result = converter.toIntList("[5]")
        assertEquals(listOf(5), result)
    }

    @Test
    fun `toIntList handles all week days`() {
        val result = converter.toIntList("[1,2,3,4,5,6,7]")
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), result)
    }

    @Test(expected = Exception::class)
    fun `toIntList throws exception for invalid json`() {
        // Gson throws JsonSyntaxException for invalid JSON
        converter.toIntList("invalid")
    }

    @Test
    fun `toIntList returns empty list for null json`() {
        // Gson returns null for "null" string, converter returns emptyList
        val result = converter.toIntList("null")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `round trip conversion preserves data`() {
        val originalList = listOf(1, 3, 5, 7)
        
        val stringValue = converter.fromIntList(originalList)
        val convertedList = converter.toIntList(stringValue)
        
        assertEquals(originalList.size, convertedList.size)
        assertEquals(originalList, convertedList)
    }

    @Test
    fun `round trip conversion for empty list`() {
        val originalList = emptyList<Int>()
        
        val stringValue = converter.fromIntList(originalList)
        val convertedList = converter.toIntList(stringValue)
        
        assertTrue(convertedList.isEmpty())
    }

    @Test
    fun `fromIntList handles negative numbers`() {
        val result = converter.fromIntList(listOf(-1, 0, 1))
        assertEquals("[-1,0,1]", result)
    }

    @Test
    fun `toIntList handles negative numbers`() {
        val result = converter.toIntList("[-1,0,1]")
        assertEquals(listOf(-1, 0, 1), result)
    }

    @Test
    fun `round trip with negative numbers preserves data`() {
        val originalList = listOf(-7, -1, 0, 1, 7)
        
        val stringValue = converter.fromIntList(originalList)
        val convertedList = converter.toIntList(stringValue)
        
        assertEquals(originalList, convertedList)
    }
}
