package com.example.data.model

import org.json.JSONObject

/**
 * Data model matching the ESP32 JSON packet structure:
 * {
 *   "temperature": 28.50,
 *   "humidity": 65.20,
 *   "gas": 420,
 *   "dust": 34.50,
 *   "aqi": 72,
 *   "airQuality": "MODERATE",
 *   "latitude": 37.774929,
 *   "longitude": -122.419416,
 *   "altitude": 15.20,
 *   "speed": 0.00,
 *   "satellites": 7
 * }
 */
data class AirQualityData(
    val temperature: Double = 0.0,
    val humidity: Double = 0.0,
    val gas: Int = 0,
    val dust: Double = 0.0,
    val aqi: Int = 0,
    val airQuality: String = "UNKNOWN",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speed: Double = 0.0,
    val satellites: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val aqiCategory: AqiCategory
        get() = AqiCategory.fromAqi(aqi, airQuality)

    val hasGpsFix: Boolean
        get() = satellites > 0 && (latitude != 0.0 || longitude != 0.0)

    companion object {
        fun fromJson(jsonString: String): AirQualityData? {
            return try {
                val json = JSONObject(jsonString)
                val temp = json.optDouble("temperature", 0.0)
                val hum = json.optDouble("humidity", 0.0)
                val gas = json.optInt("gas", 0)
                val dust = json.optDouble("dust", 0.0)
                val aqiVal = json.optInt("aqi", 0)
                val quality = json.optString("airQuality", "")
                val lat = json.optDouble("latitude", 0.0)
                val lng = json.optDouble("longitude", 0.0)
                val alt = json.optDouble("altitude", 0.0)
                val spd = json.optDouble("speed", 0.0)
                val sats = json.optInt("satellites", 0)

                AirQualityData(
                    temperature = temp,
                    humidity = hum,
                    gas = gas,
                    dust = dust,
                    aqi = aqiVal,
                    airQuality = if (quality.isNotBlank()) quality else AqiCategory.fromAqi(aqiVal).displayName,
                    latitude = lat,
                    longitude = lng,
                    altitude = alt,
                    speed = spd,
                    satellites = sats,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

enum class AqiCategory(
    val displayName: String,
    val rangeDescription: String,
    val healthAdvice: String
) {
    GOOD(
        displayName = "Good",
        rangeDescription = "0 - 50",
        healthAdvice = "Air quality is satisfactory, and air pollution poses little or no risk."
    ),
    MODERATE(
        displayName = "Moderate",
        rangeDescription = "51 - 100",
        healthAdvice = "Air quality is acceptable. Very sensitive individuals may experience minor symptoms."
    ),
    UNHEALTHY_SENSITIVE(
        displayName = "Unhealthy for Sensitive Groups",
        rangeDescription = "101 - 150",
        healthAdvice = "Members of sensitive groups may experience health effects. General public not likely affected."
    ),
    UNHEALTHY(
        displayName = "Unhealthy",
        rangeDescription = "151 - 200",
        healthAdvice = "Everyone may begin to experience health effects; sensitive groups may feel serious effects."
    ),
    VERY_UNHEALTHY(
        displayName = "Very Unhealthy",
        rangeDescription = "201 - 300",
        healthAdvice = "Health alert: The risk of health effects is increased for everyone. Avoid prolonged outdoor exertion."
    ),
    HAZARDOUS(
        displayName = "Hazardous",
        rangeDescription = "301+",
        healthAdvice = "Health warning of emergency conditions: Everyone is more likely to be affected. Remain indoors."
    );

    companion object {
        fun fromAqi(aqi: Int, statusHint: String = ""): AqiCategory {
            val upper = statusHint.uppercase()
            return when {
                upper.contains("HAZARD") -> HAZARDOUS
                upper.contains("VERY UNHEALTHY") -> VERY_UNHEALTHY
                upper.contains("SENSITIVE") -> UNHEALTHY_SENSITIVE
                upper.contains("UNHEALTHY") -> UNHEALTHY
                upper.contains("MODERATE") -> MODERATE
                upper.contains("GOOD") -> GOOD
                aqi <= 50 -> GOOD
                aqi <= 100 -> MODERATE
                aqi <= 150 -> UNHEALTHY_SENSITIVE
                aqi <= 200 -> UNHEALTHY
                aqi <= 300 -> VERY_UNHEALTHY
                else -> HAZARDOUS
            }
        }
    }
}
