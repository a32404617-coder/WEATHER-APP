package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.WeatherRepository
import com.example.location.DeviceLocationProvider
import com.example.model.*
import com.example.notification.StormNotificationManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class WeatherUiState(
    val isLoading: Boolean = false,
    val selectedLocation: WeatherLocation = WeatherLocation(
        name = "Dallas",
        region = "Texas",
        country = "United States",
        latitude = 32.7767,
        longitude = -96.7970
    ),
    val currentWeather: CurrentWeather? = null,
    val hourlyForecast: List<HourlyForecastItem> = emptyList(),
    val dailyForecast: List<DailyForecastItem> = emptyList(),
    val activeAlerts: List<SevereAlert> = emptyList(),
    val radarCells: List<RadarStormCell> = emptyList(),
    val searchResults: List<WeatherLocation> = emptyList(),
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val inspectedAlert: SevereAlert? = null,
    val inspectedArea: AffectedArea? = null,
    val errorMessage: String? = null,
    // Radar playback & map controls
    val radarPlayOffsetMinutes: Int = 0, // -60 to +30
    val isRadarPlaying: Boolean = false,
    val showRadarLayer: Boolean = true,
    val showAffectedAreasLayer: Boolean = true,
    val showStormTracksLayer: Boolean = true,
    val showWindVectorsLayer: Boolean = true,
    val radarMapZoom: Float = 1.0f,
    val settings: AppUserSettings = AppUserSettings()
)

class StormRadarViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = WeatherRepository(database)
    private val locationProvider = DeviceLocationProvider(application)
    private val notificationManager = StormNotificationManager(application)

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    val savedLocations: StateFlow<List<WeatherLocation>> = repository.savedLocations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var radarPlaybackJob: Job? = null
    private var alertMonitoringJob: Job? = null

    init {
        loadWeatherData()
        startAlertMonitoring()
    }

    fun loadWeatherData(lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val currentLoc = _uiState.value.selectedLocation
            val targetLat = lat ?: currentLoc.latitude
            val targetLon = lon ?: currentLoc.longitude

            try {
                val (current, forecasts) = repository.getWeatherData(targetLat, targetLon)
                val alerts = repository.getSevereAlerts(targetLat, targetLon, currentLoc.name, current)
                val radarCells = repository.generateRealisticRadarCells(targetLat, targetLon)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        currentWeather = current,
                        hourlyForecast = forecasts.first,
                        dailyForecast = forecasts.second,
                        activeAlerts = alerts,
                        radarCells = radarCells
                    )
                }

                // If severe alert detected, trigger notification
                if (alerts.isNotEmpty() && _uiState.value.settings.severeAlertsEnabled) {
                    alerts.firstOrNull()?.let { topAlert ->
                        notificationManager.postSevereAlertNotification(topAlert)
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unable to fetch live weather: ${e.localizedMessage ?: "Network error"}"
                    )
                }
            }
        }
    }

    fun requestGpsLocation() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val point = locationProvider.getLastKnownLocation()
            if (point != null) {
                val gpsLoc = WeatherLocation(
                    name = "Current Location",
                    region = "GPS",
                    country = "",
                    latitude = point.latitude,
                    longitude = point.longitude,
                    isCurrentGps = true
                )
                _uiState.update { it.copy(selectedLocation = gpsLoc) }
                loadWeatherData(point.latitude, point.longitude)
            } else {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Location unavailable. Please check GPS settings.")
                }
            }
        }
    }

    fun selectLocation(location: WeatherLocation) {
        _uiState.update {
            it.copy(
                selectedLocation = location,
                searchQuery = "",
                searchResults = emptyList()
            )
        }
        loadWeatherData(location.latitude, location.longitude)
    }

    fun searchLocations(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        if (query.length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true) }
            val results = repository.searchLocations(query)
            _uiState.update { it.copy(searchResults = results, isSearching = false) }
        }
    }

    fun saveCurrentLocation() {
        viewModelScope.launch {
            val loc = _uiState.value.selectedLocation.copy(isFavorite = true)
            repository.saveLocation(loc)
        }
    }

    fun deleteSavedLocation(id: Long) {
        viewModelScope.launch {
            repository.deleteLocation(id)
        }
    }

    fun triggerSevereStormSimulation(alertType: String) {
        viewModelScope.launch {
            val loc = _uiState.value.selectedLocation
            val simAlert = repository.generateSimulatedSevereAlert(
                locationName = loc.name,
                lat = loc.latitude,
                lon = loc.longitude,
                alertType = alertType
            )

            val updatedAlerts = listOf(simAlert) + _uiState.value.activeAlerts.filter { it.id != simAlert.id }
            _uiState.update {
                it.copy(
                    activeAlerts = updatedAlerts,
                    inspectedAlert = simAlert,
                    inspectedArea = simAlert.affectedArea
                )
            }

            // Fire real Android system notification!
            notificationManager.postSevereAlertNotification(simAlert)

            // Save to database
            repository.saveAlertToHistory(simAlert)
        }
    }

    fun inspectAlert(alert: SevereAlert?) {
        _uiState.update {
            it.copy(
                inspectedAlert = alert,
                inspectedArea = alert?.affectedArea
            )
        }
    }

    fun inspectAffectedArea(area: AffectedArea?) {
        _uiState.update {
            it.copy(
                inspectedArea = area,
                inspectedAlert = _uiState.value.activeAlerts.find { a -> a.affectedArea.id == area?.id }
            )
        }
    }

    fun toggleRadarPlayback() {
        val currentlyPlaying = _uiState.value.isRadarPlaying
        if (currentlyPlaying) {
            radarPlaybackJob?.cancel()
            _uiState.update { it.copy(isRadarPlaying = false) }
        } else {
            _uiState.update { it.copy(isRadarPlaying = true) }
            radarPlaybackJob = viewModelScope.launch {
                while (true) {
                    delay(500L)
                    _uiState.update { state ->
                        val nextOffset = if (state.radarPlayOffsetMinutes >= 30) -60 else state.radarPlayOffsetMinutes + 10
                        state.copy(radarPlayOffsetMinutes = nextOffset)
                    }
                }
            }
        }
    }

    fun setRadarOffset(offset: Int) {
        _uiState.update { it.copy(radarPlayOffsetMinutes = offset) }
    }

    fun toggleRadarLayer() {
        _uiState.update { it.copy(showRadarLayer = !it.showRadarLayer) }
    }

    fun toggleAffectedAreasLayer() {
        _uiState.update { it.copy(showAffectedAreasLayer = !it.showAffectedAreasLayer) }
    }

    fun toggleStormTracksLayer() {
        _uiState.update { it.copy(showStormTracksLayer = !it.showStormTracksLayer) }
    }

    fun toggleWindVectorsLayer() {
        _uiState.update { it.copy(showWindVectorsLayer = !it.showWindVectorsLayer) }
    }

    fun updateSettings(newSettings: AppUserSettings) {
        _uiState.update { it.copy(settings = newSettings) }
    }

    fun dismissAlert(alertId: String) {
        _uiState.update { state ->
            val remaining = state.activeAlerts.filter { it.id != alertId }
            state.copy(
                activeAlerts = remaining,
                inspectedAlert = if (state.inspectedAlert?.id == alertId) null else state.inspectedAlert,
                inspectedArea = if (state.inspectedArea?.id?.contains(alertId) == true) null else state.inspectedArea
            )
        }
    }

    private fun startAlertMonitoring() {
        alertMonitoringJob = viewModelScope.launch {
            while (true) {
                delay(120_000L) // every 2 minutes check active alerts
                val loc = _uiState.value.selectedLocation
                try {
                    val alerts = repository.getSevereAlerts(loc.latitude, loc.longitude, loc.name, _uiState.value.currentWeather)
                    if (alerts.isNotEmpty()) {
                        _uiState.update { it.copy(activeAlerts = alerts) }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        radarPlaybackJob?.cancel()
        alertMonitoringJob?.cancel()
    }
}
