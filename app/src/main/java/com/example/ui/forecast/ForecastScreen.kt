package com.example.ui.forecast

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.WeatherUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForecastScreen(
    uiState: WeatherUiState,
    onRefresh: () -> Unit,
    onLocationClick: () -> Unit,
    onNavigateToRadar: () -> Unit,
    onAlertClick: (SevereAlert) -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val weather = uiState.currentWeather
    val tempUnit = uiState.settings.tempUnit
    val windUnit = uiState.settings.windUnit

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SkyBlueSurface)
            .testTag("forecast_screen_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Location Bar
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onLocationClick() }
                    .testTag("location_picker_card")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (uiState.selectedLocation.isCurrentGps) Icons.Filled.MyLocation else Icons.Filled.LocationOn,
                        contentDescription = "Location",
                        tint = SkyBluePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.selectedLocation.name,
                            color = TextPrimaryDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.selectedLocation.region.isNotBlank()) {
                            Text(
                                text = "${uiState.selectedLocation.region}, ${uiState.selectedLocation.country}",
                                color = TextSecondaryDark,
                                fontSize = 12.sp
                            )
                        }
                    }
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.testTag("favorite_button")
                    ) {
                        Icon(
                            imageVector = if (uiState.selectedLocation.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (uiState.selectedLocation.isFavorite) SkyBluePrimary else TextSecondaryDark
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.testTag("refresh_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh",
                            tint = SkyBluePrimary
                        )
                    }
                }
            }
        }

        // 2. Severe Storm Active Alert Banner (if any)
        if (uiState.activeAlerts.isNotEmpty()) {
            val topAlert = uiState.activeAlerts.first()
            val alertColor = getSeverityColor(topAlert.severity)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, alertColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAlertClick(topAlert) }
                        .testTag("severe_alert_banner")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Severe Alert",
                                tint = alertColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = topAlert.event.uppercase(),
                                    color = alertColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "Affected Area: ${topAlert.affectedAreaName}",
                                    color = TextPrimaryDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            SeverityBadge(severity = topAlert.severity)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = topAlert.headline,
                            color = TextPrimaryDark,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Expires: ${topAlert.expiresTime}",
                                color = TextSecondaryDark,
                                fontSize = 11.sp
                            )
                            Button(
                                onClick = onNavigateToRadar,
                                colors = ButtonDefaults.buttonColors(containerColor = alertColor),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("view_on_map_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Map,
                                    contentDescription = "Map",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "View Affected Map", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 3. Hero Weather Card in Normal Sky Blue Gradient
        if (weather != null) {
            item {
                Card(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = SkyBluePrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_weather_card")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        SkyBluePrimary,
                                        SkyBlueLight
                                    )
                                )
                            )
                            .padding(26.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = getWeatherIcon(weather.weatherCode, weather.isDay),
                                contentDescription = weather.weatherDescription,
                                tint = Color.White,
                                modifier = Modifier.size(76.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = formatTemp(weather.temperature, tempUnit),
                                color = TextOnSky,
                                fontSize = 68.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = weather.weatherDescription,
                                color = TextOnSky,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color.White.copy(alpha = 0.22f)
                            ) {
                                Text(
                                    text = "Feels like ${formatTemp(weather.feelsLike, tempUnit)}  •  H: ${formatTemp(weather.maxTemp, tempUnit)}  L: ${formatTemp(weather.minTemp, tempUnit)}",
                                    color = TextOnSky,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Hourly Forecast Bar
        if (uiState.hourlyForecast.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "24-HOUR FORECAST",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Hourly update",
                            color = SkyBluePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.hourlyForecast.forEach { item ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
                                border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder),
                                modifier = Modifier
                                    .width(78.dp)
                                    .padding(vertical = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = item.timeFormatted,
                                        color = TextSecondaryDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Icon(
                                        imageVector = getWeatherIcon(item.weatherCode, item.isDay),
                                        contentDescription = "Weather",
                                        tint = SkyBluePrimary,
                                        modifier = Modifier.size(26.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = formatTemp(item.temperature, tempUnit),
                                        color = TextPrimaryDark,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (item.precipitationProb > 0) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${item.precipitationProb}%",
                                            color = SkyBlueDark,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. 7-Day Forecast Section
        if (uiState.dailyForecast.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = SkyBlueCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "7-DAY WEATHER OUTLOOK",
                            color = TextSecondaryDark,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        uiState.dailyForecast.forEachIndexed { index, day ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = day.dayName,
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(60.dp)
                                )
                                Icon(
                                    imageVector = getWeatherIcon(day.weatherCode, true),
                                    contentDescription = day.weatherDescription,
                                    tint = SkyBluePrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                if (day.precipitationProb > 15) {
                                    Text(
                                        text = "${day.precipitationProb}% rain",
                                        color = SkyBlueDark,
                                        fontSize = 11.sp,
                                        modifier = Modifier.width(55.dp)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(55.dp))
                                }
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = formatTemp(day.minTemp, tempUnit),
                                    color = TextSecondaryDark,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                // Temperature progress line
                                Box(
                                    modifier = Modifier
                                        .width(70.dp)
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(SkyBlueLight, AlertModerateAmber)
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = formatTemp(day.maxTemp, tempUnit),
                                    color = TextPrimaryDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (index < uiState.dailyForecast.size - 1) {
                                HorizontalDivider(
                                    color = SkyBlueBorder.copy(alpha = 0.5f),
                                    thickness = 0.8.dp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Detailed Weather Metrics Grid
        if (weather != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "Wind",
                            value = formatWind(weather.windSpeed, windUnit),
                            subtitle = "Direction: ${weather.windDirection.toInt()}°",
                            icon = Icons.Filled.Air,
                            iconTint = SkyBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Humidity",
                            value = "${weather.humidity}%",
                            subtitle = "Dew point ${formatTemp(weather.dewPoint, tempUnit)}",
                            icon = Icons.Filled.WaterDrop,
                            iconTint = SkyBlueDark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            title = "UV Index",
                            value = String.format(java.util.Locale.US, "%.1f", weather.uvIndex),
                            subtitle = if (weather.uvIndex > 6) "High - Wear protection" else "Moderate",
                            icon = Icons.Filled.WbSunny,
                            iconTint = AlertModerateAmber,
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            title = "Pressure",
                            value = "${weather.surfacePressure.toInt()} hPa",
                            subtitle = if (weather.surfacePressure < 1005) "Low pressure system" else "Stable barometer",
                            icon = Icons.Filled.Compress,
                            iconTint = SkyBluePrimary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
