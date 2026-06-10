package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlinx.coroutines.delay

class GeminiWeatherRepository {
    private val apiService = GeminiRetrofitClient.service
    private val moshi = GeminiRetrofitClient.moshiInstance

    private suspend fun <T> executeWithRetry(
        retries: Int = 4,
        initialDelayMillis: Long = 1500,
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMillis
        repeat(retries - 1) { attempt ->
            try {
                return block()
            } catch (e: HttpException) {
                if (e.code() == 429) {
                    Log.w("GeminiRepository", "HTTP 429 Too Many Requests. Retrying in $currentDelay ms (attempt ${attempt + 1}).")
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                } else {
                    throw e
                }
            } catch (e: Exception) {
                if (e.message?.contains("429") == true) {
                    Log.w("GeminiRepository", "Detected 429 in message. Retrying in $currentDelay ms (attempt ${attempt + 1}).")
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                } else {
                    throw e
                }
            }
        }
        
        try {
            return block()
        } catch (e: HttpException) {
            if (e.code() == 429) {
                throw Exception("Ошибка 429 (Too Many Requests): Превышен лимит запросов к Gemini API. Пожалуйста, подождите несколько секунд и попробуйте снова. / API Rate Limit Exceeded. Please wait a few seconds and try again.")
            } else {
                throw e
            }
        } catch (e: Exception) {
            if (e.message?.contains("429") == true) {
                throw Exception("Ошибка 429 (Too Many Requests): Превышен лимит запросов к Gemini API. Пожалуйста, подождите несколько секунд и попробуйте снова. / API Rate Limit Exceeded. Please wait a few seconds and try again.")
            } else {
                throw e
            }
        }
    }

    suspend fun getGeminiWeather(city: String, simulateDisaster: String? = null): GeminiWeatherData = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("Missing Gemini API Key! Please click the Secrets panel in AI Studio and configure GEMINI_API_KEY with your API key.")
        }

        val prompt = """
            Provide realistic current weather data, a 7-day daily forecast, and any active severe weather alerts, watches, warnings, or safety advisories for the city of $city.
            You must return a raw JSON object matching this structure exactly (do not wrap in markdown or any other enclosing text, just pure JSON):
            {
              "city": "$city",
              "temperature": 18.5,
              "condition": "Partly Cloudy",
              "icon": "cloudy",
              "windSpeed": 15.0,
              "humidity": 60,
              "commentary": "A refreshing breeze sweeps through $city today.",
              "latitude": 52.52,
              "longitude": 13.41,
              "uvIndex": 4.5,
              "forecast": [
                { "day": "Mon", "tempMax": 21.0, "tempMin": 12.0, "condition": "Sunny", "icon": "sunny" },
                { "day": "Tue", "tempMax": 19.5, "tempMin": 11.0, "condition": "Partly Cloudy", "icon": "cloudy" },
                { "day": "Wed", "tempMax": 18.0, "tempMin": 13.0, "condition": "Showers", "icon": "rainy" },
                { "day": "Thu", "tempMax": 22.0, "tempMin": 14.0, "condition": "Sunny", "icon": "sunny" },
                { "day": "Fri", "tempMax": 23.5, "tempMin": 15.0, "condition": "Thunderstorms", "icon": "thunderstorm" },
                { "day": "Sat", "tempMax": 17.0, "tempMin": 10.0, "condition": "Windy", "icon": "windy" },
                { "day": "Sun", "tempMax": 16.0, "tempMin": 9.5, "condition": "Showers", "icon": "rainy" }
              ],
              "alerts": [
                {
                  "event": "Heat Advisory",
                  "sender": "National Weather Service",
                  "severity": "Moderate",
                  "description": "Heat index values up to 105 degrees expected. Hot temperatures and high humidity may cause heat illnesses.",
                  "ends": "Today 8:00 PM"
                }
              ]
            }
            
            Valid 'icon' strings for both current weather and forecast are only: "sunny", "cloudy", "rainy", "thunderstorm", "snowy", "foggy", "windy".
            Ensure the fields are accurate to the climate or typical weather of $city for this time (June), provide accurate geographic 'latitude' and 'longitude' coordinates for this city, estimate a realistic 'uvIndex' (e.g. 0.0 to 11.0+ depending on weather/sunshine/latitude), make the 'commentary' lively, slightly witty, and highly descriptive, and generate a highly realistic 7-day forecast with consecutive days (using standard abbreviated day names starting from the upcoming day of the week).
            If there are severe/noteworthy weather conditions for the current location or season, include 1 or 2 appropriate and realistic alerts (such as Heat Advisory, Air Quality Index Watch, Severe Thunderstorm Warning, Flood Advisory, or High Wind Warning) under the "alerts" list. If the weather is perfectly safe, return either a seasonal/environmental alert (such as 'UV Exposure Notice', 'Grass Pollen Advisory' or 'Ozone Alert') so the user can see highly helpful context and localized hazard warnings in action.
            
            ${if (simulateDisaster != null) {
                """
                CRITICAL INSTRUCTION - USER SIMULATING DISASTER:
                The user has requested to simulate a severe natural disaster of type: "$simulateDisaster" for $city.
                You MUST return weather details reflecting this catastrophe!
                In the "alerts" field:
                Include a highly extreme alert object with:
                - "event": A warning matching the disaster (e.g. "Tsunami Warning", "Tornado Warning", "Earthquake Alert", "Volcanic Eruption Emergency", "Catastrophic Flood Crisis")
                - "sender": "Emergency Management & Life-Saving Command"
                - "severity": "Extreme"
                - "description": A safety and evacuation guidance. For example:
                   * For Tsunami: "Move to high ground immediately. Coastlines are extremely dangerous. Do not return until authorized."
                   * For Tornado: "Take immediate cover in an interior room or basement. Protect your head. Stay clear of windows."
                   * For Earthquake: "Drop, cover, and hold on. Beware of falling structures and severe aftershocks."
                   * For Volcanic Eruption: "Massive ash cloud and pyroclastic hazard. Evacuate exclusion zones immediately and wear respiratory protection."
                   * For Hurricane: "Inundating rainfall and devastating hurricane winds. Shelters are open. Evacuate if instructed."
                - "ends": "Active Emergency"
                In the commentary field, output a warning message in dramatic tone ordering citizens to comply with emergency orders.
                """
            } else ""}
            
            LOCALIZATION TO RUSSIAN:
            If the query "$city" includes Russian characters (Cyrillic letters), or if "$simulateDisaster" is requested by a Russian user, you MUST translate and provide all returned text fields ("city", "condition", "commentary", forecast "day" e.g., 'Пн', 'Вт', forecast "condition", alert "event", "sender", "description", etc.) in clear, professional Russian language so the user gets a fully Russian native experience.
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7
            )
        )

        try {
            val response = executeWithRetry { apiService.generateContent(apiKey, request) }
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content received from Gemini API.")

            Log.d("GeminiRepository", "Raw Gemini Response: $rawText")

            val sanitized = sanitizeJsonResponse(rawText)
            val jsonAdapter = moshi.adapter(GeminiWeatherData::class.java)
            val data = jsonAdapter.fromJson(sanitized)
                ?: throw Exception("Failed to convert Gemini output to weather data.")

            data
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error fetching weather data from Gemini API", e)
            throw e
        }
    }

    suspend fun getGeminiWeatherByCoordinates(lat: Double, lon: Double, simulateDisaster: String? = null): GeminiWeatherData = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("Missing Gemini API Key! Please click the Secrets panel in AI Studio and configure GEMINI_API_KEY with your API key.")
        }

        val prompt = """
            You are a meteorological data intelligence agent.
            First, resolve the nearest actual geographical city name or location area for the coordinates (Latitude: $lat, Longitude: $lon).
            Then, provide realistic current weather data, a 7-day daily forecast, and any active severe weather alerts, watches, warnings, or safety advisories for that location.
            You must return a raw JSON object matching this structure exactly (do not wrap in markdown or any other enclosing text, just pure JSON):
            {
              "city": "Paris",
              "temperature": 18.5,
              "condition": "Partly Cloudy",
              "icon": "cloudy",
              "windSpeed": 15.0,
              "humidity": 60,
              "commentary": "A refreshing breeze sweeps through Paris today.",
              "latitude": $lat,
              "longitude": $lon,
              "uvIndex": 4.5,
              "forecast": [
                { "day": "Mon", "tempMax": 21.0, "tempMin": 12.0, "condition": "Sunny", "icon": "sunny" },
                { "day": "Tue", "tempMax": 19.5, "tempMin": 11.0, "condition": "Partly Cloudy", "icon": "cloudy" },
                { "day": "Wed", "tempMax": 18.0, "tempMin": 13.0, "condition": "Showers", "icon": "rainy" },
                { "day": "Thu", "tempMax": 22.0, "tempMin": 14.0, "condition": "Sunny", "icon": "sunny" },
                { "day": "Fri", "tempMax": 23.5, "tempMin": 15.0, "condition": "Thunderstorms", "icon": "thunderstorm" },
                { "day": "Sat", "tempMax": 17.0, "tempMin": 10.0, "condition": "Windy", "icon": "windy" },
                { "day": "Sun", "tempMax": 16.0, "tempMin": 9.5, "condition": "Showers", "icon": "rainy" }
              ],
              "alerts": [
                {
                  "event": "Heat Advisory",
                  "sender": "National Weather Service",
                  "severity": "Moderate",
                  "description": "Heat index values up to 105 degrees expected. Hot temperatures and high humidity may cause heat illnesses.",
                  "ends": "Today 8:00 PM"
                }
              ]
            }
            
            Valid 'icon' strings for both current weather and forecast are only: "sunny", "cloudy", "rainy", "thunderstorm", "snowy", "foggy", "windy".
            Ensure the fields are accurate to the climate or typical weather of the resolved city for this time (June), provide the exact input coordinates ($lat and $lon) for the location, estimate a realistic 'uvIndex' (e.g. 0.0 to 11.0+ depending on weather/sunshine/latitude), make the 'commentary' lively, slightly witty, and highly descriptive, and generate a highly realistic 7-day forecast with consecutive days (using standard abbreviated day names starting from the upcoming day of the week).
            If there are severe/noteworthy weather conditions for the current location or season, include 1 or 2 appropriate and realistic alerts (such as Heat Advisory, Air Quality Index Watch, Severe Thunderstorm Warning, Flood Advisory, or High Wind Warning) under the "alerts" list. If the weather is perfectly safe, return either a seasonal/environmental alert (such as 'UV Exposure Notice', 'Grass Pollen Advisory' or 'Ozone Alert') so the user can see highly helpful context and localized hazard warnings in action.
            
            ${if (simulateDisaster != null) {
                """
                CRITICAL INSTRUCTION - USER SIMULATING DISASTER:
                The user has requested to simulate a severe natural disaster of type: "$simulateDisaster" for this location.
                You MUST return weather details reflecting this catastrophe!
                In the "alerts" field:
                Include a highly extreme alert object with:
                - "event": A warning matching the disaster (e.g. "Tsunami Warning", "Tornado Warning", "Earthquake Alert", "Volcanic Eruption Emergency", "Catastrophic Flood Crisis")
                - "sender": "Emergency Management & Life-Saving Command"
                - "severity": "Extreme"
                - "description": A safety and evacuation guidance.
                - "ends": "Active Emergency"
                In the commentary field, output a warning message in dramatic tone ordering citizens to comply with emergency orders.
                """
            } else ""}
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt))
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.7
            )
        )

        try {
            val response = executeWithRetry { apiService.generateContent(apiKey, request) }
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw Exception("No content received from Gemini API.")

            Log.d("GeminiRepository", "Raw Gemini Coordinate Response: $rawText")

            val sanitized = sanitizeJsonResponse(rawText)
            val jsonAdapter = moshi.adapter(GeminiWeatherData::class.java)
            val data = jsonAdapter.fromJson(sanitized)
                ?: throw Exception("Failed to convert Gemini output to weather data.")

            data
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error fetching weather data by coordinates from Gemini API", e)
            throw e
        }
    }

    private fun sanitizeJsonResponse(rawText: String): String {
        return rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
