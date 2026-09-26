package com.example.weatherapp

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherServiceAPI {
    @GET("v1/forecast/")
    fun getWeatherDetails(
        @Query("latitude") lat: Double,
        @Query("longitude") long: Double,
        @Query("temperature_unit") unit: String,
        @Query("hourly") hourly: String
    ): Call<WeatherResponse>
}