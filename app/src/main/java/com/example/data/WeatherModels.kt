package com.example.data

import com.squareup.moshi.Json
import java.util.Locale
import kotlin.math.roundToInt

// --- Open-Meteo Forecast API DTOs ---

data class WeatherResponse(
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "timezone") val timezone: String? = null,
    @Json(name = "utc_offset_seconds") val utcOffsetSeconds: Int? = null,
    @Json(name = "current") val current: CurrentWeather? = null,
    @Json(name = "hourly") val hourly: HourlyWeather? = null,
    @Json(name = "daily") val daily: DailyWeather? = null
)

data class CurrentWeather(
    @Json(name = "time") val time: String? = null,
    @Json(name = "temperature_2m") val temperature: Double = 0.0,
    @Json(name = "relative_humidity_2m") val humidity: Int = 0,
    @Json(name = "apparent_temperature") val apparentTemperature: Double = 0.0,
    @Json(name = "is_day") val isDay: Int = 1,
    @Json(name = "precipitation") val precipitation: Double = 0.0,
    @Json(name = "weather_code") val weatherCode: Int = 0,
    @Json(name = "cloud_cover") val cloudCover: Int = 0,
    @Json(name = "surface_pressure") val surfacePressure: Double = 1013.25,
    @Json(name = "wind_speed_10m") val windSpeed: Double = 0.0,
    @Json(name = "wind_direction_10m") val windDirection: Int = 0,
    @Json(name = "wind_gusts_10m") val windGusts: Double = 0.0
)

data class HourlyWeather(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
    @Json(name = "is_day") val isDay: List<Int?> = emptyList(),
    @Json(name = "uv_index") val uvIndex: List<Double?> = emptyList(),
    @Json(name = "visibility") val visibility: List<Double?> = emptyList()
)

data class DailyWeather(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "weather_code") val weatherCode: List<Int?> = emptyList(),
    @Json(name = "temperature_2m_max") val tempMax: List<Double?> = emptyList(),
    @Json(name = "temperature_2m_min") val tempMin: List<Double?> = emptyList(),
    @Json(name = "sunrise") val sunrise: List<String?> = emptyList(),
    @Json(name = "sunset") val sunset: List<String?> = emptyList(),
    @Json(name = "uv_index_max") val uvIndexMax: List<Double?> = emptyList(),
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int?> = emptyList()
)

// --- Open-Meteo Air Quality API DTOs ---

data class AirQualityResponse(
    @Json(name = "current") val current: CurrentAirQuality? = null
)

data class CurrentAirQuality(
    @Json(name = "european_aqi") val europeanAqi: Int? = null,
    @Json(name = "us_aqi") val usAqi: Int? = null,
    @Json(name = "pm10") val pm10: Double? = null,
    @Json(name = "pm2_5") val pm25: Double? = null,
    @Json(name = "nitrogen_dioxide") val nitrogenDioxide: Double? = null,
    @Json(name = "ozone") val ozone: Double? = null
)

// --- Open-Meteo Geocoding API DTOs ---

data class GeocodingResponse(
    @Json(name = "results") val results: List<GeocodingResult>? = null
)

data class GeocodingResult(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "name") val name: String = "",
    @Json(name = "latitude") val latitude: Double = 0.0,
    @Json(name = "longitude") val longitude: Double = 0.0,
    @Json(name = "country") val country: String? = null,
    @Json(name = "admin1") val admin1: String? = null,
    @Json(name = "country_code") val countryCode: String? = null,
    @Json(name = "timezone") val timezone: String? = null
)

// --- Domain & UI Models ---

data class SavedLocation(
    val name: String,
    val region: String = "",
    val country: String = "",
    val latitude: Double,
    val longitude: Double
) {
    val displaySubtitle: String
        get() = listOfNotNull(
            region.takeIf { it.isNotBlank() && it != name },
            country.takeIf { it.isNotBlank() }
        ).joinToString(", ")

    fun matchesCoordinates(lat: Double, lon: Double): Boolean {
        return kotlin.math.abs(latitude - lat) < 0.02 && kotlin.math.abs(longitude - lon) < 0.02
    }
}

data class HourlyForecastItem(
    val isoTime: String,
    val hourLabel: String,
    val temperatureC: Double,
    val precipitationProbability: Int,
    val weatherCode: Int,
    val isDay: Boolean,
    val isNow: Boolean = false
)

data class ForecastDay(
    val date: String,
    val weatherCode: Int,
    val tempMaxC: Double,
    val tempMinC: Double,
    val sunriseIso: String?,
    val sunsetIso: String?,
    val uvIndexMax: Double,
    val precipitationProbabilityMax: Int
)

data class AirQualitySummary(
    val europeanAqi: Int,
    val usAqi: Int,
    val pm25: Double,
    val pm10: Double,
    val ozone: Double,
    val nitrogenDioxide: Double
)

data class WeatherDashboardData(
    val location: SavedLocation,
    val currentTimeIso: String,
    val timezoneId: String,
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val todayHighC: Double,
    val todayLowC: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val humidityPercent: Int,
    val cloudCoverPercent: Int,
    val windSpeedKmh: Double,
    val windGustsKmh: Double,
    val windDirectionDegrees: Int,
    val surfacePressureHpa: Double,
    val visibilityKm: Double,
    val currentUvIndex: Double,
    val maxUvIndexToday: Double,
    val sunriseTime: String,
    val sunsetTime: String,
    val sunProgressFraction: Float,
    val hourlyForecast: List<HourlyForecastItem>,
    val dailyForecast: List<ForecastDay>,
    val airQuality: AirQualitySummary?,
    val lastSyncedEpochMillis: Long
)

// --- User Preferences & Unit Formatting ---

enum class TemperatureUnit(val symbol: String, val label: String) {
    CELSIUS("°C", "Celsius (°C)"),
    FAHRENHEIT("°F", "Fahrenheit (°F)")
}

enum class WindSpeedUnit(val label: String, val displayName: String) {
    KMH("km/h", "Kilometers/hour (km/h)"),
    MS("m/s", "Meters/second (m/s)"),
    MPH("mph", "Miles/hour (mph)")
}

enum class PressureUnit(val label: String, val displayName: String) {
    HPA("hPa", "Hectopascals (hPa)"),
    MMHG("mmHg", "Millimeters of mercury (mmHg)")
}

data class UserPreferences(
    val temperatureUnit: TemperatureUnit = TemperatureUnit.CELSIUS,
    val windSpeedUnit: WindSpeedUnit = WindSpeedUnit.KMH,
    val pressureUnit: PressureUnit = PressureUnit.HPA
)

object WeatherFormatter {
    fun convertTemp(celsius: Double, unit: TemperatureUnit): Double {
        return when (unit) {
            TemperatureUnit.CELSIUS -> celsius
            TemperatureUnit.FAHRENHEIT -> (celsius * 9.0 / 5.0) + 32.0
        }
    }

    fun formatTempNumber(celsius: Double, unit: TemperatureUnit): Int {
        return convertTemp(celsius, unit).roundToInt()
    }

    fun formatTemp(celsius: Double, unit: TemperatureUnit): String {
        return "${formatTempNumber(celsius, unit)}°"
    }

    fun formatTempWithUnit(celsius: Double, unit: TemperatureUnit): String {
        return "${formatTempNumber(celsius, unit)}${unit.symbol}"
    }

    fun formatWind(kmh: Double, unit: WindSpeedUnit): String {
        val converted = when (unit) {
            WindSpeedUnit.KMH -> kmh
            WindSpeedUnit.MS -> kmh / 3.6
            WindSpeedUnit.MPH -> kmh * 0.621371
        }
        return if (unit == WindSpeedUnit.MS) {
            String.format(Locale.US, "%.1f %s", converted, unit.label)
        } else {
            "${converted.roundToInt()} ${unit.label}"
        }
    }

    fun formatPressure(hpa: Double, unit: PressureUnit): String {
        return when (unit) {
            PressureUnit.HPA -> "${hpa.roundToInt()} hPa"
            PressureUnit.MMHG -> "${(hpa * 0.750062).roundToInt()} mmHg"
        }
    }

    fun windCardinalDirection(degrees: Int): String {
        val dirs = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val normalized = ((degrees % 360) + 360) % 360
        val index = ((normalized + 22.5) / 45.0).toInt() % 8
        return dirs[index]
    }

    fun weatherDescription(code: Int): String = when (code) {
        0 -> "Clear Sky"
        1 -> "Mainly Clear"
        2 -> "Partly Cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        51 -> "Light Drizzle"
        53 -> "Moderate Drizzle"
        55 -> "Dense Drizzle"
        56, 57 -> "Freezing Drizzle"
        61 -> "Light Rain"
        63 -> "Moderate Rain"
        65 -> "Heavy Rain"
        66, 67 -> "Freezing Rain"
        71 -> "Light Snow"
        73 -> "Moderate Snow"
        75 -> "Heavy Snow"
        77 -> "Snow Grains"
        80 -> "Light Showers"
        81 -> "Moderate Showers"
        82 -> "Violent Showers"
        85, 86 -> "Snow Showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm & Hail"
        else -> "Cloudy"
    }

    fun aqiCategory(aqi: Int): String = when {
        aqi <= 20 -> "Good"
        aqi <= 40 -> "Fair"
        aqi <= 60 -> "Moderate"
        aqi <= 80 -> "Poor"
        aqi <= 100 -> "Very Poor"
        else -> "Hazardous"
    }

    fun uvCategory(uv: Double): String = when {
        uv < 3.0 -> "Low"
        uv < 6.0 -> "Moderate"
        uv < 8.0 -> "High"
        uv < 11.0 -> "Very High"
        else -> "Extreme"
    }
}
