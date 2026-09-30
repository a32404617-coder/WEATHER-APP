package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AlertSeverity
import com.example.model.SevereAlert
import com.example.ui.alerts.AlertsScreen
import com.example.ui.dialogs.LocationSearchSheet
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.forecast.ForecastScreen
import com.example.ui.radar.RadarMapScreen
import com.example.ui.theme.*

enum class AppTab(val title: String) {
    FORECAST("Forecast"),
    RADAR("Radar & Map"),
    ALERTS("Severe Alerts")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StormRadarApp(
    viewModel: StormRadarViewModel,
    initialTab: String? = null
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedLocations by viewModel.savedLocations.collectAsStateWithLifecycle()

    var currentTab by remember {
        mutableStateOf(
            if (initialTab == "radar") AppTab.RADAR
            else if (initialTab == "alerts") AppTab.ALERTS
            else AppTab.FORECAST
        )
    }

    var showLocationSheet by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Request Notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    // Request Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.requestGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.FlashOn,
                            contentDescription = "StormRadar Logo",
                            tint = SkyBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "StormRadar",
                            color = TextPrimaryDark,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showLocationSheet = true },
                        modifier = Modifier.testTag("top_bar_location_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Search Location",
                            tint = SkyBluePrimary
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("top_bar_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondaryDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBlueCard,
                    titleContentColor = TextPrimaryDark
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SkyBlueCard,
                contentColor = TextPrimaryDark,
                tonalElevation = 6.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.FORECAST,
                    onClick = { currentTab = AppTab.FORECAST },
                    icon = {
                        Icon(Icons.Filled.CloudQueue, contentDescription = "Forecast")
                    },
                    label = { Text("Forecast") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SkyBluePrimary,
                        selectedTextColor = SkyBluePrimary,
                        indicatorColor = SkyBlueContainer,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    ),
                    modifier = Modifier.testTag("nav_forecast_tab")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.RADAR,
                    onClick = { currentTab = AppTab.RADAR },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.activeAlerts.isNotEmpty()) {
                                    Badge(
                                        containerColor = AlertExtremeRed
                                    ) {
                                        Text("${uiState.activeAlerts.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Radar, contentDescription = "Radar & Map")
                        }
                    },
                    label = { Text("Radar & Map") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SkyBluePrimary,
                        selectedTextColor = SkyBluePrimary,
                        indicatorColor = SkyBlueContainer,
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    ),
                    modifier = Modifier.testTag("nav_radar_tab")
                )
                NavigationBarItem(
                    selected = currentTab == AppTab.ALERTS,
                    onClick = { currentTab = AppTab.ALERTS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.activeAlerts.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(AlertExtremeRed)
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Filled.WarningAmber, contentDescription = "Severe Alerts")
                        }
                    },
                    label = { Text("Severe Alerts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = AlertExtremeRed,
                        selectedTextColor = AlertExtremeRed,
                        indicatorColor = AlertExtremeRed.copy(alpha = 0.15f),
                        unselectedIconColor = TextSecondaryDark,
                        unselectedTextColor = TextSecondaryDark
                    ),
                    modifier = Modifier.testTag("nav_alerts_tab")
                )
            }
        },
        containerColor = SkyBlueSurface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.FORECAST -> {
                    ForecastScreen(
                        uiState = uiState,
                        onRefresh = { viewModel.loadWeatherData() },
                        onLocationClick = { showLocationSheet = true },
                        onNavigateToRadar = { currentTab = AppTab.RADAR },
                        onAlertClick = { alert ->
                            viewModel.inspectAlert(alert)
                            currentTab = AppTab.RADAR
                        },
                        onFavoriteClick = { viewModel.saveCurrentLocation() }
                    )
                }
                AppTab.RADAR -> {
                    RadarMapScreen(
                        uiState = uiState,
                        onInspectArea = { viewModel.inspectAffectedArea(it) },
                        onInspectAlert = { viewModel.inspectAlert(it) },
                        onTogglePlay = { viewModel.toggleRadarPlayback() },
                        onOffsetChange = { viewModel.setRadarOffset(it) },
                        onToggleRadarLayer = { viewModel.toggleRadarLayer() },
                        onToggleAffectedAreasLayer = { viewModel.toggleAffectedAreasLayer() },
                        onToggleStormTracksLayer = { viewModel.toggleStormTracksLayer() },
                        onSimulateAlert = { alertType ->
                            viewModel.triggerSevereStormSimulation(alertType)
                        }
                    )
                }
                AppTab.ALERTS -> {
                    AlertsScreen(
                        uiState = uiState,
                        onNavigateToRadarForAlert = { alert ->
                            viewModel.inspectAlert(alert)
                            currentTab = AppTab.RADAR
                        },
                        onSimulateAlert = { alertType ->
                            viewModel.triggerSevereStormSimulation(alertType)
                        },
                        onDismissAlert = { alertId ->
                            viewModel.dismissAlert(alertId)
                        }
                    )
                }
            }

            // Loading overlay
            if (uiState.isLoading && uiState.currentWeather == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(SkyBlueSurface.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SkyBluePrimary)
                }
            }
        }
    }

    // Location search bottom sheet
    if (showLocationSheet) {
        LocationSearchSheet(
            query = uiState.searchQuery,
            onQueryChange = { viewModel.searchLocations(it) },
            isSearching = uiState.isSearching,
            searchResults = uiState.searchResults,
            savedLocations = savedLocations,
            onSelectLocation = { viewModel.selectLocation(it) },
            onUseGps = {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            },
            onDeleteSaved = { viewModel.deleteSavedLocation(it) },
            onDismiss = { showLocationSheet = false }
        )
    }

    // Settings dialog
    if (showSettingsDialog) {
        SettingsDialog(
            currentSettings = uiState.settings,
            onSaveSettings = { viewModel.updateSettings(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}
