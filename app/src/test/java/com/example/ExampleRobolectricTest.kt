package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.example.data.AirQualitySummary
import com.example.data.ForecastDay
import com.example.data.HourlyForecastItem
import com.example.data.SavedLocation
import com.example.data.UserPreferences
import com.example.data.WeatherDashboardData
import com.example.ui.components.WeatherHeroCard
import com.example.ui.theme.AuraWeatherTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `app name matches Aura Weather`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Weather", appName)
    }

    @Test
    fun `weather hero card renders city and temperature`() {
        val sampleData = WeatherDashboardData(
            location = SavedLocation("Berlin", "Berlin", "Germany", 52.52, 13.41),
            currentTimeIso = "2026-11-22T12:00",
            timezoneId = "Europe/Berlin",
            temperatureC = 14.2,
            apparentTemperatureC = 12.6,
            todayHighC = 16.0,
            todayLowC = 7.0,
            weatherCode = 2,
            isDay = true,
            humidityPercent = 64,
            cloudCoverPercent = 40,
            windSpeedKmh = 18.0,
            windGustsKmh = 27.0,
            windDirectionDegrees = 225,
            surfacePressureHpa = 1016.0,
            visibilityKm = 18.5,
            currentUvIndex = 2.5,
            maxUvIndexToday = 3.2,
            sunriseTime = "07:38",
            sunsetTime = "16:04",
            sunProgressFraction = 0.52f,
            hourlyForecast = emptyList<HourlyForecastItem>(),
            dailyForecast = emptyList<ForecastDay>(),
            airQuality = AirQualitySummary(18, 24, 6.2, 11.4, 48.0, 14.0),
            lastSyncedEpochMillis = 1_763_812_800_000L
        )

        composeTestRule.setContent {
            AuraWeatherTheme {
                WeatherHeroCard(
                    data = sampleData,
                    preferences = UserPreferences(),
                    isPinned = true,
                    onTogglePin = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("main_weather_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Berlin").assertIsDisplayed()
        composeTestRule.onNodeWithText("14").assertIsDisplayed()
        composeTestRule.onNodeWithText("Partly Cloudy").assertIsDisplayed()
    }
}
