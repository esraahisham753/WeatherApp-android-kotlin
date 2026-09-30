package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class Daily(
    val time: List<String>,
    @SerializedName("weather_code") val weatherCode: List<Int?>,
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double?>,
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double?>,
    @SerializedName("uv_index_max") val uvIndexMax: List<Double?>?,
    val sunrise: List<String>?,
    val sunset: List<String>?,
    @SerializedName("precipitation_probability_max") val precipitationProbabilityMax: List<Int?>?
)