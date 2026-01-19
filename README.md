# EtherSense

A native Android Wi-Fi environment analyzer with "Sixth Sense" sensory feedback features and scientifically-grounded network diagnostics. EtherSense goes beyond standard Wi-Fi analyzers by providing auditory, haptic, and visual feedback to help you intuitively understand your wireless environment.

## Features

### Core Analysis
- **Signal Quality Assessment**: Real-time RSSI monitoring with quality percentage calculation
- **Interference Detection**: Analyzes co-channel and adjacent channel interference from nearby access points
- **Throughput Estimation**: Calculates real-world throughput using: `LinkSpeed x Quality x (1 - Interference) x 0.65`
- **Channel Analysis**: Identifies channel congestion and recommends optimal channels
- **Speed Test**: Measure actual download/upload speeds and latency

### Scientific Network Diagnostics (NEW)
Evidence-based network analysis using established wireless communication models:

#### Distance Estimation (ITU-R P.1238)
Estimates the distance to your router using the ITU indoor propagation model:
```
d = 10^((PathLoss - 20×log₁₀(f) + 28 - Pf(n)) / N)
```
- Supports multiple environment types (Residential, Office, Commercial, Open Space)
- Shows confidence intervals and error margins

#### Throughput Prediction (Shannon-Hartley + MCS)
Predicts network throughput using Shannon capacity theorem and IEEE 802.11 MCS tables:
```
C = B × log₂(1 + SNR)
```
- Shannon theoretical capacity
- MCS-based practical throughput (802.11ax/ac/n tables)
- Real-world estimation with protocol overhead

#### Link Margin Analysis
Evaluates connection stability using link budget calculations:
```
LinkMargin = RSSI - ReceiverSensitivity
```
- Stability levels: Excellent (>20dB), Good (>12dB), Marginal (>6dB), Unstable
- Fade margin assessment for connection reliability

#### Signal Trend Prediction
Analyzes RSSI history to predict future signal quality:
- Moving average and variance analysis
- Linear regression for trend direction
- 5-second quality prediction with confidence

### "Sixth Sense" Features

#### 1. Auditory Feedback (Geiger-Style)
The app produces audio tones that vary in pitch and frequency based on signal strength:
- **Strong Signal (-30 dBm)**: High pitch (880 Hz), fast repetition
- **Weak Signal (-90 dBm)**: Low pitch (220 Hz), slow repetition

The audio feedback helps you locate optimal signal positions without constantly looking at the screen - like a Geiger counter for Wi-Fi.

```
Frequency = 220 + ((RSSI + 90) / 60) × 660 Hz
```

#### 2. Haptic Feedback (Interference Alert)
Your device vibrates when channel interference is detected:
- **Severe (>70%)**: Triple pulse pattern with maximum intensity
- **High (>50%)**: Double pulse pattern
- **Moderate (>30%)**: Single click
- **Low (<30%)**: No vibration

#### 3. Visual Aesthetics (Glowing Orb)
A custom Canvas-drawn visualization displays signal strength as a pulsating, glowing orb:
- **Color Interpolation**: Red (weak) → Yellow (fair) → Green (excellent)
- **Pulse Speed**: Faster pulse for stronger signals
- **Glow Effect**: Multiple translucent layers create a neon glow effect
- **Waveform Display**: Real-time RSSI history as an animated line graph

## Architecture

EtherSense follows Clean Architecture principles with MVVM:

```
app/
├── data/
│   ├── model/          # Data classes (WifiNetwork, SignalMetrics, SpeedTestResult)
│   ├── source/         # WifiScannerDataSource (BroadcastReceiver)
│   └── repository/     # WifiRepository, SpeedTestRepository, SettingsRepository
├── domain/
│   ├── analyzer/       # Scientific analyzers
│   │   ├── WifiAnalyzerEngine
│   │   ├── SignalQualityCalculator
│   │   ├── ChannelAnalyzer
│   │   ├── ThroughputEstimator
│   │   ├── DistanceEstimator         # ITU-R P.1238 model
│   │   ├── ScientificThroughputPredictor  # Shannon + MCS
│   │   ├── LinkMarginAnalyzer        # Link budget analysis
│   │   ├── SignalPredictor           # Trend analysis
│   │   └── NetworkDiagnosticsAnalyzer  # Orchestrator
│   ├── model/          # NetworkDiagnostics, DistanceEstimate, etc.
│   └── usecase/        # ScanWifiNetworks, AnalyzeNetwork, RunSpeedTest
├── presentation/
│   ├── dashboard/      # Main screen with visualizations
│   ├── speedtest/      # Speed test screen
│   ├── diagnostics/    # Scientific diagnostics screen
│   └── settings/       # Feedback toggles
├── feedback/
│   └── FeedbackOrchestrator  # Audio & Haptic feedback
└── ui/
    ├── theme/          # Dark futuristic theme
    ├── components/     # Reusable UI components
    └── canvas/         # GlowingOrbView, WaveformVisualizer, RadarSweepView
```

## Technical Details

### Signal Quality Calculation
```kotlin
Quality = when (rssi) {
    >= -30 -> 1.0 (100%)
    >= -50 -> 0.9 (90%)
    >= -60 -> 0.75 (75%)
    >= -70 -> 0.5 (50%)
    >= -80 -> 0.25 (25%)
    >= -90 -> 0.0 (0%)
}
```

### Interference Score
Calculated using co-channel and adjacent channel overlap:
- **2.4 GHz**: Channels 1, 6, 11 don't overlap; others have partial overlap
- **5 GHz / 6 GHz**: Channels generally don't overlap

The interference score is normalized using a sigmoid function to produce a 0-1 range.

### Throughput Estimation
```kotlin
EstimatedThroughput = TheoreticalLinkSpeed × SignalQuality × (1 - InterferenceScore) × 0.65
```
The 0.65 factor accounts for protocol overhead (~35%).

### Scientific Diagnostics Models

#### ITU-R P.1238 Indoor Propagation Model
Used for distance estimation from RSSI:
```
PathLoss = 20×log₁₀(f) + N×log₁₀(d) + Pf(n) - 28

Where:
- f: Frequency (MHz)
- d: Distance (m)
- N: Path loss exponent (Residential=2.8, Office=3.0, Commercial=2.2)
- Pf(n): Floor penetration loss factor
```

#### Shannon-Hartley Theorem
Theoretical channel capacity:
```
C = B × log₂(1 + SNR)

Where:
- C: Channel capacity (bps)
- B: Bandwidth (Hz)
- SNR: Signal-to-noise ratio (linear)
```

#### MCS (Modulation and Coding Scheme) Tables
802.11ax MCS requirements (1 spatial stream, 20 MHz):

| MCS | Modulation | Min SNR | Data Rate |
|-----|------------|---------|-----------|
| 0 | BPSK 1/2 | 5 dB | 8.6 Mbps |
| 5 | 64-QAM 2/3 | 20 dB | 68.8 Mbps |
| 9 | 256-QAM 5/6 | 32 dB | 114.7 Mbps |
| 11 | 1024-QAM 5/6 | 38 dB | 143.4 Mbps |

#### Link Margin Stability Thresholds
| Margin | Stability Level |
|--------|-----------------|
| ≥ 20 dB | Excellent (very stable) |
| ≥ 12 dB | Good (stable) |
| ≥ 6 dB | Marginal (may experience drops) |
| < 6 dB | Unstable (frequent disconnections) |

## App Navigation

The app consists of four main screens accessible via bottom navigation:

| Tab | Screen | Description |
|-----|--------|-------------|
| Wi-Fi | Dashboard | Real-time signal monitoring, nearby networks, visual feedback |
| Speed | Speed Test | Download/upload speed measurement, latency testing |
| Diagnose | Diagnostics | Scientific network analysis with detailed/simple modes |
| Settings | Settings | Language, haptic feedback, and other preferences |

The Diagnostics screen offers two display modes:
- **Detailed Mode**: Shows all scientific data (formulas, MCS index, confidence levels)
- **Simple Mode**: Quick overview with key metrics only

## Requirements

- **Android SDK**: API 26+ (Android 8.0 Oreo)
- **Target SDK**: API 35 (Android 15)
- **Permissions**:
  - `ACCESS_FINE_LOCATION` - Required for Wi-Fi scanning
  - `ACCESS_WIFI_STATE` - Read Wi-Fi information
  - `CHANGE_WIFI_STATE` - Trigger Wi-Fi scans
  - `VIBRATE` - Haptic feedback
  - `INTERNET` - Speed test functionality

## Building

1. Clone the repository
2. Open in Android Studio (Hedgehog or newer recommended)
3. Sync Gradle files
4. Build and run on a device (emulator won't have Wi-Fi scanning)

```bash
./gradlew assembleDebug
```

## Scan Throttling

Android 9+ limits Wi-Fi scanning to 4 scans per 2-minute period. EtherSense:
- Uses a 30-second minimum interval between scans
- Displays cached results when throttled
- Shows a notification when scan requests are throttled

## Tech Stack

- **Language**: Kotlin 2.0
- **UI**: Jetpack Compose with Material 3
- **DI**: Hilt
- **Async**: Coroutines + Flow
- **Permissions**: Accompanist Permissions
- **Architecture**: MVVM + Clean Architecture

## Privacy

EtherSense does not:
- Store your location data
- Upload any data to servers
- Track usage analytics

All processing happens locally on your device.

## Scientific References

The network diagnostics features are based on established wireless communication research:

- **ITU-R P.1238**: [Propagation data and prediction methods for the planning of indoor radiocommunication systems](https://www.itu.int/rec/R-REC-P.1238)
- **Shannon-Hartley Theorem**: [Channel Capacity (Wikipedia)](https://en.wikipedia.org/wiki/Shannon–Hartley_theorem)
- **MCS Index Tables**: [MCS Index (mcsindex.com)](https://mcsindex.com/)
- **Link Budget Calculation**: [RF Link Budget Guide (Cadence)](https://resources.system-analysis.cadence.com/blog/rf-link-budget-calculation-guide)

## License

This project is for educational and personal use.
