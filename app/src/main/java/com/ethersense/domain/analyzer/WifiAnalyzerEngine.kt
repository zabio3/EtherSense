package com.ethersense.domain.analyzer

import com.ethersense.data.model.SignalMetrics
import com.ethersense.data.model.WifiNetwork
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiAnalyzerEngine @Inject constructor(
    private val signalQualityCalculator: SignalQualityCalculator,
    private val channelAnalyzer: ChannelAnalyzer,
    private val throughputEstimator: ThroughputEstimator
) {

    fun analyze(
        targetNetwork: WifiNetwork,
        allNetworks: List<WifiNetwork>
    ): SignalMetrics {
        val signalQuality = signalQualityCalculator.calculate(targetNetwork.rssi)

        val interferenceScore = channelAnalyzer.calculateInterference(
            targetNetwork, allNetworks
        )

        val channelUtilization = channelAnalyzer.getChannelUtilization(
            targetNetwork.channel, allNetworks
        )

        val theoreticalLinkSpeed = throughputEstimator.calculateTheoreticalLinkSpeed(
            targetNetwork
        )

        val estimatedThroughput = throughputEstimator.estimate(
            linkSpeed = theoreticalLinkSpeed,
            signalQuality = signalQuality,
            interferenceFactor = interferenceScore
        )

        val snr = signalQualityCalculator.calculateSnr(targetNetwork.rssi)

        return SignalMetrics(
            rssi = targetNetwork.rssi,
            signalQuality = signalQuality,
            linkSpeed = theoreticalLinkSpeed,
            interferenceScore = interferenceScore,
            estimatedThroughput = estimatedThroughput,
            snr = snr,
            channelUtilization = channelUtilization
        )
    }

    fun analyzeAll(networks: List<WifiNetwork>): Map<String, SignalMetrics> {
        return networks.associateBy(
            keySelector = { it.bssid },
            valueTransform = { analyze(it, networks) }
        )
    }

    fun findBestNetwork(networks: List<WifiNetwork>): WifiNetwork? {
        if (networks.isEmpty()) return null

        val metricsMap = analyzeAll(networks)

        return networks.maxByOrNull { network ->
            val metrics = metricsMap[network.bssid] ?: return@maxByOrNull 0f
            calculateNetworkScore(metrics)
        }
    }

    private fun calculateNetworkScore(metrics: SignalMetrics): Float {
        val qualityWeight = 0.4f
        val interferenceWeight = 0.3f
        val throughputWeight = 0.3f

        val normalizedThroughput = (metrics.estimatedThroughput / 1000f).coerceIn(0f, 1f)

        return (metrics.signalQuality * qualityWeight) +
                ((1 - metrics.interferenceScore) * interferenceWeight) +
                (normalizedThroughput * throughputWeight)
    }
}
