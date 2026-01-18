package com.ethersense.data.model

data class WifiNetwork(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequency: Int,
    val channel: Int,
    val channelWidth: Int,
    val capabilities: String,
    val wifiStandard: WifiStandard,
    val timestamp: Long,
    val isConnected: Boolean = false
) {
    val frequencyBand: FrequencyBand
        get() = when {
            frequency < 3000 -> FrequencyBand.BAND_2_4_GHZ
            frequency < 6000 -> FrequencyBand.BAND_5_GHZ
            else -> FrequencyBand.BAND_6_GHZ
        }

    val securityType: SecurityType
        get() = when {
            capabilities.contains("WPA3") -> SecurityType.WPA3
            capabilities.contains("WPA2") -> SecurityType.WPA2
            capabilities.contains("WPA") -> SecurityType.WPA
            capabilities.contains("WEP") -> SecurityType.WEP
            else -> SecurityType.OPEN
        }

    val displayName: String
        get() = ssid.ifEmpty { "Hidden Network" }
}

enum class FrequencyBand(val displayName: String) {
    BAND_2_4_GHZ("2.4 GHz"),
    BAND_5_GHZ("5 GHz"),
    BAND_6_GHZ("6 GHz")
}

enum class WifiStandard(val displayName: String, val maxSpeed: Int) {
    WIFI_4("Wi-Fi 4 (802.11n)", 600),
    WIFI_5("Wi-Fi 5 (802.11ac)", 3500),
    WIFI_6("Wi-Fi 6 (802.11ax)", 9600),
    WIFI_6E("Wi-Fi 6E", 9600),
    UNKNOWN("Unknown", 100)
}

enum class SecurityType(val displayName: String) {
    OPEN("Open"),
    WEP("WEP"),
    WPA("WPA"),
    WPA2("WPA2"),
    WPA3("WPA3")
}
