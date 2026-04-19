package com.example.clockapp.data.db

import androidx.room.TypeConverter
import com.example.clockapp.data.security.SecurityConfig
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Room TypeConverter for java.time.LocalDate list
 * Uses secure Gson configuration for safe JSON serialization
 */
class LocalDateListConverter {
    private val gson: Gson = SecurityConfig.createLenientGson()

    companion object {
        /**
         * Convert LocalDate to epoch milliseconds
         */
        fun localDateToMillis(date: LocalDate?): Long? {
            return date?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        }

        /**
         * Convert epoch milliseconds to LocalDate
         */
        fun millisToLocalDate(timestamp: Long?): LocalDate? {
            return timestamp?.let {
                Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            }
        }
    }

    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = localDateToMillis(date)

    @TypeConverter
    fun toLocalDate(timestamp: Long?): LocalDate? = millisToLocalDate(timestamp)

    @TypeConverter
    fun fromLocalDateList(dates: List<LocalDate>): String {
        val timestamps = dates.map { localDateToMillis(it)!! }
        return gson.toJson(timestamps)
    }

    @TypeConverter
    fun toLocalDateList(data: String): List<LocalDate> {
        val type = object : TypeToken<List<Long>>() {}.type
        val timestamps: List<Long> = gson.fromJson(data, type) ?: emptyList()
        return timestamps.map { millisToLocalDate(it)!! }
    }
}

/**
 * Room TypeConverter for java.time.Instant
 */
class InstantConverter {
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }

    @TypeConverter
    fun toInstant(timestamp: Long?): Instant? {
        return timestamp?.let { Instant.ofEpochMilli(it) }
    }
}

/**
 * Room TypeConverter for SpecialAlarmMode enum
 */
class SpecialAlarmModeConverter {
    @TypeConverter
    fun fromSpecialAlarmMode(mode: com.example.clockapp.data.model.SpecialAlarmMode?): String? {
        return mode?.name
    }

    @TypeConverter
    fun toSpecialAlarmMode(modeName: String?): com.example.clockapp.data.model.SpecialAlarmMode? {
        return modeName?.let {
            try {
                com.example.clockapp.data.model.SpecialAlarmMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                // Fallback to default if value not found (e.g., after app update with new enum values)
                com.example.clockapp.data.model.SpecialAlarmMode.ALL_WORKDAYS
            }
        }
    }
}
