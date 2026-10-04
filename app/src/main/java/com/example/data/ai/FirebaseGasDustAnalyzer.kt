package com.example.data.ai

import android.content.Context
import com.example.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ai.ai
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GasDustAnalysis(
    val summary: String,
    val gasLevelStatus: String,
    val dustLevelStatus: String,
    val overallVerdict: String,
    val recommendation: String,
    val isAiGenerated: Boolean = true
)

class FirebaseGasDustAnalyzer {

    private fun ensureFirebaseInitialized(context: Context): Boolean {
        return try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                val apiKey = try {
                    BuildConfig.GEMINI_API_KEY.ifBlank { "placeholder-key" }
                } catch (_: Exception) {
                    "placeholder-key"
                }
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:74463786758:android:aespairquality")
                    .setProjectId("smart-air-quality-monitor")
                    .setApiKey(apiKey)
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun analyze(
        context: Context,
        gasValue: Int,
        dustDensity: Double
    ): GasDustAnalysis = withContext(Dispatchers.IO) {
        val initialized = ensureFirebaseInitialized(context)

        if (initialized) {
            try {
                val generativeModel = Firebase.ai.generativeModel(modelName = "gemini-2.5-flash")
                val prompt = """
                    You are an environmental air quality specialist. Analyze the following real-time sensor measurements from an IoT monitoring station:
                    - MQ135 Gas Sensor Reading: $gasValue ADC units (detects harmful gases including CO2, smoke, ammonia, benzene, NOx)
                    - GP2Y1010 Optical Dust Sensor: ${"%.1f".format(dustDensity)} µg/m³ (fine particulate matter PM2.5 / PM10)

                    Provide a concise summary analysis structured as follows:
                    1. Summary: A 2-sentence overview of the gas and dust condition.
                    2. Gas Status: Low / Normal / Elevated / Severe.
                    3. Dust Status: Clean / Moderate / High / Hazardous.
                    4. Verdict: One strong takeaway verdict.
                    5. Recommendation: 1 practical actionable step for people in the vicinity.

                    Keep it concise, scientific yet easy to understand for the public.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text

                if (!responseText.isNullOrBlank()) {
                    return@withContext parseResponse(responseText, gasValue, dustDensity)
                }
            } catch (e: Exception) {
                // If Firebase AI service is not active in this environment or offline,
                // fall through to the robust scientific rule-based analysis
            }
        }

        // Fallback to high-accuracy offline environmental assessment
        generateOfflineAnalysis(gasValue, dustDensity)
    }

    private fun parseResponse(rawText: String, gasValue: Int, dustDensity: Double): GasDustAnalysis {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        var summary = ""
        var gasStatus = determineGasStatus(gasValue)
        var dustStatus = determineDustStatus(dustDensity)
        var verdict = "Sensor Telemetry Evaluated"
        var recommendation = "Maintain good air circulation."

        val summaryLines = mutableListOf<String>()
        for (line in lines) {
            val lower = line.lowercase()
            when {
                lower.startsWith("1.") || lower.startsWith("summary:") -> {
                    summaryLines.add(line.replace(Regex("^(1\\.|summary:?)\\s*", RegexOption.IGNORE_CASE), ""))
                }
                lower.startsWith("2.") || lower.startsWith("gas status:") || lower.startsWith("gas:") -> {
                    val extracted = line.replace(Regex("^(2\\.|gas status:?|gas:?)\\s*", RegexOption.IGNORE_CASE), "").trim()
                    if (extracted.isNotEmpty()) gasStatus = extracted
                }
                lower.startsWith("3.") || lower.startsWith("dust status:") || lower.startsWith("dust:") -> {
                    val extracted = line.replace(Regex("^(3\\.|dust status:?|dust:?)\\s*", RegexOption.IGNORE_CASE), "").trim()
                    if (extracted.isNotEmpty()) dustStatus = extracted
                }
                lower.startsWith("4.") || lower.startsWith("verdict:") -> {
                    val extracted = line.replace(Regex("^(4\\.|verdict:?)\\s*", RegexOption.IGNORE_CASE), "").trim()
                    if (extracted.isNotEmpty()) verdict = extracted
                }
                lower.startsWith("5.") || lower.startsWith("recommendation:") -> {
                    val extracted = line.replace(Regex("^(5\\.|recommendation:?)\\s*", RegexOption.IGNORE_CASE), "").trim()
                    if (extracted.isNotEmpty()) recommendation = extracted
                }
                summaryLines.isEmpty() && !lower.contains("status") && !lower.contains("verdict") -> {
                    summaryLines.add(line)
                }
            }
        }

        if (summaryLines.isNotEmpty()) {
            summary = summaryLines.take(3).joinToString(" ")
        } else {
            summary = rawText.take(250).trim()
        }

        return GasDustAnalysis(
            summary = summary,
            gasLevelStatus = gasStatus,
            dustLevelStatus = dustStatus,
            overallVerdict = verdict,
            recommendation = recommendation,
            isAiGenerated = true
        )
    }

    fun generateOfflineAnalysis(gasValue: Int, dustDensity: Double): GasDustAnalysis {
        val gasStatus = determineGasStatus(gasValue)
        val dustStatus = determineDustStatus(dustDensity)

        val verdict = when {
            gasValue > 700 || dustDensity > 75.0 -> "Poor Air Quality Alert"
            gasValue > 400 || dustDensity > 35.0 -> "Moderate Atmospheric Exposure"
            else -> "Clean & Healthy Atmosphere"
        }

        val summary = buildString {
            append("MQ135 registered $gasValue ADC units indicating $gasStatus levels of volatile gases and smoke. ")
            append("GP2Y1010 measured ${"%.1f".format(dustDensity)} µg/m³ representing $dustStatus particulate density.")
        }

        val recommendation = when {
            dustDensity > 75.0 -> "High particulate concentration detected; activate an indoor HEPA air purifier and avoid vigorous outdoor activity."
            gasValue > 600 -> "Elevated volatile chemical or smoke concentration; check local ventilation and inspect nearby combustion or gas sources."
            dustDensity > 35.0 || gasValue > 400 -> "Slight elevation in environmental particulate or organic vapor levels; sensitive individuals should take precautions."
            else -> "All gas and dust metrics are well within safe baseline standards. No respiratory protection needed."
        }

        return GasDustAnalysis(
            summary = summary,
            gasLevelStatus = gasStatus,
            dustLevelStatus = dustStatus,
            overallVerdict = verdict,
            recommendation = recommendation,
            isAiGenerated = false
        )
    }

    private fun determineGasStatus(gasValue: Int): String {
        return when {
            gasValue < 300 -> "Optimal Clean"
            gasValue < 500 -> "Normal Baseline"
            gasValue < 750 -> "Elevated Vapor"
            else -> "Hazardous Gas Concentration"
        }
    }

    private fun determineDustStatus(dustDensity: Double): String {
        return when {
            dustDensity < 25.0 -> "Clean (Low Dust)"
            dustDensity < 50.0 -> "Moderate Particulate"
            dustDensity < 100.0 -> "High Dust Density"
            else -> "Hazardous Particulate Spike"
        }
    }
}
