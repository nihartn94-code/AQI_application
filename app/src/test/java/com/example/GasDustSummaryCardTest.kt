package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.ai.GasDustAnalysis
import com.example.ui.components.GasDustSummaryCard
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
class GasDustSummaryCardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gasDustSummaryCard_rendersInitialStateAndHandlesClick() {
        var clicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                GasDustSummaryCard(
                    gasValue = 420,
                    dustDensity = 34.5,
                    analysis = null,
                    isLoading = false,
                    errorMessage = null,
                    onAnalyzeClick = { clicked = true }
                )
            }
        }

        composeTestRule.onNodeWithTag("gas_dust_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gas_chip").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dust_chip").assertIsDisplayed()
        composeTestRule.onNodeWithText("420 ADC").assertIsDisplayed()
        composeTestRule.onNodeWithText("34.5 µg/m³").assertIsDisplayed()

        composeTestRule.onNodeWithTag("trigger_gas_dust_analysis_button").performClick()
        assertTrue(clicked)
    }

    @Test
    fun gasDustSummaryCard_rendersAnalysisState() {
        val analysis = GasDustAnalysis(
            summary = "Atmospheric gas and dust levels are well within safe baseline standards.",
            gasLevelStatus = "Normal Baseline",
            dustLevelStatus = "Clean (Low Dust)",
            overallVerdict = "Clean & Healthy Atmosphere",
            recommendation = "No respiratory protection required.",
            isAiGenerated = true
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                GasDustSummaryCard(
                    gasValue = 280,
                    dustDensity = 18.2,
                    analysis = analysis,
                    isLoading = false,
                    errorMessage = null,
                    onAnalyzeClick = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("gas_dust_summary_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gas_dust_analysis_content").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clean & Healthy Atmosphere").assertIsDisplayed()
        composeTestRule.onNodeWithText("Normal Baseline").assertIsDisplayed()
        composeTestRule.onNodeWithText("Clean (Low Dust)").assertIsDisplayed()
        composeTestRule.onNodeWithText("No respiratory protection required.").assertIsDisplayed()
    }
}
