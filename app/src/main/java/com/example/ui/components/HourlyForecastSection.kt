package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.HourlyForecastItem
import com.example.data.TemperatureUnit
import com.example.data.WeatherFormatter
import com.example.ui.theme.AuraSkyAccent
import com.example.ui.theme.AuraSunGold
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary

@Composable
fun HourlyForecastSection(
    hourlyItems: List<HourlyForecastItem>,
    temperatureUnit: TemperatureUnit
) {
    if (hourlyItems.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .auraGlassCard(28.dp)
            .padding(vertical = 18.dp)
            .testTag("hourly_forecast_section")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.forecast_24_hours).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AuraSkyAccent
            )
            val minT = remember(hourlyItems, temperatureUnit) {
                hourlyItems.minOf { WeatherFormatter.formatTempNumber(it.temperatureC, temperatureUnit) }
            }
            val maxT = remember(hourlyItems, temperatureUnit) {
                hourlyItems.maxOf { WeatherFormatter.formatTempNumber(it.temperatureC, temperatureUnit) }
            }
            Text(
                text = stringResource(R.string.hourly_temp_range, minT, maxT),
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Smooth 24h temperature trend curve
        HourlyTemperatureCurve(
            items = hourlyItems,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .padding(horizontal = 20.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(hourlyItems, key = { it.isoTime }) { item ->
                HourlyPill(item = item, temperatureUnit = temperatureUnit)
            }
        }
    }
}

@Composable
private fun HourlyTemperatureCurve(
    items: List<HourlyForecastItem>,
    modifier: Modifier = Modifier
) {
    if (items.size < 2) return
    Canvas(modifier = modifier) {
        val temps = items.map { it.temperatureC.toFloat() }
        val minVal = temps.minOrNull() ?: 0f
        val maxVal = temps.maxOrNull() ?: 1f
        val span = (maxVal - minVal).coerceAtLeast(1f)

        val stepX = size.width / (temps.size - 1).coerceAtLeast(1)
        val topPad = 8.dp.toPx()
        val usableH = (size.height - topPad * 2).coerceAtLeast(4f)

        val points = temps.mapIndexed { idx, temp ->
            val norm = (temp - minVal) / span
            Offset(
                x = idx * stepX,
                y = topPad + usableH * (1f - norm)
            )
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1 until points.size) {
                val prev = points[i - 1]
                val curr = points[i]
                val midX = (prev.x + curr.x) / 2f
                cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height)
            lineTo(points.first().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(AuraSkyAccent.copy(alpha = 0.25f), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            brush = Brush.horizontalGradient(listOf(AuraSkyAccent, AuraSunGold)),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
        )

        val nowPoint = points.first()
        drawCircle(color = AuraSkyAccent.copy(alpha = 0.35f), radius = 6.dp.toPx(), center = nowPoint)
        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = nowPoint)
    }
}

@Composable
private fun HourlyPill(
    item: HourlyForecastItem,
    temperatureUnit: TemperatureUnit
) {
    val visual = weatherVisualFor(item.weatherCode, item.isDay)
    val bgColor = if (item.isNow) AuraSkyAccent.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f)
    val borderColor = if (item.isNow) AuraSkyAccent.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.08f)

    Surface(
        modifier = Modifier.width(68.dp),
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = item.hourLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (item.isNow) AuraSkyAccent else AuraTextSecondary,
                fontWeight = if (item.isNow) FontWeight.Bold else FontWeight.Medium
            )

            Icon(
                imageVector = visual.icon,
                contentDescription = WeatherFormatter.weatherDescription(item.weatherCode),
                tint = visual.tint,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = WeatherFormatter.formatTemp(item.temperatureC, temperatureUnit),
                style = MaterialTheme.typography.titleMedium,
                color = AuraTextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            if (item.precipitationProbability >= 10) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.WaterDrop,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${item.precipitationProbability}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = Color(0xFF7DD3FC)
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}
