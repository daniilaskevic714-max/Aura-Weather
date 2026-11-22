package com.example

import com.example.data.AirQualityResponse
import com.example.data.CurrentAirQuality
import com.example.data.CurrentWeather
import com.example.data.DailyWeather
import com.example.data.HourlyWeather
import com.example.data.PressureUnit
import com.example.data.SavedLocation
import com.example.data.TemperatureUnit
import com.example.data.WeatherFormatter
import com.example.data.WeatherRepository
import com.example.data.WeatherResponse
import com.example.data.WindSpeedUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `temperature formatting converts celsius to fahrenheit accurately`() {
        assertEquals("0°", WeatherFormatter.formatTemp(0.0, TemperatureUnit.CELSIUS))
        assertEquals("32°", WeatherFormatter.formatTemp(0.0, TemperatureUnit.FAHRENHEIT))
        assertEquals("212°", WeatherFormatter.formatTemp(100.0, TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun `wind and pressure formatters convert units accurately`() {
        assertEquals("36 km/h", WeatherFormatter.formatWind(36.0, WindSpeedUnit.KMH))
        assertEquals("10.0 m/s", WeatherFormatter.formatWind(36.0, WindSpeedUnit.MS))
        assertEquals("1013 hPa", WeatherFormatter.formatPressure(1013.2, PressureUnit.HPA))
        assertEquals("760 mmHg", WeatherFormatter.formatPressure(1013.25, PressureUnit.MMHG))
    }

    @Test
    fun `wind cardinal direction resolves compass bearings`() {
        assertEquals("N", WeatherFormatter.windCardinalDirection(0))
        assertEquals("E", WeatherFormatter.windCardinalDirection(90))
        assertEquals("SW", WeatherFormatter.windCardinalDirection(225))
        assertEquals("N", WeatherFormatter.windCardinalDirection(359))
    }

    @Test
    fun `repository maps Open-Meteo response into WeatherDashboardData`() {
        val repo = WeatherRepository()
        val location = SavedLocation("Tokyo", "Tokyo", "Japan", 35.6762, 139.6503)
        val weatherResponse = WeatherResponse(
            timezone = "Asia/Tokyo",
            current = CurrentWeather(
                time = "2026-11-22T10:15",
                temperature = 17.4,
                humidity = 58,
                apparentTemperature = 16.5,
                isDay = 1,
                weatherCode = 1,
                cloudCover = 20,
                surfacePressure = 1018.0,
                windSpeed = 12.0,
                windDirection = 180,
                windGusts = 20.0
            ),
            hourly = HourlyWeather(
                time = listOf("2026-11-22T10:00", "2026-11-22T11:00", "2026-11-22T12:00"),
                temperature = listOf(17.0, 18.2, 19.0),
                precipitationProbability = listOf(5, 10, 15),
                weatherCode = listOf(1, 1, 2),
                isDay = listOf(1, 1, 1),
                uvIndex = listOf(3.4, 4.1, 4.5),
                visibility = listOf(24000.0, 24000.0, 22000.0)
            ),
            daily = DailyWeather(
                time = listOf("2026-11-22", "2026-11-23"),
                weatherCode = listOf(1, 61),
                tempMax = listOf(19.5, 15.0),
                tempMin = listOf(10.2, 9.0),
                sunrise = listOf("2026-11-22T06:24", "2026-11-23T06:25"),
                sunset = listOf("2026-11-22T16:31", "2026-11-23T16:30"),
                uvIndexMax = listOf(4.5, 2.0),
                precipitationProbabilityMax = listOf(15, 80)
            )
        )
        val aqResponse = AirQualityResponse(
            current = CurrentAirQuality(europeanAqi = 22, usAqi = 30, pm10 = 12.0, pm25 = 7.5, nitrogenDioxide = 15.0, ozone = 44.0)
        )

        val dashboard = repo.mapToDashboardData(location, weatherResponse, aqResponse)
        assertEquals("Tokyo", dashboard.location.name)
        assertEquals(3, dashboard.hourlyForecast.size)
        assertEquals("Now", dashboard.hourlyForecast.first().hourLabel)
        assertEquals(2, dashboard.dailyForecast.size)
        assertEquals("06:24", dashboard.sunriseTime)
        assertEquals("16:31", dashboard.sunsetTime)
        assertNotNull(dashboard.airQuality)
        assertEquals(22, dashboard.airQuality?.europeanAqi)
    }
}
