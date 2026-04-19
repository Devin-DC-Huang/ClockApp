package com.example.clockapp.data.api

import com.example.clockapp.data.security.SecurityConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Retrofit 客户端
 * Uses HTTPS for secure communication and secure Gson to prevent deserialization attacks
 */
object RetrofitClient {
    // Using jsdelivr CDN with HTTPS for secure data transmission
    private const val BASE_URL = "https://cdn.jsdelivr.net/"

    val holidayApi: HolidayApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // Use secure Gson with strict parsing
            .addConverterFactory(GsonConverterFactory.create(SecurityConfig.createSecureGson()))
            .build()
            .create(HolidayApi::class.java)
    }
}
