package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class Hourly(
    val time: MutableList<String>,
    @SerializedName("temperature_2m") val temperature2m: MutableList<Double>,
    @SerializedName("apparent_temperature") val apparentTemperature: List<Double?>?,
    @SerializedName("relative_humidity_2m") val relativeHumidity2m: List<Int?>?,
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int?>?
)