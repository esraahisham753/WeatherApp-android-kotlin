package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    @SerializedName("generationtime_ms") val generationTimeMs: Double,
    @SerializedName("utc_offset_seconds") val utcOffsetSeconds: Int,
    val timezone: String,
    @SerializedName("timezone_abbreviation") val timezoneAbbreviation: String,
    @SerializedName("current_weather") val currentWeather: CurrentWeather?,
    val hourly: Hourly,
    @SerializedName("hourly_units") val hourlyUnits: HourlyUnits,
    val daily: Daily?
)