package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ForecastDay
import com.example.data.TemperatureUnit
import com.example.data.WeatherFormatter
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraSkyAccent
import com.example.ui.theme.AuraSunGold
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DailyForecastSection(
    forecast: List<ForecastDay>,
    temperatureUnit: TemperatureUnit
) {
    if (forecast.isEmpty()) return

    val weekMin = remember(forecast) { forecast.minOf { it.tempMinC } }
    val weekMax = remember(forecast) { forecast.maxOf { it.tempMaxC } }
    var expandedDate by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .auraGlassCard(28.dp)
            .padding(20.dp)
            .testTag("daily_forecast_section")
    ) {
        Text(
            text = stringResource(R.string.forecast_7_days).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AuraSkyAccent
        )

        Spacer(modifier = Modifier.height(12.dp))

        forecast.forEachIndexed { index, day ->
            val isToday = index == 0
            val isExpanded = expandedDate == day.date
            DailyForecastRow(
                day = day,
                isToday = isToday,
                isExpanded = isExpanded,
                weekMinC = weekMin,
                weekMaxC = weekMax,
                temperatureUnit = temperatureUnit,
                onClick = {
                    expandedDate = if (isExpanded) null else day.date
                }
            )
            if (index < forecast.lastIndex) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.06f),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DailyForecastRow(
    day: ForecastDay,
    isToday: Boolean,
    isExpanded: Boolean,
    weekMinC: Double,
    weekMaxC: Double,
    temperatureUnit: TemperatureUnit,
    onClick: () -> Unit
) {
    val dayLabel = remember(day.date, isToday) {
        if (isToday) {
            "Today"
        } else {
            try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val formatter = SimpleDateFormat("EEE", Locale.getDefault())
                formatter.format(parser.parse(day.date) ?: Date())
            } catch (_: Exception) {
                day.date
            }
        }
    }

    val visual = weatherVisualFor(day.weatherCode, isDay = true)
    val description = WeatherFormatter.weatherDescription(day.weatherCode)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp)
            .testTag("daily_row_${day.date}")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayLabel,
                style = MaterialTheme.typography.titleMedium,
                color = if (isToday) AuraSkyAccent else AuraTextPrimary,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.width(56.dp)
            )

            Row(
                modifier = Modifier.width(62.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = description,
                    tint = visual.tint,
                    modifier = Modifier.size(22.dp)
                )
                if (day.precipitationProbabilityMax >= 15) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${day.precipitationProbabilityMax}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = AuraSkyAccent
                    )
                }
            }

            Text(
                text = WeatherFormatter.formatTemp(day.tempMinC, temperatureUnit),
                style = MaterialTheme.typography.bodyMedium,
                color = AuraTextSecondary,
                textAlign = TextAlign.End,
                modifier = Modifier.width(38.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            TemperatureRangeBar(
                dayMin = day.tempMinC,
                dayMax = day.tempMaxC,
                weekMin = weekMinC,
                weekMax = weekMaxC,
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = WeatherFormatter.formatTemp(day.tempMaxC, temperatureUnit),
                style = MaterialTheme.typography.bodyMedium,
                color = AuraTextPrimary,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.End,
                modifier = Modifier.width(38.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp, start = 4.dp, end = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextPrimary,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WbSunny,
                            contentDescription = "Max UV",
                            tint = AuraSunGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "UV ${String.format(Locale.US, "%.0f", day.uvIndexMax)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.WaterDrop,
                            contentDescription = "Rain chance",
                            tint = AuraSkyAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${day.precipitationProbabilityMax}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTextSecondary
                        )
                    }
                    val sunrise = day.sunriseIso?.substringAfter('T', "")?.take(5).orEmpty()
                    val sunset = day.sunsetIso?.substringAfter('T', "")?.take(5).orEmpty()
                    if (sunrise.isNotBlank() && sunset.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.WbTwilight,
                                contentDescription = "Sunrise and sunset",
                                tint = AuraAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$sunrise–$sunset",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureRangeBar(
    dayMin: Double,
    dayMax: Double,
    weekMin: Double,
    weekMax: Double,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val totalSpan = (weekMax - weekMin).coerceAtLeast(1.0)
        val startFrac = ((dayMin - weekMin) / totalSpan).toFloat().coerceIn(0f, 1f)
        val endFrac = ((dayMax - weekMin) / totalSpan).toFloat().coerceIn(startFrac + 0.08f, 1f)
        val radius = CornerRadius(size.height / 2f, size.height / 2f)

        drawRoundRect(
            color = Color.White.copy(alpha = 0.10f),
            cornerRadius = radius
        )

        val barStart = size.width * startFrac
        val barWidth = (size.width * (endFrac - startFrac)).coerceAtLeast(size.height)

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF38BDF8), Color(0xFFFACC15), Color(0xFFFB923C)),
                startX = 0f,
                endX = size.width
            ),
            topLeft = Offset(barStart, 0f),
            size = Size(barWidth, size.height),
            cornerRadius = radius
        )
    }
}
