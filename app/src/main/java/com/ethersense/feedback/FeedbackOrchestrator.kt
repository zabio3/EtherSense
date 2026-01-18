package com.ethersense.feedback

import com.ethersense.data.model.SignalMetrics
import com.ethersense.feedback.audio.AudioFeedbackManager
import com.ethersense.feedback.haptic.HapticFeedbackManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackOrchestrator @Inject constructor(
    private val audioManager: AudioFeedbackManager,
    private val hapticManager: HapticFeedbackManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var continuousFeedbackJob: Job? = null

    private var audioEnabled = false
    private var hapticEnabled = true
    private var lastMetrics: SignalMetrics? = null

    fun setAudioEnabled(enabled: Boolean) {
        audioEnabled = enabled
        audioManager.setEnabled(enabled)
        if (!enabled) {
            audioManager.stop()
        }
    }

    fun setHapticEnabled(enabled: Boolean) {
        hapticEnabled = enabled
        hapticManager.setEnabled(enabled)
    }

    fun provideFeedback(metrics: SignalMetrics) {
        lastMetrics = metrics

        if (audioEnabled) {
            audioManager.playSignalTone(metrics.rssi)
        }

        if (hapticEnabled) {
            hapticManager.vibrateForInterference(metrics.interferenceScore)
        }
    }

    fun startContinuousFeedback(metricsProvider: () -> SignalMetrics?) {
        continuousFeedbackJob?.cancel()
        continuousFeedbackJob = scope.launch {
            while (isActive) {
                val metrics = metricsProvider()
                if (metrics != null) {
                    lastMetrics = metrics

                    if (audioEnabled) {
                        audioManager.playSignalTone(metrics.rssi, TONE_DURATION_MS)
                    }

                    if (hapticEnabled && metrics.interferenceScore > INTERFERENCE_HAPTIC_THRESHOLD) {
                        hapticManager.vibrateForInterference(metrics.interferenceScore)
                    }

                    val interval = calculateFeedbackInterval(metrics)
                    delay(interval)
                } else {
                    delay(DEFAULT_INTERVAL_MS)
                }
            }
        }
    }

    fun startGeigerMode(rssiProvider: () -> Int) {
        if (!audioEnabled) return

        audioManager.startGeigerMode(rssiProvider)
    }

    fun stop() {
        continuousFeedbackJob?.cancel()
        continuousFeedbackJob = null
        audioManager.stop()
    }

    fun release() {
        stop()
        audioManager.release()
        scope.cancel()
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

    private fun calculateFeedbackInterval(metrics: SignalMetrics): Long {
        val qualityFactor = 1 - metrics.signalQuality
        val baseInterval = MIN_INTERVAL_MS + (qualityFactor * (MAX_INTERVAL_MS - MIN_INTERVAL_MS))
        return baseInterval.toLong()
    }

    companion object {
        private const val TONE_DURATION_MS = 50
        private const val MIN_INTERVAL_MS = 100f
        private const val MAX_INTERVAL_MS = 1000f
        private const val DEFAULT_INTERVAL_MS = 500L
        private const val INTERFERENCE_HAPTIC_THRESHOLD = 0.3f
    }
}
