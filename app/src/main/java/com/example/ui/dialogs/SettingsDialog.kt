package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppUserSettings
import com.example.model.TemperatureUnit
import com.example.model.WindSpeedUnit
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    currentSettings: AppUserSettings,
    onSaveSettings: (AppUserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var tempUnit by remember { mutableStateOf(currentSettings.tempUnit) }
    var windUnit by remember { mutableStateOf(currentSettings.windUnit) }
    var alertsEnabled by remember { mutableStateOf(currentSettings.severeAlertsEnabled) }
    var soundEnabled by remember { mutableStateOf(currentSettings.soundAlertsEnabled) }
    var vibrationEnabled by remember { mutableStateOf(currentSettings.vibrationEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SkyBlueCard,
        title = {
            Text(
                text = "Weather & Radar Settings",
                color = TextPrimaryDark,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Temperature unit
                Column {
                    Text(
                        text = "TEMPERATURE UNIT",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = tempUnit == TemperatureUnit.FAHRENHEIT,
                            onClick = { tempUnit = TemperatureUnit.FAHRENHEIT },
                            label = { Text("Fahrenheit (°F)") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyBluePrimary, selectedLabelColor = androidx.compose.ui.graphics.Color.White)
                        )
                        FilterChip(
                            selected = tempUnit == TemperatureUnit.CELSIUS,
                            onClick = { tempUnit = TemperatureUnit.CELSIUS },
                            label = { Text("Celsius (°C)") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyBluePrimary, selectedLabelColor = androidx.compose.ui.graphics.Color.White)
                        )
                    }
                }

                // Wind unit
                Column {
                    Text(
                        text = "WIND SPEED UNIT",
                        color = TextSecondaryDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = windUnit == WindSpeedUnit.MILES_PER_HOUR,
                            onClick = { windUnit = WindSpeedUnit.MILES_PER_HOUR },
                            label = { Text("mph") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyBluePrimary, selectedLabelColor = androidx.compose.ui.graphics.Color.White)
                        )
                        FilterChip(
                            selected = windUnit == WindSpeedUnit.KILOMETERS_PER_HOUR,
                            onClick = { windUnit = WindSpeedUnit.KILOMETERS_PER_HOUR },
                            label = { Text("km/h") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = SkyBluePrimary, selectedLabelColor = androidx.compose.ui.graphics.Color.White)
                        )
                    }
                }

                HorizontalDivider(color = SkyBlueBorder)

                // Severe storm notifications toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Severe Weather Notifications",
                            color = TextPrimaryDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Real-time heads-up alerts for affected storm areas",
                            color = TextSecondaryDark,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = alertsEnabled,
                        onCheckedChange = { alertsEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SkyBluePrimary,
                            checkedTrackColor = SkyBlueLight
                        )
                    )
                }

                // Sound & Vibration toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Alert Alarm & Vibration",
                        color = TextPrimaryDark,
                        fontSize = 13.sp
                    )
                    Switch(
                        checked = vibrationEnabled,
                        onCheckedChange = {
                            vibrationEnabled = it
                            soundEnabled = it
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SkyBluePrimary,
                            checkedTrackColor = SkyBlueLight
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSettings(
                        currentSettings.copy(
                            tempUnit = tempUnit,
                            windUnit = windUnit,
                            severeAlertsEnabled = alertsEnabled,
                            soundAlertsEnabled = soundEnabled,
                            vibrationEnabled = vibrationEnabled
                        )
                    )
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SkyBluePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryDark)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
