package com.ethersense.di

import com.ethersense.domain.analyzer.ChannelAnalyzer
import com.ethersense.domain.analyzer.DistanceEstimator
import com.ethersense.domain.analyzer.LinkMarginAnalyzer
import com.ethersense.domain.analyzer.NetworkDiagnosticsAnalyzer
import com.ethersense.domain.analyzer.ScientificThroughputPredictor
import com.ethersense.domain.analyzer.SignalPredictor
import com.ethersense.domain.analyzer.SignalQualityCalculator
import com.ethersense.domain.analyzer.ThroughputEstimator
import com.ethersense.domain.analyzer.WifiAnalyzerEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AnalyzerModule {

    @Provides
    @Singleton
    fun provideSignalQualityCalculator(): SignalQualityCalculator {
        return SignalQualityCalculator()
    }

    @Provides
    @Singleton
    fun provideChannelAnalyzer(): ChannelAnalyzer {
        return ChannelAnalyzer()
    }

    @Provides
    @Singleton
    fun provideThroughputEstimator(): ThroughputEstimator {
        return ThroughputEstimator()
    }

    @Provides
    @Singleton
    fun provideWifiAnalyzerEngine(
        signalQualityCalculator: SignalQualityCalculator,
        channelAnalyzer: ChannelAnalyzer,
        throughputEstimator: ThroughputEstimator
    ): WifiAnalyzerEngine {
        return WifiAnalyzerEngine(
            signalQualityCalculator,
            channelAnalyzer,
            throughputEstimator
        )
    }

    // New Scientific Diagnostic Analyzers

    @Provides
    @Singleton
    fun provideDistanceEstimator(): DistanceEstimator {
        return DistanceEstimator()
    }

    @Provides
    @Singleton
    fun provideScientificThroughputPredictor(): ScientificThroughputPredictor {
        return ScientificThroughputPredictor()
    }

    @Provides
    @Singleton
    fun provideLinkMarginAnalyzer(): LinkMarginAnalyzer {
        return LinkMarginAnalyzer()
    }

    @Provides
    @Singleton
    fun provideSignalPredictor(
        signalQualityCalculator: SignalQualityCalculator
    ): SignalPredictor {
        return SignalPredictor(signalQualityCalculator)
    }

    @Provides
    @Singleton
    fun provideNetworkDiagnosticsAnalyzer(
        distanceEstimator: DistanceEstimator,
        throughputPredictor: ScientificThroughputPredictor,
        linkMarginAnalyzer: LinkMarginAnalyzer,
        signalPredictor: SignalPredictor,
        signalQualityCalculator: SignalQualityCalculator
    ): NetworkDiagnosticsAnalyzer {
        return NetworkDiagnosticsAnalyzer(
            distanceEstimator,
            throughputPredictor,
            linkMarginAnalyzer,
            signalPredictor,
            signalQualityCalculator
        )
    }
}
