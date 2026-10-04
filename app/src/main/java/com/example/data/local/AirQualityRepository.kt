package com.example.data.local

import com.example.data.model.AirQualityData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AirQualityRepository(private val dao: AirQualityDao) {

    val latestReading: Flow<AirQualityData?> = dao.getLatestReading().map { entity ->
        entity?.toDomainModel()
    }

    val recentReadings: Flow<List<AirQualityData>> = dao.getRecentReadings(30).map { list ->
        list.map { it.toDomainModel() }
    }

    fun getHistoricalReadings(limit: Int = 60): Flow<List<AirQualityData>> =
        dao.getRecentReadings(limit).map { list ->
            list.map { it.toDomainModel() }.sortedBy { it.timestamp }
        }

    suspend fun saveReading(data: AirQualityData): Long {
        return dao.insertReading(AirQualityReadingEntity.fromDomainModel(data))
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
