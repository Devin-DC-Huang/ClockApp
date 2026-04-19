package com.example.clockapp.data.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/**
 * Calendar data model using java.time API
 * Corresponds to CalendarData in the reference project
 */
data class CalendarData(
    val year: Int,
    val holidays: List<LocalDate>,  // Legal holiday dates (java.time)
    val workdays: List<LocalDate>   // Workday adjustments(weekend workdays) (java.time)
) {
    /**
     * Get all work dates (auto calculated)
     *
     * Rules:
     * 1. Monday to Friday, and not in holidays → workday
     * 2. Weekend days in workdays list → workday
     */
    fun getAllWorkDates(): List<LocalDate> {
        val workDates = mutableListOf<LocalDate>()
        
        // Iterate through all days of the year
        var currentDate = LocalDate.of(year, Month.JANUARY, 1)
        val endDate = LocalDate.of(year, Month.DECEMBER, 31)
        
        while (!currentDate.isAfter(endDate)) {
            val dayOfWeek = currentDate.dayOfWeek
            
            // 1. Adjusted workdays (weekend makeup days)
            if (workdays.contains(currentDate)) {
                workDates.add(currentDate)
            }
            // 2. Monday to Friday, not in holidays
            else if (dayOfWeek in DayOfWeek.MONDAY..DayOfWeek.FRIDAY) {
                if (!holidays.contains(currentDate)) {
                    workDates.add(currentDate)
                }
            }
            
            currentDate = currentDate.plusDays(1)
        }
        
        return workDates.sorted()
    }
}
