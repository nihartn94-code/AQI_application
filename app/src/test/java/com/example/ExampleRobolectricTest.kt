package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AirQualityDatabase
import com.example.data.local.AirQualityReadingEntity
import com.example.data.model.AirQualityData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: AirQualityDatabase

  @Before
  fun setup() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AirQualityDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart AQI", appName)
  }

  @Test
  fun `test room database saves and retrieves air quality reading`() = runBlocking {
    val entity = AirQualityReadingEntity(
      temperature = 26.5,
      humidity = 55.0,
      gas = 350,
      dust = 25.0,
      aqi = 65,
      airQuality = "MODERATE",
      latitude = 37.7749,
      longitude = -122.4194,
      altitude = 12.0,
      speed = 0.0,
      satellites = 8
    )

    database.airQualityDao().insertReading(entity)
    val latest = database.airQualityDao().getLatestReading().first()

    assertNotNull(latest)
    assertEquals(65, latest?.aqi)
    assertEquals("MODERATE", latest?.airQuality)
  }

  @Test
  fun `test esp32 json parser`() {
    val sampleEsp32Json = """{"temperature":28.50,"humidity":65.20,"gas":420,"dust":34.50,"aqi":72,"airQuality":"MODERATE","latitude":37.774929,"longitude":-122.419416,"altitude":15.20,"speed":0.00,"satellites":7}"""
    val parsed = AirQualityData.fromJson(sampleEsp32Json)

    assertNotNull(parsed)
    assertEquals(28.50, parsed?.temperature ?: 0.0, 0.01)
    assertEquals(420, parsed?.gas)
    assertEquals(72, parsed?.aqi)
    assertEquals(7, parsed?.satellites)
  }
}

