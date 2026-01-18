package com.ethersense.di

import com.ethersense.domain.analyzer.ChannelAnalyzer
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
}
