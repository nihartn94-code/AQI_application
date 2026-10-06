package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiAnalysisResult
import com.example.data.ai.FirebaseGasDustAnalyzer
import com.example.data.ai.GasDustAnalysis
import com.example.data.ai.GeminiAirQualityAdvisor
import com.example.data.bluetooth.BluetoothConnectionState
import com.example.data.bluetooth.BluetoothDeviceInfo
import com.example.data.bluetooth.Esp32BluetoothManager
import com.example.data.local.AirQualityDatabase
import com.example.data.local.AirQualityRepository
import com.example.data.model.AirQualityData
import com.example.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AirQualityUiState(
    val reading: AirQualityData = AirQualityData(),
    val connectionState: BluetoothConnectionState = BluetoothConnectionState.Disconnected,
    val isDemoMode: Boolean = false,
    val pairedDevices: List<BluetoothDeviceInfo> = emptyList(),
    val isAiAnalyzing: Boolean = false,
    val aiAnalysisResult: AiAnalysisResult? = null,
    val isGasDustAnalyzing: Boolean = false,
    val gasDustAnalysis: GasDustAnalysis? = null,
    val gasDustError: String? = null,
    val showDeviceDialog: Boolean = false,
    val showAiDialog: Boolean = false,
    val showHistoryDialog: Boolean = false,
    val hasStoredData: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM
)

class AirQualityViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AirQualityDatabase.getInstance(application)
    private val repository = AirQualityRepository(database.airQualityDao())
    val bluetoothManager = Esp32BluetoothManager(application, viewModelScope)
    private val aiAdvisor = GeminiAirQualityAdvisor()
    private val firebaseGasDustAnalyzer = FirebaseGasDustAnalyzer()

    private val _uiState = MutableStateFlow(AirQualityUiState())
    val uiState: StateFlow<AirQualityUiState> = _uiState.asStateFlow()

    val recentHistory: StateFlow<List<AirQualityData>> = repository.recentReadings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val historicalTrend: StateFlow<List<AirQualityData>> = repository.getHistoricalReadings(50)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // 1. Observe saved data from Room Database so offline or prior readings are immediately visible
        viewModelScope.launch {
            repository.latestReading.collect { savedReading ->
                if (savedReading != null && _uiState.value.reading.timestamp <= savedReading.timestamp) {
                    _uiState.value = _uiState.value.copy(
                        reading = savedReading,
                        hasStoredData = true
                    )
                }
            }
        }

        // 2. Observe Bluetooth telemetry stream
        viewModelScope.launch {
            bluetoothManager.sensorDataStream.collect { freshReading ->
                _uiState.value = _uiState.value.copy(
                    reading = freshReading,
                    hasStoredData = true
                )
                // Persist automatically to local Room DB
                repository.saveReading(freshReading)
            }
        }

        // 3. Observe connection state
        viewModelScope.launch {
            bluetoothManager.connectionState.collect { connState ->
                _uiState.value = _uiState.value.copy(connectionState = connState)
            }
        }

        // 4. Observe demo mode
        viewModelScope.launch {
            bluetoothManager.isDemoMode.collect { isDemo ->
                _uiState.value = _uiState.value.copy(isDemoMode = isDemo)
            }
        }

        // Refresh paired devices on start
        refreshPairedDevices()
    }

    fun refreshPairedDevices() {
        val devices = bluetoothManager.getPairedDevices()
        _uiState.value = _uiState.value.copy(pairedDevices = devices)
    }

    fun openDeviceDialog() {
        refreshPairedDevices()
        _uiState.value = _uiState.value.copy(showDeviceDialog = true)
    }

    fun closeDeviceDialog() {
        _uiState.value = _uiState.value.copy(showDeviceDialog = false)
    }

    fun connectToDevice(deviceAddress: String, deviceName: String) {
        closeDeviceDialog()
        bluetoothManager.connectToDevice(deviceAddress, deviceName)
    }

    fun autoConnectEsp32() {
        if (!bluetoothManager.autoConnectEsp32()) {
            openDeviceDialog()
        }
    }

    fun disconnect() {
        bluetoothManager.disconnect()
    }

    fun toggleDemoMode(enabled: Boolean) {
        bluetoothManager.toggleDemoMode(enabled)
    }

    fun triggerAiAnalysis() {
        val current = _uiState.value.reading
        _uiState.value = _uiState.value.copy(
            isAiAnalyzing = true,
            showAiDialog = true,
            aiAnalysisResult = null
        )

        viewModelScope.launch {
            val result = aiAdvisor.analyzeAirQuality(current)
            _uiState.value = _uiState.value.copy(
                isAiAnalyzing = false,
                aiAnalysisResult = result.getOrNull() ?: aiAdvisor.generateLocalExpertAnalysis(current)
            )
        }
    }

    fun analyzeGasAndDust() {
        val current = _uiState.value.reading
        _uiState.value = _uiState.value.copy(
            isGasDustAnalyzing = true,
            gasDustError = null
        )

        viewModelScope.launch {
            try {
                val analysis = firebaseGasDustAnalyzer.analyze(
                    context = getApplication(),
                    gasValue = current.gas,
                    dustDensity = current.dust
                )
                _uiState.value = _uiState.value.copy(
                    isGasDustAnalyzing = false,
                    gasDustAnalysis = analysis,
                    gasDustError = null
                )
            } catch (e: Exception) {
                // Safe fallback to rule-based evaluation so UI remains informative
                val fallback = firebaseGasDustAnalyzer.generateOfflineAnalysis(
                    gasValue = current.gas,
                    dustDensity = current.dust
                )
                _uiState.value = _uiState.value.copy(
                    isGasDustAnalyzing = false,
                    gasDustAnalysis = fallback,
                    gasDustError = null
                )
            }
        }
    }

    fun toggleThemeMode() {
        val nextMode = when (_uiState.value.themeMode) {
            AppThemeMode.SYSTEM -> AppThemeMode.DARK
            AppThemeMode.DARK -> AppThemeMode.LIGHT
            AppThemeMode.LIGHT -> AppThemeMode.SYSTEM
        }
        _uiState.value = _uiState.value.copy(themeMode = nextMode)
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
    }

    fun closeAiDialog() {
        _uiState.value = _uiState.value.copy(showAiDialog = false)
    }

    fun openHistoryDialog() {
        _uiState.value = _uiState.value.copy(showHistoryDialog = true)
    }

    fun closeHistoryDialog() {
        _uiState.value = _uiState.value.copy(showHistoryDialog = false)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun seedDemoHistoricalData() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val samplePoints = listOf(
                AirQualityData(temperature = 24.2, humidity = 56.0, gas = 210, dust = 18.5, aqi = 42, airQuality = "Good", latitude = 13.0330048, longitude = 77.5979889, altitude = 920.0, satellites = 9, timestamp = now - 18 * 60 * 1000),
                AirQualityData(temperature = 24.5, humidity = 55.4, gas = 235, dust = 24.0, aqi = 48, airQuality = "Good", latitude = 13.0330048, longitude = 77.5979889, altitude = 920.0, satellites = 9, timestamp = now - 15 * 60 * 1000),
                AirQualityData(temperature = 24.8, humidity = 54.8, gas = 290, dust = 39.5, aqi = 65, airQuality = "Moderate", latitude = 13.0330048, longitude = 77.5979889, altitude = 920.0, satellites = 9, timestamp = now - 12 * 60 * 1000),
                AirQualityData(temperature = 25.1, humidity = 53.9, gas = 380, dust = 62.0, aqi = 88, airQuality = "Moderate", latitude = 13.0330048, longitude = 77.5979889, altitude = 920.0, satellites = 9, timestamp = now - 9 * 60 * 1000),
                AirQualityData(temperature = 25.6, humidity = 52.0, gas = 460, dust = 94.2, aqi = 118, airQuality = "Unhealthy for Sensitive Groups", latitude = 13.0330048, longitude = 77.5979889, altitude = 920.0, satellites = 9, timestamp = now - 6 * 60 * 1000),
                AirQualityData(temperature = 25.3, humidity = 53.1, gas = 340, dust = 52.8, aqi = 78, airQuality = "Moderate", latitude = 13.0330048, longitude = 77.5979889, altitude = 920.0, satellites = 9, timestamp = now - 3 * 60 * 1000),
                AirQualityData(temperature = 25.0, humidity = 54.0, gas = 265, dust = 29.4, aqi = 54, airQuality = "Moderate", latitude = 13.0330048, longitude = 77.5979889, altitude = 920.0, satellites = 9, timestamp = now)
            )
            for (point in samplePoints) {
                repository.saveReading(point)
            }
            _uiState.value = _uiState.value.copy(
                reading = samplePoints.last(),
                hasStoredData = true
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
    }
}
