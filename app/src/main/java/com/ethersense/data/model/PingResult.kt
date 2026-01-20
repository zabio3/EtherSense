package com.ethersense.data.model

data class PingResult(
    val host: String,
    val ipAddress: String,
    val rttMin: Float,
    val rttAvg: Float,
    val rttMax: Float,
    val packetLoss: Float,
    val packetsTransmitted: Int,
    val packetsReceived: Int,
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
)

data class PingProgress(
    val sequenceNumber: Int,
    val rtt: Float,
    val ttl: Int,
    val host: String
)
