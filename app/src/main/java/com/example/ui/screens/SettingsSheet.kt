package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.PressureUnit
import com.example.data.TemperatureUnit
import com.example.data.UserPreferences
import com.example.data.WindSpeedUnit
import com.example.ui.theme.AuraDeepNavy
import com.example.ui.theme.AuraSkyAccent
import com.example.ui.theme.AuraSurface
import com.example.ui.theme.AuraTextPrimary
import com.example.ui.theme.AuraTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    preferences: UserPreferences,
    onSelectTempUnit: (TemperatureUnit) -> Unit,
    onSelectWindUnit: (WindSpeedUnit) -> Unit,
    onSelectPressureUnit: (PressureUnit) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AuraDeepNavy,
        contentColor = AuraTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineMedium,
                color = AuraTextPrimary
            )

            // Temperature Unit
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "TEMPERATURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraSkyAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TemperatureUnit.entries.forEach { unit ->
                        val selected = preferences.temperatureUnit == unit
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectTempUnit(unit) },
                            label = { Text(unit.label) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AuraSurface,
                                labelColor = AuraTextSecondary,
                                selectedContainerColor = AuraSkyAccent.copy(alpha = 0.22f),
                                selectedLabelColor = AuraSkyAccent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selected) AuraSkyAccent else Color.White.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("unit_temp_${unit.name.lowercase()}")
                        )
                    }
                }
            }

            // Wind Speed Unit
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "WIND SPEED",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraSkyAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WindSpeedUnit.entries.forEach { unit ->
                        val selected = preferences.windSpeedUnit == unit
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectWindUnit(unit) },
                            label = { Text(unit.label) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AuraSurface,
                                labelColor = AuraTextSecondary,
                                selectedContainerColor = AuraSkyAccent.copy(alpha = 0.22f),
                                selectedLabelColor = AuraSkyAccent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selected) AuraSkyAccent else Color.White.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("unit_wind_${unit.name.lowercase()}")
                        )
                    }
                }
            }

            // Pressure Unit
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "BAROMETRIC PRESSURE",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraSkyAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PressureUnit.entries.forEach { unit ->
                        val selected = preferences.pressureUnit == unit
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectPressureUnit(unit) },
                            label = { Text(unit.label) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = AuraSurface,
                                labelColor = AuraTextSecondary,
                                selectedContainerColor = AuraSkyAccent.copy(alpha = 0.22f),
                                selectedLabelColor = AuraSkyAccent
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (selected) AuraSkyAccent else Color.White.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("unit_pressure_${unit.name.lowercase()}")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Meteorological telemetry powered by Open-Meteo Forecast, Geocoding & Air Quality APIs.",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTextSecondary
            )
        }
    }
}
