package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class Hourly(
    val time: MutableList<String>,
    @SerializedName("temperature_2m") val temperature2m: MutableList<Double>
) {
}