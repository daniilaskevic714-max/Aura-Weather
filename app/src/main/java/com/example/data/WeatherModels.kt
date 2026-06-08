package com.example.data

import com.squareup.moshi.Json

data class WeatherResponse(
    @Json(name = "current") val current: CurrentWeather?,
    @Json(name = "daily") val daily: DailyWeather?
)

data class CurrentWeather(
    @Json(name = "temperature_2m") val temperature: Double,
    @Json(name = "weather_code") val weatherCode: Int,
    @Json(name = "is_day") val isDay: Int,
    @Json(name = "relative_humidity_2m") val humidity: Int,
    @Json(name = "wind_speed_10m") val windSpeed: Double
)

data class DailyWeather(
    @Json(name = "time") val time: List<String>,
    @Json(name = "weather_code") val weatherCode: List<Int>,
    @Json(name = "temperature_2m_max") val tempMax: List<Double>,
    @Json(name = "temperature_2m_min") val tempMin: List<Double>
)

data class ForecastDay(
    val date: String,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double
)
