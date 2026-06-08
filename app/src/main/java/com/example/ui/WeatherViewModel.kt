package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ForecastDay
import com.example.data.WeatherRepository
import com.example.data.WeatherResponse
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

class WeatherViewModel : ViewModel() {
    private val repository by lazy { WeatherRepository() }
    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        fetchWeather()
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
