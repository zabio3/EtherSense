package com.ethersense.di

import android.content.Context
import android.os.Vibrator
import com.ethersense.feedback.FeedbackOrchestrator
import com.ethersense.feedback.audio.AudioFeedbackManager
import com.ethersense.feedback.haptic.HapticFeedbackManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FeedbackModule {

    @Provides
    @Singleton
    fun provideAudioFeedbackManager(
        @ApplicationContext context: Context
    ): AudioFeedbackManager {
        return AudioFeedbackManager(context)
    }

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
        audioManager: AudioFeedbackManager,
        hapticManager: HapticFeedbackManager
    ): FeedbackOrchestrator {
        return FeedbackOrchestrator(audioManager, hapticManager)
    }
}
