package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AuraAmber
import com.example.ui.theme.AuraAquaMint
import com.example.ui.theme.AuraSkyAccent
import com.example.ui.theme.AuraSunGold
import com.example.ui.theme.AuraTextPrimary

data class WeatherVisual(
    val icon: ImageVector,
    val tint: Color
)

fun weatherVisualFor(code: Int, isDay: Boolean): WeatherVisual {
    return when (code) {
        0 -> if (isDay) {
            WeatherVisual(Icons.Rounded.WbSunny, AuraSunGold)
        } else {
            WeatherVisual(Icons.Rounded.NightsStay, AuraSkyAccent)
        }
        1, 2 -> if (isDay) {
            WeatherVisual(Icons.Rounded.CloudQueue, AuraAmber)
        } else {
            WeatherVisual(Icons.Rounded.NightsStay, AuraSkyAccent)
        }
        3 -> WeatherVisual(Icons.Rounded.WbCloudy, Color(0xFFCBD5E1))
        45, 48 -> WeatherVisual(Icons.Rounded.Cloud, Color(0xFF94A3B8))
        51, 53, 55, 56, 57 -> WeatherVisual(Icons.Rounded.Grain, AuraSkyAccent)
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherVisual(Icons.Rounded.Umbrella, Color(0xFF38BDF8))
        71, 73, 75, 77, 85, 86 -> WeatherVisual(Icons.Rounded.AcUnit, Color(0xFFA5F3FC))
        95, 96, 99 -> WeatherVisual(Icons.Rounded.Thunderstorm, Color(0xFFC084FC))
        else -> WeatherVisual(Icons.Rounded.WaterDrop, AuraAquaMint)
    }
}

fun Modifier.auraGlassCard(cornerRadius: Dp = 24.dp): Modifier = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.09f),
                Color.White.copy(alpha = 0.03f)
            )
        )
    )
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.04f)
            )
        ),
        shape = RoundedCornerShape(cornerRadius)
    )

@Composable
fun AnimatedWeatherHeroIcon(
    weatherCode: Int,
    isDay: Boolean,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val visual = weatherVisualFor(weatherCode, isDay)
    val transition = rememberInfiniteTransition(label = "hero_weather_icon")

    val animMod = when (weatherCode) {
        0 -> if (isDay) {
            val rot by transition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
                label = "sun_spin"
            )
            Modifier.graphicsLayer { rotationZ = rot }
        } else {
            val bob by transition.animateFloat(
                initialValue = -4f,
                targetValue = 4f,
                animationSpec = infiniteRepeatable(tween(3600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "moon_bob"
            )
            Modifier.graphicsLayer { translationY = bob }
        }
        61, 63, 65, 80, 81, 82, 95, 96, 99 -> {
            val scale by transition.animateFloat(
                initialValue = 0.95f,
                targetValue = 1.05f,
                animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "storm_pulse"
            )
            Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        }
        else -> {
            val drift by transition.animateFloat(
                initialValue = -5f,
                targetValue = 5f,
                animationSpec = infiniteRepeatable(tween(3800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "cloud_drift"
            )
            Modifier.graphicsLayer { translationX = drift }
        }
    }

    val glowScale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_scale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .graphicsLayer { scaleX = glowScale; scaleY = glowScale }
                .background(
                    Brush.radialGradient(
                        colors = listOf(visual.tint.copy(alpha = 0.34f), Color.Transparent)
                    )
                )
        )
        Icon(
            imageVector = visual.icon,
            contentDescription = contentDescription,
            tint = AuraTextPrimary,
            modifier = Modifier
                .fillMaxSize(0.76f)
                .then(animMod)
        )
    }
}
