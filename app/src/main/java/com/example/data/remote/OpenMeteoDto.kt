package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String?,
    @Json(name = "current") val current: CurrentDto?,
    @Json(name = "hourly") val hourly: HourlyDto?,
    @Json(name = "daily") val daily: DailyDto?
)

@JsonClass(generateAdapter = true)
data class CurrentDto(
    val time: String?,
    val interval: Int?,
    @Json(name = "temperature_2m") val temperature2m: Double?,
    @Json(name = "relative_humidity_2m") val relativeHumidity2m: Double?,
    @Json(name = "apparent_temperature") val apparentTemperature: Double?,
    @Json(name = "is_day") val isDay: Int?,
    val precipitation: Double?,
    @Json(name = "weather_code") val weatherCode: Int?,
    @Json(name = "surface_pressure") val surfacePressure: Double?,
    @Json(name = "wind_speed_10m") val windSpeed10m: Double?,
    @Json(name = "wind_direction_10m") val windDirection10m: Double?,
    @Json(name = "uv_index") val uvIndex: Double?
)

@JsonClass(generateAdapter = true)
data class HourlyDto(
    val time: List<String>?,
    @Json(name = "temperature_2m") val temperature2m: List<Double>?,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "is_day") val isDay: List<Int>?
)

@JsonClass(generateAdapter = true)
data class DailyDto(
    val time: List<String>?,
    @Json(name = "weather_code") val weatherCode: List<Int>?,
    @Json(name = "temperature_2m_max") val temperature2mMax: List<Double>?,
    @Json(name = "temperature_2m_min") val temperature2mMin: List<Double>?,
    @Json(name = "uv_index_max") val uvIndexMax: List<Double>?,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>?,
    @Json(name = "wind_speed_10m_max") val windSpeed10mMax: List<Double>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResponse(
    val results: List<GeocodingResultDto>?
)

@JsonClass(generateAdapter = true)
data class GeocodingResultDto(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double?,
    val country: String?,
    @Json(name = "admin1") val admin1: String?,
    @Json(name = "country_code") val countryCode: String?
)
