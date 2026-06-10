package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.unit.Dp
import com.example.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.testTag
import com.example.R
import com.example.data.ForecastDay
import com.example.data.WeatherResponse
import com.example.data.GeminiWeatherData
import java.text.SimpleDateFormat
import java.util.*

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import kotlin.math.sin

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val geminiState by viewModel.geminiState.collectAsState()
    val isAiMode by viewModel.isAiMode.collectAsState()
    val currentCity by viewModel.currentCity.collectAsState()

    var searchInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    var isLocating by remember { mutableStateOf(false) }

    fun fetchCurrentLocation() {
        isLocating = true
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        
        val hasFinePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        if (hasFinePermission || hasCoarsePermission) {
            val provider = if (hasNetwork) LocationManager.NETWORK_PROVIDER else if (hasGps) LocationManager.GPS_PROVIDER else LocationManager.PASSIVE_PROVIDER
            try {
                // Get last known fallback first as a super fast responder
                val lastKnown = locationManager.getLastKnownLocation(provider)
                if (lastKnown != null) {
                    val lat = lastKnown.latitude
                    val lon = lastKnown.longitude
                    viewModel.fetchGeminiWeatherByCoordinates(lat, lon)
                    isLocating = false
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        locationManager.getCurrentLocation(
                            provider,
                            null,
                            context.mainExecutor
                        ) { location ->
                            if (location != null) {
                                viewModel.fetchGeminiWeatherByCoordinates(location.latitude, location.longitude)
                            } else {
                                Toast.makeText(context, "Device coordinates not locked. Simulating current location...", Toast.LENGTH_LONG).show()
                                viewModel.fetchGeminiWeatherByCoordinates(40.7128, -74.0060) // New York fallback
                            }
                            isLocating = false
                        }
                    } else {
                        val lastKnownGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        val lastKnownNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        val bestLocation = lastKnownGps ?: lastKnownNet
                        if (bestLocation != null) {
                            viewModel.fetchGeminiWeatherByCoordinates(bestLocation.latitude, bestLocation.longitude)
                        } else {
                            Toast.makeText(context, "Device coordinates not locked. Simulating current location...", Toast.LENGTH_SHORT).show()
                            viewModel.fetchGeminiWeatherByCoordinates(48.8566, 2.3522) // Paris fallback
                        }
                        isLocating = false
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Permission error: ${e.message}", Toast.LENGTH_SHORT).show()
                isLocating = false
            } catch (e: Exception) {
                Log.e("WeatherScreen", "Location dispatch failed", e)
                Toast.makeText(context, "Location lookup failed. Simulating London...", Toast.LENGTH_SHORT).show()
                viewModel.fetchGeminiWeatherByCoordinates(51.5074, -0.1278) // London fallback
                isLocating = false
            }
        } else {
            Toast.makeText(context, "Requesting location permissions...", Toast.LENGTH_SHORT).show()
            isLocating = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            fetchCurrentLocation()
        } else {
            Toast.makeText(context, "Location permission denied. Simulating Tokyo...", Toast.LENGTH_LONG).show()
            viewModel.fetchGeminiWeatherByCoordinates(35.6762, 139.6503) // Tokyo fallback
        }
    }

    val activeStyle = remember(isAiMode, uiState, geminiState) {
        if (isAiMode) {
            when (val state = geminiState) {
                is GeminiWeatherUiState.Success -> {
                    val alertEvent = state.data.alerts?.firstOrNull()?.event?.lowercase() ?: ""
                    val alertDesc = state.data.alerts?.firstOrNull()?.description?.lowercase() ?: ""
                    when {
                        alertEvent.contains("tsunami") || alertDesc.contains("tsunami") || alertDesc.contains("цунами") -> "tsunami"
                        alertEvent.contains("tornado") || alertDesc.contains("tornado") || alertDesc.contains("торнадо") -> "tornado"
                        alertEvent.contains("hurricane") || alertDesc.contains("hurricane") || alertDesc.contains("ураган") -> "hurricane"
                        alertEvent.contains("volcan") || alertDesc.contains("volcan") || alertDesc.contains("вулкан") -> "volcano"
                        alertEvent.contains("flood") || alertDesc.contains("flood") || alertDesc.contains("наводн") -> "flood"
                        alertEvent.contains("earthquake") || alertDesc.contains("earthquake") || alertDesc.contains("землетряс") -> "volcano"
                        else -> state.data.icon.lowercase()
                    }
                }
                else -> "neutral"
            }
        } else {
            when (val state = uiState) {
                is WeatherUiState.Success -> {
                    getWeatherDescription(state.weather.current?.weatherCode ?: 0).lowercase()
                }
                else -> "neutral"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Dynamic live atmospheric aurora gradient waves
        AtmosphericAuroraBackground(style = activeStyle)
        
        // Active visual simulation particle dynamics
        AtmosphericWeatherParticles(style = activeStyle)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
            ) {
            // Dynamic Top Navigation / Search Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchInput,
                    onValueChange = { searchInput = it },
                    placeholder = { Text("Search city (e.g., Paris)...", color = ImmersiveTextSecondary) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("city_search_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.08f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                        focusedBorderColor = ImmersivePrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    trailingIcon = {
                        if (searchInput.isNotEmpty()) {
                            IconButton(onClick = { searchInput = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = ImmersivePrimary)
                            }
                        }
                    }
                )

                Button(
                    onClick = {
                        if (searchInput.trim().isNotEmpty()) {
                            val city = searchInput.trim()
                            if (isAiMode) {
                                viewModel.fetchGeminiWeather(city)
                            } else {
                                viewModel.fetchGeminiWeather(city)
                            }
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("city_search_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.03f)))),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Execute search", tint = ImmersivePrimary)
                }

                Button(
                    onClick = {
                        val hasFinePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        val hasCoarsePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                        if (hasFinePermission || hasCoarsePermission) {
                            fetchCurrentLocation()
                        } else {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("use_current_location_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.2.dp, Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.03f)))),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    if (isLocating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = ImmersivePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Use current location",
                            tint = ImmersivePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // AI vs Standard toggle row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .glassmorphic(16.dp)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Button(
                    onClick = { viewModel.setAiMode(true) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("toggle_ai_mode"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isAiMode) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                        contentColor = if (isAiMode) Color.White else ImmersiveTextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (isAiMode) ImmersivePrimary else ImmersiveTextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("🤖 AI Weather", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Button(
                    onClick = { viewModel.setAiMode(false) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("toggle_standard_mode"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isAiMode) Color.White.copy(alpha = 0.12f) else Color.Transparent,
                        contentColor = if (!isAiMode) Color.White else ImmersiveTextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = if (!isAiMode) ImmersivePrimary else ImmersiveTextSecondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("📡 Standard Radar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = isAiMode) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_warning),
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SIMULATE HAZARD / СИМУЛИРОВАТЬ СТИХИЮ:",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = ImmersiveTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        val disasters = listOf(
                            Triple("Tsunami", "Tsunami / Цунами", R.drawable.ic_tsunami),
                            Triple("Tornado", "Tornado / Торнадо", R.drawable.ic_tornado),
                            Triple("Hurricane", "Hurricane / Ураган", R.drawable.ic_hurricane),
                            Triple("Earthquake", "Earthquake / Землетрясение", R.drawable.ic_earthquake),
                            Triple("Volcanic Eruption", "Volcano / Вулкан", R.drawable.ic_volcano),
                            Triple("Flood", "Flood / Наводнение", R.drawable.ic_flood)
                        )
                        
                        items(disasters) { (type, label, iconRes) ->
                            AssistChip(
                                onClick = {
                                    viewModel.fetchGeminiWeather(currentCity, type)
                                },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(id = iconRes),
                                        contentDescription = null,
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 11.sp) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFFF44336).copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, Color(0xFFF44336).copy(alpha = 0.4f)),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isAiMode) {
                // Gemini flow
                when (val state = geminiState) {
                    is GeminiWeatherUiState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Awaiting query...", color = ImmersiveTextSecondary)
                        }
                    }
                    is GeminiWeatherUiState.Loading -> {
                        GeminiWeatherSkeleton()
                    }
                    is GeminiWeatherUiState.Success -> {
                        GeminiWeatherDisplay(state.data)
                    }
                    is GeminiWeatherUiState.Error -> {
                        if (state.message.contains("Secrets panel") || state.message.contains("key") || state.message.contains("Key")) {
                            GeminiApiKeyWarningCard()
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = state.message,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Button(
                                        onClick = { viewModel.fetchGeminiWeather(currentCity) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ImmersiveSurface)
                                    ) {
                                        Text("Повторить попытку / Retry", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Original standard flow
                when (val state = uiState) {
                    is WeatherUiState.Loading -> {
                        StandardWeatherSkeleton()
                    }
                    is WeatherUiState.Success -> {
                        WeatherContent(state.weather, state.forecast, currentCity)
                    }
                    is WeatherUiState.Error -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = state.message, color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.fetchWeather() }) {
                                    Text("Повторить попытку / Retry")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
fun AnimatedWeatherIcon(
    icon: ImageVector,
    color: Color,
    animationStyle: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "weather_icon_transition")
    
    val animationModifier = when (animationStyle.lowercase()) {
        "sunny", "clear sky", "clear" -> {
            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 16000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "sun_rotation"
            )
            Modifier.graphicsLayer(rotationZ = rotation)
        }
        "cloudy", "partly cloudy", "foggy", "cloud", "fog", "drizzle" -> {
            val translationY by infiniteTransition.animateFloat(
                initialValue = -6f,
                targetValue = 6f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 4000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "cloud_floating"
            )
            Modifier.graphicsLayer(translationY = translationY)
        }
        "rainy", "thunderstorm", "snowy", "rain", "snow", "rain showers" -> {
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "rain_pulse"
            )
            Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
        }
        "windy", "wind" -> {
            val translationX by infiniteTransition.animateFloat(
                initialValue = -8f,
                targetValue = 8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "wind_drift"
            )
            Modifier.graphicsLayer(translationX = translationX)
        }
        else -> {
            Modifier
        }
    }

    // Glow scale pulsing
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_glow_pulse"
    )

    Box(
        modifier = modifier.then(animationModifier),
        contentAlignment = Alignment.Center
    ) {
        // Deep radial glow backdrop
        Box(
            modifier = Modifier
                .size(76.dp)
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                }
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.35f), Color.Transparent)
                    )
                )
        )
        
        // Secondary neon colored outline layer
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 1.04f
                    scaleY = 1.04f
                    alpha = 0.5f
                },
            tint = color
        )
        
        // Intense bright white core of the outline sign
        Icon(
            imageVector = icon,
            contentDescription = "Weather Animated Icon",
            modifier = Modifier.fillMaxSize(),
            tint = Color.White
        )
    }
}

@Composable
fun GeminiForecastItem(forecastDay: com.example.data.GeminiForecastDay) {
    val (icon, color) = remember(forecastDay.icon) {
        val iconRes = when (forecastDay.icon.lowercase()) {
            "sunny" -> Icons.Outlined.WbSunny
            "cloudy" -> Icons.Outlined.CloudQueue
            "rainy" -> Icons.Outlined.Umbrella
            "thunderstorm" -> Icons.Outlined.Thunderstorm
            "snowy" -> Icons.Outlined.AcUnit
            "foggy" -> Icons.Outlined.Cloud
            "windy" -> Icons.Outlined.Air
            else -> Icons.Outlined.WbCloudy
        }
        val colorRes = when (forecastDay.icon.lowercase()) {
            "sunny" -> Color(0xFFFFD600)
            "cloudy" -> Color(0xFFCFD1D6)
            "rainy" -> Color(0xFF29B6F6)
            "thunderstorm" -> Color(0xFFAB47BC)
            "snowy" -> Color(0xFF80DEEA)
            "foggy" -> Color(0xFF90A4AE)
            "windy" -> Color(0xFF66BB6A)
            else -> ImmersivePrimary
        }
        iconRes to colorRes
    }

    Card(
        modifier = Modifier
            .width(115.dp)
            .padding(end = 8.dp)
            .glassmorphic(24.dp)
            .testTag("gemini_forecast_card_${forecastDay.day.lowercase()}"),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = forecastDay.day.uppercase(),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                color = ImmersiveTextSecondary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Icon(
                imageVector = icon,
                contentDescription = forecastDay.condition,
                modifier = Modifier.size(36.dp),
                tint = color
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = forecastDay.condition,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${forecastDay.tempMax.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImmersivePrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${forecastDay.tempMin.toInt()}°",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
        }
    }
}

fun getDisasterSafetySteps(eventName: String): List<String> {
    val ev = eventName.lowercase()
    return when {
        ev.contains("tsunami") || ev.contains("цунами") -> listOf(
            "Move inland to high ground immediately upon receiving warning / Немедленно уходите на возвышенность при угрозе цунами.",
            "Stay away from beaches, coastal harbors, and river mouths / Держитесь как можно дальше от побережья и устьев рек.",
            "Never go down to the shore to watch a tsunami wave / Ни в коем случае не спускайтесь к берегу посмотреть на волны.",
            "Monitor official emergency signals and instructions / Внимательно следите за официальными сигналами оповещения."
        )
        ev.contains("tornado") || ev.contains("торнадо") || ev.contains("смерч") -> listOf(
            "Seek shelter in a basement, storm cellar, or interior room / Срочно укройтесь в подвале, погребе или надежной внутренней комнате.",
            "Stay away from windows, outer walls, and glass surfaces / Находитесь вдали от окон, внешних стен и стекол.",
            "Protect your head and neck with arms, heavy clothes, or pillows / Защитите голову и шею руками, одеждой или подушками.",
            "If in a mobile home or vehicle, evacuate to a sturdy building / Немедленно покиньте легкие строения или автомобили."
        )
        ev.contains("earthquake") || ev.contains("землетрясение") -> listOf(
            "Drop, Cover, and Hold On under sturdy furniture / Лягте на пол, укройтесь под крепким столом и держитесь.",
            "Stay inside until shaking stops; beware of falling objects / Не выбегайте на улицу во время толчков, берегитесь обломков.",
            "If outdoors, move to an open area away from power lines and buildings / На улице отойдите на открытое пространство.",
            "Be prepared for potential strong aftershocks / Будьте готовы к повторным толчкам (афтершокам)."
        )
        ev.contains("hurricane") || ev.contains("ураган") || ev.contains("тайфун") || ev.contains("циклон") -> listOf(
            "Secure high-risk outdoor items and reinforce windows/doors / Закрепите предметы на улице и надежно закройте все окна.",
            "Stay indoors in a central room, away from glass and outer walls / Находитесь во внутренних помещениях здания.",
            "Keep emergency food, drinking water, and flashlight ready / Подготовьте аварийный запас еды, воды и фонарик.",
            "Evacuate immediately if order is issued by emergency staff / Срочно эвакуируйтесь при объявлении официального приказа."
        )
        ev.contains("volcan") || ev.contains("извержение") -> listOf(
            "Evacuate the exclusion zone according to local instructions / Срочно эвакуируйтесь из опасной зоны вулкана.",
            "Wear high-efficiency respiratory protection (N95) and goggles / Используйте респираторы и очки для защиты от пепла.",
            "Stay inside with all doors and windows tightly closed / В зоне пеплопада закройте все окна и двери и оставайтесь дома.",
            "Avoid valley drainage basins where volcanic mudslides/lahars occur / Избегайте долин рек, подверженных сходу селей."
        )
        ev.contains("flood") || ev.contains("наводнение") || ev.contains("паводок") -> listOf(
            "Move immediately to higher floors or higher land / Срочно перейдите на верхние этажи здания или возвышенности.",
            "Do NOT walk, swim, or drive through flood waters / Не пытайтесь переходить или переезжать потоки воды.",
            "Turn off domestic electricity, gas, and water inputs / Отключите в доме электричество, газ и воду.",
            "Have your emergency document backpack and supplies ready / Держите готовым тревожный рюкзак с документами."
        )
        else -> listOf(
            "Follow active evacuations and advice from municipal authorities / Следуйте указаниям представителей власти и служб спасения.",
            "Keep emergency contacts and a fully charged phone with you / Держите при себе заряженный телефон и контакты экстренных служб.",
            "Avoid unnecessary travel until active advisory ends / Воздержитесь от поездок до окончания действия предупреждения."
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun GeminiAlertItem(alert: com.example.data.GeminiAlert) {
    var isExpanded by remember { mutableStateOf(false) }
    
    val (bgColor, borderColor, iconColor, icon) = remember(alert.severity) {
        when (alert.severity.lowercase()) {
            "extreme" -> Quad(
                Color(0x22F44336), 
                Color(0x66F44336), 
                Color(0xFFE53935),
                Icons.Rounded.Warning
            )
            "severe" -> Quad(
                Color(0x22FF9800), 
                Color(0x66FF9800),
                Color(0xFFFB8C00),
                Icons.Rounded.Warning
            )
            "moderate" -> Quad(
                Color(0x1ADBFF00), 
                Color(0x40FFD54F),
                Color(0xFFFFD54F),
                Icons.Rounded.Info
            )
            else -> Quad(
                Color(0x1229B6F6), 
                Color(0x3329B6F6),
                Color(0xFF29B6F6),
                Icons.Rounded.Info
            )
        }
    }

    val isCritical = alert.severity.lowercase() == "extreme" || alert.severity.lowercase() == "severe"
    val infiniteTransition = rememberInfiniteTransition(label = "severe_alert_glow")
    val alphaGlow by if (isCritical) {
        infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alert_glow_alpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    val animBorderColor = if (isCritical) iconColor.copy(alpha = alphaGlow) else borderColor

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("gemini_alert_card_${alert.event.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, animBorderColor),
        onClick = { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = "Alert Icon",
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.event,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Issued by ${alert.sender}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = alert.severity.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = iconColor,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = alert.description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.9f),
                lineHeight = 18.sp
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ends: ${alert.ends}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isExpanded) "Hide Guide" else "Tap for Survival Info",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = "Expand guidelines",
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "SAFETY INSTRUCTIONS / ИНСТРУКЦИИ ПО БЕЗОПАСНОСТИ:",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.5.sp),
                        color = iconColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val steps = remember(alert.event) {
                        getDisasterSafetySteps(alert.event)
                    }
                    
                    steps.forEach { step ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "• ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = iconColor,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.95f),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiWeatherDisplay(data: GeminiWeatherData) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("main_weather_card"),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .glassmorphic(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Transparent
                )
            ) {
                AnimatedContent(
                    targetState = data,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                                slideInVertically(animationSpec = tween(400, easing = FastOutSlowInEasing)) { it / 3 } +
                                scaleIn(initialScale = 0.92f, animationSpec = tween(400, easing = FastOutSlowInEasing)))
                            .togetherWith(
                                fadeOut(animationSpec = tween(300, easing = FastOutSlowInEasing)) +
                                scaleOut(targetScale = 0.95f, animationSpec = tween(300, easing = FastOutSlowInEasing))
                            )
                    },
                    label = "weather_content_crossfade"
                ) { targetData ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = targetData.city.uppercase(),
                            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                            color = ImmersivePrimary,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val (icon, color) = remember(targetData.icon) {
                            val iconRes = when (targetData.icon.lowercase()) {
                                "sunny" -> Icons.Outlined.WbSunny
                                "cloudy" -> Icons.Outlined.CloudQueue
                                "rainy" -> Icons.Outlined.Umbrella
                                "thunderstorm" -> Icons.Outlined.Thunderstorm
                                "snowy" -> Icons.Outlined.AcUnit
                                "foggy" -> Icons.Outlined.Cloud
                                "windy" -> Icons.Outlined.Air
                                else -> Icons.Outlined.WbCloudy
                            }
                            val colorRes = when (targetData.icon.lowercase()) {
                                "sunny" -> Color(0xFFFFD600)
                                "cloudy" -> Color(0xFFCFD1D6)
                                "rainy" -> Color(0xFF29B6F6)
                                "thunderstorm" -> Color(0xFFAB47BC)
                                "snowy" -> Color(0xFF80DEEA)
                                "foggy" -> Color(0xFF90A4AE)
                                "windy" -> Color(0xFF66BB6A)
                                else -> ImmersivePrimary
                            }
                            iconRes to colorRes
                        }

                        AnimatedWeatherIcon(
                            icon = icon,
                            color = color,
                            animationStyle = targetData.icon,
                            modifier = Modifier.size(110.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(verticalAlignment = Alignment.Top) {
                            AnimatedContent(
                                targetState = targetData.temperature.toInt(),
                                transitionSpec = {
                                    (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + 
                                     slideInVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it / 2 })
                                        .togetherWith(fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                                },
                                label = "temp_text_crossfade"
                            ) { targetTemp ->
                                Text(
                                    text = "$targetTemp",
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontSize = 110.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-4).sp
                                    ),
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "°",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.Light
                                ),
                                color = ImmersivePrimary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        Text(
                            text = targetData.condition,
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WeatherDetailItem(Icons.Default.Air, "${targetData.windSpeed} km/h", "Wind", Modifier.weight(1f))
                            WeatherDetailItem(Icons.Default.WaterDrop, "${targetData.humidity}%", "Humidity", Modifier.weight(1f))
                            val uvDisplay = when {
                                targetData.uvIndex == null -> "Low"
                                targetData.uvIndex < 3 -> "Low"
                                targetData.uvIndex < 6 -> "Mod"
                                targetData.uvIndex < 8 -> "High"
                                targetData.uvIndex < 11 -> "V. High"
                                else -> "Extreme"
                            }
                            WeatherDetailItem(Icons.Default.WbSunny, uvDisplay, "UV Index", Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        item {
            WeatherMetricsGrid(
                windSpeed = data.windSpeed,
                humidity = data.humidity,
                uvIndex = data.uvIndex,
                isDay = true
            )
        }

        data.alerts?.takeIf { it.isNotEmpty() }?.let { alertList ->
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ACTIVE ALERTS & ADVISORIES",
                            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
                            color = Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            items(alertList) { alert ->
                GeminiAlertItem(alert)
            }
        }

        data.forecast?.let { forecastList ->
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "7-DAY OUTLOOK",
                        style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
                        color = ImmersivePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(forecastList) { dayItem ->
                        GeminiForecastItem(dayItem)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))

            // AI commentary bubble
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomEnd = 24.dp,
                    bottomStart = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = ImmersivePrimary.copy(alpha = 0.08f)
                ),
                border = BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.15f))
            ) {
                AnimatedContent(
                    targetState = data.commentary,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) togetherWith
                        fadeOut(animationSpec = tween(250, easing = FastOutSlowInEasing))
                    },
                    label = "commentary_transition"
                ) { targetCommentary ->
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = ImmersivePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GEMINI SUMMARY",
                                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
                                color = ImmersivePrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "\"$targetCommentary\"",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ImmersiveTextPrimary,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GeminiApiKeyWarningCard() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Gemini API Key Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "This weather application utilizes AI-enhanced weather conditions generated in real-time from Gemini 3.5 Flash.\n\nTo unlock, please configure GEMINI_API_KEY inside the Secrets panel of Google AI Studio.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = ImmersiveTextPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun WeatherContent(weather: WeatherResponse, forecast: List<ForecastDay>, activeCity: String) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        HeaderSection(activeCity)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                CurrentWeatherCard(weather)
                
                val current = weather.current
                if (current != null) {
                    WeatherMetricsGrid(
                        windSpeed = current.windSpeed,
                        humidity = current.humidity,
                        uvIndex = 1.8,
                        isDay = current.isDay == 1
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.forecast_7_days).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "View Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        forecast.drop(1).take(5).forEachIndexed { index, day ->
                            ForecastItem(day)
                            if (index < 4) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSection(activeCity: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp),
                color = ImmersiveSurface
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp),
                    tint = ImmersivePrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = activeCity,
                    style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 0.sp),
                    fontWeight = FontWeight.SemiBold
                )
                val sdf = SimpleDateFormat("EEEE, dd MMM", Locale.ENGLISH)
                val currentDate = sdf.format(Date()).uppercase()
                Text(
                    text = currentDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = ImmersiveTextSecondary
                )
            }
        }

        Surface(
            modifier = Modifier.size(48.dp),
            shape = RoundedCornerShape(16.dp),
            color = ImmersiveSurface
        ) {
            Icon(
                Icons.Default.Menu,
                contentDescription = null,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
fun CurrentWeatherCard(weather: WeatherResponse) {
    val current = weather.current ?: return
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .glassmorphic(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedWeatherIcon(
                icon = getWeatherIcon(current.weatherCode),
                color = Color.White,
                animationStyle = getWeatherDescription(current.weatherCode),
                modifier = Modifier.size(128.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.Top) {
                AnimatedContent(
                    targetState = current.temperature.toInt(),
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(300, easing = FastOutSlowInEasing)) + 
                         slideInVertically(animationSpec = tween(300, easing = FastOutSlowInEasing)) { it / 2 })
                            .togetherWith(fadeOut(animationSpec = tween(200, easing = FastOutSlowInEasing)))
                    },
                    label = "current_temp_text_crossfade"
                ) { targetTemp ->
                    Text(
                        text = "$targetTemp",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 110.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-4).sp
                        ),
                        color = Color.White
                    )
                }
                Text(
                    text = "°",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = ImmersivePrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = getWeatherDescription(current.weatherCode),
                style = MaterialTheme.typography.titleMedium,
                color = ImmersiveTextSecondary,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WeatherDetailItem(Icons.Default.Air, "${current.windSpeed} km/h", "Wind", Modifier.weight(1f))
                WeatherDetailItem(Icons.Default.WaterDrop, "${current.humidity}%", "Humidity", Modifier.weight(1f))
                WeatherDetailItem(Icons.Default.WbSunny, "Low", "UV Index", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun WeatherDetailItem(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(24.dp),
        color = ImmersiveSurface.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.04f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = ImmersiveTextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}

@Composable
fun ForecastItem(day: ForecastDay) {
    val dateStr = day.date
    val dayOfWeek = remember(dateStr) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val date = inputFormat.parse(dateStr)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            "???"
        }
    }

    Row(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = dayOfWeek,
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Icon(
            imageVector = getWeatherIcon(day.weatherCode),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (getWeatherDescription(day.weatherCode) == "Clear sky") Color(0xFFFFD600) else ImmersivePrimary
        )

        Row(
            modifier = Modifier.width(80.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${day.tempMax.toInt()}°",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ImmersivePrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "${day.tempMin.toInt()}°",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Rounded.WbSunny
        1, 2, 3 -> Icons.Rounded.CloudQueue
        45, 48 -> Icons.Rounded.Cloud
        51, 53, 55, 56, 57 -> Icons.Rounded.WaterDrop
        61, 63, 65, 66, 67 -> Icons.Rounded.Umbrella
        71, 73, 75, 77 -> Icons.Rounded.AcUnit
        80, 81, 82 -> Icons.Rounded.Thunderstorm
        85, 86 -> Icons.Rounded.AcUnit
        95, 96, 99 -> Icons.Rounded.Thunderstorm
        else -> Icons.Rounded.WbCloudy
    }
}

fun getWeatherDescription(code: Int): String {
    return when (code) {
        0 -> "Clear sky"
        1, 2, 3 -> "Partly cloudy"
        45, 48 -> "Foggy"
        51, 53, 55 -> "Drizzle"
        61, 63, 65 -> "Rain"
        71, 73, 75 -> "Snow"
        80, 81, 82 -> "Rain showers"
        95 -> "Thunderstorm"
        else -> "Cloudy"
    }
}

fun Modifier.glassmorphic(
    cornerRadius: Dp = 24.dp
): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.08f),
                Color.White.copy(alpha = 0.02f)
            )
        )
    )
    .border(
        width = 1.2.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.03f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

@Composable
fun rememberShimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer_transition_pulse")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    return Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.05f),
            Color.White.copy(alpha = 0.18f),
            Color.White.copy(alpha = 0.05f)
        ),
        start = Offset(10f, 10f),
        end = Offset(translateAnim.value + 10f, translateAnim.value + 10f)
    )
}

@Composable
fun GeminiWeatherSkeleton() {
    val shimmerBrush = rememberShimmerBrush()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("gemini_weather_skeleton"),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 32.dp),
        userScrollEnabled = false
    ) {
        // Main Weather Card skeleton
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ImmersiveSurface.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // City Name bar
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated Weather Icon circle
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(55.dp))
                            .background(shimmerBrush)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Temperature Big Text bar
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(shimmerBrush)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Condition Text bar
                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Detail Items Row (3 cards)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(80.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(shimmerBrush)
                            )
                        }
                    }
                }
            }
        }

        // Active Alerts Title skeleton & Item
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurface.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(140.dp)
                                .height(16.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }

        // 7-Day Outlook section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            // Forecast item rows placeholder
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                userScrollEnabled = false
            ) {
                items(4) {
                    Card(
                        modifier = Modifier
                            .width(115.dp)
                            .padding(end = 8.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = ImmersiveSurface.copy(alpha = 0.25f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(shimmerBrush)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(shimmerBrush)
                            )
                        }
                    }
                }
            }
        }

        // Gemini Summary commentary bubble
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(
                    topStart = 24.dp,
                    topEnd = 24.dp,
                    bottomEnd = 24.dp,
                    bottomStart = 4.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = ImmersivePrimary.copy(alpha = 0.04f)
                ),
                border = BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(shimmerBrush)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}

@Composable
fun StandardWeatherSkeleton() {
    val shimmerBrush = rememberShimmerBrush()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("standard_weather_skeleton")
    ) {
        // Header placeholder (City & Date)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(140.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 32.dp),
            userScrollEnabled = false
        ) {
            // Current Weather Card skeleton
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .clip(RoundedCornerShape(64.dp))
                                .background(shimmerBrush)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(shimmerBrush)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(80.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(shimmerBrush)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 7-Day outlook header loading
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(shimmerBrush)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Forecast List card skeleton
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ImmersiveSurface.copy(alpha = 0.25f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        repeat(4) { index ->
                            Row(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(shimmerBrush)
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(shimmerBrush)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(80.dp)
                                        .height(16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(shimmerBrush)
                                )
                            }
                            if (index < 3) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = Color.White.copy(alpha = 0.03f),
                                    thickness = 1.dp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UvIndexWidget(uvIndex: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .glassmorphic(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UV INDEX / УФ-ИНДЕКС",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = ImmersiveTextSecondary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = null,
                    tint = Color(0xFFFFD600),
                    modifier = Modifier.size(14.dp)
                )
            }
            
            val doubleVal = uvIndex
            val category = when {
                doubleVal < 3.0 -> "Low / Низкий"
                doubleVal < 6.0 -> "Moderate / Средний"
                doubleVal < 8.0 -> "High / Высокий"
                doubleVal < 11.0 -> "Very High / Очень выс."
                else -> "Extreme / Экстрем."
            }
            
            val uvColor = when {
                doubleVal < 3.0 -> Color(0xFF4CAF50)
                doubleVal < 6.0 -> Color(0xFFFFEB3B)
                doubleVal < 8.0 -> Color(0xFFFF9800)
                doubleVal < 11.0 -> Color(0xFFF44336)
                else -> Color(0xFF9C27B0)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format("%.1f", doubleVal),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = uvColor
                    )
                }
                
                // Beautiful Curved Glow Arc Gauge
                Canvas(modifier = Modifier.size(54.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.width / 2f - 4.dp.toPx()
                    
                    // Draw outer dim arc guide
                    drawArc(
                        color = Color.White.copy(alpha = 0.08f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    // Draw neon spectrum active arc
                    val activeAngle = (doubleVal.coerceIn(0.0, 11.0) / 11.0 * 270.0).toFloat()
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(Color(0xFF4CAF50), Color(0xFFFFEA00), Color(0xFFE040FB))
                        ),
                        startAngle = 135f,
                        sweepAngle = activeAngle.coerceAtLeast(10f),
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                    
                    // Calculate glowing thumb coordinates
                    val thumbAngleRad = ((135f + activeAngle) * (Math.PI / 180.0)).toFloat()
                    val tx = cx + radius * kotlin.math.cos(thumbAngleRad.toDouble()).toFloat()
                    val ty = cy + radius * kotlin.math.sin(thumbAngleRad.toDouble()).toFloat()
                    
                    // Outer glow halo
                    drawCircle(
                        color = uvColor.copy(alpha = 0.4f),
                        radius = 6.dp.toPx(),
                        center = Offset(tx, ty)
                    )
                    // Inner bright white cursor core
                    drawCircle(
                        color = Color.White,
                        radius = 2.5.dp.toPx(),
                        center = Offset(tx, ty)
                    )
                }
            }
            
            // Linear spectrum bar index at the bottom
            Canvas(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))) {
                val brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4CAF50), // Low
                        Color(0xFFFFEB3B), // Moderate
                        Color(0xFFFF9800), // High
                        Color(0xFFF44336), // Very High
                        Color(0xFF9C27B0)  // Extreme
                    )
                )
                drawRect(brush = brush)
            }
        }
    }
}

@Composable
fun WindCompassWidget(windSpeed: Double) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .glassmorphic(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WIND / ВЕТЕР",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = ImmersiveTextSecondary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.Air,
                    contentDescription = null,
                    tint = ImmersivePrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${windSpeed.toInt()} km/h",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    val desc = when {
                        windSpeed < 5.0 -> "Calm / Штиль"
                        windSpeed < 15.0 -> "Breeze / Легкий"
                        windSpeed < 30.0 -> "Moderate / Умерен."
                        windSpeed < 50.0 -> "Strong / Сильный"
                        else -> "Gale / Буря"
                    }
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = ImmersiveTextSecondary
                    )
                }

                val transition = rememberInfiniteTransition(label = "turbine_rotation")
                val rotationAngle by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = if (windSpeed > 1) {
                                (3500 / (windSpeed / 10).coerceAtLeast(0.5)).toInt().coerceIn(300, 6000)
                            } else {
                                10000000 
                            },
                            easing = LinearEasing
                        ),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "angle"
                )

                // High-End Compass Dial & Pin on Canvas
                Canvas(modifier = Modifier.size(54.dp)) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.width / 2f
                    
                    // Draw circular compass dial base
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = ImmersivePrimary.copy(alpha = 0.2f),
                        radius = radius,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    
                    // Cardinal markings N, E, S, W ticking
                    val tickLength = 3.dp.toPx()
                    for (deg in 0..315 step 45) {
                        val angleRad = (deg * (Math.PI / 180.0)).toFloat()
                        val outerX = cx + radius * kotlin.math.cos(angleRad.toDouble()).toFloat()
                        val outerY = cy + radius * kotlin.math.sin(angleRad.toDouble()).toFloat()
                        val innerX = cx + (radius - tickLength) * kotlin.math.cos(angleRad.toDouble()).toFloat()
                        val innerY = cy + (radius - tickLength) * kotlin.math.sin(angleRad.toDouble()).toFloat()
                        
                        drawLine(
                            color = if (deg % 90 == 0) ImmersivePrimary.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.2f),
                            start = Offset(innerX, innerY),
                            end = Offset(outerX, outerY),
                            strokeWidth = (if (deg % 90 == 0) 1.5.dp else 0.8.dp).toPx()
                        )
                    }

                    // Rotating wind vector arrow pointing with soft glow
                    val vectorAngleRad = (rotationAngle * (Math.PI / 180.0)).toFloat()
                    val arrowX = cx + (radius - 5.dp.toPx()) * kotlin.math.cos(vectorAngleRad.toDouble()).toFloat()
                    val arrowY = cy + (radius - 5.dp.toPx()) * kotlin.math.sin(vectorAngleRad.toDouble()).toFloat()
                    
                    // Draw vector wind arrow
                    drawLine(
                        color = ImmersivePrimary,
                        start = Offset(cx, cy),
                        end = Offset(arrowX, arrowY),
                        strokeWidth = 2.dp.toPx()
                    )
                    
                    drawCircle(
                        color = ImmersivePrimary,
                        radius = 3.dp.toPx(),
                        center = Offset(arrowX, arrowY)
                    )
                    
                    // Central hub pin
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(cx, cy)
                    )
                }
            }
        }
    }
}

@Composable
fun HumidityGaugeWidget(humidity: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .glassmorphic(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HUMIDITY / ВЛАЖНОСТЬ",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = ImmersiveTextSecondary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = Color(0xFF29B6F6),
                    modifier = Modifier.size(14.dp)
                )
            }
            
            val status = when {
                humidity < 35 -> "Dry / Сухость"
                humidity < 60 -> "Optimal / Комфорт"
                else -> "Humid / Влажность"
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$humidity%",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF29B6F6)
                    )
                }
                
                // Magnificent Round Glowing 3D Wave Beaker Gauge
                val transition = rememberInfiniteTransition(label = "wave_motion")
                val waveOffset by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = (2f * Math.PI).toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(2500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "wave_offset"
                )

                Canvas(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val radius = size.width / 2f
                    
                    // Beaker cylinder back fill
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                    
                    // Wave liquid calculation
                    val level = humidity / 100f
                    val fillY = size.height * (1f - level)
                    
                    val path1 = Path().apply {
                        moveTo(0f, size.height)
                        for (x in 0..size.width.toInt() step 2) {
                            val progress = x.toFloat() / size.width
                            val sineVal = sin(progress * 2f * Math.PI.toFloat() * 1.2f + waveOffset)
                            val y = fillY + sineVal * 3.dp.toPx()
                            lineTo(x.toFloat(), y)
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    
                    val path2 = Path().apply {
                        moveTo(0f, size.height)
                        for (x in 0..size.width.toInt() step 2) {
                            val progress = x.toFloat() / size.width
                            // Opposite direction wave
                            val sineVal = sin(-progress * 2f * Math.PI.toFloat() * 1.5f + waveOffset + 11f)
                            val y = fillY + sineVal * 2.5.dp.toPx()
                            lineTo(x.toFloat(), y)
                        }
                        lineTo(size.width, size.height)
                        close()
                    }
                    
                    // Draw outer subtle liquid backlayer
                    drawPath(
                        path = path2,
                        color = Color(0xFF0288D1).copy(alpha = 0.35f)
                    )
                    
                    // Draw rich primary liquid wave with smooth water gradients
                    drawPath(
                        path = path1,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF29B6F6).copy(alpha = 0.9f),
                                Color(0xFF0288D1).copy(alpha = 0.5f)
                            )
                        )
                    )
                    
                    // Glass highlight sheen on the gauge sphere cover
                    drawArc(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = true,
                        style = Stroke(width = 0.5.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun SunProgressWidget(isDay: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(135.dp)
            .glassmorphic(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUN ORBIT / СУТОЧНЫЙ ЦИКЛ",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                    color = ImmersiveTextSecondary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = null,
                    tint = if (isDay) Color(0xFFFFD600) else Color(0xFFCFD1D6),
                    modifier = Modifier.size(14.dp)
                )
            }
            
            Column {
                Text(
                    text = if (isDay) "Day / День" else "Night / Ночь",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Text(
                    text = if (isDay) "Set at 21:04 / Закат" else "Rise at 05:12 / Восход",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = ImmersiveTextSecondary
                )
            }
            
            // Pulsing sun ray animations
            val transition = rememberInfiniteTransition(label = "sun_pulse_orbit")
            val sunRayScale by transition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulse"
            )

            Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                val width = size.width
                val height = size.height
                val horizonY = height - 2.dp.toPx()
                
                // Horizon line underlay
                drawLine(
                    color = Color.White.copy(alpha = 0.12f),
                    start = Offset(0f, horizonY),
                    end = Offset(width, horizonY),
                    strokeWidth = 1.2.dp.toPx()
                )
                
                // Parabolic solar trajectory arc path
                val arcPath = Path().apply {
                    moveTo(4.dp.toPx(), horizonY)
                    quadraticTo(
                        width / 2f,
                        -12.dp.toPx(),
                        width - 4.dp.toPx(),
                        horizonY
                    )
                }
                
                // Shaded daylight representation gradient beneath solar parabola
                val shaderPath = Path().apply {
                    addPath(arcPath)
                    lineTo(width - 4.dp.toPx(), horizonY)
                    lineTo(4.dp.toPx(), horizonY)
                    close()
                }
                
                drawPath(
                    path = shaderPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isDay) Color(0xFFFFD600).copy(alpha = 0.08f) else Color(0xFF80DEEA).copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )

                // Smooth dashed trajectory stroke
                drawPath(
                    path = arcPath,
                    color = if (isDay) Color(0xFFFFD600).copy(alpha = 0.35f) else Color(0xFF80DEEA).copy(alpha = 0.15f),
                    style = Stroke(
                        width = 1.6.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                    )
                )
                
                // Active Celestial position index logic
                val progressFraction = if (isDay) 0.44f else 0.78f
                val orbitX = 4.dp.toPx() + (width - 8.dp.toPx()) * progressFraction
                
                // Calculate vertex coordinates via normalized parabolic offset
                val normX = progressFraction * 2f - 1f
                val vertexPeak = -11.dp.toPx()
                val orbitY = vertexPeak + (horizonY - vertexPeak) * (normX * normX)
                
                if (isDay) {
                    // Soft glowing aura halo
                    drawCircle(
                        color = Color(0xFFFFD600).copy(alpha = 0.25f),
                        radius = (8.dp.toPx() * sunRayScale),
                        center = Offset(orbitX, orbitY)
                    )
                    // Sun core
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = 4.5.dp.toPx(),
                        center = Offset(orbitX, orbitY)
                    )
                } else {
                    // Moon aura
                    drawCircle(
                        color = Color(0xFFE2E2E6).copy(alpha = 0.15f),
                        radius = 7.dp.toPx(),
                        center = Offset(orbitX, orbitY)
                    )
                    // Moon core
                    drawCircle(
                        color = Color(0xFFE2E2E6),
                        radius = 3.5.dp.toPx(),
                        center = Offset(orbitX, orbitY)
                    )
                }
            }
        }
    }
}

@Composable
fun AtmosphericAuroraBackground(style: String) {
    val transition = rememberInfiniteTransition(label = "aurora_movement")
    
    val pos1X by transition.animateFloat(
        initialValue = 0.1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(25000, easing = LinearEasing), RepeatMode.Reverse), label = "pos1X"
    )
    val pos1Y by transition.animateFloat(
        initialValue = 0.2f, targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Reverse), label = "pos1Y"
    )
    
    val pos2X by transition.animateFloat(
        initialValue = 0.8f, targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(30000, easing = LinearEasing), RepeatMode.Reverse), label = "pos2X"
    )
    val pos2Y by transition.animateFloat(
        initialValue = 0.7f, targetValue = 0.1f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse), label = "pos2Y"
    )

    val scaleFactor by transition.animateFloat(
        initialValue = 0.9f, targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(8000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale"
    )

    val (bgStart, bgEnd, blob1, blob2, blob3) = remember(style) {
        val styleLower = style.lowercase()
        when {
            styleLower.contains("sun") || styleLower.contains("clear") -> listOf(
                Color(0xFF0C4A6E), Color(0xFF0284C7), 
                Color(0xFFFDE047).copy(alpha = 0.28f), 
                Color(0xFFF97316).copy(alpha = 0.22f),  
                Color(0xFF38BDF8).copy(alpha = 0.25f)  
            )
            styleLower.contains("rain") || styleLower.contains("thunder") || styleLower.contains("storm") || styleLower.contains("drizzle") -> listOf(
                Color(0xFF030712), Color(0xFF1E1B4B), 
                Color(0xFF6366F1).copy(alpha = 0.32f), 
                Color(0xFFEC4899).copy(alpha = 0.22f), 
                Color(0xFF3B82F6).copy(alpha = 0.20f)  
            )
            styleLower.contains("snow") || styleLower.contains("ice") -> listOf(
                Color(0xFF082F49), Color(0xFF0F172A), 
                Color(0xFF06B6D4).copy(alpha = 0.30f), 
                Color(0xFFE2E8F0).copy(alpha = 0.25f), 
                Color(0xFF38BDF8).copy(alpha = 0.22f)  
            )
            styleLower.contains("cloud") || styleLower.contains("fog") || styleLower.contains("mist") -> listOf(
                Color(0xFF0F172A), Color(0xFF1E293B), 
                Color(0xFFC084FC).copy(alpha = 0.25f), 
                Color(0xFF94A3B8).copy(alpha = 0.20f), 
                Color(0xFF475569).copy(alpha = 0.18f)   
            )
            styleLower.contains("volcano") || styleLower.contains("eruption") -> listOf(
                Color(0xFF0C0202), Color(0xFF1E0A0A), 
                Color(0xFFEF4444).copy(alpha = 0.32f), 
                Color(0xFFF97316).copy(alpha = 0.25f), 
                Color(0xFF781E1E).copy(alpha = 0.20f)  
            )
            styleLower.contains("tsunami") || styleLower.contains("flood") || styleLower.contains("wave") -> listOf(
                Color(0xFF02162E), Color(0xFF0F2D54), 
                Color(0xFF0D9488).copy(alpha = 0.28f), 
                Color(0xFF2563EB).copy(alpha = 0.25f), 
                Color(0xFF0284C7).copy(alpha = 0.20f)  
            )
            styleLower.contains("tornado") || styleLower.contains("hurricane") || styleLower.contains("wind") -> listOf(
                Color(0xFF0F172A), Color(0xFF202B3E), 
                Color(0xFF10B981).copy(alpha = 0.24f), 
                Color(0xFF64748B).copy(alpha = 0.22f), 
                Color(0xFF0369A1).copy(alpha = 0.20f)  
            )
            else -> listOf(
                Color(0xFF0F172A), Color(0xFF020617), 
                Color(0xFF8B5CF6).copy(alpha = 0.38f), 
                Color(0xFF3B82F6).copy(alpha = 0.32f), 
                Color(0xFFEC4899).copy(alpha = 0.25f)   
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgStart, bgEnd)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob1, Color.Transparent),
                    center = Offset(width * pos1X, height * pos1Y),
                    radius = (width * 0.5f) * scaleFactor
                ),
                radius = (width * 0.5f) * scaleFactor,
                center = Offset(width * pos1X, height * pos1Y)
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob2, Color.Transparent),
                    center = Offset(width * pos2X, height * pos2Y),
                    radius = (width * 0.45f) * scaleFactor
                ),
                radius = (width * 0.45f) * scaleFactor,
                center = Offset(width * pos2X, height * pos2Y)
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(blob3, Color.Transparent),
                    center = Offset(width / 2f, height / 2f),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = Offset(width / 2f, height / 2f)
            )
        }
    }
}

@Composable
fun AtmosphericWeatherParticles(style: String) {
    val styleLower = style.lowercase()
    val transition = rememberInfiniteTransition(label = "weathersim_transition")
    
    val sweepFloat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val rainFloat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain"
    )

    val snowFloat by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        when {
            styleLower.contains("sun") || styleLower.contains("clear") -> {
                val cx = width * 0.85f
                val cy = height * 0.15f
                
                val rayScale = 1.0f + 0.05f * sin(sweepFloat * 2f * Math.PI.toFloat())
                for (i in 0..7) {
                    val angle = (i * 45f) * (Math.PI / 180.0)
                    val rx = cx + 80.dp.toPx() * rayScale * kotlin.math.cos(angle).toFloat()
                    val ry = cy + 80.dp.toPx() * rayScale * kotlin.math.sin(angle).toFloat()
                    
                    drawLine(
                        color = Color(0xFFFFD600).copy(alpha = 0.12f),
                        start = Offset(cx, cy),
                        end = Offset(rx, ry),
                        strokeWidth = 3.dp.toPx()
                    )
                }
                
                // Fine gold sunbeams and ambient floating solar dust particles
                val solarMotes = 25
                for (i in 0 until solarMotes) {
                    val x = ((i * 19309) % width.toInt()).toFloat()
                    val y = ((i * 11579) % height.toInt()).toFloat()
                    
                    // Gentle float up and sideways
                    val floatY = (y - sweepFloat * height * 0.15f) % height
                    val positiveY = if (floatY < 0) floatY + height else floatY
                    val floatX = (x + sin(sweepFloat * Math.PI.toFloat() + i) * 15.dp.toPx()) % width
                    
                    val goldGlow = sin(sweepFloat * 2f * Math.PI.toFloat() + i).coerceIn(0f, 1f)
                    val goldAlpha = (0.08f + 0.22f * goldGlow)
                    
                    drawCircle(
                        color = Color(0xFFFFD54F).copy(alpha = goldAlpha),
                        radius = (1.5.dp + (i % 2).dp).toPx(),
                        center = Offset(floatX, positiveY)
                    )
                }
            }
            
            styleLower.contains("rain") || styleLower.contains("thunder") || styleLower.contains("storm") || styleLower.contains("drizzle") -> {
                val totalDrops = 65
                for (i in 0 until totalDrops) {
                    val xSeed = (i * 7919) % width.toInt()
                    val ySeed = (i * 9973) % height.toInt()
                    val speedScalar = 0.8f + (i % 4) * 0.40f
                    
                    val dY = (ySeed + (rainFloat / 100f) * height * speedScalar) % height
                    
                    // Dynamic wind sway angle based on intensity
                    val windVelocity = if (styleLower.contains("thunder") || styleLower.contains("storm")) 25.dp.toPx() else 8.dp.toPx()
                    val dX = (xSeed + dY * 0.12f + sweepFloat * windVelocity) % width 
                    
                    // Fade near top and bottom boundaries to prevent clipping popping
                    val verticalFade = when {
                        dY < 120.dp.toPx() -> dY / 120.dp.toPx()
                        dY > height - 120.dp.toPx() -> (height - dY) / 120.dp.toPx()
                        else -> 1.0f
                    }.coerceIn(0f, 1f)
                    
                    val dropAlpha = (0.12f + (i % 5) * 0.11f) * verticalFade
                    val strokeW = (0.8f + (i % 3) * 0.4f).dp.toPx()
                    val dropLength = (10 + (i % 3) * 6).dp.toPx()
                    
                    val slantX = strokeW * 1.5f + (if (styleLower.contains("thunder")) 5f else 2f)
                    
                    drawLine(
                        color = Color(0xFF38BDF8).copy(alpha = dropAlpha),
                        start = Offset(dX, dY),
                        end = Offset(dX + slantX, dY + dropLength),
                        strokeWidth = strokeW
                    )
                }
            }
            
            styleLower.contains("snow") || styleLower.contains("ice") -> {
                val totalFlakes = 65
                for (i in 0 until totalFlakes) {
                    val xSeed = (i * 12347) % width.toInt()
                    val ySeed = (i * 8707) % height.toInt()
                    val speedScalar = 0.4f + (i % 5) * 0.15f
                    
                    val dY = (ySeed + snowFloat * height * speedScalar) % height
                    val swayFreq = 2f + (i % 3) * 1.5f
                    val swayAmp = (10 + (i % 3) * 8).dp.toPx()
                    val sway = sin(snowFloat * swayFreq * Math.PI.toFloat() + i) * swayAmp
                    val dX = (xSeed + sway) % width
                    
                    // Fade near top/bottom boundaries to prevent flake popping
                    val verticalFade = when {
                        dY < 150.dp.toPx() -> dY / 150.dp.toPx()
                        dY > height - 150.dp.toPx() -> (height - dY) / 150.dp.toPx()
                        else -> 1.0f
                    }.coerceIn(0f, 1f)
                    
                    val baseAlpha = 0.25f + (i % 5) * 0.14f
                    val flakeAlpha = baseAlpha * verticalFade
                    val baseRadius = (1.5.dp + (i % 4).dp).toPx()
                    
                    if (i % 8 == 0) {
                        // Cinematic visual bokeh style snowflake blurring
                        val bokehRadius = baseRadius * 3.5f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = flakeAlpha * 0.5f), Color.Transparent),
                                center = Offset(dX, dY),
                                radius = bokehRadius
                            ),
                            radius = bokehRadius,
                            center = Offset(dX, dY)
                        )
                    } else {
                        drawCircle(
                            color = Color.White.copy(alpha = flakeAlpha),
                            radius = baseRadius,
                            center = Offset(dX, dY)
                        )
                    }
                }
            }
            
            styleLower.contains("wind") || styleLower.contains("tornado") || styleLower.contains("hurricane") -> {
                val isTornado = styleLower.contains("tornado") || styleLower.contains("hurricane")
                
                if (isTornado) {
                    val cx = width / 2f
                    val cy = height * 0.7f
                    val baseRadius = 120.dp.toPx()
                    
                    for (layer in 0..4) {
                        val layerRadius = baseRadius - layer * 20.dp.toPx()
                        val rotateAngle = sweepFloat * 360f * (1.5f - layer * 0.2f)
                        val pointsCount = 4
                        
                        val path = Path()
                        for (p in 0..pointsCount) {
                            val dotProgress = p.toFloat() / pointsCount
                            val angleRad = (rotateAngle + dotProgress * 360f) * (Math.PI / 180.0)
                            val px = cx + layerRadius * kotlin.math.cos(angleRad).toFloat()
                            val py = (cy - layer * 35.dp.toPx()) + (layerRadius * 0.3f) * kotlin.math.sin(angleRad).toFloat()
                            
                            if (p == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        
                        drawPath(
                            path = path,
                            color = Color(0xFF64748B).copy(alpha = 0.12f + (layer * 0.03f)),
                            style = Stroke(
                                width = (2.dp + layer.dp).toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 15f), sweepFloat * 100f)
                            )
                        )
                    }
                    
                    // Swirling orbital atmospheric dust/debris
                    val swarmCount = 20
                    for (i in 0 until swarmCount) {
                        val orbitalProgress = (sweepFloat * 2f + i * 0.05f) % 1f
                        val layer = i % 5
                        val radius = (baseRadius - layer * 20.dp.toPx()) * (0.8f + 0.4f * sin(orbitalProgress * 2f * Math.PI.toFloat()))
                        val angleRad = (orbitalProgress * 360f) * (Math.PI / 180.0)
                        
                        val px = cx + radius * kotlin.math.cos(angleRad).toFloat()
                        val py = (cy - layer * 35.dp.toPx()) + (radius * 0.25f) * kotlin.math.sin(angleRad).toFloat()
                        
                        drawCircle(
                            color = Color(0xFFC084FC).copy(alpha = 0.35f),
                            radius = (1.dp + (i % 2).dp).toPx(),
                            center = Offset(px, py)
                        )
                    }
                } else {
                    val linesCount = 8
                    for (i in 0 until linesCount) {
                        val yOffset = (height * 0.15f) + i * (height * 0.1f)
                        val speedScalar = 1.2f + (i % 3) * 0.4f
                        val animOffset = (sweepFloat * width * speedScalar) % width
                        
                        val path = Path().apply {
                            moveTo(animOffset - 160.dp.toPx(), yOffset)
                            cubicTo(
                                animOffset - 80.dp.toPx(), yOffset - 30.dp.toPx(),
                                animOffset, yOffset + 30.dp.toPx(),
                                animOffset + 80.dp.toPx(), yOffset
                            )
                        }
                        
                        drawPath(
                            path = path,
                            color = Color(0xFFC084FC).copy(alpha = 0.14f),
                            style = Stroke(
                                width = (1.dp + (i % 2).dp).toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(30f, 30f), sweepFloat * 20f)
                            )
                        )
                    }
                }
            }
            
            styleLower.contains("volcano") || styleLower.contains("eruption") -> {
                val totalSparks = 45
                for (i in 0 until totalSparks) {
                    val xSeed = (i * 3121) % width.toInt()
                    val ySeed = (i * 5437) % height.toInt()
                    val speedScalar = 0.5f + (i % 4) * 0.25f
                    val dY = (ySeed - sweepFloat * height * speedScalar) % height
                    val positiveY = if (dY < 0) dY + height else dY
                    
                    val sway = sin(sweepFloat * 4f * Math.PI.toFloat() + i) * 15.dp.toPx()
                    val dX = (xSeed + sway) % width
                    
                    val flicker = 0.3f + 0.7f * sin(sweepFloat * 8f * Math.PI.toFloat() + i).coerceIn(0f, 1f)
                    val sparkleColor = if (i % 3 == 0) {
                        Color(0xFFFF3D00).copy(alpha = 0.75f * flicker)
                    } else if (i % 3 == 1) {
                        Color(0xFFFF9100).copy(alpha = 0.75f * flicker)
                    } else {
                        Color(0xFFFFD600).copy(alpha = 0.55f * flicker)
                    }
                    
                    drawCircle(
                        color = sparkleColor,
                        radius = (1.5.dp + (i % 3).dp).toPx(),
                        center = Offset(dX, positiveY)
                    )
                }
            }
            
            styleLower.contains("tsunami") || styleLower.contains("flood") -> {
                val waveHeight = 60.dp.toPx()
                val cy = height - waveHeight
                val path = Path()
                path.moveTo(0f, height)
                path.lineTo(0f, cy)
                
                for (x in 0..width.toInt() step 10) {
                    val progress = x / width
                    val sineVal = sin(progress * 2f * Math.PI.toFloat() + sweepFloat * 2f * Math.PI.toFloat())
                    val y = cy + sineVal * 12.dp.toPx()
                    path.lineTo(x.toFloat(), y)
                }
                
                path.lineTo(width, height)
                path.close()
                
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D9488).copy(alpha = 0.35f),
                            Color(0xFF042F40).copy(alpha = 0.15f)
                        )
                    )
                )
            }
            
            else -> {
                val starCount = 30
                for (i in 0 until starCount) {
                    val x = ((i * 123457) % width.toInt()).toFloat()
                    val y = ((i * 76543) % height.toInt()).toFloat()
                    val glowFactor = 0.3f + 0.7f * sin(sweepFloat * 2f * Math.PI.toFloat() + i).coerceIn(0f, 1f)
                    
                    drawCircle(
                        color = Color.White.copy(alpha = 0.45f * glowFactor),
                        radius = (1.dp + (i % 2).dp).toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

@Composable
fun WeatherMetricsGrid(
    windSpeed: Double,
    humidity: Int,
    uvIndex: Double?,
    isDay: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag("weather_metrics_grid")
    ) {
        Text(
            text = "WEATHER DYNAMICS / ПОКАЗАТЕЛИ АТМОСФЕРЫ",
            style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 1.sp),
            color = ImmersivePrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UvIndexWidget(uvIndex = uvIndex ?: 2.4)
                HumidityGaugeWidget(humidity = humidity)
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WindCompassWidget(windSpeed = windSpeed)
                SunProgressWidget(isDay = isDay)
            }
        }
    }
}
