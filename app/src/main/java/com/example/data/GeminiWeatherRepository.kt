package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiWeatherRepository {
    private val apiService = GeminiRetrofitClient.service
    private val moshi = GeminiRetrofitClient.moshiInstance

    suspend fun getGeminiWeather(city: String): GeminiWeatherData = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("Missing Gemini API Key! Please click the Secrets panel in AI Studio and configure GEMINI_API_KEY with your API key.")
        }

        val prompt = """
            Provide realistic current weather data for the city of $city.
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
              "longitude": 13.41
            }
            
            Valid 'icon' strings are only: "sunny", "cloudy", "rainy", "thunderstorm", "snowy", "foggy", "windy".
            Ensure the fields are accurate to the climate or typical weather of $city for this time, provide accurate geographic 'latitude' and 'longitude' coordinates for this city, and make the 'commentary' lively, slightly witty, and highly descriptive.
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
            val response = apiService.generateContent(apiKey, request)
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

    private fun sanitizeJsonResponse(rawText: String): String {
        return rawText
            .trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
    }
}
