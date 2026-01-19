package com.ethersense.data.model

data class SpeedTestResult(
    val downloadSpeedMbps: Float,
    val uploadSpeedMbps: Float,
    val pingMs: Long,
    val jitterMs: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val serverLocation: String = ""
) {
    val downloadQuality: SpeedQuality
        get() = when {
            downloadSpeedMbps >= 100 -> SpeedQuality.EXCELLENT
            downloadSpeedMbps >= 50 -> SpeedQuality.GOOD
            downloadSpeedMbps >= 25 -> SpeedQuality.FAIR
            downloadSpeedMbps >= 10 -> SpeedQuality.POOR
            else -> SpeedQuality.VERY_POOR
        }

    val uploadQuality: SpeedQuality
        get() = when {
            uploadSpeedMbps >= 50 -> SpeedQuality.EXCELLENT
            uploadSpeedMbps >= 20 -> SpeedQuality.GOOD
            uploadSpeedMbps >= 10 -> SpeedQuality.FAIR
            uploadSpeedMbps >= 5 -> SpeedQuality.POOR
            else -> SpeedQuality.VERY_POOR
        }

    val pingQuality: SpeedQuality
        get() = when {
            pingMs <= 20 -> SpeedQuality.EXCELLENT
            pingMs <= 50 -> SpeedQuality.GOOD
            pingMs <= 100 -> SpeedQuality.FAIR
            pingMs <= 200 -> SpeedQuality.POOR
            else -> SpeedQuality.VERY_POOR
        }

    val streamingCapability: String
        get() = when {
            downloadSpeedMbps >= 25 -> "4K"
            downloadSpeedMbps >= 10 -> "HD 1080p"
            downloadSpeedMbps >= 5 -> "HD 720p"
            downloadSpeedMbps >= 3 -> "SD"
            else -> "低画質のみ"
        }

    val gamingCapability: String
        get() = when {
            pingMs <= 30 && jitterMs <= 10 -> "最適"
            pingMs <= 60 && jitterMs <= 20 -> "良好"
            pingMs <= 100 && jitterMs <= 30 -> "可能"
            else -> "不向き"
        }

    val videoCallCapability: String
        get() = when {
            downloadSpeedMbps >= 10 && uploadSpeedMbps >= 5 && pingMs <= 100 -> "快適"
            downloadSpeedMbps >= 5 && uploadSpeedMbps >= 2 && pingMs <= 150 -> "良好"
            downloadSpeedMbps >= 2 && uploadSpeedMbps >= 1 -> "可能"
            else -> "困難"
        }
}

enum class SpeedQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    VERY_POOR
}

data class SpeedTestProgress(
    val phase: SpeedTestPhase,
    val progress: Float = 0f,
    val currentSpeedMbps: Float = 0f
)

enum class SpeedTestPhase {
    IDLE,
    CONNECTING,
    PING,
    DOWNLOAD,
    UPLOAD,
    COMPLETED,
    ERROR
}
