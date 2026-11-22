package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.UserPreferences
import com.example.data.WeatherDashboardData
import com.example.data.WeatherFormatter
import com.example.ui.theme.AuraSkyAccent
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WeatherHeroCard(
    data: WeatherDashboardData,
    preferences: UserPreferences,
    isPinned: Boolean,
    onTogglePin: () -> Unit
) {
    val conditionText = WeatherFormatter.weatherDescription(data.weatherCode)
    val tempNumber = WeatherFormatter.formatTempNumber(data.temperatureC, preferences.temperatureUnit)
    val feelsLike = WeatherFormatter.formatTemp(data.apparentTemperatureC, preferences.temperatureUnit)
    val high = WeatherFormatter.formatTemp(data.todayHighC, preferences.temperatureUnit)
    val low = WeatherFormatter.formatTemp(data.todayLowC, preferences.temperatureUnit)
    val updatedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(data.lastSyncedEpochMillis))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .auraGlassCard(32.dp)
            .padding(24.dp)
            .testTag("main_weather_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top Row: City + Pin action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.location.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = AuraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val subtitle = data.location.displaySubtitle
                    if (subtitle.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onTogglePin,
                    modifier = Modifier.testTag("pin_location_button")
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                        contentDescription = if (isPinned) "Remove from saved locations" else "Save location",
                        tint = if (isPinned) AuraSkyAccent else AuraTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Asymmetric Hero Display: Temperature + Animated Icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        AnimatedContent(
                            targetState = tempNumber,
                            transitionSpec = {
                                (fadeIn() + slideInVertically { it / 3 }).togetherWith(fadeOut())
                            },
                            label = "hero_temp"
                        ) { value ->
                            Text(
                                text = "$value",
                                style = MaterialTheme.typography.displayLarge,
                                color = AuraTextPrimary
                            )
                        }
                        Text(
                            text = preferences.temperatureUnit.symbol,
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            color = AuraSkyAccent,
                            modifier = Modifier.padding(top = 12.dp, start = 2.dp)
                        )
                    }
                    Text(
                        text = conditionText,
                        style = MaterialTheme.typography.titleLarge,
                        color = AuraTextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                AnimatedWeatherHeroIcon(
                    weatherCode = data.weatherCode,
                    isDay = data.isDay,
                    contentDescription = conditionText,
                    modifier = Modifier.size(104.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Telemetry Footer Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Text(
                        text = "${stringResource(R.string.feels_like)} $feelsLike  •  H:$high  L:$low",
                        style = MaterialTheme.typography.labelMedium,
                        color = AuraTextPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = AuraTextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = updatedTime,
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary
                    )
                }
            }
        }
    }
}
