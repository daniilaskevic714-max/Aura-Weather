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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ForecastDay
import com.example.data.WeatherResponse
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val uiState by viewModel.uiState.collectAsState()

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
    ) {
        when (val state = uiState) {
            is WeatherUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is WeatherUiState.Success -> {
                WeatherContent(state.weather, state.forecast)
            }
            is WeatherUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Button(onClick = { viewModel.fetchWeather() }) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WeatherContent(weather: WeatherResponse, forecast: List<ForecastDay>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        HeaderSection()
        
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
fun HeaderSection() {
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
                    text = "Berlin",
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
