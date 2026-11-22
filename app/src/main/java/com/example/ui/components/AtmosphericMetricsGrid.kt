package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AirQualitySummary
import com.example.data.UserPreferences
import com.example.data.WeatherDashboardData
import com.example.data.WeatherFormatter
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraAquaMint
import com.example.ui.theme.AuraCoralWarn
import com.example.ui.theme.AuraSkyAccent
import com.example.ui.theme.AuraSunGold
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AtmosphericMetricsGrid(
    data: WeatherDashboardData,
    preferences: UserPreferences
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weather_metrics_grid"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.atmospheric_details).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AuraSkyAccent,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Row 1: UV Index & Wind Compass
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UvIndexCard(
                currentUv = data.currentUvIndex,
                maxUv = data.maxUvIndexToday,
                modifier = Modifier.weight(1f)
            )
            WindCompassCard(
                speedKmh = data.windSpeedKmh,
                gustsKmh = data.windGustsKmh,
                directionDegrees = data.windDirectionDegrees,
                preferences = preferences,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Humidity Wave & Real Sun Orbit
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HumidityWaveCard(
                humidity = data.humidityPercent,
                cloudCover = data.cloudCoverPercent,
                modifier = Modifier.weight(1f)
            )
            SunOrbitCard(
                isDay = data.isDay,
                sunriseTime = data.sunriseTime,
                sunsetTime = data.sunsetTime,
                progressFraction = data.sunProgressFraction,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Pressure & Visibility
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SimpleMetricCard(
                title = stringResource(R.string.metric_pressure).uppercase(),
                icon = Icons.Rounded.Compress,
                iconTint = AuraSkyAccent,
                primaryValue = WeatherFormatter.formatPressure(data.surfacePressureHpa, preferences.pressureUnit),
                subtitle = if (data.surfacePressureHpa >= 1013.0) "High pressure" else "Low pressure",
                modifier = Modifier.weight(1f)
            )
            SimpleMetricCard(
                title = stringResource(R.string.metric_visibility).uppercase(),
                icon = Icons.Rounded.Visibility,
                iconTint = AuraAquaMint,
                primaryValue = String.format(Locale.US, "%.1f km", data.visibilityKm),
                subtitle = when {
                    data.visibilityKm >= 10.0 -> "Crystal clear"
                    data.visibilityKm >= 4.0 -> "Moderate haze"
                    else -> "Low visibility"
                },
                modifier = Modifier.weight(1f)
            )
        }

        // Air Quality Card (if available)
        data.airQuality?.let { aqi ->
            AirQualityCard(airQuality = aqi)
        }
    }
}

@Composable
private fun UvIndexCard(
    currentUv: Double,
    maxUv: Double,
    modifier: Modifier = Modifier
) {
    val category = WeatherFormatter.uvCategory(currentUv)
    val uvColor = when {
        currentUv < 3.0 -> AuraAquaMint
        currentUv < 6.0 -> AuraSunGold
        currentUv < 8.0 -> AuraAmber
        else -> AuraCoralWarn
    }

    Column(
        modifier = modifier
            .height(144.dp)
            .auraGlassCard(24.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CardHeader(title = stringResource(R.string.metric_uv_index).uppercase(), icon = Icons.Rounded.WbSunny, tint = AuraSunGold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = String.format(Locale.US, "%.1f", currentUv),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AuraTextPrimary
                )
                Text(
                    text = "$category • Max ${String.format(Locale.US, "%.0f", maxUv)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = uvColor
                )
            }

            Canvas(modifier = Modifier.size(52.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = size.width / 2f - 4.dp.toPx()

                drawArc(
                    color = Color.White.copy(alpha = 0.10f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                val sweep = (currentUv.coerceIn(0.0, 11.0) / 11.0 * 270.0).toFloat().coerceAtLeast(10f)
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(AuraAquaMint, AuraSunGold, AuraCoralWarn)
                    ),
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                val rad = Math.toRadians((135f + sweep).toDouble())
                val tx = cx + radius * cos(rad).toFloat()
                val ty = cy + radius * sin(rad).toFloat()
                drawCircle(color = uvColor.copy(alpha = 0.45f), radius = 6.dp.toPx(), center = Offset(tx, ty))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(tx, ty))
            }
        }
    }
}

@Composable
private fun WindCompassCard(
    speedKmh: Double,
    gustsKmh: Double,
    directionDegrees: Int,
    preferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val cardinal = WeatherFormatter.windCardinalDirection(directionDegrees)

    Column(
        modifier = modifier
            .height(144.dp)
            .auraGlassCard(24.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CardHeader(title = stringResource(R.string.metric_wind).uppercase(), icon = Icons.Rounded.Air, tint = AuraSkyAccent)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = WeatherFormatter.formatWind(speedKmh, preferences.windSpeedUnit),
                    style = MaterialTheme.typography.titleLarge,
                    color = AuraTextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$cardinal • Gusts ${WeatherFormatter.formatWind(gustsKmh, preferences.windSpeedUnit)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )
            }

            Canvas(modifier = Modifier.size(52.dp)) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val r = size.width / 2f - 2.dp.toPx()

                drawCircle(
                    color = Color.White.copy(alpha = 0.06f),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = AuraSkyAccent.copy(alpha = 0.25f),
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )

                for (deg in 0 until 360 step 45) {
                    val rad = Math.toRadians(deg.toDouble())
                    val tick = if (deg % 90 == 0) 4.dp.toPx() else 2.dp.toPx()
                    drawLine(
                        color = if (deg == 0) AuraCoralWarn else Color.White.copy(alpha = 0.3f),
                        start = Offset(cx + (r - tick) * cos(rad).toFloat(), cy + (r - tick) * sin(rad).toFloat()),
                        end = Offset(cx + r * cos(rad).toFloat(), cy + r * sin(rad).toFloat()),
                        strokeWidth = if (deg % 90 == 0) 1.5.dp.toPx() else 0.8.dp.toPx()
                    )
                }

                // Compass angle: 0° = North (up = -90° in screen radians)
                val arrowRad = Math.toRadians((directionDegrees - 90).toDouble())
                val tipX = cx + (r - 6.dp.toPx()) * cos(arrowRad).toFloat()
                val tipY = cy + (r - 6.dp.toPx()) * sin(arrowRad).toFloat()
                val tailX = cx - (r * 0.45f) * cos(arrowRad).toFloat()
                val tailY = cy - (r * 0.45f) * sin(arrowRad).toFloat()

                drawLine(
                    color = AuraSkyAccent,
                    start = Offset(tailX, tailY),
                    end = Offset(tipX, tipY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawCircle(color = AuraSkyAccent, radius = 3.dp.toPx(), center = Offset(tipX, tipY))
                drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(cx, cy))
            }
        }
    }
}

@Composable
private fun HumidityWaveCard(
    humidity: Int,
    cloudCover: Int,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "humidity_wave")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "wave"
    )

    Column(
        modifier = modifier
            .height(144.dp)
            .auraGlassCard(24.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CardHeader(title = stringResource(R.string.metric_humidity).uppercase(), icon = Icons.Rounded.WaterDrop, tint = AuraSkyAccent)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "$humidity%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = AuraTextPrimary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Cloud,
                        contentDescription = null,
                        tint = AuraTextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = " Clouds $cloudCover%",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTextSecondary
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(26.dp))
            ) {
                drawCircle(color = Color.White.copy(alpha = 0.06f))
                val fillY = size.height * (1f - (humidity / 100f).coerceIn(0.05f, 0.95f))
                val path = Path().apply {
                    moveTo(0f, size.height)
                    for (x in 0..size.width.toInt() step 2) {
                        val normX = x / size.width
                        val y = fillY + sin(normX * 2f * Math.PI.toFloat() + wavePhase) * 2.5.dp.toPx()
                        lineTo(x.toFloat(), y)
                    }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        listOf(AuraSkyAccent.copy(alpha = 0.85f), Color(0xFF0284C7).copy(alpha = 0.45f))
                    )
                )
            }
        }
    }
}

@Composable
private fun SunOrbitCard(
    isDay: Boolean,
    sunriseTime: String,
    sunsetTime: String,
    progressFraction: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(144.dp)
            .auraGlassCard(24.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CardHeader(
            title = if (isDay) stringResource(R.string.metric_daylight).uppercase() else stringResource(R.string.metric_nightfall).uppercase(),
            icon = Icons.Rounded.WbTwilight,
            tint = AuraAmber
        )

        Column {
            Text(
                text = if (isDay) "Set $sunsetTime" else "Rise $sunriseTime",
                style = MaterialTheme.typography.titleLarge,
                color = AuraTextPrimary
            )
            Text(
                text = "↑ $sunriseTime  •  ↓ $sunsetTime",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
        ) {
            val w = size.width
            val h = size.height
            val horizonY = h - 3.dp.toPx()

            drawLine(
                color = Color.White.copy(alpha = 0.14f),
                start = Offset(0f, horizonY),
                end = Offset(w, horizonY),
                strokeWidth = 1.dp.toPx()
            )

            val arcPath = Path().apply {
                moveTo(4.dp.toPx(), horizonY)
                quadraticTo(w / 2f, -10.dp.toPx(), w - 4.dp.toPx(), horizonY)
            }

            drawPath(
                path = arcPath,
                color = if (isDay) AuraSunGold.copy(alpha = 0.40f) else AuraSkyAccent.copy(alpha = 0.25f),
                style = Stroke(
                    width = 1.6.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                )
            )

            if (isDay) {
                val t = progressFraction.coerceIn(0.05f, 0.95f)
                val x = 4.dp.toPx() + (w - 8.dp.toPx()) * t
                val norm = t * 2f - 1f
                val peak = -8.dp.toPx()
                val y = peak + (horizonY - peak) * (norm * norm)
                drawCircle(color = AuraSunGold.copy(alpha = 0.30f), radius = 7.dp.toPx(), center = Offset(x, y))
                drawCircle(color = AuraSunGold, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}

@Composable
private fun SimpleMetricCard(
    title: String,
    icon: ImageVector,
    iconTint: Color,
    primaryValue: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .height(112.dp)
            .auraGlassCard(24.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        CardHeader(title = title, icon = icon, tint = iconTint)
        Column {
            Text(
                text = primaryValue,
                style = MaterialTheme.typography.titleLarge,
                color = AuraTextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary
            )
        }
    }
}

@Composable
private fun AirQualityCard(airQuality: AirQualitySummary) {
    val category = WeatherFormatter.aqiCategory(airQuality.europeanAqi)
    val aqiColor = when {
        airQuality.europeanAqi <= 20 -> AuraAquaMint
        airQuality.europeanAqi <= 40 -> AuraSkyAccent
        airQuality.europeanAqi <= 60 -> AuraSunGold
        else -> AuraCoralWarn
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .auraGlassCard(24.dp)
            .padding(18.dp)
            .testTag("air_quality_card")
    ) {
        CardHeader(
            title = stringResource(R.string.air_quality_title).uppercase(),
            icon = Icons.Rounded.Eco,
            tint = aqiColor
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${airQuality.europeanAqi} — $category",
                    style = MaterialTheme.typography.titleLarge,
                    color = AuraTextPrimary
                )
                Text(
                    text = "European AQI (US AQI: ${airQuality.usAqi})",
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTextSecondary
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            PollutantBadge("PM2.5", String.format(Locale.US, "%.1f", airQuality.pm25))
            PollutantBadge("PM10", String.format(Locale.US, "%.1f", airQuality.pm10))
            PollutantBadge("O₃", String.format(Locale.US, "%.0f", airQuality.ozone))
            PollutantBadge("NO₂", String.format(Locale.US, "%.0f", airQuality.nitrogenDioxide))
        }
    }
}

@Composable
private fun PollutantBadge(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = AuraTextSecondary)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "$value µg/m³",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = AuraTextPrimary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CardHeader(title: String, icon: ImageVector, tint: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = AuraTextSecondary
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
    }
}
