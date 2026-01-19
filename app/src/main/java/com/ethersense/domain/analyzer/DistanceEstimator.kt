package com.ethersense.domain.analyzer

import com.ethersense.domain.model.DistanceEstimate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.pow

/**
 * Distance estimator using ITU-R P.1238 indoor propagation model.
 *
 * ITU-R P.1238 formula:
 * L = 20×log₁₀(f) + N×log₁₀(d) + Pf(n) - 28
 *
 * Where:
 * - L: Total path loss (dB)
 * - f: Frequency (MHz)
 * - N: Distance power loss coefficient (environment dependent)
 * - d: Distance (m)
 * - Pf(n): Floor penetration loss factor
 *
 * Solving for distance:
 * d = 10^((L - 20×log₁₀(f) + 28 - Pf(n)) / N)
 *
 * Reference: https://en.wikipedia.org/wiki/ITU_model_for_indoor_attenuation
 */
@Singleton
class DistanceEstimator @Inject constructor() {

    /**
     * Environment types with their path loss exponents (N) based on ITU-R P.1238
     */
    enum class EnvironmentType(
        val pathLossExponent: Float,
        val displayName: String,
        val displayNameJa: String
    ) {
        RESIDENTIAL(2.8f, "Residential", "住宅"),
        OFFICE(3.0f, "Office", "オフィス"),
        COMMERCIAL(2.2f, "Commercial", "商業施設"),
        OPEN_SPACE(2.0f, "Open Space", "開放空間")
    }

    companion object {
        // Typical router transmit power in dBm
        private const val DEFAULT_TX_POWER = 20

        // Estimation error margin (percentage)
        private const val ERROR_MARGIN_LOW = 0.3f  // -30%
        private const val ERROR_MARGIN_HIGH = 0.5f // +50%
    }

    /**
     * Estimate distance from router based on RSSI using ITU-R P.1238 model.
     *
     * @param rssi Received signal strength in dBm
     * @param txPower Transmit power of the router in dBm (default: 20 dBm)
     * @param frequencyMhz Frequency in MHz
     * @param environmentType Type of indoor environment
     * @param floorPenetrationLoss Additional loss for floor penetration (default: 0)
     * @return DistanceEstimate with estimated distance and confidence
     */
    fun estimateDistance(
        rssi: Int,
        txPower: Int = DEFAULT_TX_POWER,
        frequencyMhz: Int,
        environmentType: EnvironmentType = EnvironmentType.RESIDENTIAL,
        floorPenetrationLoss: Float = 0f
    ): DistanceEstimate {
        // Calculate path loss: L = TxPower - RSSI
        val pathLoss = txPower - rssi

        // ITU-R P.1238 reverse calculation
        // L = 20×log₁₀(f) + N×log₁₀(d) + Pf(n) - 28
        // N×log₁₀(d) = L - 20×log₁₀(f) + 28 - Pf(n)
        // log₁₀(d) = (L - 20×log₁₀(f) + 28 - Pf(n)) / N
        // d = 10^((L - 20×log₁₀(f) + 28 - Pf(n)) / N)

        val freqComponent = 20 * log10(frequencyMhz.toDouble())
        val n = environmentType.pathLossExponent

        val exponent = (pathLoss - freqComponent + 28 - floorPenetrationLoss) / n
        val estimatedDistance = 10.0.pow(exponent).toFloat()

        // Clamp to reasonable range (0.5m to 100m for indoor)
        val clampedDistance = estimatedDistance.coerceIn(0.5f, 100f)

        // Calculate confidence based on signal strength and path loss exponent uncertainty
        val confidence = calculateConfidence(rssi, pathLoss.toFloat(), n)

        // Calculate error bounds
        val minDistance = (clampedDistance * (1 - ERROR_MARGIN_LOW)).coerceAtLeast(0.5f)
        val maxDistance = clampedDistance * (1 + ERROR_MARGIN_HIGH)

        return DistanceEstimate(
            estimatedMeters = clampedDistance,
            minMeters = minDistance,
            maxMeters = maxDistance,
            confidence = confidence,
            model = "ITU-R P.1238"
        )
    }

    /**
     * Calculate confidence score based on signal conditions.
     * Higher RSSI and lower path loss result in higher confidence.
     */
    private fun calculateConfidence(rssi: Int, pathLoss: Float, n: Float): Float {
        // Base confidence from RSSI (stronger signal = more confident)
        val rssiConfidence = when {
            rssi >= -50 -> 0.9f
            rssi >= -60 -> 0.8f
            rssi >= -70 -> 0.7f
            rssi >= -80 -> 0.5f
            else -> 0.3f
        }

        // Adjust for extreme path loss values (less confident at edges)
        val pathLossConfidence = when {
            pathLoss < 40 -> 0.7f  // Unusually low loss
            pathLoss > 90 -> 0.5f  // Very high loss
            else -> 1.0f
        }

        // Adjust for environment uncertainty (higher N = more multipath = less certain)
        val environmentConfidence = when {
            n <= 2.2f -> 0.9f
            n <= 2.8f -> 0.85f
            n <= 3.0f -> 0.8f
            else -> 0.7f
        }

        return (rssiConfidence * pathLossConfidence * environmentConfidence).coerceIn(0f, 1f)
    }

    /**
     * Get recommended environment type based on frequency band.
     * 2.4 GHz penetrates walls better, so residential is typical.
     * 5 GHz is more affected by obstacles.
     */
    fun getRecommendedEnvironment(frequencyMhz: Int): EnvironmentType {
        return if (frequencyMhz < 3000) {
            EnvironmentType.RESIDENTIAL
        } else {
            EnvironmentType.OFFICE
        }
    }
}
