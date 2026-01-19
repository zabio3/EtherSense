package com.ethersense.domain.analyzer

import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.model.WifiStandard
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

@Singleton
class ThroughputEstimator @Inject constructor() {

    fun estimate(
        linkSpeed: Int,
        signalQuality: Float,
        interferenceFactor: Float,
        protocolOverhead: Float = DEFAULT_PROTOCOL_OVERHEAD
    ): Float {
        // Use sigmoid-based quality factor for more realistic estimation
        val qualityFactor = calculateQualityFactor(signalQuality)
        val interferencePenalty = 1 - (interferenceFactor * INTERFERENCE_WEIGHT)
        val effectiveQuality = qualityFactor * interferencePenalty
        return linkSpeed * effectiveQuality * protocolOverhead
    }

    fun calculateTheoreticalLinkSpeed(network: WifiNetwork): Int {
        val baseSpeed = getBaseSpeedForStandard(network.wifiStandard)
        val widthMultiplier = getChannelWidthMultiplier(network.channelWidth)
        val bandMultiplier = getBandMultiplier(network.frequency)
        val streamMultiplier = getEstimatedSpatialStreams(network.wifiStandard)

        return (baseSpeed * widthMultiplier * bandMultiplier * streamMultiplier).toInt()
    }

    fun estimateForNetwork(
        network: WifiNetwork,
        signalQuality: Float,
        interferenceFactor: Float
    ): Float {
        val linkSpeed = calculateTheoreticalLinkSpeed(network)
        return estimate(linkSpeed, signalQuality, interferenceFactor)
    }

    /**
     * Calculates a more realistic quality factor using a sigmoid curve
     * This better represents how signal quality affects actual throughput
     */
    private fun calculateQualityFactor(signalQuality: Float): Float {
        // Sigmoid function centered at 0.5 quality
        // Maps quality to effective throughput percentage
        val steepness = 5f
        val midpoint = 0.5f
        return (1f / (1f + exp(-steepness * (signalQuality - midpoint)))).coerceIn(0.1f, 1f)
    }

    /**
     * Updated base speeds to reflect more realistic single-stream PHY rates
     * These values represent typical single spatial stream rates
     */
    private fun getBaseSpeedForStandard(standard: WifiStandard): Int {
        return when (standard) {
            WifiStandard.WIFI_4 -> 150   // 802.11n: 150 Mbps per stream (40MHz)
            WifiStandard.WIFI_5 -> 433   // 802.11ac: 433 Mbps per stream (80MHz)
            WifiStandard.WIFI_6 -> 600   // 802.11ax: 600 Mbps per stream (80MHz, 1024-QAM)
            WifiStandard.WIFI_6E -> 600  // 802.11ax (6GHz): Same as Wi-Fi 6
            WifiStandard.UNKNOWN -> 72   // Legacy 802.11g
        }
    }

    private fun getChannelWidthMultiplier(channelWidth: Int): Float {
        return when (channelWidth) {
            20 -> 1.0f
            40 -> 2.0f
            80 -> 4.0f
            160 -> 8.0f
            320 -> 16.0f  // Wi-Fi 7 support
            else -> 1.0f
        }
    }

    /**
     * Band multiplier accounts for propagation characteristics
     * 2.4GHz has better range but more interference
     * 5GHz/6GHz have cleaner spectrum but shorter range
     */
    private fun getBandMultiplier(frequency: Int): Float {
        return when {
            frequency > 5925 -> 1.1f   // 6GHz band (Wi-Fi 6E) - cleanest spectrum
            frequency > 5000 -> 1.0f   // 5GHz band
            else -> 0.85f              // 2.4GHz band - more interference
        }
    }

    /**
     * Estimates typical spatial stream count based on Wi-Fi standard
     * Most consumer devices support 2 streams, high-end support 3-4
     */
    private fun getEstimatedSpatialStreams(standard: WifiStandard): Float {
        return when (standard) {
            WifiStandard.WIFI_4 -> 1.5f   // Typically 1-2 streams
            WifiStandard.WIFI_5 -> 2.0f   // Typically 2 streams
            WifiStandard.WIFI_6 -> 2.0f   // Typically 2 streams (phones), up to 4 (laptops)
            WifiStandard.WIFI_6E -> 2.0f  // Same as Wi-Fi 6
            WifiStandard.UNKNOWN -> 1.0f
        }
    }

    companion object {
        // Protocol overhead (TCP/IP headers, retransmissions, etc.)
        // Real-world is typically 60-70% efficiency
        private const val DEFAULT_PROTOCOL_OVERHEAD = 0.65f

        // How much interference affects throughput (0-1)
        private const val INTERFERENCE_WEIGHT = 0.8f
    }
}
