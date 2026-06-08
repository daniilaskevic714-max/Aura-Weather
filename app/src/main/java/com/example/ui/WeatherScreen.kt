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
import com.example.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ImmersiveBgStart,
                        ImmersiveBgEnd
                    )
                )
            )
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
                        focusedContainerColor = ImmersiveSurface,
                        unfocusedContainerColor = ImmersiveSurface.copy(alpha = 0.5f),
                        focusedBorderColor = ImmersivePrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
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
                                // Standard mode requires lat/lon, but for seamless feel we still query Gemini
                                // update or we can just fetch standard forecast via Gemini query too
                                viewModel.fetchGeminiWeather(city)
                            }
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .testTag("city_search_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveSurface),
                    shape = RoundedCornerShape(16.dp),
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
                    colors = ButtonDefaults.buttonColors(containerColor = ImmersiveSurface),
                    shape = RoundedCornerShape(16.dp),
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
                    .clip(RoundedCornerShape(16.dp))
                    .background(ImmersiveSurface.copy(alpha = 0.5f))
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
                        containerColor = if (isAiMode) ImmersiveSurface else Color.Transparent,
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
                        containerColor = if (!isAiMode) ImmersiveSurface else Color.Transparent,
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
                    Text(
                        text = "🚨 SIMULATE HAZARD / СИМУЛИРОВАТЬ СТИХИЮ:",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = ImmersiveTextSecondary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        val disasters = listOf(
                            "Tsunami" to "🌊 Tsunami / Цунами",
                            "Tornado" to "🌪️ Tornado / Торнадо",
                            "Hurricane" to "🌀 Hurricane / Ураган",
                            "Earthquake" to "💥 Earthquake / Землетрясение",
                            "Volcanic Eruption" to "🌋 Volcano / Вулкан",
                            "Flood" to "🌊 Flood / Наводнение"
                        )
                        
                        items(disasters) { (type, label) ->
                            AssistChip(
                                onClick = {
                                    viewModel.fetchGeminiWeather(currentCity, type)
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
                                        Text("Retry", color = Color.White)
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
                                    Text("Retry")
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
                initialValue = -8f,
                targetValue = 8f,
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
                initialValue = 0.93f,
                targetValue = 1.07f,
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
                initialValue = -10f,
                targetValue = 10f,
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

    Icon(
        imageVector = icon,
        contentDescription = "Weather Animated Icon",
        modifier = modifier.then(animationModifier),
        tint = color
    )
}

@Composable
fun GeminiForecastItem(forecastDay: com.example.data.GeminiForecastDay) {
    val (icon, color) = remember(forecastDay.icon) {
        val iconRes = when (forecastDay.icon.lowercase()) {
            "sunny" -> Icons.Rounded.WbSunny
            "cloudy" -> Icons.Rounded.CloudQueue
            "rainy" -> Icons.Rounded.Umbrella
            "thunderstorm" -> Icons.Rounded.Thunderstorm
            "snowy" -> Icons.Rounded.AcUnit
            "foggy" -> Icons.Rounded.Cloud
            "windy" -> Icons.Rounded.Air
            else -> Icons.Rounded.WbCloudy
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
            .testTag("gemini_forecast_card_${forecastDay.day.lowercase()}"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = ImmersiveSurface.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = ImmersiveSurface.copy(alpha = 0.4f)
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                                "sunny" -> Icons.Rounded.WbSunny
                                "cloudy" -> Icons.Rounded.CloudQueue
                                "rainy" -> Icons.Rounded.Umbrella
                                "thunderstorm" -> Icons.Rounded.Thunderstorm
                                "snowy" -> Icons.Rounded.AcUnit
                                "foggy" -> Icons.Rounded.Cloud
                                "windy" -> Icons.Rounded.Air
                                else -> Icons.Rounded.WbCloudy
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
                                        fontSize = 80.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "°",
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold
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
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = ImmersiveSurface.copy(alpha = 0.4f)
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(200.dp)
                .background(ImmersivePrimary.copy(alpha = 0.05f), RoundedCornerShape(100.dp))
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
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
                        style = MaterialTheme.typography.displayLarge,
                        color = Color.White
                    )
                }
                Text(
                    text = "°",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                    color = ImmersivePrimary,
                    modifier = Modifier.padding(top = 12.dp)
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
