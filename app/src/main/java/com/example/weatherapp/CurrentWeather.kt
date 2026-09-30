package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class CurrentWeather(
    val time: String,
    val temperature: Double,
    @SerializedName("windspeed") val windSpeed: Double,
    @SerializedName("winddirection") val windDirection: Double,
    @SerializedName("weathercode") val weatherCode: Int,
    @SerializedName("is_day") val isDay: Int
)