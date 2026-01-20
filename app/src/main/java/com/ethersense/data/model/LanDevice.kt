package com.ethersense.data.model

data class LanDevice(
    val ipAddress: String,
    val hostname: String? = null,
    val macAddress: String? = null,
    val vendor: String? = null,
    val services: List<String> = emptyList(),
    val isReachable: Boolean = true,
    val responseTime: Long? = null
)

data class LanScanProgress(
    val currentIp: String,
    val scannedCount: Int,
    val totalCount: Int,
    val foundDevices: Int
)
