package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PressureUnit
import com.example.data.SavedLocation
import com.example.data.TemperatureUnit
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.data.WeatherDashboardData
import com.example.data.WeatherRepository
import com.example.data.WindSpeedUnit
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface WeatherUiState {
    data object Loading : WeatherUiState
    data class Success(val data: WeatherDashboardData) : WeatherUiState
    data class Error(val message: String, val location: SavedLocation) : WeatherUiState
}

sealed interface SearchUiState {
    data object Idle : SearchUiState
    data object Searching : SearchUiState
    data class Results(val query: String, val items: List<SavedLocation>) : SearchUiState
    data class Error(val message: String) : SearchUiState
}

@OptIn(FlowPreview::class)
class WeatherViewModel(application: Application) : AndroidViewModel(application) {
    private val weatherRepository = WeatherRepository()
    private val prefsRepository = UserPreferencesRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<WeatherUiState>(WeatherUiState.Loading)
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isLocating = MutableStateFlow(false)
    val isLocating: StateFlow<Boolean> = _isLocating.asStateFlow()

    private val _locationMessage = MutableStateFlow<String?>(null)
    val locationMessage: StateFlow<String?> = _locationMessage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val searchState: StateFlow<SearchUiState> = _searchState.asStateFlow()

    val preferences: StateFlow<UserPreferences> = prefsRepository.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    val savedLocations: StateFlow<List<SavedLocation>> = prefsRepository.savedLocationsFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPreferencesRepository.DEFAULT_LOCATIONS
        )

    val activeLocation: StateFlow<SavedLocation> = prefsRepository.activeLocationFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UserPreferencesRepository.DEFAULT_LOCATIONS.first()
        )

    init {
        viewModelScope.launch {
            val initialLocation = prefsRepository.activeLocationFlow.first()
            loadWeather(initialLocation, showFullLoader = true)
        }

        viewModelScope.launch {
            _searchQuery
                .debounce(350L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    performCitySearch(query)
                }
        }
    }

    fun selectLocation(location: SavedLocation, saveToList: Boolean = false) {
        viewModelScope.launch {
            prefsRepository.setActiveLocation(location)
            if (saveToList) {
                prefsRepository.addSavedLocation(location)
            }
            loadWeather(location, showFullLoader = true)
        }
    }

    fun togglePinLocation(location: SavedLocation) {
        viewModelScope.launch {
            val exists = savedLocations.value.any { it.matchesCoordinates(location.latitude, location.longitude) }
            if (exists) {
                if (savedLocations.value.size > 1) {
                    prefsRepository.removeSavedLocation(location)
                }
            } else {
                prefsRepository.addSavedLocation(location)
            }
        }
    }

    fun removeSavedLocation(location: SavedLocation) {
        viewModelScope.launch {
            prefsRepository.removeSavedLocation(location)
        }
    }

    fun refreshWeather() {
        viewModelScope.launch {
            val loc = activeLocation.value
            loadWeather(loc, showFullLoader = _uiState.value !is WeatherUiState.Success)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        if (query.trim().length < 2) {
            _searchState.value = SearchUiState.Idle
        }
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _searchState.value = SearchUiState.Idle
    }

    fun setLocating(locating: Boolean) {
        _isLocating.value = locating
    }

    fun postLocationMessage(message: String?) {
        _locationMessage.value = message
    }

    fun loadWeatherByCoordinates(context: Context, lat: Double, lon: Double) {
        viewModelScope.launch {
            _isLocating.value = true
            try {
                val resolved = weatherRepository.reverseGeocode(context, lat, lon)
                prefsRepository.setActiveLocation(resolved)
                prefsRepository.addSavedLocation(resolved)
                loadWeather(resolved, showFullLoader = true)
            } catch (e: Exception) {
                _locationMessage.value = e.message ?: "Failed to resolve current location"
            } finally {
                _isLocating.value = false
            }
        }
    }

    fun setTemperatureUnit(unit: TemperatureUnit) {
        viewModelScope.launch { prefsRepository.setTemperatureUnit(unit) }
    }

    fun setWindSpeedUnit(unit: WindSpeedUnit) {
        viewModelScope.launch { prefsRepository.setWindSpeedUnit(unit) }
    }

    fun setPressureUnit(unit: PressureUnit) {
        viewModelScope.launch { prefsRepository.setPressureUnit(unit) }
    }

    private suspend fun performCitySearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            _searchState.value = SearchUiState.Idle
            return
        }
        _searchState.value = SearchUiState.Searching
        try {
            val results = weatherRepository.searchCities(trimmed)
            _searchState.value = SearchUiState.Results(trimmed, results)
        } catch (e: Exception) {
            _searchState.value = SearchUiState.Error(e.message ?: "City search failed")
        }
    }

    private suspend fun loadWeather(location: SavedLocation, showFullLoader: Boolean) {
        if (showFullLoader) {
            _uiState.value = WeatherUiState.Loading
        } else {
            _isRefreshing.value = true
        }
        try {
            val dashboard = weatherRepository.getWeatherDashboard(location)
            _uiState.value = WeatherUiState.Success(dashboard)
        } catch (e: Exception) {
            if (_uiState.value !is WeatherUiState.Success || showFullLoader) {
                _uiState.value = WeatherUiState.Error(
                    message = e.message ?: "Unable to fetch weather telemetry",
                    location = location
                )
            } else {
                _locationMessage.value = "Refresh failed: ${e.message ?: "Network error"}"
            }
        } finally {
            _isRefreshing.value = false
        }
    }
}
