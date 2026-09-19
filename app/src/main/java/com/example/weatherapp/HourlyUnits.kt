package com.example.weatherapp

import com.google.gson.annotations.SerializedName

data class HourlyUnits(
    @SerializedName("temperature_2m") val temperature2m: String
) {
}