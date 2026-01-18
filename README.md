# EtherSense

A native Android Wi-Fi environment analyzer with "Sixth Sense" sensory feedback features. EtherSense goes beyond standard Wi-Fi analyzers by providing auditory, haptic, and visual feedback to help you intuitively understand your wireless environment.

## Features

### Core Analysis
- **Signal Quality Assessment**: Real-time RSSI monitoring with quality percentage calculation
- **Interference Detection**: Analyzes co-channel and adjacent channel interference from nearby access points
- **Throughput Estimation**: Calculates real-world throughput using: `LinkSpeed x Quality x (1 - Interference) x 0.65`
- **Channel Analysis**: Identifies channel congestion and recommends optimal channels

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
│   ├── model/          # Data classes (WifiNetwork, SignalMetrics)
│   ├── source/         # WifiScannerDataSource (BroadcastReceiver)
│   └── repository/     # WifiRepository
├── domain/
│   ├── analyzer/       # WifiAnalyzerEngine, Calculators
│   └── usecase/        # ScanWifiNetworks, AnalyzeNetwork
├── presentation/
│   ├── dashboard/      # Main screen with visualizations
│   └── settings/       # Feedback toggles
├── feedback/
│   ├── audio/          # AudioFeedbackManager (ToneGenerator)
│   └── haptic/         # HapticFeedbackManager (Vibrator)
└── ui/
    ├── theme/          # Dark futuristic theme
    └── canvas/         # GlowingOrbView, WaveformVisualizer
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

## Requirements

- **Android SDK**: API 26+ (Android 8.0 Oreo)
- **Target SDK**: API 34 (Android 14)
- **Permissions**:
  - `ACCESS_FINE_LOCATION` - Required for Wi-Fi scanning
  - `ACCESS_WIFI_STATE` - Read Wi-Fi information
  - `CHANGE_WIFI_STATE` - Trigger Wi-Fi scans
  - `VIBRATE` - Haptic feedback

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

## License

This project is for educational and personal use.
