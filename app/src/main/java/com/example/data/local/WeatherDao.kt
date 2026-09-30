package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WeatherDao {

    @Query("SELECT * FROM saved_locations ORDER BY isFavorite DESC, lastViewedTimestamp DESC")
    fun getAllSavedLocations(): Flow<List<SavedLocationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocation(location: SavedLocationEntity): Long

    @Query("DELETE FROM saved_locations WHERE id = :id")
    suspend fun deleteLocationById(id: Long)

    @Update
    suspend fun updateLocation(location: SavedLocationEntity)

    @Query("SELECT * FROM storm_alerts WHERE isDismissed = 0 ORDER BY timestamp DESC")
    fun getActiveAlerts(): Flow<List<StormAlertEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: StormAlertEntity)

    @Query("UPDATE storm_alerts SET isDismissed = 1 WHERE alertId = :alertId")
    suspend fun dismissAlert(alertId: String)

    @Query("DELETE FROM storm_alerts WHERE timestamp < :cutoffTimestamp")
    suspend fun clearOldAlerts(cutoffTimestamp: Long)
}
