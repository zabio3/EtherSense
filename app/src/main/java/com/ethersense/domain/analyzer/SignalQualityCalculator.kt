package com.ethersense.domain.analyzer

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SignalQualityCalculator @Inject constructor() {

    fun calculate(rssi: Int): Float {
        return when {
            rssi >= EXCELLENT_THRESHOLD -> 1.0f
            rssi >= VERY_GOOD_THRESHOLD -> {
                0.9f + (rssi - VERY_GOOD_THRESHOLD) * 0.005f
            }
            rssi >= GOOD_THRESHOLD -> {
                0.75f + (rssi - GOOD_THRESHOLD) * 0.015f
            }
            rssi >= FAIR_THRESHOLD -> {
                0.5f + (rssi - FAIR_THRESHOLD) * 0.025f
            }
            rssi >= POOR_THRESHOLD -> {
                0.25f + (rssi - POOR_THRESHOLD) * 0.025f
            }
            rssi >= WEAK_THRESHOLD -> {
                (rssi - WEAK_THRESHOLD) * 0.025f
            }
            else -> 0.0f
        }.coerceIn(0f, 1f)
    }

    fun calculateSnr(rssi: Int, noiseFloor: Int = DEFAULT_NOISE_FLOOR): Float {
        return (rssi - noiseFloor).toFloat().coerceAtLeast(0f)
    }

    fun rssiToPercentage(rssi: Int): Int {
        return (calculate(rssi) * 100).toInt()
    }

    fun getRssiDescription(rssi: Int): String {
        return when {
            rssi >= EXCELLENT_THRESHOLD -> "Excellent"
            rssi >= VERY_GOOD_THRESHOLD -> "Very Good"
            rssi >= GOOD_THRESHOLD -> "Good"
            rssi >= FAIR_THRESHOLD -> "Fair"
            rssi >= POOR_THRESHOLD -> "Poor"
            else -> "Weak"
        }
    }

    companion object {
        private const val EXCELLENT_THRESHOLD = -30
        private const val VERY_GOOD_THRESHOLD = -50
        private const val GOOD_THRESHOLD = -60
        private const val FAIR_THRESHOLD = -70
        private const val POOR_THRESHOLD = -80
        private const val WEAK_THRESHOLD = -90

        private const val DEFAULT_NOISE_FLOOR = -95
    }
}
