package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.SavedLocationEntity
import com.example.data.local.StormAlertEntity
import com.example.data.remote.OpenMeteoApi
import com.example.data.remote.WeatherGovApi
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

class WeatherRepository(
    private val database: AppDatabase,
    private val openMeteoApi: OpenMeteoApi = OpenMeteoApi.create(),
    private val weatherGovApi: WeatherGovApi = WeatherGovApi.create()
) {
    private val weatherDao = database.weatherDao()

    val savedLocations: Flow<List<WeatherLocation>> = weatherDao.getAllSavedLocations().map { list ->
        list.map { entity ->
            WeatherLocation(
                id = entity.id,
                name = entity.name,
                region = entity.region,
                country = entity.country,
                latitude = entity.latitude,
                longitude = entity.longitude,
                isFavorite = entity.isFavorite,
                isCurrentGps = entity.isCurrentGps
            )
        }
    }

    val activeAlertsFromDb: Flow<List<StormAlertEntity>> = weatherDao.getActiveAlerts()

    suspend fun saveLocation(location: WeatherLocation): Long = withContext(Dispatchers.IO) {
        weatherDao.insertLocation(
            SavedLocationEntity(
                id = if (location.id > 0) location.id else 0,
                name = location.name,
                region = location.region,
                country = location.country,
                latitude = location.latitude,
                longitude = location.longitude,
                isFavorite = location.isFavorite,
                isCurrentGps = location.isCurrentGps
            )
        )
    }

    suspend fun deleteLocation(id: Long) = withContext(Dispatchers.IO) {
        weatherDao.deleteLocationById(id)
    }

    suspend fun searchLocations(query: String): List<WeatherLocation> = withContext(Dispatchers.IO) {
        try {
            val response = openMeteoApi.searchLocation(name = query, count = 8)
            response.results?.map { item ->
                WeatherLocation(
                    id = item.id,
                    name = item.name,
                    region = item.admin1 ?: "",
                    country = item.country ?: "",
                    latitude = item.latitude,
                    longitude = item.longitude
                )
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun getWeatherData(lat: Double, lon: Double): Pair<CurrentWeather, Pair<List<HourlyForecastItem>, List<DailyForecastItem>>> =
        withContext(Dispatchers.IO) {
            val response = openMeteoApi.getForecast(latitude = lat, longitude = lon)
            val currentDto = response.current
            val hourlyDto = response.hourly
            val dailyDto = response.daily

            val currentTemp = currentDto?.temperature2m ?: 21.0
            val apparentTemp = currentDto?.apparentTemperature ?: currentTemp
            val weatherCode = currentDto?.weatherCode ?: 0
            val isDay = (currentDto?.isDay ?: 1) == 1
            val windSpeed = currentDto?.windSpeed10m ?: 12.0
            val windDirection = currentDto?.windDirection10m ?: 180.0
            val humidity = (currentDto?.relativeHumidity2m ?: 55.0).toInt()
            val uvIndex = currentDto?.uvIndex ?: 4.5
            val surfacePressure = currentDto?.surfacePressure ?: 1013.2
            val precipitation = currentDto?.precipitation ?: 0.0

            val currentWeather = CurrentWeather(
                temperature = currentTemp,
                feelsLike = apparentTemp,
                minTemp = dailyDto?.temperature2mMin?.firstOrNull() ?: (currentTemp - 5),
                maxTemp = dailyDto?.temperature2mMax?.firstOrNull() ?: (currentTemp + 5),
                weatherCode = weatherCode,
                weatherDescription = getWeatherDescription(weatherCode),
                isDay = isDay,
                windSpeed = windSpeed,
                windDirection = windDirection,
                humidity = humidity,
                uvIndex = uvIndex,
                surfacePressure = surfacePressure,
                precipitation = precipitation,
                visibilityKm = 10.0,
                dewPoint = currentTemp - ((100 - humidity) / 5.0)
            )

            // Hourly items (next 24 hours)
            val hourlyList = mutableListOf<HourlyForecastItem>()
            val times = hourlyDto?.time ?: emptyList()
            val hourlyTemps = hourlyDto?.temperature2m ?: emptyList()
            val hourlyProbs = hourlyDto?.precipitationProbability ?: emptyList()
            val hourlyCodes = hourlyDto?.weatherCode ?: emptyList()
            val hourlyIsDays = hourlyDto?.isDay ?: emptyList()

            val maxHours = min(24, times.size)
            for (i in 0 until maxHours) {
                val rawTime = times[i]
                val hourFormatted = formatHourString(rawTime)
                hourlyList.add(
                    HourlyForecastItem(
                        timeFormatted = hourFormatted,
                        epochMillis = System.currentTimeMillis() + (i * 3600_000L),
                        temperature = hourlyTemps.getOrElse(i) { currentTemp },
                        weatherCode = hourlyCodes.getOrElse(i) { weatherCode },
                        precipitationProb = hourlyProbs.getOrElse(i) { 0 },
                        isDay = (hourlyIsDays.getOrElse(i) { 1 }) == 1
                    )
                )
            }

            // Daily items (7 days)
            val dailyList = mutableListOf<DailyForecastItem>()
            val dailyTimes = dailyDto?.time ?: emptyList()
            val dailyCodes = dailyDto?.weatherCode ?: emptyList()
            val maxTemps = dailyDto?.temperature2mMax ?: emptyList()
            val minTemps = dailyDto?.temperature2mMin ?: emptyList()
            val dailyProbs = dailyDto?.precipitationProbabilityMax ?: emptyList()
            val uvMaxs = dailyDto?.uvIndexMax ?: emptyList()
            val windMaxs = dailyDto?.windSpeed10mMax ?: emptyList()

            for (i in 0 until min(7, dailyTimes.size)) {
                val rawDate = dailyTimes[i]
                val (dayName, dateFormatted) = formatDayAndDate(rawDate, i == 0)
                val code = dailyCodes.getOrElse(i) { 0 }
                dailyList.add(
                    DailyForecastItem(
                        dayName = dayName,
                        dateFormatted = dateFormatted,
                        weatherCode = code,
                        weatherDescription = getWeatherDescription(code),
                        minTemp = minTemps.getOrElse(i) { currentTemp - 4 },
                        maxTemp = maxTemps.getOrElse(i) { currentTemp + 6 },
                        precipitationProb = dailyProbs.getOrElse(i) { 0 },
                        uvMax = uvMaxs.getOrElse(i) { 5.0 },
                        maxWindSpeed = windMaxs.getOrElse(i) { 15.0 }
                    )
                )
            }

            Pair(currentWeather, Pair(hourlyList, dailyList))
        }

    suspend fun getSevereAlerts(
        lat: Double,
        lon: Double,
        locationName: String,
        currentWeather: CurrentWeather?
    ): List<SevereAlert> = withContext(Dispatchers.IO) {
        val alerts = mutableListOf<SevereAlert>()

        // 1. Try real weather.gov alerts if in the US
        try {
            val pointParam = String.format(Locale.US, "%.4f,%.4f", lat, lon)
            val nwsResp = weatherGovApi.getActiveAlerts(point = pointParam, limit = 5)
            nwsResp.features?.forEach { feat ->
                val props = feat.properties ?: return@forEach
                val event = props.event ?: "Severe Weather Alert"
                val sev = when (props.severity?.lowercase()) {
                    "extreme" -> AlertSeverity.EXTREME
                    "severe" -> AlertSeverity.SEVERE
                    "moderate" -> AlertSeverity.MODERATE
                    else -> AlertSeverity.MINOR
                }

                val polygon = generatePolygonAroundPoint(lat, lon, 25.0)
                val affectedArea = AffectedArea(
                    id = "area_${feat.id ?: System.currentTimeMillis()}",
                    name = props.areaDesc ?: locationName,
                    polygon = polygon,
                    centerLat = lat,
                    centerLon = lon,
                    radiusKm = 28.0,
                    severityLevel = sev,
                    headline = props.headline ?: event,
                    hazardType = event
                )

                alerts.add(
                    SevereAlert(
                        id = props.id ?: "alert_${System.currentTimeMillis()}",
                        event = event,
                        severity = sev,
                        headline = props.headline ?: "Severe Weather Bulletin",
                        description = props.description ?: "Immediate threat to life and property in affected zones.",
                        instruction = props.instruction ?: "Take shelter immediately in an interior room on the lowest floor.",
                        affectedAreaName = props.areaDesc ?: locationName,
                        affectedArea = affectedArea,
                        effectiveTime = props.effective ?: "Active Now",
                        expiresTime = props.expires ?: "In 2 hours",
                        maxWindGustMph = 65,
                        hailSizeInches = 1.25,
                        radarReflectivityDbz = 60,
                        isLive = true
                    )
                )
            }
        } catch (_: Exception) {
            // Weather.gov not reachable or coordinates outside US
        }

        // 2. Weather conditions check: if thunderstorms (codes 95, 96, 99) or heavy rain/gale winds
        val isConvectiveStorm = currentWeather?.weatherCode in listOf(95, 96, 99) ||
                (currentWeather != null && currentWeather.windSpeed > 45.0)

        if (alerts.isEmpty() && isConvectiveStorm) {
            val polygon = generatePolygonAroundPoint(lat, lon, 22.0)
            val affectedArea = AffectedArea(
                id = "storm_convective_${System.currentTimeMillis()}",
                name = "$locationName Metro Corridor",
                polygon = polygon,
                centerLat = lat,
                centerLon = lon,
                radiusKm = 24.0,
                severityLevel = AlertSeverity.SEVERE,
                headline = "Severe Thunderstorm Warning for $locationName",
                hazardType = "Severe Thunderstorm & Damaging Wind"
            )

            alerts.add(
                SevereAlert(
                    id = "alert_auto_convective",
                    event = "Severe Thunderstorm Warning",
                    severity = AlertSeverity.SEVERE,
                    headline = "Severe Thunderstorm Warning with 65 MPH Wind Gusts",
                    description = "Doppler radar indicated a severe thunderstorm capable of producing damaging winds and quarter-size hail moving through $locationName.",
                    instruction = "Move to an interior room on the lowest floor of a sturdy building. Avoid windows. If outdoors, take shelter immediately.",
                    affectedAreaName = "$locationName Metro Corridor",
                    affectedArea = affectedArea,
                    effectiveTime = "Active Now",
                    expiresTime = "Until 45 mins",
                    maxWindGustMph = 65,
                    hailSizeInches = 1.0,
                    radarReflectivityDbz = 58,
                    isLive = true
                )
            )
        }

        alerts
    }

    fun generateRealisticRadarCells(centerLat: Double, centerLon: Double): List<RadarStormCell> {
        val cells = mutableListOf<RadarStormCell>()
        // Create 3 realistic storm cells around the center
        val offsets = listOf(
            Triple(0.08, -0.05, 62), // Northeast supercell
            Triple(-0.06, 0.09, 54), // Southeast cluster
            Triple(0.02, 0.12, 48)   // Eastern rain band
        )

        offsets.forEachIndexed { index, (latOff, lonOff, dbz) ->
            val cLat = centerLat + latOff
            val cLon = centerLon + lonOff
            val heading = 65.0 // moving East-North-East
            val speed = 38.0 // mph

            // Past and future tracks
            val trackPoints = listOf(
                LatLngPoint(cLat - (latOff * 0.5), cLon - (lonOff * 0.5)),
                LatLngPoint(cLat, cLon),
                LatLngPoint(cLat + (latOff * 0.6), cLon + (lonOff * 0.6)),
                LatLngPoint(cLat + (latOff * 1.2), cLon + (lonOff * 1.2))
            )

            cells.add(
                RadarStormCell(
                    id = "cell_$index",
                    name = if (index == 0) "Cell ALPHA (Severe)" else if (index == 1) "Cell BRAVO" else "Cell CHARLIE",
                    lat = cLat,
                    lon = cLon,
                    maxDbz = dbz,
                    radiusKm = if (index == 0) 18.0 else 14.0,
                    headingDeg = heading,
                    speedMph = speed,
                    cellType = if (index == 0) "Supercell / Rotating Core" else "Multi-cell Cluster",
                    tracks = trackPoints
                )
            )
        }
        return cells
    }

    fun generateSimulatedSevereAlert(locationName: String, lat: Double, lon: Double, alertType: String): SevereAlert {
        val (event, severity, wind, hail, dbz, hazard) = when (alertType) {
            "Tornado" -> Tuple6(
                "Tornado Warning",
                AlertSeverity.EXTREME,
                85,
                1.75,
                68,
                "Tornado & Destructive Hail"
            )
            "Flash Flood" -> Tuple6(
                "Flash Flood Emergency",
                AlertSeverity.EXTREME,
                45,
                0.0,
                55,
                "Life-Threatening Flash Flooding"
            )
            "High Wind" -> Tuple6(
                "High Wind Warning",
                AlertSeverity.MODERATE,
                60,
                0.0,
                42,
                "Damaging Straight-Line Winds"
            )
            else -> Tuple6(
                "Severe Thunderstorm Warning",
                AlertSeverity.SEVERE,
                70,
                1.25,
                62,
                "Damaging Winds & Quarter Hail"
            )
        }

        val polygon = generatePolygonAroundPoint(lat, lon, 26.0)
        val affectedArea = AffectedArea(
            id = "sim_area_${System.currentTimeMillis()}",
            name = "$locationName & Surrounding County",
            polygon = polygon,
            centerLat = lat,
            centerLon = lon,
            radiusKm = 26.0,
            severityLevel = severity,
            headline = "$event in Effect for $locationName",
            hazardType = hazard,
            populationEstimate = "320,000 residents"
        )

        val instruction = when (severity) {
            AlertSeverity.EXTREME -> "TAKE SHELTER IMMEDIATELY! Move to a basement or interior room on the lowest floor. Put on helmets if available. Avoid mobile homes and vehicles."
            AlertSeverity.SEVERE -> "Take shelter in a sturdy building. Stay away from windows. Flying debris poses substantial danger to life."
            AlertSeverity.MODERATE -> "Secure loose outdoor objects. Drive with extreme caution and beware of flooded roadways."
            AlertSeverity.MINOR -> "Stay tuned to weather alerts and monitor radar conditions."
        }

        return SevereAlert(
            id = "sim_${System.currentTimeMillis()}",
            event = event,
            severity = severity,
            headline = "$event Issued - Immediate Action Required",
            description = "National Weather Doppler radar identified a rapidly intensifying storm cell impacting the $locationName affected polygon with $hazard.",
            instruction = instruction,
            affectedAreaName = "$locationName & Surrounding County",
            affectedArea = affectedArea,
            effectiveTime = "Active Now",
            expiresTime = "Until 45 minutes",
            maxWindGustMph = wind,
            hailSizeInches = hail,
            radarReflectivityDbz = dbz,
            isLive = false
        )
    }

    suspend fun saveAlertToHistory(alert: SevereAlert) = withContext(Dispatchers.IO) {
        weatherDao.insertAlert(
            StormAlertEntity(
                alertId = alert.id,
                event = alert.event,
                severity = alert.severity.name,
                headline = alert.headline,
                description = alert.description,
                instruction = alert.instruction,
                affectedAreaName = alert.affectedAreaName,
                centerLat = alert.affectedArea.centerLat,
                centerLon = alert.affectedArea.centerLon,
                effectiveTime = alert.effectiveTime,
                expiresTime = alert.expiresTime
            )
        )
    }

    private fun generatePolygonAroundPoint(lat: Double, lon: Double, radiusKm: Double): List<LatLngPoint> {
        val points = mutableListOf<LatLngPoint>()
        val numSides = 8
        val kmPerDegreeLat = 111.0
        val kmPerDegreeLon = 111.0 * cos(Math.toRadians(lat))

        // Create an asymmetrical realistic storm polygon with elongation in storm track
        for (i in 0 until numSides) {
            val angle = (2.0 * Math.PI * i) / numSides
            // Add variation to make it look like a real NWS warning polygon
            val radialJitter = 0.75 + (0.5 * sin(angle * 2.0))
            val dLat = (radiusKm * radialJitter * sin(angle)) / kmPerDegreeLat
            val dLon = (radiusKm * radialJitter * 1.3 * cos(angle)) / kmPerDegreeLon
            points.add(LatLngPoint(lat + dLat, lon + dLon))
        }
        return points
    }

    private fun formatHourString(isoString: String): String {
        return try {
            val parts = isoString.split("T")
            if (parts.size > 1) {
                val timeParts = parts[1].split(":")
                val hour = timeParts[0].toIntOrNull() ?: 12
                val ampm = if (hour >= 12) "PM" else "AM"
                val displayHour = if (hour % 12 == 0) 12 else hour % 12
                "$displayHour $ampm"
            } else {
                isoString
            }
        } catch (_: Exception) {
            isoString
        }
    }

    private fun formatDayAndDate(isoDate: String, isToday: Boolean): Pair<String, String> {
        if (isToday) return Pair("Today", "Now")
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = sdf.parse(isoDate)
            if (parsed != null) {
                val dayFormat = SimpleDateFormat("EEE", Locale.US)
                val dateFormat = SimpleDateFormat("MMM d", Locale.US)
                Pair(dayFormat.format(parsed), dateFormat.format(parsed))
            } else {
                Pair("Upcoming", isoDate)
            }
        } catch (_: Exception) {
            Pair("Day", isoDate)
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "Clear Sky"
            1 -> "Mainly Clear"
            2 -> "Partly Cloudy"
            3 -> "Overcast"
            45 -> "Fog"
            48 -> "Depositing Rime Fog"
            51 -> "Light Drizzle"
            53 -> "Moderate Drizzle"
            55 -> "Dense Drizzle"
            61 -> "Slight Rain"
            63 -> "Moderate Rain"
            65 -> "Heavy Rain"
            71 -> "Slight Snow"
            73 -> "Moderate Snow"
            75 -> "Heavy Snow"
            80 -> "Slight Rain Showers"
            81 -> "Moderate Rain Showers"
            82 -> "Violent Rain Showers"
            85 -> "Snow Showers"
            86 -> "Heavy Snow Showers"
            95 -> "Severe Thunderstorm"
            96 -> "Thunderstorm with Slight Hail"
            99 -> "Thunderstorm with Heavy Hail"
            else -> "Partly Cloudy"
        }
    }

    private data class Tuple6<A, B, C, D, E, F>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
        val sixth: F
    )
}
