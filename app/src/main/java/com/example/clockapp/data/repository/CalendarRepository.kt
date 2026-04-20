package com.example.clockapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.clockapp.data.api.RetrofitClient
import com.example.clockapp.data.db.AlarmDatabase
import com.example.clockapp.data.model.Alarm
import com.example.clockapp.data.model.CalendarData
import com.example.clockapp.data.model.SpecialAlarmMode
import com.example.clockapp.data.security.SecurityConfig
import com.example.clockapp.data.validator.HolidayDataValidator
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Calendar data repository using java.time API
 * Manages local cache and network fetch for holidays
 */
class CalendarRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    // Use secure Gson for parsing untrusted data
    private val gson: Gson = SecurityConfig.createSecureGson()

    companion object {
        private const val TAG = "CalendarRepository"
        private const val PREFS_NAME = "calendar_prefs"
        private const val KEY_DATA_PREFIX = "calendar_data_"
        private const val KEY_NEXT_YEAR_PREFETCH_ATTEMPT = "next_year_prefetch_attempt"
        private const val KEY_NEXT_YEAR_PREFETCH_SUCCESS = "next_year_prefetch_success"
        private const val KEY_DECEMBER_REMIND_SHOWN = "december_remind_shown_"
        private const val KEY_DECEMBER_REMIND_DISABLED = "december_remind_disabled_"
        private const val PREFETCH_START_MONTH = 12  // December
        private const val PREFETCH_START_DAY = 1

        @Volatile
        private var INSTANCE: CalendarRepository? = null

        fun getInstance(context: Context): CalendarRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CalendarRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    /**
     * Get calendar data (local cache first)
     * @throws WorkdayDataException when network fails and no local cache exists
     */
    @Throws(WorkdayDataException::class)
    suspend fun getCalendarData(year: Int): CalendarData {
        // Check local cache first
        val localData = getFromLocal(year)
        if (localData != null) {
            Log.d(TAG, "Using cached data for year: $year")
            return localData
        }

        // Fetch from network
        return fetchFromNetwork(year)
    }

    /**
     * Fetch calendar data from network
     * @throws WorkdayDataException when network request fails
     */
    @Throws(WorkdayDataException::class)
    private suspend fun fetchFromNetwork(year: Int): CalendarData {
        return try {
            Log.d(TAG, "Fetching calendar data for year: $year")
            val response = RetrofitClient.holidayApi.getHolidayData(year)
            Log.d(TAG, "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful}")

            if (response.isSuccessful) {
                val body = response.body()
                Log.d(TAG, "Response body year: ${body?.year}, days count: ${body?.days?.size}")

                // Validate response data
                val validationResult = HolidayDataValidator.validate(body, year)
                when (validationResult) {
                    is HolidayDataValidator.ValidationResult.Failure -> {
                        Log.e(TAG, "Data validation failed: ${validationResult.reason}")
                        throw WorkdayDataException("Data validation failed: ${validationResult.reason}")
                    }
                    is HolidayDataValidator.ValidationResult.Success -> {
                        Log.d(TAG, "Data validation passed")
                    }
                }

                val days = body?.days
                
                // Check if days array is null or empty
                if (days.isNullOrEmpty()) {
                    Log.w(TAG, "Year $year data is empty (no holidays published yet)")
                    throw WorkdayDataException("$year 年节假日数据尚未发布，请使用普通闹钟或等待数据更新")
                }
                
                val holidays = mutableListOf<LocalDate>()
                val workdays = mutableListOf<LocalDate>()

                days.forEach { day ->
                    Log.d(TAG, "Processing: ${day.name} -> date=${day.date}, isOffDay=${day.isOffDay}")
                    val date = parseDate(day.date)
                    if (date != null) {
                        if (day.isOffDay) {
                            // isOffDay=true means holiday (rest day)
                            holidays.add(date)
                        } else {
                            // isOffDay=false means work day (makeup day)
                            workdays.add(date)
                        }
                    }
                }

                Log.d(TAG, "Parsed: ${holidays.size} holidays, ${workdays.size} workdays")

                // Check if parsed data is empty
                if (holidays.isEmpty() && workdays.isEmpty()) {
                    Log.w(TAG, "Year $year parsed data is empty")
                    throw WorkdayDataException("$year 年节假日数据尚未发布，请使用普通闹钟或等待数据更新")
                }

                val calendarData = CalendarData(year, holidays, workdays)

                // Validate converted CalendarData
                if (!HolidayDataValidator.validateCalendarData(calendarData, year)) {
                    throw WorkdayDataException("Calendar data validation failed after conversion")
                }

                saveToLocal(calendarData)
                calendarData
            } else if (response.code() == 404) {
                Log.e(TAG, "Year $year data not found (404)")
                throw WorkdayDataException("$year 年节假日数据尚未发布，请使用普通闹钟或等待数据更新")
            } else {
                Log.e(TAG, "Server error: ${response.code()}, body: ${response.errorBody()?.string()}")
                throw WorkdayDataException("Server error: ${response.code()}")
            }
        } catch (e: WorkdayDataException) {
            throw e
        } catch (e: java.net.UnknownHostException) {
            Log.e(TAG, "UnknownHostException: ${e.message}")
            throw WorkdayDataException("Unable to resolve server address, please check network connection")
        } catch (e: java.net.ConnectException) {
            Log.e(TAG, "ConnectException: ${e.message}")
            throw WorkdayDataException("Unable to connect to server, please check network connection")
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.javaClass.simpleName} - ${e.message}", e)
            throw WorkdayDataException("Network error: ${e.javaClass.simpleName} - ${e.message}")
        }
    }

    /**
     * Parse date string in "yyyy-MM-dd" format
     */
    private fun parseDate(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Exception thrown when workday data cannot be obtained
     */
    class WorkdayDataException(message: String) : Exception(message)

    /**
     * Get from local storage
     * Returns null if no data exists or data is empty (no holidays and no workdays)
     */
    private fun getFromLocal(year: Int): CalendarData? {
        val json = prefs.getString(KEY_DATA_PREFIX + year, null) ?: return null

        return try {
            val type = object : TypeToken<Map<String, List<Long>>>() {}.type
            val data: Map<String, List<Long>> = gson.fromJson(json, type)

            val holidays = data["holidays"]?.mapNotNull { 
                com.example.clockapp.data.db.LocalDateListConverter.millisToLocalDate(it) 
            } ?: emptyList()
            
            val workdays = data["workdays"]?.mapNotNull { 
                com.example.clockapp.data.db.LocalDateListConverter.millisToLocalDate(it) 
            } ?: emptyList()

            // Consider data invalid if both holidays and workdays are empty
            if (holidays.isEmpty() && workdays.isEmpty()) {
                Log.d(TAG, "Local data for year $year is empty (no holidays or workdays)")
                return null
            }

            CalendarData(
                year = year,
                holidays = holidays,
                workdays = workdays
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Save to local storage
     */
    private fun saveToLocal(data: CalendarData) {
        val map = mapOf(
            "holidays" to data.holidays.mapNotNull { 
                com.example.clockapp.data.db.LocalDateListConverter.localDateToMillis(it) 
            },
            "workdays" to data.workdays.mapNotNull { 
                com.example.clockapp.data.db.LocalDateListConverter.localDateToMillis(it) 
            }
        )
        prefs.edit().putString(KEY_DATA_PREFIX + data.year, gson.toJson(map)).apply()
    }

    /**
     * Check if valid data exists for specified year
     * Returns false if no data or data is empty (no holidays and no workdays)
     */
    fun hasYearData(year: Int): Boolean {
        return getFromLocal(year) != null
    }

    /**
     * Get calendar data synchronously from local cache only
     * Returns null if no cached data exists
     */
    fun getCalendarDataSync(year: Int): CalendarData? {
        return getFromLocal(year)
    }

    /**
     * Check if it's time to prefetch next year's data
     * (From December 1st until year end)
     */
    fun shouldPrefetchNextYear(): Boolean {
        val today = LocalDate.now()
        val currentYear = today.year
        val nextYear = currentYear + 1

        // Check if we're in the prefetch period (Dec 1 - Dec 31)
        if (!isPrefetchPeriod()) {
            return false
        }

        // Check if we already have data for next year
        if (hasYearData(nextYear)) {
            return false
        }

        // Check if we already successfully prefetched today
        val lastAttemptDay = prefs.getInt(KEY_NEXT_YEAR_PREFETCH_ATTEMPT + nextYear, 0)
        val currentDay = today.dayOfMonth
        if (lastAttemptDay == currentDay && prefs.getBoolean(KEY_NEXT_YEAR_PREFETCH_SUCCESS + nextYear, false)) {
            return false
        }

        return true
    }

    /**
     * Check if current date is in prefetch period (December)
     */
    fun isPrefetchPeriod(): Boolean {
        val today = LocalDate.now()
        return today.monthValue == PREFETCH_START_MONTH
    }

    /**
     * Prefetch next year's calendar data
     * @return true if prefetch succeeded
     */
    suspend fun prefetchNextYearData(): Boolean {
        val today = LocalDate.now()
        val nextYear = today.year + 1
        val currentDay = today.dayOfMonth

        return try {
            Log.d(TAG, "Prefetching calendar data for next year: $nextYear")
            val calendarData = getCalendarData(nextYear)
            Log.d(TAG, "Successfully prefetched data for year $nextYear")

            // Append next year's dates to all special alarms
            appendNextYearDatesToSpecialAlarms(calendarData, nextYear)

            // Mark as successfully prefetched today
            prefs.edit()
                .putInt(KEY_NEXT_YEAR_PREFETCH_ATTEMPT + nextYear, currentDay)
                .putBoolean(KEY_NEXT_YEAR_PREFETCH_SUCCESS + nextYear, true)
                .apply()

            true
        } catch (e: Exception) {
            val message = if (e is WorkdayDataException) {
                "Next year ($nextYear) data not available yet: ${e.message}"
            } else {
                "Failed to prefetch data for year $nextYear"
            }
            Log.w(TAG, message, e)
            // Mark as attempted today (but failed)
            prefs.edit()
                .putInt(KEY_NEXT_YEAR_PREFETCH_ATTEMPT + nextYear, currentDay)
                .putBoolean(KEY_NEXT_YEAR_PREFETCH_SUCCESS + nextYear, false)
                .apply()
            false
        }
    }

    /**
     * Append next year's dates to all special alarms
     * Uses Alarm.year field as deduplication marker
     * @param calendarData Calendar data for next year
     * @param nextYear The year being appended
     */
    private suspend fun appendNextYearDatesToSpecialAlarms(calendarData: CalendarData, nextYear: Int) {
        try {
            val alarmDao = AlarmDatabase.getDatabase(context).alarmDao()
            val specialAlarms = alarmDao.getAllSpecialAlarms()

            Log.d(TAG, "Found ${specialAlarms.size} special alarms to process for year $nextYear")

            var updatedCount = 0
            specialAlarms.forEach { alarm ->
                // Deduplication check: skip if already appended this year
                val alarmYear = alarm.year
                if (alarmYear != null && alarmYear >= nextYear) {
                    Log.d(TAG, "Skipping alarm ${alarm.id}, already has year $alarmYear")
                    return@forEach
                }

                // Calculate dates for next year based on special alarm mode
                val newDates = calculateDatesForMode(calendarData, alarm.specialAlarmMode)

                // Merge and deduplicate dates
                val mergedDates = (alarm.dates + newDates).distinct().sorted()

                // Update alarm with new dates and year marker
                val updatedAlarm = alarm.copy(
                    dates = mergedDates,
                    year = nextYear
                )

                alarmDao.updateAlarm(updatedAlarm)
                updatedCount++
                Log.d(TAG, "Updated alarm ${alarm.id}: appended ${newDates.size} dates for year $nextYear, total dates: ${mergedDates.size}")
            }

            Log.d(TAG, "Successfully updated $updatedCount special alarms with dates for year $nextYear")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to append dates to special alarms", e)
            // Don't throw - prefetch should still succeed even if alarm update fails
        }
    }

    /**
     * Calculate dates for a specific alarm mode
     * @param calendarData Calendar data containing holidays and workdays
     * @param mode Special alarm mode
     * @return List of dates for the year
     */
    private fun calculateDatesForMode(calendarData: CalendarData, mode: SpecialAlarmMode): List<LocalDate> {
        return when (mode) {
            SpecialAlarmMode.ALL_HOLIDAYS -> calculateAllHolidays(calendarData)
            SpecialAlarmMode.FIRST_WORKDAY_ONLY -> calculateFirstWorkdays(calendarData)
            SpecialAlarmMode.ALL_WORKDAYS -> calendarData.getAllWorkDates()
        }
    }

    /**
     * Calculate all holidays (official holidays + weekends, excluding workday adjustments)
     */
    private fun calculateAllHolidays(calendarData: CalendarData): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val startDate = LocalDate.of(calendarData.year, 1, 1)
        val endDate = LocalDate.of(calendarData.year, 12, 31)

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek.value
            val isLegalHoliday = calendarData.holidays.any { it == currentDate }
            val isWeekend = dayOfWeek in listOf(6, 7)
            val isWorkdayAdjustment = calendarData.workdays.any { it == currentDate }

            if ((isLegalHoliday || isWeekend) && !isWorkdayAdjustment) {
                dates.add(currentDate)
            }
            currentDate = currentDate.plusDays(1)
        }
        return dates
    }

    /**
     * Calculate first workdays after holidays (including weekends)
     */
    private fun calculateFirstWorkdays(calendarData: CalendarData): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val startDate = LocalDate.of(calendarData.year, 1, 1)
        val endDate = LocalDate.of(calendarData.year, 12, 31)

        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek.value
            val isLegalHoliday = calendarData.holidays.any { it == currentDate }
            val isWeekend = dayOfWeek in listOf(6, 7)
            val isWorkdayAdjustment = calendarData.workdays.any { it == currentDate }
            val isWorkday = (!isLegalHoliday && !isWeekend) || isWorkdayAdjustment

            if (isWorkday) {
                val prevDate = currentDate.minusDays(1)
                val prevDayOfWeek = prevDate.dayOfWeek.value
                val isPrevLegalHoliday = calendarData.holidays.any { it == prevDate }
                val isPrevWeekend = prevDayOfWeek in listOf(6, 7)
                val isPrevWorkdayAdjustment = calendarData.workdays.any { it == prevDate }
                val isPrevDayHoliday = (isPrevLegalHoliday || isPrevWeekend) && !isPrevWorkdayAdjustment

                if (isPrevDayHoliday) {
                    dates.add(currentDate)
                }
            }
            currentDate = currentDate.plusDays(1)
        }
        return dates
    }

    /**
     * Get the next year that needs prefetch
     */
    fun getNextYearForPrefetch(): Int {
        return LocalDate.now().year + 1
    }

    /**
     * December reminder phase for setting next year's alarm
     */
    enum class DecemberPhase {
        NONE,       // Not December
        EARLY,      // Dec 1-10: 月初
        MIDDLE,     // Dec 11-20: 月中
        LATE,       // Dec 21-30: 月末
        LAST_DAY    // Dec 31: 最后一天
    }

    /**
     * Get December reminder info for setting next year's alarm
     * @return Triple of (phase, shouldShow, message)
     */
    fun getDecemberReminderInfo(): Triple<DecemberPhase, Boolean, String> {
        val today = LocalDate.now()
        val currentMonth = today.monthValue
        val currentDay = today.dayOfMonth
        val currentYear = today.year
        val nextYear = currentYear + 1

        if (currentMonth != 12) {
            return Triple(DecemberPhase.NONE, false, "")
        }

        // Check if reminder is disabled for this year
        val disabledKey = KEY_DECEMBER_REMIND_DISABLED + currentYear
        if (prefs.getBoolean(disabledKey, false)) {
            return Triple(DecemberPhase.NONE, false, "")
        }

        val phase = when {
            currentDay <= 10 -> DecemberPhase.EARLY
            currentDay <= 20 -> DecemberPhase.MIDDLE
            currentDay <= 30 -> DecemberPhase.LATE
            else -> DecemberPhase.LAST_DAY
        }

        // Check if we already showed reminder for this phase
        val reminderKey = KEY_DECEMBER_REMIND_SHOWN + currentYear + "_" + phase.name
        val alreadyShown = prefs.getBoolean(reminderKey, false)

        val message = when (phase) {
            DecemberPhase.EARLY -> "12月已至，建议开始设置 $nextYear 年的闹钟"
            DecemberPhase.MIDDLE -> "12月过半，请及时设置 $nextYear 年的闹钟"
            DecemberPhase.LATE -> "12月即将结束，请尽快设置 $nextYear 年的闹钟"
            DecemberPhase.LAST_DAY -> "今天是12月31日，请设置 $nextYear 年的闹钟"
            else -> ""
        }

        return Triple(phase, !alreadyShown, message)
    }

    /**
     * Disable December reminder for current year
     */
    fun disableDecemberReminderForYear() {
        val currentYear = LocalDate.now().year
        val disabledKey = KEY_DECEMBER_REMIND_DISABLED + currentYear
        prefs.edit().putBoolean(disabledKey, true).apply()
    }

    /**
     * Mark December reminder as shown for current phase
     */
    fun markDecemberReminderShown() {
        val today = LocalDate.now()
        val currentYear = today.year
        val currentDay = today.dayOfMonth

        val phase = when {
            currentDay <= 10 -> DecemberPhase.EARLY
            currentDay <= 20 -> DecemberPhase.MIDDLE
            currentDay <= 30 -> DecemberPhase.LATE
            else -> DecemberPhase.LAST_DAY
        }

        val reminderKey = KEY_DECEMBER_REMIND_SHOWN + currentYear + "_" + phase.name
        prefs.edit().putBoolean(reminderKey, true).apply()
    }
}
