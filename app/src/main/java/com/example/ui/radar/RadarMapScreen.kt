package com.example.ui.radar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.WeatherUiState
import com.example.ui.components.SeverityBadge
import com.example.ui.components.getSeverityColor
import com.example.ui.theme.*
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarMapScreen(
    uiState: WeatherUiState,
    onInspectArea: (AffectedArea?) -> Unit,
    onInspectAlert: (SevereAlert?) -> Unit,
    onTogglePlay: () -> Unit,
    onOffsetChange: (Int) -> Unit,
    onToggleRadarLayer: () -> Unit,
    onToggleAffectedAreasLayer: () -> Unit,
    onToggleStormTracksLayer: () -> Unit,
    onSimulateAlert: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Map pan & zoom state
    var scale by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Radar antenna sweep rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepRotation"
    )

    // Affected area warning pulse animation
    val warningPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "WarningPulse"
    )

    val textMeasurer = rememberTextMeasurer()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RadarSkyNight)
            .testTag("radar_map_container")
    ) {
        // 1. Full Canvas Interactive Map
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 5.0f)
                        panOffset += pan
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val hitArea = findNearestAffectedArea(
                            tapOffset = tapOffset,
                            centerOffset = Offset(size.width / 2f, size.height / 2f) + panOffset,
                            scale = scale,
                            affectedAreas = uiState.activeAlerts.map { it.affectedArea }
                        )
                        onInspectArea(hitArea)
                    }
                }
                .testTag("radar_canvas")
        ) {
            val center = Offset(size.width / 2f, size.height / 2f) + panOffset

            // A. Base Geographical Map & Grid
            drawMapBaseGrid(center, scale, textMeasurer)

            // B. Radar Precipitation Echoes
            if (uiState.showRadarLayer) {
                drawRadarPrecipitationEchoes(
                    center = center,
                    scale = scale,
                    cells = uiState.radarCells,
                    timeOffsetMinutes = uiState.radarPlayOffsetMinutes
                )
            }

            // C. Severe Storm Affected Area Polygons
            if (uiState.showAffectedAreasLayer) {
                drawAffectedAreasPolygons(
                    center = center,
                    scale = scale,
                    alerts = uiState.activeAlerts,
                    inspectedArea = uiState.inspectedArea,
                    pulseAlpha = warningPulseAlpha,
                    textMeasurer = textMeasurer
                )
            }

            // D. Storm Tracks & Movement Direction Vectors
            if (uiState.showStormTracksLayer) {
                drawStormTracks(
                    center = center,
                    scale = scale,
                    cells = uiState.radarCells,
                    timeOffsetMinutes = uiState.radarPlayOffsetMinutes,
                    textMeasurer = textMeasurer
                )
            }

            // E. Rotating Radar Sweep Beam
            drawRadarSweepBeam(center, scale, sweepAngle)

            // F. User / Selected Location Beacon Marker
            drawLocationBeacon(center, uiState.selectedLocation.name, textMeasurer)
        }

        // 2. Top Quick Layers Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = uiState.showRadarLayer,
                onClick = onToggleRadarLayer,
                label = { Text("Radar", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Radar,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SkyBluePrimary,
                    selectedLabelColor = Color.White,
                    containerColor = SkyBlueCard.copy(alpha = 0.9f),
                    labelColor = TextPrimaryDark
                )
            )
            FilterChip(
                selected = uiState.showAffectedAreasLayer,
                onClick = onToggleAffectedAreasLayer,
                label = { Text("Affected Areas (${uiState.activeAlerts.size})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = if (uiState.showAffectedAreasLayer) AlertExtremeRed else AlertSevereOrange,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AlertExtremeRed,
                    selectedLabelColor = Color.White,
                    containerColor = SkyBlueCard.copy(alpha = 0.9f),
                    labelColor = TextPrimaryDark
                )
            )
            FilterChip(
                selected = uiState.showStormTracksLayer,
                onClick = onToggleStormTracksLayer,
                label = { Text("Tracks", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SkyBluePrimary,
                    selectedLabelColor = Color.White,
                    containerColor = SkyBlueCard.copy(alpha = 0.9f),
                    labelColor = TextPrimaryDark
                )
            )
        }

        // 3. Map Utility Floating Buttons (Re-center, Zoom In, Zoom Out)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 70.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    panOffset = Offset.Zero
                    scale = 1.0f
                },
                containerColor = SkyBlueCard,
                contentColor = SkyBluePrimary,
                modifier = Modifier.size(42.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Recenter", modifier = Modifier.size(20.dp))
            }
            FloatingActionButton(
                onClick = { scale = (scale * 1.25f).coerceAtMost(5.0f) },
                containerColor = SkyBlueCard,
                contentColor = TextPrimaryDark,
                modifier = Modifier.size(42.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Zoom In", modifier = Modifier.size(20.dp))
            }
            FloatingActionButton(
                onClick = { scale = (scale / 1.25f).coerceAtLeast(0.5f) },
                containerColor = SkyBlueCard,
                contentColor = TextPrimaryDark,
                modifier = Modifier.size(42.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Zoom Out", modifier = Modifier.size(20.dp))
            }
        }

        // 4. Radar Reflectivity Legend (dBZ) Bar
        Surface(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp)
                .width(36.dp),
            shape = RoundedCornerShape(12.dp),
            color = SkyBlueCard.copy(alpha = 0.92f),
            border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "dBZ", color = TextSecondaryDark, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(10.dp)
                        .height(110.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    RadarEchoPurple, // 70+
                                    RadarEchoRed,    // 60
                                    RadarEchoOrange, // 50
                                    RadarEchoYellow, // 40
                                    RadarEchoGreen,  // 30
                                    Color.Transparent // 15
                                )
                            )
                        )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Hail", color = RadarEchoPurple, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                Text(text = "Rain", color = RadarEchoGreen, fontSize = 8.sp)
            }
        }

        // 5. Bottom Controls: Radar Timeline & Inspected Area Sheet
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Selected / Inspected Affected Area Card
            AnimatedVisibility(
                visible = uiState.inspectedArea != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                uiState.inspectedArea?.let { area ->
                    val alert = uiState.activeAlerts.find { it.affectedArea.id == area.id }
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            getSeverityColor(area.severityLevel)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("affected_area_card")
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SeverityBadge(severity = area.severityLevel)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = area.name,
                                    color = TextPrimaryDark,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onInspectArea(null) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Close",
                                        tint = TextSecondaryDark
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "⚠️ Hazard: ${area.hazardType}",
                                color = getSeverityColor(area.severityLevel),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            if (alert != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = alert.instruction,
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Max Winds: ${alert.maxWindGustMph} mph",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Hail: ${alert.hailSizeInches}\"",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                    Text(
                                        text = "Reflectivity: ${alert.radarReflectivityDbz} dBZ",
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Radar Player & Timeline Bar in Sky Blue
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueCard.copy(alpha = 0.95f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("radar_timeline_controller")
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledIconButton(
                                onClick = onTogglePlay,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = if (uiState.isRadarPlaying) AlertSevereOrange else SkyBluePrimary
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = if (uiState.isRadarPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = "Play/Pause Radar",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (uiState.radarPlayOffsetMinutes == 0) "LIVE RADAR"
                                    else if (uiState.radarPlayOffsetMinutes > 0) "FORECAST (+${uiState.radarPlayOffsetMinutes}m)"
                                    else "PAST (${uiState.radarPlayOffsetMinutes}m)",
                                    color = if (uiState.radarPlayOffsetMinutes > 0) AlertModerateAmber else SkyBluePrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Doppler HD Reflectivity",
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Simulation button to test warning
                        OutlinedButton(
                            onClick = { onSimulateAlert("Tornado") },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AlertExtremeRed)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FlashOn,
                                contentDescription = "Simulate",
                                tint = AlertExtremeRed,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Warning", color = AlertExtremeRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Timeline Slider (-60m to +30m)
                    Slider(
                        value = uiState.radarPlayOffsetMinutes.toFloat(),
                        onValueChange = { onOffsetChange(it.toInt()) },
                        valueRange = -60f..30f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = SkyBluePrimary,
                            activeTrackColor = SkyBlueLight,
                            inactiveTrackColor = SkyBlueContainer
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("-60 min", color = TextMutedDark, fontSize = 10.sp)
                        Text("LIVE NOW", color = SkyBluePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text("+30 min", color = AlertModerateAmber, fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

// ----------------- Canvas Drawing Helpers -----------------

private fun DrawScope.drawMapBaseGrid(center: Offset, scale: Float, textMeasurer: TextMeasurer) {
    val radiusRings = listOf(60f, 130f, 210f, 300f)
    val ringLabels = listOf("15 km", "35 km", "60 km", "100 km")

    // Draw range rings
    radiusRings.forEachIndexed { i, r ->
        val scaledR = r * scale
        drawCircle(
            color = SkyBlueBorder.copy(alpha = 0.35f),
            radius = scaledR,
            center = center,
            style = Stroke(width = 1.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
        )
        // Draw distance label
        val label = ringLabels[i]
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(center.x + scaledR + 4f, center.y - 12f),
            style = TextStyle(color = SkyBlueLight.copy(alpha = 0.8f), fontSize = 9.sp)
        )
    }

    // Crosshairs
    drawLine(
        color = SkyBlueBorder.copy(alpha = 0.25f),
        start = Offset(center.x, 0f),
        end = Offset(center.x, size.height),
        strokeWidth = 1f
    )
    drawLine(
        color = SkyBlueBorder.copy(alpha = 0.25f),
        start = Offset(0f, center.y),
        end = Offset(size.width, center.y),
        strokeWidth = 1f
    )
}

private fun DrawScope.drawRadarPrecipitationEchoes(
    center: Offset,
    scale: Float,
    cells: List<RadarStormCell>,
    timeOffsetMinutes: Int
) {
    cells.forEach { cell ->
        val timeFracHours = timeOffsetMinutes / 60.0
        val headingRad = Math.toRadians(cell.headingDeg)
        val distanceKm = cell.speedMph * 1.609 * timeFracHours
        val kmToPixels = 3.5f * scale

        val dx = (distanceKm * sin(headingRad) * kmToPixels).toFloat()
        val dy = (-distanceKm * cos(headingRad) * kmToPixels).toFloat()

        val cellCenter = Offset(
            x = center.x + ((cell.lon - (-96.7970)) * 1400f * scale).toFloat() + dx,
            y = center.y - ((cell.lat - 32.7767) * 1400f * scale).toFloat() + dy
        )

        val baseRadius = (cell.radiusKm * kmToPixels).toFloat().coerceAtLeast(35f)

        // 1. Outer rain field (green, 30 dBZ)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RadarEchoYellow.copy(alpha = 0.5f),
                    RadarEchoGreen.copy(alpha = 0.4f),
                    RadarEchoGreen.copy(alpha = 0.15f),
                    Color.Transparent
                ),
                center = cellCenter,
                radius = baseRadius * 1.4f
            ),
            radius = baseRadius * 1.4f,
            center = cellCenter
        )

        // 2. Storm core (Orange / Red, 50-60 dBZ)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RadarEchoPurple.copy(alpha = 0.85f),
                    RadarEchoRed.copy(alpha = 0.75f),
                    RadarEchoOrange.copy(alpha = 0.6f),
                    Color.Transparent
                ),
                center = cellCenter,
                radius = baseRadius * 0.7f
            ),
            radius = baseRadius * 0.7f,
            center = cellCenter
        )

        // 3. Hail / rotation vortex core (Purple/Pink, 65+ dBZ)
        if (cell.maxDbz >= 60) {
            drawCircle(
                color = RadarEchoPurple.copy(alpha = 0.9f),
                radius = baseRadius * 0.25f,
                center = cellCenter
            )
        }
    }
}

private fun DrawScope.drawAffectedAreasPolygons(
    center: Offset,
    scale: Float,
    alerts: List<SevereAlert>,
    inspectedArea: AffectedArea?,
    pulseAlpha: Float,
    textMeasurer: TextMeasurer
) {
    alerts.forEach { alert ->
        val area = alert.affectedArea
        val color = getSeverityColor(area.severityLevel)
        val isSelected = inspectedArea?.id == area.id

        val path = Path()

        if (area.polygon.isNotEmpty()) {
            val first = area.polygon.first()
            val startX = center.x + ((first.longitude - (-96.7970)) * 1400f * scale).toFloat()
            val startY = center.y - ((first.latitude - 32.7767) * 1400f * scale).toFloat()
            path.moveTo(startX, startY)

            for (i in 1 until area.polygon.size) {
                val pt = area.polygon[i]
                val px = center.x + ((pt.longitude - (-96.7970)) * 1400f * scale).toFloat()
                val py = center.y - ((pt.latitude - 32.7767) * 1400f * scale).toFloat()
                path.lineTo(px, py)
            }
            path.close()

            val fillAlpha = if (isSelected) 0.45f else (pulseAlpha * 0.4f)
            drawPath(
                path = path,
                color = color.copy(alpha = fillAlpha)
            )

            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = if (isSelected) 3.5f else 2.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 6f), 0f)
                )
            )

            val centerPtX = center.x + ((area.centerLon - (-96.7970)) * 1400f * scale).toFloat()
            val centerPtY = center.y - ((area.centerLat - 32.7767) * 1400f * scale).toFloat()

            drawCircle(
                color = color,
                radius = 7f,
                center = Offset(centerPtX, centerPtY)
            )

            val tagText = "⚠️ ${alert.event.uppercase()}"
            drawText(
                textMeasurer = textMeasurer,
                text = tagText,
                topLeft = Offset(centerPtX - 50f, centerPtY - 24f),
                style = TextStyle(
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
    }
}

private fun DrawScope.drawStormTracks(
    center: Offset,
    scale: Float,
    cells: List<RadarStormCell>,
    timeOffsetMinutes: Int,
    textMeasurer: TextMeasurer
) {
    cells.forEach { cell ->
        val headingRad = Math.toRadians(cell.headingDeg)
        val kmToPixels = 3.5f * scale

        val cellX = center.x + ((cell.lon - (-96.7970)) * 1400f * scale).toFloat()
        val cellY = center.y - ((cell.lat - 32.7767) * 1400f * scale).toFloat()

        val arrowLength = (cell.speedMph * 1.609 * 0.75 * kmToPixels).toFloat()
        val targetX = cellX + (arrowLength * sin(headingRad)).toFloat()
        val targetY = cellY - (arrowLength * cos(headingRad)).toFloat()

        val conePath = Path().apply {
            moveTo(cellX, cellY)
            lineTo(targetX + 18f, targetY - 10f)
            lineTo(targetX - 18f, targetY + 10f)
            close()
        }
        drawPath(
            path = conePath,
            color = SkyBlueLight.copy(alpha = 0.15f)
        )

        drawLine(
            color = SkyBlueLight,
            start = Offset(cellX, cellY),
            end = Offset(targetX, targetY),
            strokeWidth = 2.5f
        )

        drawCircle(
            color = SkyBlueLight,
            radius = 4.5f,
            center = Offset(targetX, targetY)
        )

        val label = "${cell.name} • ${cell.speedMph.toInt()} mph"
        drawText(
            textMeasurer = textMeasurer,
            text = label,
            topLeft = Offset(cellX + 12f, cellY - 16f),
            style = TextStyle(color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        )
    }
}

private fun DrawScope.drawRadarSweepBeam(center: Offset, scale: Float, sweepAngle: Float) {
    val maxRadius = 360f * scale
    val rad = Math.toRadians(sweepAngle.toDouble())
    val endX = center.x + (maxRadius * cos(rad)).toFloat()
    val endY = center.y + (maxRadius * sin(rad)).toFloat()

    drawLine(
        color = SkyBlueLight.copy(alpha = 0.75f),
        start = center,
        end = Offset(endX, endY),
        strokeWidth = 2f
    )
}

private fun DrawScope.drawLocationBeacon(center: Offset, locationName: String, textMeasurer: TextMeasurer) {
    drawCircle(
        color = SkyBlueLight.copy(alpha = 0.35f),
        radius = 24f,
        center = center
    )
    drawCircle(
        color = SkyBluePrimary,
        radius = 8f,
        center = center
    )
    drawCircle(
        color = Color.White,
        radius = 3.5f,
        center = center
    )

    drawText(
        textMeasurer = textMeasurer,
        text = "📍 $locationName",
        topLeft = Offset(center.x + 12f, center.y + 4f),
        style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    )
}

private fun findNearestAffectedArea(
    tapOffset: Offset,
    centerOffset: Offset,
    scale: Float,
    affectedAreas: List<AffectedArea>
): AffectedArea? {
    var nearest: AffectedArea? = null
    var minDistance = Float.MAX_VALUE

    affectedAreas.forEach { area ->
        val areaPx = centerOffset.x + ((area.centerLon - (-96.7970)) * 1400f * scale).toFloat()
        val areaPy = centerOffset.y - ((area.centerLat - 32.7767) * 1400f * scale).toFloat()
        val dist = hypot(tapOffset.x - areaPx, tapOffset.y - areaPy)
        if (dist < 100f && dist < minDistance) {
            minDistance = dist
            nearest = area
        }
    }
    return nearest
}
