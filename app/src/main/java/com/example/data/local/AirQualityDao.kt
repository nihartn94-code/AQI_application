package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AirQualityDao {
    @Query("SELECT * FROM air_quality_readings ORDER BY timestamp DESC LIMIT 1")
    fun getLatestReading(): Flow<AirQualityReadingEntity?>

    @Query("SELECT * FROM air_quality_readings ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentReadings(limit: Int = 30): Flow<List<AirQualityReadingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReading(reading: AirQualityReadingEntity): Long

    @Query("DELETE FROM air_quality_readings")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM air_quality_readings")
    suspend fun getCount(): Int
}
