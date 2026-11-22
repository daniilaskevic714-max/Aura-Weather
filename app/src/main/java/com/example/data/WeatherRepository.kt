package com.example.data

import android.content.Context
import android.location.Geocoder
import android.os.Build
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

class WeatherRepository {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "AuraWeatherAndroid/2.0")
                .build()
            chain.proceed(request)
        }
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    private val forecastApi: WeatherApiService = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(WeatherApiService::class.java)

    private val airQualityApi: AirQualityApiService = Retrofit.Builder()
        .baseUrl("https://air-quality-api.open-meteo.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(AirQualityApiService::class.java)

    private val geocodingApi: GeocodingApiService = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .client(client)
        .build()
        .create(GeocodingApiService::class.java)

    suspend fun getWeatherDashboard(location: SavedLocation): WeatherDashboardData =
        withContext(Dispatchers.IO) {
            coroutineScope {
                val forecastDeferred = async {
                    forecastApi.getForecast(location.latitude, location.longitude)
                }
                val airQualityDeferred = async {
                    try {
                        airQualityApi.getAirQuality(location.latitude, location.longitude)
                    } catch (e: Exception) {
                        Log.w("WeatherRepository", "Air quality fetch optional failure: ${e.message}")
                        null
                    }
                }

                val weather = forecastDeferred.await()
                val airQuality = airQualityDeferred.await()
                mapToDashboardData(location, weather, airQuality)
            }
        }

    suspend fun searchCities(query: String): List<SavedLocation> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()
        val lang = Locale.getDefault().language.ifBlank { "en" }
        val response = geocodingApi.searchLocations(query = trimmed, count = 8, language = lang)
        response.results.orEmpty()
            .filter { it.name.isNotBlank() }
            .map { result ->
                SavedLocation(
                    name = result.name,
                    region = result.admin1.orEmpty(),
                    country = result.country.orEmpty(),
                    latitude = result.latitude,
                    longitude = result.longitude
                )
            }
    }

    @Suppress("DEPRECATION")
    suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): SavedLocation =
        withContext(Dispatchers.IO) {
            val fallbackName = String.format(Locale.US, "%.2f°, %.2f°", lat, lon)
            try {
                if (!Geocoder.isPresent()) {
                    return@withContext SavedLocation(name = "Current Location", region = fallbackName, latitude = lat, longitude = lon)
                }
                val geocoder = Geocoder(context, Locale.getDefault())
                val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(lat, lon, 1) { addresses ->
                            cont.resume(addresses.firstOrNull())
                        }
                    }
                } else {
                    geocoder.getFromLocation(lat, lon, 1)?.firstOrNull()
                }

                if (address != null) {
                    val city = address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                        ?: "Current Location"
                    SavedLocation(
                        name = city,
                        region = address.adminArea.orEmpty(),
                        country = address.countryName.orEmpty(),
                        latitude = lat,
                        longitude = lon
                    )
                } else {
                    SavedLocation(name = "Current Location", region = fallbackName, latitude = lat, longitude = lon)
                }
            } catch (e: Exception) {
                Log.w("WeatherRepository", "Reverse geocoding failed: ${e.message}")
                SavedLocation(name = "Current Location", region = fallbackName, latitude = lat, longitude = lon)
            }
        }

    internal fun mapToDashboardData(
        location: SavedLocation,
        weather: WeatherResponse,
        airQualityResponse: AirQualityResponse?
    ): WeatherDashboardData {
        val current = weather.current ?: throw IllegalStateException("Weather telemetry unavailable")
        val currentTimeIso = current.time.orEmpty()
        val currentHourPrefix = if (currentTimeIso.length >= 13) currentTimeIso.substring(0, 13) else ""

        val hourly = weather.hourly
        val hourlyTimes = hourly?.time.orEmpty()
        val startIndex = hourlyTimes.indexOfFirst { it.startsWith(currentHourPrefix) }
            .takeIf { it >= 0 } ?: 0

        val hourlyItems = mutableListOf<HourlyForecastItem>()
        val endIndex = (startIndex + 24).coerceAtMost(hourlyTimes.size)
        for (i in startIndex until endIndex) {
            val iso = hourlyTimes[i]
            val hourLabel = if (i == startIndex) "Now" else extractTimePart(iso)
            hourlyItems.add(
                HourlyForecastItem(
                    isoTime = iso,
                    hourLabel = hourLabel,
                    temperatureC = hourly?.temperature?.getOrNull(i) ?: current.temperature,
                    precipitationProbability = hourly?.precipitationProbability?.getOrNull(i) ?: 0,
                    weatherCode = hourly?.weatherCode?.getOrNull(i) ?: current.weatherCode,
                    isDay = (hourly?.isDay?.getOrNull(i) ?: current.isDay) == 1,
                    isNow = i == startIndex
                )
            )
        }

        val currentUv = hourly?.uvIndex?.getOrNull(startIndex) ?: 0.0
        val visibilityMeters = hourly?.visibility?.getOrNull(startIndex) ?: 10000.0

        val daily = weather.daily
        val dailyList = mutableListOf<ForecastDay>()
        val dailyTimes = daily?.time.orEmpty()
        for (i in dailyTimes.indices) {
            dailyList.add(
                ForecastDay(
                    date = dailyTimes[i],
                    weatherCode = daily?.weatherCode?.getOrNull(i) ?: 0,
                    tempMaxC = daily?.tempMax?.getOrNull(i) ?: current.temperature,
                    tempMinC = daily?.tempMin?.getOrNull(i) ?: current.temperature,
                    sunriseIso = daily?.sunrise?.getOrNull(i),
                    sunsetIso = daily?.sunset?.getOrNull(i),
                    uvIndexMax = daily?.uvIndexMax?.getOrNull(i) ?: 0.0,
                    precipitationProbabilityMax = daily?.precipitationProbabilityMax?.getOrNull(i) ?: 0
                )
            )
        }

        val today = dailyList.firstOrNull()
        val sunriseTime = extractTimePart(today?.sunriseIso)
        val sunsetTime = extractTimePart(today?.sunsetIso)
        val sunProgress = computeSunProgress(
            currentIso = currentTimeIso,
            sunriseIso = today?.sunriseIso,
            sunsetIso = today?.sunsetIso,
            isDay = current.isDay == 1
        )

        val aqCurrent = airQualityResponse?.current
        val airQualitySummary = if (aqCurrent != null && (aqCurrent.europeanAqi != null || aqCurrent.usAqi != null)) {
            AirQualitySummary(
                europeanAqi = aqCurrent.europeanAqi ?: 0,
                usAqi = aqCurrent.usAqi ?: 0,
                pm25 = aqCurrent.pm25 ?: 0.0,
                pm10 = aqCurrent.pm10 ?: 0.0,
                ozone = aqCurrent.ozone ?: 0.0,
                nitrogenDioxide = aqCurrent.nitrogenDioxide ?: 0.0
            )
        } else null

        return WeatherDashboardData(
            location = location,
            currentTimeIso = currentTimeIso,
            timezoneId = weather.timezone ?: "UTC",
            temperatureC = current.temperature,
            apparentTemperatureC = current.apparentTemperature,
            todayHighC = today?.tempMaxC ?: current.temperature,
            todayLowC = today?.tempMinC ?: current.temperature,
            weatherCode = current.weatherCode,
            isDay = current.isDay == 1,
            humidityPercent = current.humidity,
            cloudCoverPercent = current.cloudCover,
            windSpeedKmh = current.windSpeed,
            windGustsKmh = current.windGusts,
            windDirectionDegrees = current.windDirection,
            surfacePressureHpa = current.surfacePressure,
            visibilityKm = visibilityMeters / 1000.0,
            currentUvIndex = currentUv,
            maxUvIndexToday = today?.uvIndexMax ?: currentUv,
            sunriseTime = sunriseTime,
            sunsetTime = sunsetTime,
            sunProgressFraction = sunProgress,
            hourlyForecast = hourlyItems,
            dailyForecast = dailyList,
            airQuality = airQualitySummary,
            lastSyncedEpochMillis = System.currentTimeMillis()
        )
    }

    private fun extractTimePart(iso: String?): String {
        if (iso.isNullOrBlank()) return "--:--"
        val tIndex = iso.indexOf('T')
        return if (tIndex != -1 && iso.length >= tIndex + 6) {
            iso.substring(tIndex + 1, tIndex + 6)
        } else {
            "--:--"
        }
    }

    private fun computeSunProgress(
        currentIso: String?,
        sunriseIso: String?,
        sunsetIso: String?,
        isDay: Boolean
    ): Float {
        val currentMinutes = isoToMinutesOfDay(currentIso)
        val sunriseMinutes = isoToMinutesOfDay(sunriseIso)
        val sunsetMinutes = isoToMinutesOfDay(sunsetIso)
        if (currentMinutes == null || sunriseMinutes == null || sunsetMinutes == null || sunsetMinutes <= sunriseMinutes) {
            return if (isDay) 0.5f else 0.85f
        }
        return if (isDay) {
            ((currentMinutes - sunriseMinutes).toFloat() / (sunsetMinutes - sunriseMinutes).toFloat())
                .coerceIn(0.05f, 0.95f)
        } else {
            0.0f
        }
    }

    private fun isoToMinutesOfDay(iso: String?): Int? {
        val hhmm = extractTimePart(iso)
        if (hhmm == "--:--") return null
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return h * 60 + m
    }
}
