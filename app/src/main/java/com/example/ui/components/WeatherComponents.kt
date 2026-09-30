package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlertSeverity
import com.example.model.TemperatureUnit
import com.example.model.WindSpeedUnit
import com.example.ui.theme.*

fun formatTemp(celsius: Double, unit: TemperatureUnit): String {
    val temp = if (unit == TemperatureUnit.FAHRENHEIT) {
        (celsius * 9.0 / 5.0) + 32.0
    } else {
        celsius
    }
    return "${temp.toInt()}°"
}

fun formatWind(kmh: Double, unit: WindSpeedUnit): String {
    return if (unit == WindSpeedUnit.MILES_PER_HOUR) {
        val mph = (kmh * 0.621371).toInt()
        "$mph mph"
    } else {
        "${kmh.toInt()} km/h"
    }
}

fun getWeatherIcon(code: Int, isDay: Boolean): ImageVector {
    return when (code) {
        0 -> if (isDay) Icons.Filled.WbSunny else Icons.Filled.NightsStay
        1, 2 -> if (isDay) Icons.Filled.WbCloudy else Icons.Filled.NightsStay
        3 -> Icons.Filled.Cloud
        45, 48 -> Icons.Filled.Grain // Fog
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Filled.WaterDrop // Rain
        71, 73, 75, 77, 85, 86 -> Icons.Filled.AcUnit // Snow
        95, 96, 99 -> Icons.Filled.FlashOn // Thunderstorm
        else -> Icons.Filled.WbCloudy
    }
}

fun getSeverityColor(severity: AlertSeverity): Color {
    return when (severity) {
        AlertSeverity.EXTREME -> AlertExtremeRed
        AlertSeverity.SEVERE -> AlertSevereOrange
        AlertSeverity.MODERATE -> AlertModerateAmber
        AlertSeverity.MINOR -> AlertMinorBlue
    }
}

@Composable
fun SeverityBadge(severity: AlertSeverity, modifier: Modifier = Modifier) {
    val color = getSeverityColor(severity)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = severity.name,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color = SkyBluePrimary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title.uppercase(),
                    color = TextSecondaryDark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = TextMutedDark,
                fontSize = 11.sp
            )
        }
    }
}
