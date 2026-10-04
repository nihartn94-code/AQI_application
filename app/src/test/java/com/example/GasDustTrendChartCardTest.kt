package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.model.AirQualityData
import com.example.ui.components.GasDustTrendChartCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GasDustTrendChartCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun trendCard_rendersEmptyStateAndHandlesSeedClick() {
        var seedClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                GasDustTrendChartCard(
                    history = emptyList(),
                    isDarkMode = false,
                    onSeedSampleData = { seedClicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("gas_dust_trend_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Historical Trend").assertIsDisplayed()
        composeTestRule.onNodeWithText("No Database Trend Data Yet").assertIsDisplayed()

        composeTestRule.onNodeWithTag("seed_sample_points_button").performClick()
        assertTrue(seedClicked)
    }

    @Test
    fun trendCard_rendersWithDataAndTogglesEngine() {
        val now = System.currentTimeMillis()
        val mockData = listOf(
            AirQualityData(gas = 210, dust = 18.5, timestamp = now - 60000),
            AirQualityData(gas = 320, dust = 35.0, timestamp = now - 30000),
            AirQualityData(gas = 280, dust = 28.2, timestamp = now)
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                GasDustTrendChartCard(
                    history = mockData,
                    isDarkMode = true,
                    onSeedSampleData = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("gas_dust_trend_card").assertIsDisplayed()
        composeTestRule.onNodeWithText("Historical Trend").assertIsDisplayed()
        composeTestRule.onNodeWithText("3 in DB").assertIsDisplayed()

        // Verify Series toggles exist
        composeTestRule.onNodeWithTag("toggle_gas_series").assertIsDisplayed()
        composeTestRule.onNodeWithTag("toggle_dust_series").assertIsDisplayed()

        // Switch to Native M3 engine
        composeTestRule.onNodeWithTag("engine_native_button").assertIsDisplayed().performClick()

        // Switch back to Recharts engine
        composeTestRule.onNodeWithTag("engine_recharts_button").assertIsDisplayed().performClick()
    }
}
