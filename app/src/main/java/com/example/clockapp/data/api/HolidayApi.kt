package com.example.clockapp.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Holiday API interface
 * Using jsdelivr CDN with holiday-cn data
 */
interface HolidayApi {
    /**
     * Get holiday data for specified year
     */
    @GET("gh/NateScarlet/holiday-cn@master/{year}.json")
    suspend fun getHolidayData(@Path("year") year: Int): Response<HolidayResponse>
}

/**
 * API response data class
 */
data class HolidayResponse(
    val year: Int,
    val days: List<HolidayDay>?
)

data class HolidayDay(
    val name: String,
    val date: String,  // Format: yyyy-MM-dd
    val isOffDay: Boolean  // true: holiday/rest day, false: work day (makeup day)
)
