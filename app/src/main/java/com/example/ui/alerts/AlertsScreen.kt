package com.example.ui.alerts

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertSeverity
import com.example.model.SevereAlert
import com.example.ui.WeatherUiState
import com.example.ui.components.SeverityBadge
import com.example.ui.components.getSeverityColor
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    uiState: WeatherUiState,
    onNavigateToRadarForAlert: (SevereAlert) -> Unit,
    onSimulateAlert: (String) -> Unit,
    onDismissAlert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredAlerts = remember(uiState.activeAlerts, selectedFilter) {
        when (selectedFilter) {
            "Extreme" -> uiState.activeAlerts.filter { it.severity == AlertSeverity.EXTREME }
            "Severe" -> uiState.activeAlerts.filter { it.severity == AlertSeverity.SEVERE }
            else -> uiState.activeAlerts
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SkyBlueSurface)
            .testTag("alerts_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Simulator & Notification Trigger Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, SkyBlueBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_simulator_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = "Notifications",
                            tint = SkyBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "REAL-TIME ALERT NOTIFICATIONS",
                                color = SkyBluePrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Test high-priority system notification & affected area",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Trigger a real-time storm warning simulation to test device notifications with custom alarm, vibration, and interactive affected area polygon:",
                        color = TextPrimaryDark,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onSimulateAlert("Tornado") },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertExtremeRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulate_tornado_button")
                        ) {
                            Text("🌪️ Tornado", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onSimulateAlert("Severe Thunderstorm") },
                            colors = ButtonDefaults.buttonColors(containerColor = AlertSevereOrange),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.1f)
                                .testTag("simulate_thunderstorm_button")
                        ) {
                            Text("⚡ Severe Storm", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onSimulateAlert("Flash Flood") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🌊 Flash Flood", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onSimulateAlert("High Wind") },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💨 High Wind", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // 2. Filter Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Extreme", "Severe").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SkyBluePrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // 3. Active Storm Alerts List
        if (filteredAlerts.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "No Alerts",
                            tint = RadarEchoGreen,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Active Severe Alerts",
                            color = TextPrimaryDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Weather conditions in ${uiState.selectedLocation.name} are currently calm. Use the buttons above to test severe alerts.",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredAlerts, key = { it.id }) { alert ->
                val severityColor = getSeverityColor(alert.severity)
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, severityColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("alert_item_${alert.id}")
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SeverityBadge(severity = alert.severity)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = alert.event,
                                color = TextPrimaryDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDismissAlert(alert.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss",
                                    tint = TextSecondaryDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Affected Area Highlight box
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = severityColor.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, severityColor.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Place,
                                        contentDescription = "Area",
                                        tint = severityColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "EFFECTED AREA:",
                                        color = severityColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = alert.affectedAreaName,
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Estimated Population in Warning Polygon: ${alert.affectedArea.populationEstimate}",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = alert.headline,
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = alert.instruction,
                            color = severityColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Valid: ${alert.effectiveTime}",
                                    color = TextMutedDark,
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = "Expires: ${alert.expiresTime}",
                                    color = TextSecondaryDark,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { onNavigateToRadarForAlert(alert) },
                                colors = ButtonDefaults.buttonColors(containerColor = severityColor),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = "Map",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Inspect on Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 4. Storm Safety & Preparedness Guidelines
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Shield,
                            contentDescription = "Safety",
                            tint = SkyBluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SEVERE WEATHER SAFETY RULES",
                            color = SkyBluePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SafetyRuleItem(
                        title = "Tornado Warning",
                        instruction = "Go to the lowest floor, basement, or interior room without windows. Protect your head with a helmet or thick blankets."
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SafetyRuleItem(
                        title = "Flash Flood Emergency",
                        instruction = "Never drive or walk into flooded roads. 12 inches of water can float small cars. 'Turn Around, Don't Drown!'"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SafetyRuleItem(
                        title = "Severe Thunderstorm",
                        instruction = "Seek substantial shelter indoors. Stay away from windows and avoid wired electronic devices until all lightning ceases."
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyRuleItem(title: String, instruction: String) {
    Column {
        Text(
            text = "• $title",
            color = TextPrimaryDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = instruction,
            color = TextSecondaryDark,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
