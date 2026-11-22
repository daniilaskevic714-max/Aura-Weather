package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AuraDarkColorScheme = darkColorScheme(
    primary = AuraSkyAccent,
    onPrimary = AuraMidnight,
    secondary = AuraAquaMint,
    onSecondary = AuraMidnight,
    tertiary = AuraSunGold,
    onTertiary = AuraMidnight,
    background = AuraMidnight,
    onBackground = AuraTextPrimary,
    surface = AuraSurface,
    onSurface = AuraTextPrimary,
    surfaceVariant = AuraSurfaceVariant,
    onSurfaceVariant = AuraTextSecondary,
    outline = AuraOutline,
    error = AuraCoralWarn,
    onError = AuraMidnight
)

val AuraShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun AuraWeatherTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AuraDarkColorScheme,
        typography = AuraTypography,
        shapes = AuraShapes,
        content = content
    )
}
