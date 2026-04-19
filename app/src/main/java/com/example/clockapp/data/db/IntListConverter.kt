package com.example.clockapp.data.db

import androidx.room.TypeConverter
import com.example.clockapp.data.security.SecurityConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverter for List<Int>
 * Uses secure Gson configuration for safe JSON serialization
 */
class IntListConverter {
    private val gson: Gson = SecurityConfig.createLenientGson()

    @TypeConverter
    fun fromIntList(list: List<Int>): String {
        return gson.toJson(list)
    }

    @TypeConverter
    fun toIntList(data: String): List<Int> {
        val type = object : TypeToken<List<Int>>() {}.type
        return gson.fromJson(data, type) ?: emptyList()
    }
}
