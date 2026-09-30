package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.WeatherLocation
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationSearchSheet(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean,
    searchResults: List<WeatherLocation>,
    savedLocations: List<WeatherLocation>,
    onSelectLocation: (WeatherLocation) -> Unit,
    onUseGps: () -> Unit,
    onDeleteSaved: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SkyBlueCard,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Weather Locations",
                color = TextPrimaryDark,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Search text field
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search city, state, or country...", color = TextSecondaryDark) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = SkyBluePrimary)
                },
                trailingIcon = if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear", tint = TextSecondaryDark)
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("location_search_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Current GPS Location Button
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SkyBlueContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onUseGps()
                        onDismiss()
                    }
                    .testTag("use_gps_button")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = "GPS",
                        tint = SkyBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Use Current GPS Location",
                        color = SkyBlueDark,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Results or Saved Locations
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (query.length >= 2) {
                    item {
                        Text(
                            text = "SEARCH RESULTS",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    if (isSearching) {
                        item {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth(),
                                color = SkyBluePrimary
                            )
                        }
                    }
                    items(searchResults) { loc ->
                        LocationRowItem(
                            location = loc,
                            isSaved = false,
                            onSelect = {
                                onSelectLocation(loc)
                                onDismiss()
                            },
                            onDelete = {}
                        )
                    }
                } else {
                    item {
                        Text(
                            text = "PINNED & SAVED CITIES",
                            color = TextSecondaryDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    if (savedLocations.isEmpty()) {
                        val defaultCities = listOf(
                            WeatherLocation(name = "Dallas", region = "Texas", country = "USA", latitude = 32.7767, longitude = -96.7970),
                            WeatherLocation(name = "Oklahoma City", region = "Oklahoma", country = "USA", latitude = 35.4676, longitude = -97.5164),
                            WeatherLocation(name = "Miami", region = "Florida", country = "USA", latitude = 25.7617, longitude = -80.1918),
                            WeatherLocation(name = "Chicago", region = "Illinois", country = "USA", latitude = 41.8781, longitude = -87.6298)
                        )
                        items(defaultCities) { loc ->
                            LocationRowItem(
                                location = loc,
                                isSaved = false,
                                onSelect = {
                                    onSelectLocation(loc)
                                    onDismiss()
                                },
                                onDelete = {}
                            )
                        }
                    } else {
                        items(savedLocations) { loc ->
                            LocationRowItem(
                                location = loc,
                                isSaved = true,
                                onSelect = {
                                    onSelectLocation(loc)
                                    onDismiss()
                                },
                                onDelete = { onDeleteSaved(loc.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRowItem(
    location: WeatherLocation,
    isSaved: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SkyBlueSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, SkyBlueBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = SkyBluePrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = location.name,
                    color = TextPrimaryDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                if (location.region.isNotBlank()) {
                    Text(
                        text = "${location.region}, ${location.country}",
                        color = TextSecondaryDark,
                        fontSize = 11.sp
                    )
                }
            }
            if (isSaved) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondaryDark,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
