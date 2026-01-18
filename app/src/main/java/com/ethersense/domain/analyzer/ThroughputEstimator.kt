package com.ethersense.domain.analyzer

import com.ethersense.data.model.WifiNetwork
import com.ethersense.data.model.WifiStandard
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThroughputEstimator @Inject constructor() {

    fun estimate(
        linkSpeed: Int,
        signalQuality: Float,
        interferenceFactor: Float,
        protocolOverhead: Float = DEFAULT_PROTOCOL_OVERHEAD
    ): Float {
        val effectiveQuality = signalQuality * (1 - interferenceFactor)
        return linkSpeed * effectiveQuality * protocolOverhead
    }

    fun calculateTheoreticalLinkSpeed(network: WifiNetwork): Int {
        val baseSpeed = getBaseSpeedForStandard(network.wifiStandard)
        val widthMultiplier = getChannelWidthMultiplier(network.channelWidth)
        val bandMultiplier = if (network.frequency > 5000) 1.0f else 0.8f

        return (baseSpeed * widthMultiplier * bandMultiplier).toInt()
    }

    fun estimateForNetwork(
        network: WifiNetwork,
        signalQuality: Float,
        interferenceFactor: Float
    ): Float {
        val linkSpeed = calculateTheoreticalLinkSpeed(network)
        return estimate(linkSpeed, signalQuality, interferenceFactor)
    }

    private fun getBaseSpeedForStandard(standard: WifiStandard): Int {
        return when (standard) {
            WifiStandard.WIFI_4 -> 150
            WifiStandard.WIFI_5 -> 433
            WifiStandard.WIFI_6 -> 600
            WifiStandard.WIFI_6E -> 600
            WifiStandard.UNKNOWN -> 72
        }
    }

    private fun getChannelWidthMultiplier(channelWidth: Int): Float {
        return when (channelWidth) {
            20 -> 1.0f
            40 -> 2.0f
            80 -> 4.0f
            160 -> 8.0f
            else -> 1.0f
        }
    }

    companion object {
        private const val DEFAULT_PROTOCOL_OVERHEAD = 0.65f
    }
}
