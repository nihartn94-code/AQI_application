package com.example.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import com.example.ui.theme.AppThemeMode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AiAnalysisDialog
import com.example.ui.components.AqiGaugeCard
import com.example.ui.components.ConnectionBanner
import com.example.ui.components.DeviceSelectionDialog
import com.example.ui.components.GasDustSummaryCard
import com.example.ui.components.GasDustTrendChartCard
import com.example.ui.components.GpsMapCard
import com.example.ui.components.HistoryDialog
import com.example.ui.components.SensorMetricsGrid
import com.example.ui.theme.AqiGood
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AirQualityViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val history by viewModel.recentHistory.collectAsState()
    val historicalTrend by viewModel.historicalTrend.collectAsState()
    val reading = uiState.reading

    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (uiState.themeMode) {
        AppThemeMode.SYSTEM -> isSystemDark
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val timeFormat = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Smart Air Quality",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                        Text(
                            text = "ESP32 IoT Environmental Station",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Theme Mode Toggle Button
                    IconButton(
                        onClick = { viewModel.toggleThemeMode() },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        val (icon, desc) = when (uiState.themeMode) {
                            AppThemeMode.LIGHT -> Icons.Default.LightMode to "Current: Light Mode (Tap for System)"
                            AppThemeMode.DARK -> Icons.Default.DarkMode to "Current: Dark Mode (Tap for Light)"
                            AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto to "Current: System Theme (Tap for Dark)"
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = desc,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // History Button
                    IconButton(
                        onClick = { viewModel.openHistoryDialog() },
                        modifier = Modifier.testTag("open_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "View Saved History"
                        )
                    }

                    // Connect ESP32 Button
                    IconButton(
                        onClick = { viewModel.openDeviceDialog() },
                        modifier = Modifier.testTag("open_devices_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Select Bluetooth Device"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { viewModel.triggerAiAnalysis() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                },
                text = {
                    Text(
                        text = "AI Analysis",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("ai_analysis_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Bluetooth Connection Banner
            ConnectionBanner(
                connectionState = uiState.connectionState,
                isDemoMode = uiState.isDemoMode,
                onConnectClick = { viewModel.autoConnectEsp32() },
                onDisconnectClick = { viewModel.disconnect() },
                onToggleDemoMode = { enabled -> viewModel.toggleDemoMode(enabled) }
            )

            // 2. Hero Air Quality Gauge Card
            AqiGaugeCard(
                aqi = reading.aqi,
                category = reading.aqiCategory,
                statusText = reading.airQuality
            )

            // 3. Environmental Sensor Grid (Temp, Humidity, Gas, Dust)
            SensorMetricsGrid(
                temperature = reading.temperature,
                humidity = reading.humidity,
                gasValue = reading.gas,
                dustDensity = reading.dust
            )

            // 4. Firebase Gemini Gas & Dust Density AI Summary Card
            GasDustSummaryCard(
                gasValue = reading.gas,
                dustDensity = reading.dust,
                analysis = uiState.gasDustAnalysis,
                isLoading = uiState.isGasDustAnalyzing,
                errorMessage = uiState.gasDustError,
                onAnalyzeClick = { viewModel.analyzeGasAndDust() }
            )

            // 5. Historical Trend Line Graph (Recharts / Room Database)
            GasDustTrendChartCard(
                history = historicalTrend,
                isDarkMode = isDark,
                onSeedSampleData = { viewModel.seedDemoHistoricalData() }
            )

            // 6. GPS & Map Telemetry Card
            GpsMapCard(
                latitude = reading.latitude,
                longitude = reading.longitude,
                altitude = reading.altitude,
                speed = reading.speed,
                satellites = reading.satellites
            )

            // 5. Local Storage Status Chip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (uiState.hasStoredData) {
                            "Last recorded: ${timeFormat.format(Date(reading.timestamp))}"
                        } else {
                            "Waiting for initial sensor reading..."
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Saved to Room DB",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    // Dialogs
    if (uiState.showDeviceDialog) {
        DeviceSelectionDialog(
            pairedDevices = uiState.pairedDevices,
            isDemoMode = uiState.isDemoMode,
            onDeviceSelected = { address, name ->
                viewModel.connectToDevice(address, name)
            },
            onToggleDemoMode = { enabled ->
                viewModel.toggleDemoMode(enabled)
            },
            onRefresh = { viewModel.refreshPairedDevices() },
            onDismiss = { viewModel.closeDeviceDialog() }
        )
    }

    if (uiState.showAiDialog) {
        AiAnalysisDialog(
            isLoading = uiState.isAiAnalyzing,
            result = uiState.aiAnalysisResult,
            onDismiss = { viewModel.closeAiDialog() }
        )
    }

    if (uiState.showHistoryDialog) {
        HistoryDialog(
            history = history,
            onClearHistory = { viewModel.clearHistory() },
            onDismiss = { viewModel.closeHistoryDialog() }
        )
    }
}
