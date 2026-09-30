package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storm_alerts")
data class StormAlertEntity(
    @PrimaryKey
    val alertId: String,
    val event: String,
    val severity: String, // EXTREME, SEVERE, MODERATE, MINOR
    val headline: String,
    val description: String,
    val instruction: String,
    val affectedAreaName: String,
    val centerLat: Double,
    val centerLon: Double,
    val effectiveTime: String,
    val expiresTime: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isDismissed: Boolean = false,
    val polygonGeoJson: String = "" // serialized coordinates list
)
