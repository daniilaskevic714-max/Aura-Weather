package com.example.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.SavedLocation
import com.example.ui.WeatherUiState
import com.example.ui.WeatherViewModel
import com.example.ui.components.AtmosphericBackground
import com.example.ui.components.AtmosphericMetricsGrid
import com.example.ui.components.DailyForecastSection
import com.example.ui.components.HourlyForecastSection
import com.example.ui.components.WeatherHeroCard
import com.example.ui.components.auraGlassCard
import com.example.ui.theme.AuraCoralWarn
import com.example.ui.theme.AuraMidnight
import com.example.ui.theme.AuraSkyAccent
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDashboardScreen(viewModel: WeatherViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isLocating by viewModel.isLocating.collectAsStateWithLifecycle()
    val locationMessage by viewModel.locationMessage.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()
    val activeLocation by viewModel.activeLocation.collectAsStateWithLifecycle()

    var showSearchSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(locationMessage) {
        locationMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.postLocationMessage(null)
        }
    }

    @SuppressLint("MissingPermission")
    fun resolveCurrentDeviceLocation() {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            viewModel.postLocationMessage("Location permission is required to detect your city.")
            return
        }

        viewModel.setLocating(true)
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        val lastKnown = providers.firstNotNullOfOrNull { provider ->
            runCatching {
                if (locationManager.isProviderEnabled(provider)) {
                    locationManager.getLastKnownLocation(provider)
                } else null
            }.getOrNull()
        }

        if (lastKnown != null) {
            showSearchSheet = false
            viewModel.loadWeatherByCoordinates(context, lastKnown.latitude, lastKnown.longitude)
            return
        }

        val activeProvider = providers.firstOrNull { provider ->
            runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
        }

        if (activeProvider != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            runCatching {
                locationManager.getCurrentLocation(activeProvider, null, context.mainExecutor) { loc ->
                    if (loc != null) {
                        showSearchSheet = false
                        viewModel.loadWeatherByCoordinates(context, loc.latitude, loc.longitude)
                    } else {
                        viewModel.setLocating(false)
                        viewModel.postLocationMessage("GPS fix unavailable right now. Please select a city manually.")
                    }
                }
            }.onFailure {
                viewModel.setLocating(false)
                viewModel.postLocationMessage("Location service unavailable on this device.")
            }
        } else {
            viewModel.setLocating(false)
            viewModel.postLocationMessage("No GPS signal available. Please search for your city.")
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            resolveCurrentDeviceLocation()
        } else {
            viewModel.postLocationMessage("Location permission denied. Use search to choose any city.")
        }
    }

    fun triggerLocationLookup() {
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (hasFine || hasCoarse) {
            resolveCurrentDeviceLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val (bgCode, bgIsDay) = when (val state = uiState) {
        is WeatherUiState.Success -> state.data.weatherCode to state.data.isDay
        else -> 1 to true
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AtmosphericBackground(weatherCode = bgCode, isDay = bgIsDay)

        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { _ ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 720.dp)
                        .fillMaxSize()
                        .align(Alignment.TopCenter)
                ) {
                    TopToolbar(
                        activeLocation = activeLocation,
                        onOpenSearch = { showSearchSheet = true },
                        onOpenSettings = { showSettingsSheet = true }
                    )

                    SavedLocationPills(
                        locations = savedLocations,
                        activeLocation = activeLocation,
                        onSelectLocation = { viewModel.selectLocation(it) }
                    )

                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refreshWeather() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        when (val state = uiState) {
                            is WeatherUiState.Loading -> {
                                WeatherLoadingSkeleton(bottomPadding = navBottom + 24.dp)
                            }
                            is WeatherUiState.Error -> {
                                WeatherErrorView(
                                    message = state.message,
                                    onRetry = { viewModel.refreshWeather() },
                                    onSearch = { showSearchSheet = true }
                                )
                            }
                            is WeatherUiState.Success -> {
                                val data = state.data
                                val isPinned = savedLocations.any {
                                    it.matchesCoordinates(data.location.latitude, data.location.longitude)
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(
                                        start = 18.dp,
                                        end = 18.dp,
                                        top = 8.dp,
                                        bottom = navBottom + 28.dp
                                    ),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    item(key = "hero") {
                                        WeatherHeroCard(
                                            data = data,
                                            preferences = preferences,
                                            isPinned = isPinned,
                                            onTogglePin = { viewModel.togglePinLocation(data.location) }
                                        )
                                    }
                                    item(key = "hourly") {
                                        HourlyForecastSection(
                                            hourlyItems = data.hourlyForecast,
                                            temperatureUnit = preferences.temperatureUnit
                                        )
                                    }
                                    item(key = "daily") {
                                        DailyForecastSection(
                                            forecast = data.dailyForecast,
                                            temperatureUnit = preferences.temperatureUnit
                                        )
                                    }
                                    item(key = "metrics") {
                                        AtmosphericMetricsGrid(
                                            data = data,
                                            preferences = preferences
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSearchSheet) {
            CitySearchSheet(
                query = searchQuery,
                searchState = searchState,
                savedLocations = savedLocations,
                activeLocation = activeLocation,
                isLocating = isLocating,
                onQueryChange = viewModel::updateSearchQuery,
                onClearQuery = viewModel::clearSearch,
                onSelectSearchResult = { loc ->
                    showSearchSheet = false
                    viewModel.clearSearch()
                    viewModel.selectLocation(loc, saveToList = true)
                },
                onSelectSavedLocation = { loc ->
                    showSearchSheet = false
                    viewModel.selectLocation(loc)
                },
                onRemoveSavedLocation = viewModel::removeSavedLocation,
                onRequestCurrentLocation = ::triggerLocationLookup,
                onDismiss = { showSearchSheet = false }
            )
        }

        if (showSettingsSheet) {
            SettingsSheet(
                preferences = preferences,
                onSelectTempUnit = viewModel::setTemperatureUnit,
                onSelectWindUnit = viewModel::setWindSpeedUnit,
                onSelectPressureUnit = viewModel::setPressureUnit,
                onDismiss = { showSettingsSheet = false }
            )
        }
    }
}

@Composable
private fun TopToolbar(
    activeLocation: SavedLocation,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenSearch)
                .testTag("open_search_button"),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = AuraSkyAccent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = activeLocation.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = AuraTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search cities",
                    tint = AuraTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier.size(48.dp)
        ) {
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("open_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Units and preferences",
                    tint = AuraTextPrimary
                )
            }
        }
    }
}

@Composable
private fun SavedLocationPills(
    locations: List<SavedLocation>,
    activeLocation: SavedLocation,
    onSelectLocation: (SavedLocation) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(locations, key = { "${it.name}_${it.latitude}_${it.longitude}" }) { loc ->
            val selected = loc.matchesCoordinates(activeLocation.latitude, activeLocation.longitude)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (selected) AuraSkyAccent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected) AuraSkyAccent.copy(alpha = 0.55f) else Color.White.copy(alpha = 0.10f)
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectLocation(loc) }
                    .testTag("saved_city_chip_${loc.name.lowercase().replace(" ", "_")}")
            ) {
                Text(
                    text = loc.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) AuraSkyAccent else AuraTextSecondary,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun WeatherLoadingSkeleton(bottomPadding: androidx.compose.ui.unit.Dp) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .padding(bottom = bottomPadding)
            .testTag("weather_loading_skeleton"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .auraGlassCard(32.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.loading_weather),
                style = MaterialTheme.typography.bodyLarge,
                color = AuraTextSecondary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .auraGlassCard(28.dp)
                .background(Color.White.copy(alpha = 0.02f))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .auraGlassCard(28.dp)
                .background(Color.White.copy(alpha = 0.02f))
        )
    }
}

@Composable
private fun WeatherErrorView(
    message: String,
    onRetry: () -> Unit,
    onSearch: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("weather_error_view"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .auraGlassCard(28.dp)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                tint = AuraCoralWarn,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.error_fetching),
                style = MaterialTheme.typography.titleLarge,
                color = AuraTextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AuraSkyAccent,
                        contentColor = AuraMidnight
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onSearch,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.10f),
                        contentColor = AuraTextPrimary
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Choose City")
                }
            }
        }
    }
}
