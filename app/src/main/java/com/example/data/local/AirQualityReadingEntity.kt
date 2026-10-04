package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.AirQualityData

@Entity(tableName = "air_quality_readings")
data class AirQualityReadingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val temperature: Double,
    val humidity: Double,
    val gas: Int,
    val dust: Double,
    val aqi: Int,
    val airQuality: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val speed: Double,
    val satellites: Int,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): AirQualityData {
        return AirQualityData(
            temperature = temperature,
            humidity = humidity,
            gas = gas,
            dust = dust,
            aqi = aqi,
            airQuality = airQuality,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude,
            speed = speed,
            satellites = satellites,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomainModel(data: AirQualityData): AirQualityReadingEntity {
            return AirQualityReadingEntity(
                temperature = data.temperature,
                humidity = data.humidity,
                gas = data.gas,
                dust = data.dust,
                aqi = data.aqi,
                airQuality = data.airQuality,
                latitude = data.latitude,
                longitude = data.longitude,
                altitude = data.altitude,
                speed = data.speed,
                satellites = data.satellites,
                timestamp = data.timestamp
            )
        }
    }
}
