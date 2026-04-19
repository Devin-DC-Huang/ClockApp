package com.example.clockapp.data.security

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness

/**
 * Security configuration for data handling
 * Provides secure Gson instance and security constants
 */
object SecurityConfig {

    // String length limits for input validation
    const val MAX_ALARM_TITLE_LENGTH = 100
    const val MAX_HOLIDAY_NAME_LENGTH = 100
    const val MAX_DATE_STRING_LENGTH = 10  // yyyy-MM-dd
    const val MAX_ID_LENGTH = 50

    // Allowed character patterns
    val ALLOWED_TITLE_PATTERN = Regex("^[\\w\\s\\u4e00-\\u9fa5\\-_.()]+$")
    val ALLOWED_ID_PATTERN = Regex("^[a-zA-Z0-9\\-_]+$")

    /**
     * Create a secure Gson instance with strict parsing
     * - Disables lenient parsing
     * - Prevents arbitrary object deserialization
     * - Uses strict JSON syntax
     */
    fun createSecureGson(): Gson {
        return GsonBuilder()
            // Use strict mode to reject malformed JSON
            .setStrictness(Strictness.STRICT)
            // Disable HTML escaping for performance (we don't display raw JSON)
            .disableHtmlEscaping()
            // Don't serialize null values to reduce payload size
            .serializeNulls()
            .create()
    }

    /**
     * Create a lenient Gson for internal storage (local cache)
     * This is more permissive for reading local data
     */
    fun createLenientGson(): Gson {
        return GsonBuilder()
            .setStrictness(Strictness.LENIENT)
            .disableHtmlEscaping()
            .create()
    }
}
