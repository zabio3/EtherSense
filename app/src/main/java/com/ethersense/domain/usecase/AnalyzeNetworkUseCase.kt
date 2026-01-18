package com.ethersense.domain.usecase

import com.ethersense.data.model.SignalMetrics
import com.ethersense.data.model.WifiNetwork
import com.ethersense.domain.analyzer.WifiAnalyzerEngine
import javax.inject.Inject

class AnalyzeNetworkUseCase @Inject constructor(
    private val analyzerEngine: WifiAnalyzerEngine
) {
    operator fun invoke(
        targetNetwork: WifiNetwork,
        allNetworks: List<WifiNetwork>
    ): SignalMetrics {
        return analyzerEngine.analyze(targetNetwork, allNetworks)
    }

    fun findBestNetwork(networks: List<WifiNetwork>): WifiNetwork? {
        return analyzerEngine.findBestNetwork(networks)
    }
}
