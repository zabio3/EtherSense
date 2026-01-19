package com.ethersense.domain.model

import com.ethersense.data.model.WifiNetwork

/**
 * Distance estimation result using ITU-R P.1238 model
 */
data class DistanceEstimate(
    val estimatedMeters: Float,
    val minMeters: Float,
    val maxMeters: Float,
    val confidence: Float,
    val model: String = "ITU-R P.1238"
)

/**
 * Throughput prediction based on Shannon capacity and MCS tables
 */
data class ThroughputPrediction(
    val shannonCapacity: Float,
    val mcsBasedThroughput: Float,
    val estimatedReal: Float,
    val mcsIndex: Int,
    val modulation: String,
    val confidence: Float
)

/**
 * Link margin analysis result
 */
data class LinkMarginAnalysis(
    val marginDb: Float,
    val stability: StabilityLevel,
    val fadeMarginDb: Float,
    val headroom: String,
    val headroomJa: String,
    val recommendation: String?,
    val recommendationJa: String?
)

enum class StabilityLevel(
    val minMarginDb: Float,
    val displayName: String,
    val displayNameJa: String
) {
    EXCELLENT(20f, "Very Stable", "非常に安定"),
    GOOD(12f, "Stable", "安定"),
    MARGINAL(6f, "Marginal", "ギリギリ"),
    UNSTABLE(0f, "Unstable", "不安定")
}

/**
 * Signal trend analysis
 */
data class SignalTrend(
    val direction: TrendDirection,
    val rateOfChange: Float,
    val movingAverage: Float,
    val variance: Float
)

enum class TrendDirection(val displayName: String, val displayNameJa: String) {
    IMPROVING("Improving", "改善中"),
    STABLE("Stable", "安定"),
    DEGRADING("Degrading", "悪化中")
}

/**
 * Future quality prediction
 */
data class QualityPrediction(
    val predictedRssi: Int,
    val predictedQuality: Float,
    val confidence: Float,
    val warning: String?,
    val warningJa: String?
)

/**
 * Overall diagnostic level
 */
enum class DiagnosticLevel(val displayName: String, val displayNameJa: String) {
    EXCELLENT("Excellent", "優秀"),
    GOOD("Good", "良好"),
    FAIR("Fair", "普通"),
    POOR("Poor", "要改善"),
    CRITICAL("Critical", "重大な問題")
}

/**
 * Comprehensive network diagnostics combining all analysis results
 */
data class NetworkDiagnostics(
    val timestamp: Long,
    val network: WifiNetwork,
    val distanceEstimate: DistanceEstimate,
    val throughputPrediction: ThroughputPrediction,
    val linkMarginAnalysis: LinkMarginAnalysis,
    val signalTrend: SignalTrend,
    val qualityPrediction: QualityPrediction,
    val overallScore: Float,
    val overallLevel: DiagnosticLevel,
    val recommendations: List<String>,
    val recommendationsJa: List<String>
) {
    val overallPercentage: Int
        get() = (overallScore * 100).toInt()
}
