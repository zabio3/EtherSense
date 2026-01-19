# EtherSense

Wi-Fi Network Diagnostics App

<p align="center">
  <img src="docs/images/wifi.png" width="280" alt="EtherSense Wi-Fi Dashboard">
</p>

## Features

### Real-time Signal Monitoring
Displays signal strength (RSSI), link speed, estimated throughput, and channel interference for your connected Wi-Fi in real-time.

### Speed Test
Measures download/upload speeds and latency. Also evaluates suitability for video streaming, online gaming, and video calls.

### Network Diagnostics

#### Distance Estimation (ITU-R P.1238)
Estimates the distance to your router from RSSI using the ITU indoor radio propagation model. Applies path loss coefficients based on environment type (residential, office, commercial).

#### Throughput Prediction (Shannon-Hartley)
Predicts theoretical maximum channel capacity using the Shannon-Hartley theorem and real-world throughput based on IEEE 802.11 MCS tables.

#### Link Margin Analysis
Calculates link margin from the difference between receiver sensitivity and RSSI to evaluate connection stability (Excellent >20dB / Good >12dB / Marginal >6dB / Unstable).

#### Signal Trend Prediction
Uses moving averages and linear regression on historical RSSI data to predict near-future signal quality.

## Download

Download the APK from [GitHub Releases](https://github.com/zabio3/EtherSense/releases).

## Screenshots

| Speed Test | Diagnostics | Settings |
|:---:|:---:|:---:|
| ![Speed Test](docs/images/speedtest.png) | ![Diagnostics](docs/images/diagnostics.png) | ![Settings](docs/images/settings.png) |

## Privacy

- All processing runs locally on the device
- No data is sent to servers

## Build

```bash
./gradlew assembleDebug
```

Requires Android 8.0 (API 26) or higher.
