package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ForecastDay
import com.example.data.WeatherRepository
import com.example.data.WeatherResponse
import com.example.data.GeminiWeatherRepository
import com.example.data.GeminiWeatherData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import kotlinx.coroutines.withTimeout
import android.util.Log

sealed class WeatherUiState {
    object Loading : WeatherUiState()
    data class Success(
        val weather: WeatherResponse,
        val forecast: List<ForecastDay>
    ) : WeatherUiState()
    data class Error(val message: String) : WeatherUiState()
}

sealed class GeminiWeatherUiState {
    object Idle : GeminiWeatherUiState()
    object Loading : GeminiWeatherUiState()
    data class Success(val data: GeminiWeatherData) : GeminiWeatherUiState()
    data class Error(val message: String) : GeminiWeatherUiState()
}

class WeatherViewModel : ViewModel() {
    private val repository by lazy { WeatherRepository() }
    private val geminiRepository by lazy { GeminiWeatherRepository() }

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _geminiState = MutableStateFlow<GeminiWeatherUiState>(GeminiWeatherUiState.Idle)
    val geminiState = _geminiState.asStateFlow()

    private val _isAiMode = MutableStateFlow(true) // Default to Gemini AI mode
    val isAiMode = _isAiMode.asStateFlow()

    private val _currentCity = MutableStateFlow("Berlin")
    val currentCity = _currentCity.asStateFlow()

    init {
        // Fetch static forecast from Open-Meteo
        fetchWeather()
        // Fetch real-time AI weather from Gemini
        fetchGeminiWeather("Berlin")
    }

    fun setAiMode(enabled: Boolean) {
        _isAiMode.value = enabled
    }

    fun fetchGeminiWeather(city: String, simulateDisaster: String? = null) {
        _currentCity.value = city
        viewModelScope.launch {
            _geminiState.value = GeminiWeatherUiState.Loading
            try {
                val data = geminiRepository.getGeminiWeather(city, simulateDisaster)
                _geminiState.value = GeminiWeatherUiState.Success(data)
                
                // Sync standard radar view with resolved coordinates
                val lat = data.latitude
                val lon = data.longitude
                if (lat != null && lon != null) {
                    fetchWeather(lat, lon)
                }
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching Gemini weather", e)
                _geminiState.value = GeminiWeatherUiState.Error(e.message ?: "Unknown API error")
            }
        }
    }

    fun fetchGeminiWeatherByCoordinates(lat: Double, lon: Double) {
        viewModelScope.launch {
            _geminiState.value = GeminiWeatherUiState.Loading
            try {
                val data = geminiRepository.getGeminiWeatherByCoordinates(lat, lon)
                _geminiState.value = GeminiWeatherUiState.Success(data)
                _currentCity.value = data.city
                
                // Sync standard radar view with resolved coordinates
                val resolvedLat = data.latitude ?: lat
                val resolvedLon = data.longitude ?: lon
                fetchWeather(resolvedLat, resolvedLon)
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching Gemini weather by coordinates", e)
                _geminiState.value = GeminiWeatherUiState.Error(e.message ?: "Unknown API error")
            }
        }
    }

    fun fetchWeather(lat: Double = 52.52, lon: Double = 13.41) {
        viewModelScope.launch {
            _uiState.value = WeatherUiState.Loading
            try {
                val response = withTimeout(10000L) {
                    repository.getWeatherData(lat, lon)
                }
                val daily = response.daily
                val forecast = mutableListOf<ForecastDay>()
                
                if (daily != null) {
                    for (i in daily.time.indices) {
                        forecast.add(
                            ForecastDay(
                                date = daily.time[i],
                                weatherCode = daily.weatherCode[i],
                                tempMax = daily.tempMax[i],
                                tempMin = daily.tempMin[i]
                            )
                        )
                    }
                }
                
                _uiState.value = WeatherUiState.Success(response, forecast)
                Log.d("WeatherViewModel", "Weather data fetched successfully")
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather", e)
                _uiState.value = WeatherUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

