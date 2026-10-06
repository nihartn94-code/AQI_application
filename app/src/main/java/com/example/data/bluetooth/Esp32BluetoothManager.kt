package com.example.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.data.model.AirQualityData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID
import kotlin.random.Random

data class BluetoothDeviceInfo(
    val name: String,
    val address: String,
    val isLikelyEsp32: Boolean = name.contains("ESP32", ignoreCase = true) || name.contains("AQI", ignoreCase = true)
)

sealed interface BluetoothConnectionState {
    data object Disconnected : BluetoothConnectionState
    data class Connecting(val deviceName: String) : BluetoothConnectionState
    data class Connected(val deviceName: String, val deviceAddress: String) : BluetoothConnectionState
    data class Error(val message: String, val isBluetoothDisabled: Boolean = false) : BluetoothConnectionState
}

class Esp32BluetoothManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        // Standard Serial Port Profile (SPP) UUID used by ESP32 BluetoothSerial
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val DEFAULT_ESP32_NAME = "ESP32_AQI"
    }

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        try {
            BluetoothAdapter.getDefaultAdapter()
        } catch (e: Exception) {
            null
        }
    }

    private val _connectionState = MutableStateFlow<BluetoothConnectionState>(BluetoothConnectionState.Disconnected)
    val connectionState: StateFlow<BluetoothConnectionState> = _connectionState.asStateFlow()

    private val _sensorDataStream = MutableSharedFlow<AirQualityData>(extraBufferCapacity = 10)
    val sensorDataStream: SharedFlow<AirQualityData> = _sensorDataStream.asSharedFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private var activeSocket: BluetoothSocket? = null
    private var connectionJob: Job? = null
    private var demoJob: Job? = null

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        val adapter = bluetoothAdapter ?: return emptyList()
        if (!adapter.isEnabled) return emptyList()

        return try {
            adapter.bondedDevices?.map { device ->
                BluetoothDeviceInfo(
                    name = device.name ?: "Unknown Device",
                    address = device.address ?: "00:00:00:00:00:00"
                )
            }?.sortedByDescending { it.isLikelyEsp32 } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Connects to the given Bluetooth device address via Bluetooth Classic RFCOMM SPP socket.
     */
    @SuppressLint("MissingPermission")
    fun connectToDevice(deviceAddress: String, deviceName: String = "ESP32_AQI") {
        stopDemoMode()
        disconnect()

        val adapter = bluetoothAdapter
        if (adapter == null) {
            _connectionState.value = BluetoothConnectionState.Error("Bluetooth is not supported on this device.")
            return
        }

        if (!adapter.isEnabled) {
            _connectionState.value = BluetoothConnectionState.Error(
                "Bluetooth is turned off. Please turn on Bluetooth in settings.",
                isBluetoothDisabled = true
            )
            return
        }

        connectionJob = scope.launch(Dispatchers.IO) {
            _connectionState.value = BluetoothConnectionState.Connecting(deviceName)

            try {
                val device: BluetoothDevice = adapter.getRemoteDevice(deviceAddress)
                // Cancel discovery before connection as it slows down connection
                try { adapter.cancelDiscovery() } catch (_: Exception) {}

                val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                activeSocket = socket
                socket.connect()

                _connectionState.value = BluetoothConnectionState.Connected(
                    deviceName = device.name ?: deviceName,
                    deviceAddress = deviceAddress
                )

                // Read incoming newline-delimited JSON stream from ESP32
                val reader = BufferedReader(InputStreamReader(socket.inputStream))
                var buffer = StringBuilder()

                while (isActive && socket.isConnected) {
                    val line = reader.readLine() ?: break
                    val trimmed = line.trim()
                    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                        val parsed = AirQualityData.fromJson(trimmed)
                        if (parsed != null) {
                            _sensorDataStream.emit(parsed)
                        }
                    }
                }

                // If loop terminated normally
                if (isActive) {
                    _connectionState.value = BluetoothConnectionState.Disconnected
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _connectionState.value = BluetoothConnectionState.Error(
                        "Connection failed: ${e.localizedMessage ?: "Could not connect to $deviceName"}"
                    )
                }
            } finally {
                cleanupSocket()
            }
        }
    }

    /**
     * Connects automatically to the first paired device named ESP32_AQI or containing ESP32.
     */
    fun autoConnectEsp32(): Boolean {
        val paired = getPairedDevices()
        val target = paired.firstOrNull { it.name.equals(DEFAULT_ESP32_NAME, ignoreCase = true) }
            ?: paired.firstOrNull { it.isLikelyEsp32 }
            ?: paired.firstOrNull()

        return if (target != null) {
            connectToDevice(target.address, target.name)
            true
        } else {
            false
        }
    }

    /**
     * Disconnects any active Bluetooth connection.
     */
    fun disconnect() {
        connectionJob?.cancel()
        connectionJob = null
        cleanupSocket()
        if (_connectionState.value !is BluetoothConnectionState.Disconnected) {
            _connectionState.value = BluetoothConnectionState.Disconnected
        }
    }

    private fun cleanupSocket() {
        try {
            activeSocket?.close()
        } catch (_: Exception) {}
        activeSocket = null
    }

    /**
     * Toggles Demo Mode: generates realistic ESP32 telemetry packets matching the exact
     * user ESP32 firmware timing and calculations. Ideal for presentations, testing without
     * hardware, and streaming emulator demonstration.
     */
    fun toggleDemoMode(enabled: Boolean) {
        if (enabled) {
            disconnect()
            _isDemoMode.value = true
            _connectionState.value = BluetoothConnectionState.Connected(
                deviceName = "ESP32_AQI (Simulation)",
                deviceAddress = "AA:BB:CC:DD:EE:FF"
            )
            startDemoDataStream()
        } else {
            stopDemoMode()
        }
    }

    private fun startDemoDataStream() {
        demoJob?.cancel()
        demoJob = scope.launch(Dispatchers.Default) {
            var step = 0
            // Base values
            var temp = 27.4
            var hum = 58.2
            var gasVal = 380
            var dust = 28.5
            val baseLat = 13.0330048
            val baseLng = 77.5979889

            while (isActive && _isDemoMode.value) {
                step++
                // Generate natural slight variations
                temp = (temp + (Random.nextDouble(-0.3, 0.4))).coerceIn(20.0, 38.0)
                hum = (hum + (Random.nextDouble(-0.6, 0.7))).coerceIn(35.0, 85.0)
                gasVal = (gasVal + Random.nextInt(-15, 20)).coerceIn(200, 800)
                dust = (dust + Random.nextDouble(-3.0, 4.0)).coerceIn(5.0, 160.0)

                // Replicate exact ESP32 AQI calculation:
                // AQI = (dustDensity * 0.6) + (gasValue * 0.2) + (temperature * 0.2)
                val calcAqi = ((dust * 0.6) + (gasVal * 0.2) + (temp * 0.2)).toInt()

                val airQualityStr = when {
                    calcAqi <= 50 -> "GOOD"
                    calcAqi <= 100 -> "MODERATE"
                    calcAqi <= 150 -> "UNHEALTHY FOR SENSITIVE GROUPS"
                    calcAqi <= 200 -> "UNHEALTHY"
                    calcAqi <= 300 -> "VERY UNHEALTHY"
                    else -> "HAZARDOUS"
                }

                // Simulate slight GPS drift around 13.0330048, 77.5979889
                val curLat = if (step == 1) baseLat else baseLat + (Random.nextDouble(-0.00002, 0.00002))
                val curLng = if (step == 1) baseLng else baseLng + (Random.nextDouble(-0.00002, 0.00002))

                val simulatedData = AirQualityData(
                    temperature = Math.round(temp * 100.0) / 100.0,
                    humidity = Math.round(hum * 100.0) / 100.0,
                    gas = gasVal,
                    dust = Math.round(dust * 100.0) / 100.0,
                    aqi = calcAqi,
                    airQuality = airQualityStr,
                    latitude = Math.round(curLat * 10000000.0) / 10000000.0,
                    longitude = Math.round(curLng * 10000000.0) / 10000000.0,
                    altitude = 920.0,
                    speed = 0.0,
                    satellites = 9,
                    timestamp = System.currentTimeMillis()
                )

                _sensorDataStream.emit(simulatedData)
                // ESP32 sends data every 5000ms
                delay(5000)
            }
        }
    }

    private fun stopDemoMode() {
        demoJob?.cancel()
        demoJob = null
        _isDemoMode.value = false
        if (_connectionState.value is BluetoothConnectionState.Connected) {
            val connected = _connectionState.value as BluetoothConnectionState.Connected
            if (connected.deviceName.contains("Simulation")) {
                _connectionState.value = BluetoothConnectionState.Disconnected
            }
        }
    }
}
