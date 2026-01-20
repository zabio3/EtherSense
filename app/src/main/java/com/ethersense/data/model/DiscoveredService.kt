package com.ethersense.data.model

data class DiscoveredService(
    val name: String,
    val type: String,
    val host: String?,
    val port: Int?,
    val txtRecords: Map<String, String> = emptyMap(),
    val protocol: ServiceProtocol
)

enum class ServiceProtocol {
    MDNS,
    SSDP
}

data class NetworkInfo(
    val localIpAddress: String?,
    val subnetMask: String?,
    val gateway: String?,
    val dnsServers: List<String>,
    val externalIpAddress: String?,
    val ssid: String?,
    val bssid: String?,
    val linkSpeed: Int?,
    val frequency: Int?,
    val rssi: Int?,
    val networkType: String?,
    val isWifiConnected: Boolean,
    val isCellularConnected: Boolean
)
