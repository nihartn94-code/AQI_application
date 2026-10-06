# Smart Air Quality Monitor 🌿💨

An advanced, production-grade Android application built with **Kotlin** and **Jetpack Compose** that connects to an **ESP32 IoT environmental station** over **Bluetooth Classic (SPP)**. The app monitors real-time atmospheric metrics, persists telemetry locally via **Room Database**, delivers health insights with **Firebase Gemini AI**, and visualizes trends using an interactive **Recharts** graph.

---

## 📱 Features

- **Real-Time Environmental Telemetry**:
  - **Air Quality Index (AQI)**: Hero radial gauge with official EPA color categories (Good, Moderate, Sensitive, Unhealthy, Hazardous).
  - **Atmospheric Sensors**: Temperature (°C), Relative Humidity (%), MQ-135 Gas (ADC), and Sharp GP2Y1010AU0F Dust Density (µg/m³).
  - **GPS Telemetry & Mapping**: Real-time latitude, longitude, altitude, speed, satellite count, and interactive map card.

- **ESP32 Bluetooth Classic (SPP)**:
  - Quick auto-connect to paired ESP32 devices (`ESP32_AQI`).
  - Device picker dialog with signal indicators and status banners.
  - **Demo / Simulation Mode**: One-tap simulation mode generating realistic sensor data streams for presentations and testing without physical hardware.

- **Firebase Gemini AI Integration**:
  - **Overall Health Advisor**: Context-aware AI summary detailing health recommendations and ventilation advice.
  - **Gas & Dust Summary Card**: Targeted analysis of MQ-135 gas and optical dust density with EPA baseline comparisons.
  - **Offline Fallback Engine**: Rule-based environmental algorithm that runs even without an internet connection or API key.

- **Data Visualization & Recharts**:
  - **Dual-Series Trend Graph**: Plots MQ-135 Gas (Cyan `#00B4D8`) and GP2Y1010 Dust (Amber `#FF9100`) historical readings.
  - **Recharts Engine**: Embedded React 18 & Recharts 2.x responsive line chart with interactive hover/touch tooltips, Cartesian grid, and dual Y-axes.
  - **Native Compose Canvas Mode**: Instant hardware-accelerated 120fps bezier spline chart fallback.
  - **Customizable Range & Filters**: Toggle individual series and select 15, 30, or 50 data points.

- **Room Database Local Persistence**:
  - Offline-first architecture: Automatically stores all sensor readings in a local SQLite database.
  - History viewer dialog with timestamps and clear database functionality.

- **Material 3 Design & Dark / Light Modes**:
  - Full support for **Dark Mode**, **Light Mode**, and **System Default**.
  - Accessible touch targets (≥ 48dp), high-contrast palettes, and clean M3 cards.

---

## 🛠️ Architecture & Tech Stack

| Layer | Technology |
|---|---|
| **UI Framework** | Jetpack Compose, Material 3, Material Icons Extended |
| **Language** | Kotlin 2.0+ (Coroutines & StateFlow) |
| **Architecture** | MVVM (Model-View-ViewModel) + Repository Pattern |
| **Local Database** | Room 2.6 (SQLite, KSP, Flow) |
| **AI Integration** | Firebase Vertex AI SDK (`firebase-ai`) + Gemini 2.5 Flash |
| **Charts** | Recharts (React 18 in WebView) + Jetpack Compose Native Canvas |
| **Connectivity** | Android Bluetooth Classic (`BluetoothSocket` / RFCOMM SPP) |
| **Secrets Management**| Secrets Gradle Plugin (`.env`) |
| **Testing** | JUnit 4, Robolectric, Compose UI Test Rule |

---

## 🔌 Hardware Setup (ESP32 Station)

### Suggested Pin Connections

| Sensor | ESP32 GPIO | Description |
|---|---|---|
| **MQ-135 Gas Sensor** | `GPIO 34` (ADC1_CH6) | Analog output for gas concentration |
| **GP2Y1010AU0F Dust** | `GPIO 32` (ADC1_CH4) | Analog output (amplified/filtered) |
| **Dust Sensor IRED LED** | `GPIO 25` | Pulse control (0.32ms pulse) |
| **DHT22 / DHT11** | `GPIO 4` | Digital temperature & humidity |
| **NEO-6M GPS Module** | `GPIO 16 (RX2)`, `GPIO 17 (TX2)` | UART Serial GPS stream (NMEA) |

### ESP32 Telemetry Format

The ESP32 transmits standard comma-delimited CSV packets over Bluetooth Serial (`BluetoothSerial.h`):

```text
temp,hum,gas,dust,aqi,lat,lon,alt,speed,sat
25.4,52.1,310,42.5,75,13.0330048,77.5979889,920.0,0.0,9
```

---

## 🚀 Getting Started

### Prerequisites

- Android Studio Meerkat or newer
- Android SDK 35 (Android 15) or 36
- Java 17 or Java 21 JDK

### Configuration

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-username>/<repo-name>.git
   cd <repo-name>
   ```

2. Configure environment variables:
   Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
   Add your Gemini API Key if using cloud analysis:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

3. Build the project:
   ```bash
   gradle assembleDebug
   ```
   The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

4. Run unit and Robolectric UI tests:
   ```bash
   gradle testDebugUnitTest
   ```

---

## 🧪 Testing

The project includes local JVM tests using **Robolectric** for instant CUJ validation without requiring physical devices:
- `GasDustSummaryCardTest.kt`: Validates gas and dust telemetry chip presentation, click triggers, and AI evaluation states.
- `GasDustTrendChartCardTest.kt`: Tests empty state rendering, demo seed triggers, series toggles, and Recharts/Native engine switching.
- `ExampleRobolectricTest.kt`: Tests core application view models and navigation.

---

## 📄 License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
