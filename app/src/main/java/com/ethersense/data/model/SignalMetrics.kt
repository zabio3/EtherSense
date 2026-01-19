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

    /**
     * Overall connection score (0-100) based on multiple factors
     */
    val connectionScore: Int
        get() {
            val qualityWeight = 0.4f
            val interferenceWeight = 0.3f
            val throughputWeight = 0.3f

            val normalizedThroughput = (estimatedThroughput / 1000f).coerceIn(0f, 1f)
            val interferenceFactor = 1f - interferenceScore

            val score = (signalQuality * qualityWeight +
                    interferenceFactor * interferenceWeight +
                    normalizedThroughput * throughputWeight) * 100

            return score.toInt().coerceIn(0, 100)
        }

    /**
     * Connection score grade (A-F)
     */
    val connectionGrade: String
        get() = when {
            connectionScore >= 90 -> "A+"
            connectionScore >= 80 -> "A"
            connectionScore >= 70 -> "B"
            connectionScore >= 60 -> "C"
            connectionScore >= 50 -> "D"
            else -> "F"
        }

    /**
     * Streaming capability based on estimated throughput
     */
    val streamingCapability: String
        get() = when {
            estimatedThroughput >= 25 -> "4K Ultra HD"
            estimatedThroughput >= 10 -> "Full HD 1080p"
            estimatedThroughput >= 5 -> "HD 720p"
            estimatedThroughput >= 3 -> "SD 480p"
            else -> "低画質のみ"
        }

    fun getStreamingCapability(isJapanese: Boolean): String = when {
        estimatedThroughput >= 25 -> "4K Ultra HD"
        estimatedThroughput >= 10 -> "Full HD 1080p"
        estimatedThroughput >= 5 -> "HD 720p"
        estimatedThroughput >= 3 -> "SD 480p"
        else -> if (isJapanese) "低画質のみ" else "Low quality only"
    }

    /**
     * Gaming suitability based on signal stability
     */
    val gamingSuitability: String
        get() = when {
            signalQuality >= 0.8f && interferenceScore <= 0.2f -> "最適"
            signalQuality >= 0.6f && interferenceScore <= 0.4f -> "良好"
            signalQuality >= 0.4f && interferenceScore <= 0.5f -> "可能"
            else -> "不向き"
        }

    fun getGamingSuitability(isJapanese: Boolean): String = when {
        signalQuality >= 0.8f && interferenceScore <= 0.2f -> if (isJapanese) "最適" else "Optimal"
        signalQuality >= 0.6f && interferenceScore <= 0.4f -> if (isJapanese) "良好" else "Good"
        signalQuality >= 0.4f && interferenceScore <= 0.5f -> if (isJapanese) "可能" else "Fair"
        else -> if (isJapanese) "不向き" else "Poor"
    }

    /**
     * Video call suitability
     */
    val videoCallSuitability: String
        get() = when {
            estimatedThroughput >= 10 && signalQuality >= 0.6f -> "快適"
            estimatedThroughput >= 5 && signalQuality >= 0.4f -> "良好"
            estimatedThroughput >= 2 -> "可能"
            else -> "困難"
        }

    fun getVideoCallSuitability(isJapanese: Boolean): String = when {
        estimatedThroughput >= 10 && signalQuality >= 0.6f -> if (isJapanese) "快適" else "Excellent"
        estimatedThroughput >= 5 && signalQuality >= 0.4f -> if (isJapanese) "良好" else "Good"
        estimatedThroughput >= 2 -> if (isJapanese) "可能" else "Fair"
        else -> if (isJapanese) "困難" else "Poor"
    }
}

enum class SignalQualityLevel(val displayName: String, val displayNameJa: String) {
    EXCELLENT("Excellent", "優秀"),
    GOOD("Good", "良好"),
    FAIR("Fair", "普通"),
    POOR("Poor", "やや悪い"),
    WEAK("Weak", "弱い")
}

enum class InterferenceLevel(val displayName: String, val displayNameJa: String) {
    NONE("None", "なし"),
    LOW("Low", "低"),
    MODERATE("Moderate", "中"),
    HIGH("High", "高"),
    SEVERE("Severe", "深刻")
}
