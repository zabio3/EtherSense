package com.ethersense.feedback.haptic

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HapticFeedbackManager @Inject constructor(
    private val vibrator: Vibrator
) {
    private var isEnabled = true
    private var lastVibrationTime = 0L

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }

    fun vibrateForInterference(interferenceScore: Float) {
        if (!isEnabled || !canVibrate()) return
        if (!shouldVibrate()) return

        val effect = when {
            interferenceScore >= SEVERE_THRESHOLD -> VibrationPatterns.SEVERE_INTERFERENCE
            interferenceScore >= HIGH_THRESHOLD -> VibrationPatterns.HIGH_INTERFERENCE
            interferenceScore >= MODERATE_THRESHOLD -> VibrationPatterns.MODERATE_INTERFERENCE
            interferenceScore >= LOW_THRESHOLD -> VibrationPatterns.LOW_INTERFERENCE
            else -> return
        }

        vibrate(effect)
        lastVibrationTime = System.currentTimeMillis()
    }

    fun vibrateOnNetworkDiscovered() {
        if (!isEnabled || !canVibrate()) return

        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
        vibrate(effect)
    }

    fun vibrateOnConnectionChange() {
        if (!isEnabled || !canVibrate()) return

        val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
        vibrate(effect)
    }

    fun vibrateForSignalQuality(quality: Float) {
        if (!isEnabled || !canVibrate()) return
        if (!shouldVibrate()) return

        val effect = when {
            quality <= 0.2f -> VibrationPatterns.SIGNAL_WEAK
            quality <= 0.4f -> VibrationPatterns.SIGNAL_POOR
            else -> return
        }

        vibrate(effect)
        lastVibrationTime = System.currentTimeMillis()
    }

    fun vibrateCustomPattern(pattern: LongArray, amplitudes: IntArray) {
        if (!isEnabled || !canVibrate()) return

        val effect = VibrationEffect.createWaveform(pattern, amplitudes, -1)
        vibrate(effect)
    }

    private fun canVibrate(): Boolean {
        return vibrator.hasVibrator()
    }

    private fun shouldVibrate(): Boolean {
        return System.currentTimeMillis() - lastVibrationTime > VIBRATION_COOLDOWN_MS
    }

    private fun vibrate(effect: VibrationEffect) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(effect)
        }
    }

    companion object {
        private const val SEVERE_THRESHOLD = 0.7f
        private const val HIGH_THRESHOLD = 0.5f
        private const val MODERATE_THRESHOLD = 0.3f
        private const val LOW_THRESHOLD = 0.1f
        private const val VIBRATION_COOLDOWN_MS = 500L
    }
}

object VibrationPatterns {
    val SEVERE_INTERFERENCE: VibrationEffect = VibrationEffect.createWaveform(
        longArrayOf(0, 100, 50, 100, 50, 100),
        intArrayOf(0, 255, 0, 255, 0, 255),
        -1
    )

    val HIGH_INTERFERENCE: VibrationEffect = VibrationEffect.createWaveform(
        longArrayOf(0, 80, 60, 80),
        intArrayOf(0, 200, 0, 200),
        -1
    )

    val MODERATE_INTERFERENCE: VibrationEffect = VibrationEffect.createWaveform(
        longArrayOf(0, 60, 80, 60),
        intArrayOf(0, 150, 0, 150),
        -1
    )

    val LOW_INTERFERENCE: VibrationEffect = VibrationEffect.createPredefined(
        VibrationEffect.EFFECT_TICK
    )

    val SIGNAL_WEAK: VibrationEffect = VibrationEffect.createWaveform(
        longArrayOf(0, 200, 100, 200),
        intArrayOf(0, 180, 0, 180),
        -1
    )

    val SIGNAL_POOR: VibrationEffect = VibrationEffect.createWaveform(
        longArrayOf(0, 100, 100, 100),
        intArrayOf(0, 120, 0, 120),
        -1
    )
}
