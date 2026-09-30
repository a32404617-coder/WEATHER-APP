package com.example.model

data class LatLngPoint(
    val latitude: Double,
    val longitude: Double
)

data class AffectedArea(
    val id: String,
    val name: String,
    val polygon: List<LatLngPoint>,
    val centerLat: Double,
    val centerLon: Double,
    val radiusKm: Double,
    val severityLevel: AlertSeverity,
    val headline: String,
    val hazardType: String,
    val populationEstimate: String = "150,000+"
)

enum class AlertSeverity {
    EXTREME, // Red (Tornado Warning, Flash Flood Emergency)
    SEVERE,  // Orange/Amber (Severe Thunderstorm, High Wind)
    MODERATE,// Yellow (Flood Watch, Special Weather Statement)
    MINOR    // Blue/Cyan (Advisory, Wind Advisory)
}

data class SevereAlert(
    val id: String,
    val event: String,
    val severity: AlertSeverity,
    val headline: String,
    val description: String,
    val instruction: String,
    val affectedAreaName: String,
    val affectedArea: AffectedArea,
    val effectiveTime: String,
    val expiresTime: String,
    val maxWindGustMph: Int,
    val hailSizeInches: Double,
    val radarReflectivityDbz: Int,
    val isLive: Boolean = true
)

data class RadarStormCell(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val maxDbz: Int,
    val radiusKm: Double,
    val headingDeg: Double,
    val speedMph: Double,
    val cellType: String, // "Supercell", "Squall Line", "Pulse Storm", "Hail Core"
    val tracks: List<LatLngPoint> // past to projected future positions
)

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val minTemp: Double,
    val maxTemp: Double,
    val weatherCode: Int,
    val weatherDescription: String,
    val isDay: Boolean,
    val windSpeed: Double,
    val windDirection: Double,
    val humidity: Int,
    val uvIndex: Double,
    val surfacePressure: Double,
    val precipitation: Double,
    val visibilityKm: Double,
    val dewPoint: Double
)

data class HourlyForecastItem(
    val timeFormatted: String,
    val epochMillis: Long,
    val temperature: Double,
    val weatherCode: Int,
    val precipitationProb: Int,
    val isDay: Boolean
)

data class DailyForecastItem(
    val dayName: String,
    val dateFormatted: String,
    val weatherCode: Int,
    val weatherDescription: String,
    val minTemp: Double,
    val maxTemp: Double,
    val precipitationProb: Int,
    val uvMax: Double,
    val maxWindSpeed: Double
)

data class WeatherLocation(
    val id: Long = 0,
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val isCurrentGps: Boolean = false,
    val isFavorite: Boolean = false
)

enum class TemperatureUnit {
    CELSIUS,
    FAHRENHEIT
}

enum class WindSpeedUnit {
    KILOMETERS_PER_HOUR,
    MILES_PER_HOUR
}

data class AppUserSettings(
    val tempUnit: TemperatureUnit = TemperatureUnit.FAHRENHEIT,
    val windUnit: WindSpeedUnit = WindSpeedUnit.MILES_PER_HOUR,
    val severeAlertsEnabled: Boolean = true,
    val soundAlertsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val radarSweepAnimation: Boolean = true,
    val radarSpeedIndex: Int = 1 // 0: Slow, 1: Normal, 2: Fast
)
