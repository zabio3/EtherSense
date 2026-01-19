package com.ethersense.domain.analyzer

import com.ethersense.domain.model.QualityPrediction
import com.ethersense.domain.model.SignalTrend
import com.ethersense.domain.model.TrendDirection
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Signal predictor for analyzing RSSI trends and predicting future signal quality.
 *
 * Uses statistical methods:
 * - Moving average for trend smoothing
 * - Linear regression for trend direction
 * - Variance calculation for stability assessment
 *
 * Reference:
 * - https://arxiv.org/pdf/1812.08856 (ML for Wireless Link Quality Estimation)
 */
@Singleton
class SignalPredictor @Inject constructor(
    private val signalQualityCalculator: SignalQualityCalculator
) {

    companion object {
        // Default window size for moving average (samples)
        private const val DEFAULT_WINDOW_SIZE = 10

        // Minimum samples required for meaningful prediction
        private const val MIN_SAMPLES_FOR_PREDICTION = 5

        // Threshold for determining trend direction (dB/sample)
        private const val IMPROVING_THRESHOLD = 0.5f
        private const val DEGRADING_THRESHOLD = -0.5f

        // Variance thresholds
        private const val LOW_VARIANCE_THRESHOLD = 4f  // < 2 dB std dev
        private const val HIGH_VARIANCE_THRESHOLD = 25f // > 5 dB std dev
    }

    /**
     * Analyze signal trend from RSSI history.
     *
     * @param rssiHistory List of RSSI values (oldest to newest)
     * @param windowSize Window size for moving average calculation
     * @return SignalTrend with direction, rate of change, and statistics
     */
    fun predictSignalTrend(
        rssiHistory: List<Int>,
        windowSize: Int = DEFAULT_WINDOW_SIZE
    ): SignalTrend {
        if (rssiHistory.isEmpty()) {
            return SignalTrend(
                direction = TrendDirection.STABLE,
                rateOfChange = 0f,
                movingAverage = 0f,
                variance = 0f
            )
        }

        // Calculate moving average of recent samples
        val recentSamples = rssiHistory.takeLast(windowSize)
        val movingAverage = recentSamples.average().toFloat()

        // Calculate variance
        val variance = calculateVariance(recentSamples)

        // Calculate rate of change using linear regression
        val rateOfChange = calculateRateOfChange(recentSamples)

        // Determine trend direction
        val direction = when {
            rateOfChange >= IMPROVING_THRESHOLD -> TrendDirection.IMPROVING
            rateOfChange <= DEGRADING_THRESHOLD -> TrendDirection.DEGRADING
            else -> TrendDirection.STABLE
        }

        return SignalTrend(
            direction = direction,
            rateOfChange = rateOfChange,
            movingAverage = movingAverage,
            variance = variance
        )
    }

    /**
     * Predict future signal quality.
     *
     * @param rssiHistory List of RSSI values (oldest to newest)
     * @param secondsAhead How far ahead to predict (assuming 1 sample/second)
     * @return QualityPrediction with predicted values and confidence
     */
    fun predictFutureQuality(
        rssiHistory: List<Int>,
        secondsAhead: Int = 5
    ): QualityPrediction {
        if (rssiHistory.size < MIN_SAMPLES_FOR_PREDICTION) {
            // Not enough data for prediction
            val currentRssi = rssiHistory.lastOrNull() ?: -70
            val currentQuality = signalQualityCalculator.calculate(currentRssi)
            return QualityPrediction(
                predictedRssi = currentRssi,
                predictedQuality = currentQuality,
                confidence = 0.3f, // Low confidence due to insufficient data
                warning = "Insufficient data for prediction",
                warningJa = "予測に必要なデータが不足しています"
            )
        }

        val trend = predictSignalTrend(rssiHistory)

        // Predict future RSSI using linear extrapolation
        // Limit prediction range to avoid unrealistic values
        val maxChange = 10f // Maximum reasonable change in secondsAhead seconds
        val predictedChange = (trend.rateOfChange * secondsAhead).coerceIn(-maxChange, maxChange)
        val predictedRssi = (trend.movingAverage + predictedChange).toInt().coerceIn(-100, -20)

        val predictedQuality = signalQualityCalculator.calculate(predictedRssi)

        // Calculate confidence based on:
        // 1. Sample count (more samples = higher confidence)
        // 2. Variance (lower variance = higher confidence)
        // 3. Trend consistency
        val sampleConfidence = (rssiHistory.size.toFloat() / 50f).coerceIn(0.3f, 1f)
        val varianceConfidence = when {
            trend.variance < LOW_VARIANCE_THRESHOLD -> 0.9f
            trend.variance < HIGH_VARIANCE_THRESHOLD -> 0.7f
            else -> 0.5f
        }
        val confidence = (sampleConfidence * varianceConfidence).coerceIn(0.3f, 0.95f)

        // Generate warnings
        val (warning, warningJa) = generateWarning(trend, predictedQuality)

        return QualityPrediction(
            predictedRssi = predictedRssi,
            predictedQuality = predictedQuality,
            confidence = confidence,
            warning = warning,
            warningJa = warningJa
        )
    }

    /**
     * Calculate variance of RSSI samples.
     */
    private fun calculateVariance(samples: List<Int>): Float {
        if (samples.size < 2) return 0f

        val mean = samples.average()
        val sumSquaredDiff = samples.sumOf { (it - mean) * (it - mean) }
        return (sumSquaredDiff / (samples.size - 1)).toFloat()
    }

    /**
     * Calculate rate of change using simple linear regression.
     * Returns slope in dB per sample.
     */
    private fun calculateRateOfChange(samples: List<Int>): Float {
        if (samples.size < 2) return 0f

        val n = samples.size
        val xMean = (n - 1) / 2.0
        val yMean = samples.average()

        var numerator = 0.0
        var denominator = 0.0

        for (i in samples.indices) {
            val xDiff = i - xMean
            val yDiff = samples[i] - yMean
            numerator += xDiff * yDiff
            denominator += xDiff * xDiff
        }

        return if (denominator > 0) (numerator / denominator).toFloat() else 0f
    }

    /**
     * Generate warning messages based on trend analysis.
     */
    private fun generateWarning(
        trend: SignalTrend,
        predictedQuality: Float
    ): Pair<String?, String?> {
        return when {
            trend.direction == TrendDirection.DEGRADING && trend.rateOfChange < -1.5f ->
                "Signal is degrading rapidly" to "信号が急速に悪化しています"

            trend.direction == TrendDirection.DEGRADING && predictedQuality < 0.3f ->
                "Signal quality may become poor soon" to "まもなく信号品質が低下する可能性があります"

            trend.variance > HIGH_VARIANCE_THRESHOLD ->
                "Signal is unstable" to "信号が不安定です"

            trend.direction == TrendDirection.IMPROVING && trend.rateOfChange > 1.5f ->
                "Signal is improving" to "信号が改善しています"

            else -> null to null
        }
    }

    /**
     * Check if signal is stable (low variance over time).
     */
    fun isSignalStable(rssiHistory: List<Int>, maxVariance: Float = LOW_VARIANCE_THRESHOLD): Boolean {
        if (rssiHistory.size < MIN_SAMPLES_FOR_PREDICTION) return true // Assume stable with insufficient data
        val variance = calculateVariance(rssiHistory.takeLast(DEFAULT_WINDOW_SIZE))
        return variance < maxVariance
    }

    /**
     * Calculate signal stability score (0-1, higher = more stable).
     */
    fun calculateStabilityScore(rssiHistory: List<Int>): Float {
        if (rssiHistory.size < MIN_SAMPLES_FOR_PREDICTION) return 0.5f

        val variance = calculateVariance(rssiHistory.takeLast(DEFAULT_WINDOW_SIZE))
        val stdDev = sqrt(variance)

        // Map std dev to stability score
        // 0-1 dB std dev = 1.0 stability
        // 5+ dB std dev = 0.2 stability
        return when {
            stdDev <= 1f -> 1.0f
            stdDev <= 2f -> 0.9f
            stdDev <= 3f -> 0.7f
            stdDev <= 4f -> 0.5f
            stdDev <= 5f -> 0.3f
            else -> 0.2f
        }
    }
}
