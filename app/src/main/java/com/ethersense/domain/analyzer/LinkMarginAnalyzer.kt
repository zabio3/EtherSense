package com.ethersense.domain.analyzer

import com.ethersense.data.model.WifiStandard
import com.ethersense.domain.model.LinkMarginAnalysis
import com.ethersense.domain.model.StabilityLevel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow

/**
 * Link margin analyzer for assessing connection stability.
 *
 * Link Budget equation:
 * P_rx = P_tx + G_tx - L_tx - PathLoss + G_rx - L_rx
 *
 * Link Margin = RSSI - ReceiverSensitivity
 *
 * A positive link margin indicates the connection can tolerate additional
 * signal degradation before failing. Generally:
 * - > 20 dB: Excellent stability
 * - 12-20 dB: Good stability
 * - 6-12 dB: Marginal (may experience occasional drops)
 * - < 6 dB: Unstable (frequent disconnections likely)
 *
 * Reference:
 * - https://www.mathworks.com/discovery/link-budget.html
 * - IEEE 802.11 receiver sensitivity specifications
 */
@Singleton
class LinkMarginAnalyzer @Inject constructor() {

    companion object {
        // Recommended minimum fade margin for reliable operation
        private const val RECOMMENDED_FADE_MARGIN = 6f

        // Stability thresholds
        private const val EXCELLENT_MARGIN = 20f
        private const val GOOD_MARGIN = 12f
        private const val MARGINAL_MARGIN = 6f
    }

    /**
     * Receiver sensitivity table by Wi-Fi standard and MCS index.
     * Values in dBm - lower (more negative) = more sensitive.
     * These are typical values; actual sensitivity varies by device.
     */
    private val sensitivityTable = mapOf(
        // Wi-Fi 6 (802.11ax) - 20 MHz
        Pair(WifiStandard.WIFI_6, 0) to -82,
        Pair(WifiStandard.WIFI_6, 1) to -79,
        Pair(WifiStandard.WIFI_6, 2) to -77,
        Pair(WifiStandard.WIFI_6, 3) to -74,
        Pair(WifiStandard.WIFI_6, 4) to -70,
        Pair(WifiStandard.WIFI_6, 5) to -66,
        Pair(WifiStandard.WIFI_6, 6) to -65,
        Pair(WifiStandard.WIFI_6, 7) to -64,
        Pair(WifiStandard.WIFI_6, 8) to -59,
        Pair(WifiStandard.WIFI_6, 9) to -57,
        Pair(WifiStandard.WIFI_6, 10) to -54,
        Pair(WifiStandard.WIFI_6, 11) to -52,

        // Wi-Fi 6E (same as Wi-Fi 6 for simplicity)
        Pair(WifiStandard.WIFI_6E, 0) to -82,
        Pair(WifiStandard.WIFI_6E, 1) to -79,
        Pair(WifiStandard.WIFI_6E, 2) to -77,
        Pair(WifiStandard.WIFI_6E, 3) to -74,
        Pair(WifiStandard.WIFI_6E, 4) to -70,
        Pair(WifiStandard.WIFI_6E, 5) to -66,
        Pair(WifiStandard.WIFI_6E, 6) to -65,
        Pair(WifiStandard.WIFI_6E, 7) to -64,
        Pair(WifiStandard.WIFI_6E, 8) to -59,
        Pair(WifiStandard.WIFI_6E, 9) to -57,
        Pair(WifiStandard.WIFI_6E, 10) to -54,
        Pair(WifiStandard.WIFI_6E, 11) to -52,

        // Wi-Fi 5 (802.11ac) - 20 MHz
        Pair(WifiStandard.WIFI_5, 0) to -82,
        Pair(WifiStandard.WIFI_5, 1) to -79,
        Pair(WifiStandard.WIFI_5, 2) to -77,
        Pair(WifiStandard.WIFI_5, 3) to -74,
        Pair(WifiStandard.WIFI_5, 4) to -70,
        Pair(WifiStandard.WIFI_5, 5) to -66,
        Pair(WifiStandard.WIFI_5, 6) to -65,
        Pair(WifiStandard.WIFI_5, 7) to -64,
        Pair(WifiStandard.WIFI_5, 8) to -59,
        Pair(WifiStandard.WIFI_5, 9) to -57,

        // Wi-Fi 4 (802.11n) - 20 MHz
        Pair(WifiStandard.WIFI_4, 0) to -82,
        Pair(WifiStandard.WIFI_4, 1) to -79,
        Pair(WifiStandard.WIFI_4, 2) to -77,
        Pair(WifiStandard.WIFI_4, 3) to -74,
        Pair(WifiStandard.WIFI_4, 4) to -70,
        Pair(WifiStandard.WIFI_4, 5) to -66,
        Pair(WifiStandard.WIFI_4, 6) to -65,
        Pair(WifiStandard.WIFI_4, 7) to -64
    )

    /**
     * Get receiver sensitivity for a given Wi-Fi standard and MCS index.
     *
     * @param wifiStandard Wi-Fi standard
     * @param mcsIndex MCS index
     * @return Receiver sensitivity in dBm
     */
    fun getReceiverSensitivity(wifiStandard: WifiStandard, mcsIndex: Int): Int {
        // Try exact match first
        sensitivityTable[Pair(wifiStandard, mcsIndex)]?.let { return it }

        // Fallback: find closest MCS for the standard
        val standardSensitivities = sensitivityTable.filter { it.key.first == wifiStandard }
        if (standardSensitivities.isNotEmpty()) {
            val closestMcs = standardSensitivities.keys.minByOrNull {
                kotlin.math.abs(it.second - mcsIndex)
            }
            closestMcs?.let { return sensitivityTable[it] ?: -82 }
        }

        // Ultimate fallback: conservative estimate for unknown standards
        return when {
            mcsIndex >= 10 -> -54
            mcsIndex >= 8 -> -59
            mcsIndex >= 5 -> -66
            mcsIndex >= 3 -> -74
            else -> -82
        }
    }

    /**
     * Analyze link margin for current connection.
     *
     * @param rssi Current received signal strength in dBm
     * @param wifiStandard Wi-Fi standard of the connection
     * @param mcsIndex Current MCS index (estimated or actual)
     * @return LinkMarginAnalysis with stability assessment
     */
    fun analyzeLinkMargin(
        rssi: Int,
        wifiStandard: WifiStandard,
        mcsIndex: Int
    ): LinkMarginAnalysis {
        val sensitivity = getReceiverSensitivity(wifiStandard, mcsIndex)
        val margin = (rssi - sensitivity).toFloat()

        val stability = when {
            margin >= EXCELLENT_MARGIN -> StabilityLevel.EXCELLENT
            margin >= GOOD_MARGIN -> StabilityLevel.GOOD
            margin >= MARGINAL_MARGIN -> StabilityLevel.MARGINAL
            else -> StabilityLevel.UNSTABLE
        }

        val fadeMargin = margin - RECOMMENDED_FADE_MARGIN
        val hasSufficientMargin = fadeMargin >= 0

        val (headroom, headroomJa) = when {
            margin >= 25f -> "Plenty of headroom" to "十分な余裕あり"
            margin >= 15f -> "Good headroom" to "良好な余裕"
            margin >= 8f -> "Adequate headroom" to "適度な余裕"
            margin >= 3f -> "Limited headroom" to "余裕が少ない"
            else -> "No headroom" to "余裕なし"
        }

        val (recommendation, recommendationJa) = generateRecommendation(
            margin, stability, hasSufficientMargin
        )

        return LinkMarginAnalysis(
            marginDb = margin,
            stability = stability,
            fadeMarginDb = fadeMargin,
            headroom = headroom,
            headroomJa = headroomJa,
            recommendation = recommendation,
            recommendationJa = recommendationJa
        )
    }

    /**
     * Generate recommendation based on link margin analysis.
     */
    private fun generateRecommendation(
        margin: Float,
        stability: StabilityLevel,
        hasSufficientMargin: Boolean
    ): Pair<String?, String?> {
        return when {
            stability == StabilityLevel.EXCELLENT -> null to null

            stability == StabilityLevel.GOOD && hasSufficientMargin -> null to null

            stability == StabilityLevel.GOOD && !hasSufficientMargin ->
                "Consider moving closer to the router for better stability" to
                        "より安定した接続のため、ルーターに近づくことを検討してください"

            stability == StabilityLevel.MARGINAL ->
                "Connection may experience occasional drops. Move closer to router or reduce obstacles" to
                        "接続が途切れる可能性があります。ルーターに近づくか、障害物を減らしてください"

            else ->
                "Connection is unstable. Significantly reduce distance to router or check for interference" to
                        "接続が不安定です。ルーターまでの距離を大幅に縮めるか、干渉を確認してください"
        }
    }

    /**
     * Calculate theoretical maximum distance where connection would still work.
     * This is a rough estimate using simplified path loss model.
     */
    fun estimateMaxRange(
        currentRssi: Int,
        currentDistanceMeters: Float,
        wifiStandard: WifiStandard,
        mcsIndex: Int
    ): Float {
        val sensitivity = getReceiverSensitivity(wifiStandard, mcsIndex)
        val currentMargin = currentRssi - sensitivity

        // Using inverse square law approximation:
        // At max range, RSSI = sensitivity (margin = 0)
        // Path loss increases by ~20 dB per decade of distance
        // Additional margin loss = 20 × log10(maxDist / currentDist)
        // currentMargin = 20 × log10(maxDist / currentDist)
        // maxDist = currentDist × 10^(currentMargin / 20)

        val ratio = 10.0.pow(currentMargin / 20.0)
        return (currentDistanceMeters * ratio).toFloat().coerceIn(1f, 1000f)
    }
}
