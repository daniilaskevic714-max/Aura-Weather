package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.clip
import com.example.ui.theme.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val geminiState by viewModel.geminiState.collectAsState()
    val isAiMode by viewModel.isAiMode.collectAsState()
    val currentCity by viewModel.currentCity.collectAsState()

    var searchInput by remember { mutableStateOf("") }

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

            Spacer(modifier = Modifier.height(16.dp))

            if (isAiMode) {
                // Gemini flow
                when (val state = geminiState) {
                    is GeminiWeatherUiState.Idle -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Awaiting query...", color = ImmersiveTextSecondary)
                        }
                    }
                    is GeminiWeatherUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = ImmersivePrimary)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Analyzing atmosphere via Gemini...", color = ImmersiveTextSecondary, fontWeight = FontWeight.Medium)
                            }
                        }
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ImmersivePrimary)
                        }
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = data.city.uppercase(),
                        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
                        color = ImmersivePrimary,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val (icon, color) = remember(data.icon) {
                        val iconRes = when (data.icon.lowercase()) {
                            "sunny" -> Icons.Rounded.WbSunny
                            "cloudy" -> Icons.Rounded.CloudQueue
                            "rainy" -> Icons.Rounded.Umbrella
                            "thunderstorm" -> Icons.Rounded.Thunderstorm
                            "snowy" -> Icons.Rounded.AcUnit
                            "foggy" -> Icons.Rounded.Cloud
                            "windy" -> Icons.Rounded.Air
                            else -> Icons.Rounded.WbCloudy
                        }
                        val colorRes = when (data.icon.lowercase()) {
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

                    Icon(
                        imageVector = icon,
                        contentDescription = "AI Weather Condition",
                        modifier = Modifier.size(110.dp),
                        tint = color
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.Top) {
                        Text(
                            text = "${data.temperature.toInt()}",
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontSize = 80.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
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
                        text = data.condition,
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WeatherDetailItem(Icons.Default.Air, "${data.windSpeed} km/h", "Wind", Modifier.weight(1f))
                        WeatherDetailItem(Icons.Default.WaterDrop, "${data.humidity}%", "Humidity", Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))

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
                        text = "\"${data.commentary}\"",
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
            Icon(
                imageVector = getWeatherIcon(current.weatherCode),
                contentDescription = null,
                modifier = Modifier.size(128.dp),
                tint = Color.White
            )

            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = "${current.temperature.toInt()}",
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White
                )
                Text(
                    text = "°",
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 36.sp),
                    color = ImmersivePrimary,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }

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
