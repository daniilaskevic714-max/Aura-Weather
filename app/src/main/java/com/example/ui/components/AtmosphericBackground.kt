package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.sin

private data class SkyPalette(
    val top: Color,
    val bottom: Color,
    val glowPrimary: Color,
    val glowSecondary: Color
)

@Composable
fun AtmosphericBackground(
    weatherCode: Int,
    isDay: Boolean
) {
    val palette = remember(weatherCode, isDay) {
        when {
            weatherCode in listOf(95, 96, 99) -> SkyPalette(
                top = Color(0xFF070815),
                bottom = Color(0xFF1A1635),
                glowPrimary = Color(0xFF8B5CF6).copy(alpha = 0.30f),
                glowSecondary = Color(0xFF3B82F6).copy(alpha = 0.22f)
            )
            weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82) -> SkyPalette(
                top = Color(0xFF071122),
                bottom = Color(0xFF0F243E),
                glowPrimary = Color(0xFF0EA5E9).copy(alpha = 0.28f),
                glowSecondary = Color(0xFF6366F1).copy(alpha = 0.20f)
            )
            weatherCode in listOf(71, 73, 75, 77, 85, 86) -> SkyPalette(
                top = Color(0xFF0B1C2E),
                bottom = Color(0xFF1A344E),
                glowPrimary = Color(0xFF67E8F9).copy(alpha = 0.24f),
                glowSecondary = Color(0xFFE2E8F0).copy(alpha = 0.18f)
            )
            weatherCode in listOf(0, 1) && isDay -> SkyPalette(
                top = Color(0xFF0A2A4D),
                bottom = Color(0xFF09162B),
                glowPrimary = Color(0xFFFACC15).copy(alpha = 0.24f),
                glowSecondary = Color(0xFF38BDF8).copy(alpha = 0.26f)
            )
            weatherCode in listOf(0, 1) && !isDay -> SkyPalette(
                top = Color(0xFF050914),
                bottom = Color(0xFF0F1B33),
                glowPrimary = Color(0xFF38BDF8).copy(alpha = 0.24f),
                glowSecondary = Color(0xFF818CF8).copy(alpha = 0.22f)
            )
            else -> SkyPalette(
                top = Color(0xFF0B1424),
                bottom = Color(0xFF17243A),
                glowPrimary = Color(0xFF94A3B8).copy(alpha = 0.20f),
                glowSecondary = Color(0xFF38BDF8).copy(alpha = 0.16f)
            )
        }
    }

    val transition = rememberInfiniteTransition(label = "sky_aurora")
    val drift1 by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(tween(22000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift1"
    )
    val drift2 by transition.animateFloat(
        initialValue = 0.80f,
        targetValue = 0.22f,
        animationSpec = infiniteRepeatable(tween(26000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift2"
    )
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "particles_pulse"
    )
    val rainPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "rain_phase"
    )
    val breath by transition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(7000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.top, palette.bottom)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width.coerceAtLeast(1f)
            val h = size.height.coerceAtLeast(1f)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.glowPrimary, Color.Transparent),
                    center = Offset(w * drift1, h * 0.22f),
                    radius = w * 0.55f * breath
                ),
                radius = w * 0.55f * breath,
                center = Offset(w * drift1, h * 0.22f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(palette.glowSecondary, Color.Transparent),
                    center = Offset(w * drift2, h * 0.72f),
                    radius = w * 0.52f * breath
                ),
                radius = w * 0.52f * breath,
                center = Offset(w * drift2, h * 0.72f)
            )

            val isRain = weatherCode in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82, 95, 96, 99)
            val isSnow = weatherCode in listOf(71, 73, 75, 77, 85, 86)

            when {
                isRain -> {
                    val count = 42
                    for (i in 0 until count) {
                        val x = ((i * 7919) % w.toInt()).toFloat()
                        val ySeed = ((i * 9973) % h.toInt()).toFloat()
                        val speed = 0.85f + (i % 3) * 0.35f
                        val y = (ySeed + rainPhase * h * speed) % h
                        val alpha = (0.10f + (i % 4) * 0.06f)
                        drawLine(
                            color = Color(0xFF7DD3FC).copy(alpha = alpha),
                            start = Offset(x, y),
                            end = Offset(x + 3.dp.toPx(), y + 14.dp.toPx()),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
                isSnow -> {
                    val count = 38
                    for (i in 0 until count) {
                        val xSeed = ((i * 12347) % w.toInt()).toFloat()
                        val ySeed = ((i * 8707) % h.toInt()).toFloat()
                        val y = (ySeed + pulse * h * (0.4f + (i % 3) * 0.2f)) % h
                        val x = (xSeed + sin(pulse * 2f * Math.PI.toFloat() + i) * 12.dp.toPx()) % w
                        drawCircle(
                            color = Color.White.copy(alpha = 0.28f + (i % 3) * 0.12f),
                            radius = (1.5.dp + (i % 2).dp).toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
                !isDay -> {
                    val stars = 26
                    for (i in 0 until stars) {
                        val x = ((i * 15485863) % w.toInt()).toFloat()
                        val y = ((i * 32452843) % (h * 0.55f).toInt().coerceAtLeast(1)).toFloat()
                        val twinkle = 0.25f + 0.55f * sin(pulse * 2f * Math.PI.toFloat() + i).coerceIn(0f, 1f)
                        drawCircle(
                            color = Color.White.copy(alpha = twinkle),
                            radius = (1.dp + (i % 2).dp * 0.6f).toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
