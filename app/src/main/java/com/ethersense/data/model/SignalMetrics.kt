package com.ethersense.data.model

data class SignalMetrics(
    val rssi: Int,
    val signalQuality: Float,
    val linkSpeed: Int,
    val interferenceScore: Float,
    val estimatedThroughput: Float,
    val snr: Float,
    val channelUtilization: Float
) {
    val qualityLevel: SignalQualityLevel
        get() = when {
            signalQuality >= 0.8f -> SignalQualityLevel.EXCELLENT
            signalQuality >= 0.6f -> SignalQualityLevel.GOOD
            signalQuality >= 0.4f -> SignalQualityLevel.FAIR
            signalQuality >= 0.2f -> SignalQualityLevel.POOR
            else -> SignalQualityLevel.WEAK
        }

    val interferenceLevel: InterferenceLevel
        get() = when {
            interferenceScore >= 0.7f -> InterferenceLevel.SEVERE
            interferenceScore >= 0.5f -> InterferenceLevel.HIGH
            interferenceScore >= 0.3f -> InterferenceLevel.MODERATE
            interferenceScore >= 0.1f -> InterferenceLevel.LOW
            else -> InterferenceLevel.NONE
        }

    val qualityPercentage: Int
        get() = (signalQuality * 100).toInt()

    val throughputDisplay: String
        get() = String.format("%.1f", estimatedThroughput)
}

enum class SignalQualityLevel(val displayName: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor"),
    WEAK("Weak")
}

enum class InterferenceLevel(val displayName: String) {
    NONE("None"),
    LOW("Low"),
    MODERATE("Moderate"),
    HIGH("High"),
    SEVERE("Severe")
}
