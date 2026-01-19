package com.ethersense.di

import android.os.Vibrator
import com.ethersense.feedback.FeedbackOrchestrator
import com.ethersense.feedback.haptic.HapticFeedbackManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeedbackModule {

    @Provides
    @Singleton
    fun provideHapticFeedbackManager(
        vibrator: Vibrator
    ): HapticFeedbackManager {
        return HapticFeedbackManager(vibrator)
    }

    @Provides
    @Singleton
    fun provideFeedbackOrchestrator(
        hapticManager: HapticFeedbackManager
    ): FeedbackOrchestrator {
        return FeedbackOrchestrator(hapticManager)
    }
}
