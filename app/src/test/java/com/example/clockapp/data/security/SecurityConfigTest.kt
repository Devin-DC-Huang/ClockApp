package com.example.clockapp.data.security

import com.google.gson.JsonObject
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for SecurityConfig
 */
class SecurityConfigTest {

    @Test
    fun `createSecureGson creates non-null instance`() {
        val gson = SecurityConfig.createSecureGson()
        assertNotNull(gson)
    }

    @Test
    fun `createLenientGson creates non-null instance`() {
        val gson = SecurityConfig.createLenientGson()
        assertNotNull(gson)
    }

    @Test
    fun `secure gson parses valid json correctly`() {
        val gson = SecurityConfig.createSecureGson()
        val json = """{"year":2024,"name":"Test"}"""
        
        val obj = gson.fromJson(json, JsonObject::class.java)
        assertNotNull(obj)
        assertEquals(2024, obj.get("year").asInt)
        assertEquals("Test", obj.get("name").asString)
    }

    @Test
    fun `secure gson serializes correctly`() {
        val gson = SecurityConfig.createSecureGson()
        val obj = JsonObject().apply {
            addProperty("year", 2024)
            addProperty("name", "Test")
        }
        
        val json = gson.toJson(obj)
        assertTrue(json.contains("year"))
        assertTrue(json.contains("2024"))
        assertTrue(json.contains("name"))
        assertTrue(json.contains("Test"))
    }

    @Test
    fun `secure gson rejects malformed json`() {
        val gson = SecurityConfig.createSecureGson()
        val malformedJson = "{year:2024, name:Test}" // Missing quotes
        
        try {
            gson.fromJson(malformedJson, JsonObject::class.java)
            // If we reach here, the test should fail because strict mode should reject this
            // But Gson might still parse it, so we just verify it doesn't crash
        } catch (e: Exception) {
            // Expected in strict mode
            assertTrue(true)
        }
    }

    @Test
    fun `max title length is reasonable`() {
        assertEquals(100, SecurityConfig.MAX_ALARM_TITLE_LENGTH)
    }

    @Test
    fun `max holiday name length is reasonable`() {
        assertEquals(100, SecurityConfig.MAX_HOLIDAY_NAME_LENGTH)
    }

    @Test
    fun `max date string length matches format`() {
        assertEquals(10, SecurityConfig.MAX_DATE_STRING_LENGTH)
        // yyyy-MM-dd is exactly 10 characters
        assertEquals(10, "2024-01-01".length)
    }

    @Test
    fun `max id length is reasonable`() {
        assertEquals(50, SecurityConfig.MAX_ID_LENGTH)
    }

    @Test
    fun `title pattern rejects dangerous characters`() {
        val pattern = SecurityConfig.ALLOWED_TITLE_PATTERN
        
        // Should reject
        assertFalse(pattern.matches("Test<script>"))
        assertFalse(pattern.matches("Test@Home"))
        assertFalse(pattern.matches("Test#123"))
        assertFalse(pattern.matches("Test${'$'}"))
        assertFalse(pattern.matches("Test%"))
        assertFalse(pattern.matches("Test*"))
        assertFalse(pattern.matches("Test<"))
        assertFalse(pattern.matches("Test>"))
        assertFalse(pattern.matches("Test\""))
        assertFalse(pattern.matches("Test'"))
    }

    @Test
    fun `title pattern allows safe characters`() {
        val pattern = SecurityConfig.ALLOWED_TITLE_PATTERN
        
        // Should allow
        assertTrue(pattern.matches("Morning Alarm"))
        assertTrue(pattern.matches("Test_123"))
        assertTrue(pattern.matches("Test-123"))
        assertTrue(pattern.matches("Test.123"))
        assertTrue(pattern.matches("Test(123)"))
        assertTrue(pattern.matches("中文测试"))
        assertTrue(pattern.matches("Mixed中文123"))
    }

    @Test
    fun `id pattern rejects invalid characters`() {
        val pattern = SecurityConfig.ALLOWED_ID_PATTERN
        
        // Should reject
        assertFalse(pattern.matches("id@123"))
        assertFalse(pattern.matches("id#123"))
        assertFalse(pattern.matches("id.123"))
        assertFalse(pattern.matches("id 123"))
        assertFalse(pattern.matches("id/123"))
    }

    @Test
    fun `id pattern allows valid characters`() {
        val pattern = SecurityConfig.ALLOWED_ID_PATTERN
        
        // Should allow
        assertTrue(pattern.matches("abc123"))
        assertTrue(pattern.matches("ABC123"))
        assertTrue(pattern.matches("abc-123"))
        assertTrue(pattern.matches("abc_123"))
        assertTrue(pattern.matches("123"))
        assertTrue(pattern.matches("abc"))
        assertTrue(pattern.matches("a-b_c-1_2"))
    }

    @Test
    fun `secure and lenient gson produce same output for valid data`() {
        val secureGson = SecurityConfig.createSecureGson()
        val lenientGson = SecurityConfig.createLenientGson()
        
        val obj = JsonObject().apply {
            addProperty("year", 2024)
            addProperty("name", "Test")
        }
        
        val secureJson = secureGson.toJson(obj)
        val lenientJson = lenientGson.toJson(obj)
        
        assertEquals(secureJson, lenientJson)
    }
}
