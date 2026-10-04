package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [AirQualityReadingEntity::class], version = 1, exportSchema = false)
abstract class AirQualityDatabase : RoomDatabase() {
    abstract fun airQualityDao(): AirQualityDao

    companion object {
        @Volatile
        private var INSTANCE: AirQualityDatabase? = null

        fun getInstance(context: Context): AirQualityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AirQualityDatabase::class.java,
                    "smart_air_quality.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
