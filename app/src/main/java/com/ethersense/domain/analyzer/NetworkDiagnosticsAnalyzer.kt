package com.ethersense.domain.analyzer

import com.ethersense.data.model.WifiNetwork
import com.ethersense.domain.model.DiagnosticLevel
import com.ethersense.domain.model.NetworkDiagnostics
import com.ethersense.domain.model.StabilityLevel
import com.ethersense.domain.model.TrendDirection
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive network diagnostics analyzer that orchestrates all diagnostic components.
 *
 * Combines:
 * - Distance estimation (ITU-R P.1238)
 * - Throughput prediction (Shannon + MCS)
 * - Link margin analysis
 * - Signal trend prediction
 *
 * To provide a complete network health assessment.
 */
@Singleton
class NetworkDiagnosticsAnalyzer @Inject constructor(
    private val distanceEstimator: DistanceEstimator,
    private val throughputPredictor: ScientificThroughputPredictor,
    private val linkMarginAnalyzer: LinkMarginAnalyzer,
    private val signalPredictor: SignalPredictor,
    private val signalQualityCalculator: SignalQualityCalculator
) {

    companion object {
        private const val DEFAULT_NOISE_FLOOR = -95
    }

    /**
     * Perform comprehensive network diagnostics.
     *
     * @param network The Wi-Fi network to analyze
     * @param rssiHistory Historical RSSI values for trend analysis
     * @param environmentType Environment type for distance estimation
     * @return Complete NetworkDiagnostics result
     */
    fun analyze(
        network: WifiNetwork,
        rssiHistory: List<Int> = emptyList(),
        environmentType: DistanceEstimator.EnvironmentType = DistanceEstimator.EnvironmentType.RESIDENTIAL
    ): NetworkDiagnostics {
        // 1. Distance Estimation
        val distanceEstimate = distanceEstimator.estimateDistance(
            rssi = network.rssi,
            frequencyMhz = network.frequency,
            environmentType = environmentType
        )

        // 2. Calculate SNR
        val snrDb = (network.rssi - DEFAULT_NOISE_FLOOR).toFloat()

        // 3. Throughput Prediction
        val throughputPrediction = throughputPredictor.predictThroughput(
            snrDb = snrDb,
            wifiStandard = network.wifiStandard,
            channelWidth = network.channelWidth,
            spatialStreams = 1 // Assume single stream for safety
        )

        // 4. Link Margin Analysis
        val linkMarginAnalysis = linkMarginAnalyzer.analyzeLinkMargin(
            rssi = network.rssi,
            wifiStandard = network.wifiStandard,
            mcsIndex = throughputPrediction.mcsIndex
        )

        // 5. Signal Trend Analysis
        val effectiveHistory = if (rssiHistory.isEmpty()) listOf(network.rssi) else rssiHistory
        val signalTrend = signalPredictor.predictSignalTrend(effectiveHistory)

        // 6. Quality Prediction
        val qualityPrediction = signalPredictor.predictFutureQuality(effectiveHistory)

        // 7. Calculate Overall Score
        val overallScore = calculateOverallScore(
            signalQuality = signalQualityCalculator.calculate(network.rssi),
            throughputConfidence = throughputPrediction.confidence,
            linkMarginStability = linkMarginAnalysis.stability,
            signalTrendDirection = signalTrend.direction,
            signalVariance = signalTrend.variance
        )

        // 8. Determine Overall Level
        val overallLevel = determineOverallLevel(overallScore)

        // 9. Generate Recommendations
        val (recommendations, recommendationsJa) = generateRecommendations(
            network = network,
            distanceEstimate = distanceEstimate,
            linkMarginAnalysis = linkMarginAnalysis,
            signalTrend = signalTrend,
            throughputPrediction = throughputPrediction
        )

        return NetworkDiagnostics(
            timestamp = System.currentTimeMillis(),
            network = network,
            distanceEstimate = distanceEstimate,
            throughputPrediction = throughputPrediction,
            linkMarginAnalysis = linkMarginAnalysis,
            signalTrend = signalTrend,
            qualityPrediction = qualityPrediction,
            overallScore = overallScore,
            overallLevel = overallLevel,
            recommendations = recommendations,
            recommendationsJa = recommendationsJa
        )
    }

    /**
     * Calculate overall diagnostic score (0-1).
     */
    private fun calculateOverallScore(
        signalQuality: Float,
        throughputConfidence: Float,
        linkMarginStability: StabilityLevel,
        signalTrendDirection: TrendDirection,
        signalVariance: Float
    ): Float {
        // Weight factors
        val signalWeight = 0.35f
        val throughputWeight = 0.25f
        val stabilityWeight = 0.25f
        val trendWeight = 0.15f

        // Signal quality component (0-1)
        val signalScore = signalQuality

        // Throughput confidence component (0-1)
        val throughputScore = throughputConfidence

        // Stability component (0-1)
        val stabilityScore = when (linkMarginStability) {
            StabilityLevel.EXCELLENT -> 1.0f
            StabilityLevel.GOOD -> 0.75f
            StabilityLevel.MARGINAL -> 0.5f
            StabilityLevel.UNSTABLE -> 0.25f
        }

        // Trend component (0-1)
        val trendScore = when (signalTrendDirection) {
            TrendDirection.IMPROVING -> 1.0f
            TrendDirection.STABLE -> 0.75f
            TrendDirection.DEGRADING -> 0.4f
        }

        // Variance penalty (high variance reduces score)
        val variancePenalty = if (signalVariance > 25f) 0.1f else 0f

        val rawScore = (signalScore * signalWeight) +
                (throughputScore * throughputWeight) +
                (stabilityScore * stabilityWeight) +
                (trendScore * trendWeight)

        return (rawScore - variancePenalty).coerceIn(0f, 1f)
    }

    /**
     * Determine overall diagnostic level from score.
     */
    private fun determineOverallLevel(score: Float): DiagnosticLevel {
        return when {
            score >= 0.8f -> DiagnosticLevel.EXCELLENT
            score >= 0.6f -> DiagnosticLevel.GOOD
            score >= 0.4f -> DiagnosticLevel.FAIR
            score >= 0.2f -> DiagnosticLevel.POOR
            else -> DiagnosticLevel.CRITICAL
        }
    }

    /**
     * Generate actionable recommendations based on analysis.
     */
    private fun generateRecommendations(
        network: WifiNetwork,
        distanceEstimate: com.ethersense.domain.model.DistanceEstimate,
        linkMarginAnalysis: com.ethersense.domain.model.LinkMarginAnalysis,
        signalTrend: com.ethersense.domain.model.SignalTrend,
        throughputPrediction: com.ethersense.domain.model.ThroughputPrediction
    ): Pair<List<String>, List<String>> {
        val recommendations = mutableListOf<String>()
        val recommendationsJa = mutableListOf<String>()

        // Distance-based recommendation
        if (distanceEstimate.estimatedMeters > 15f) {
            recommendations.add("Consider moving closer to the router (estimated ${String.format("%.1f", distanceEstimate.estimatedMeters)}m away)")
            recommendationsJa.add("ルーターに近づくことを検討してください（推定距離: ${String.format("%.1f", distanceEstimate.estimatedMeters)}m）")
        }

        // Link margin recommendation
        if (linkMarginAnalysis.stability == StabilityLevel.UNSTABLE) {
            recommendations.add("Connection is unstable. Reduce distance or obstacles to router")
            recommendationsJa.add("接続が不安定です。ルーターまでの距離や障害物を減らしてください")
        } else if (linkMarginAnalysis.stability == StabilityLevel.MARGINAL) {
            recommendations.add("Connection has limited headroom. Minor interference may cause drops")
            recommendationsJa.add("接続の余裕が少ないです。軽微な干渉でも切断の可能性があります")
        }

        // Signal trend recommendation
        if (signalTrend.direction == TrendDirection.DEGRADING && signalTrend.rateOfChange < -1f) {
            recommendations.add("Signal is degrading. Check if you're moving away from router")
            recommendationsJa.add("信号が悪化しています。ルーターから離れていないか確認してください")
        }

        // High variance recommendation
        if (signalTrend.variance > 25f) {
            recommendations.add("Signal is fluctuating. Check for moving obstacles or interference sources")
            recommendationsJa.add("信号が変動しています。移動する障害物や干渉源を確認してください")
        }

        // Band recommendation
        if (network.frequency < 3000 && throughputPrediction.mcsIndex < 5) {
            recommendations.add("Consider switching to 5GHz band for better performance if available")
            recommendationsJa.add("可能であれば5GHz帯に切り替えることで性能が向上する可能性があります")
        }

        // Channel width recommendation
        if (network.channelWidth < 40 && network.frequency >= 5000) {
            recommendations.add("Using narrow channel (${network.channelWidth}MHz). Wider channel could improve speed")
            recommendationsJa.add("狭いチャンネル幅（${network.channelWidth}MHz）を使用中。より広いチャンネルで速度が向上する可能性があります")
        }

        // If everything looks good
        if (recommendations.isEmpty()) {
            recommendations.add("Network connection is healthy. No issues detected")
            recommendationsJa.add("ネットワーク接続は良好です。問題は検出されませんでした")
        }

        return Pair(recommendations, recommendationsJa)
    }
}
