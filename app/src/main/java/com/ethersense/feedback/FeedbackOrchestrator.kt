package com.ethersense.feedback

import com.ethersense.data.model.SignalMetrics
import com.ethersense.feedback.haptic.HapticFeedbackManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackOrchestrator @Inject constructor(
    private val hapticManager: HapticFeedbackManager
) {
    private var hapticEnabled = true

    fun setHapticEnabled(enabled: Boolean) {
        hapticEnabled = enabled
        hapticManager.setEnabled(enabled)
    }

    fun provideFeedback(metrics: SignalMetrics) {
        if (hapticEnabled) {
            hapticManager.vibrateForInterference(metrics.interferenceScore)
        }
    }

    fun stop() {
        // Nothing to stop without audio
    }

    fun release() {
        // Nothing to release without audio
    }

    fun onNetworkDiscovered() {
        if (hapticEnabled) {
            hapticManager.vibrateOnNetworkDiscovered()
        }
    }

    fun onConnectionChanged() {
        if (hapticEnabled) {
            hapticManager.vibrateOnConnectionChange()
        }
    }
}
