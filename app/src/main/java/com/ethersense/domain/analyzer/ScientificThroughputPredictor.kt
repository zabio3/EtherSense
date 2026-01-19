package com.ethersense.domain.analyzer

import com.ethersense.data.model.WifiStandard
import com.ethersense.domain.model.ThroughputPrediction
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log2
import kotlin.math.pow

/**
 * Scientific throughput predictor using Shannon-Hartley theorem and MCS tables.
 *
 * Shannon-Hartley theorem:
 * C = B × log₂(1 + SNR)
 *
 * Where:
 * - C: Channel capacity (bits per second)
 * - B: Bandwidth (Hz)
 * - SNR: Signal-to-noise ratio (linear, not dB)
 *
 * MCS (Modulation and Coding Scheme) tables provide practical throughput
 * based on SNR thresholds defined in IEEE 802.11 standards.
 *
 * References:
 * - https://en.wikipedia.org/wiki/Shannon–Hartley_theorem
 * - https://mcsindex.com/
 */
@Singleton
class ScientificThroughputPredictor @Inject constructor() {

    companion object {
        // Default noise floor in dBm for Wi-Fi environments
        private const val DEFAULT_NOISE_FLOOR = -95

        // Protocol overhead factor (MAC, IP headers, retransmissions)
        // Real-world throughput is typically 50-70% of PHY rate
        private const val PROTOCOL_OVERHEAD = 0.65f

        // Guard interval penalty for short GI vs long GI
        private const val SHORT_GI_BONUS = 1.11f
    }

    /**
     * MCS entry defining modulation, coding rate, SNR requirement, and data rate
     */
    data class McsEntry(
        val index: Int,
        val modulation: String,
        val codingRate: String,
        val minSnrDb: Float,
        val dataRateMbps20Mhz: Float
    )

    /**
     * MCS table for 802.11ax (Wi-Fi 6)
     * Data rates are for 1 spatial stream, 20 MHz, long GI (0.8μs)
     */
    private val mcsTable11ax = listOf(
        McsEntry(0, "BPSK", "1/2", 5f, 8.6f),
        McsEntry(1, "QPSK", "1/2", 8f, 17.2f),
        McsEntry(2, "QPSK", "3/4", 11f, 25.8f),
        McsEntry(3, "16-QAM", "1/2", 14f, 34.4f),
        McsEntry(4, "16-QAM", "3/4", 17f, 51.6f),
        McsEntry(5, "64-QAM", "2/3", 20f, 68.8f),
        McsEntry(6, "64-QAM", "3/4", 23f, 77.4f),
        McsEntry(7, "64-QAM", "5/6", 26f, 86.0f),
        McsEntry(8, "256-QAM", "3/4", 29f, 103.2f),
        McsEntry(9, "256-QAM", "5/6", 32f, 114.7f),
        McsEntry(10, "1024-QAM", "3/4", 35f, 129.0f),
        McsEntry(11, "1024-QAM", "5/6", 38f, 143.4f)
    )

    /**
     * MCS table for 802.11ac (Wi-Fi 5)
     * Data rates are for 1 spatial stream, 20 MHz, long GI
     */
    private val mcsTable11ac = listOf(
        McsEntry(0, "BPSK", "1/2", 5f, 6.5f),
        McsEntry(1, "QPSK", "1/2", 8f, 13.0f),
        McsEntry(2, "QPSK", "3/4", 11f, 19.5f),
        McsEntry(3, "16-QAM", "1/2", 14f, 26.0f),
        McsEntry(4, "16-QAM", "3/4", 17f, 39.0f),
        McsEntry(5, "64-QAM", "2/3", 20f, 52.0f),
        McsEntry(6, "64-QAM", "3/4", 23f, 58.5f),
        McsEntry(7, "64-QAM", "5/6", 26f, 65.0f),
        McsEntry(8, "256-QAM", "3/4", 29f, 78.0f),
        McsEntry(9, "256-QAM", "5/6", 32f, 86.7f)
    )

    /**
     * MCS table for 802.11n (Wi-Fi 4)
     * Data rates are for 1 spatial stream, 20 MHz, long GI
     */
    private val mcsTable11n = listOf(
        McsEntry(0, "BPSK", "1/2", 5f, 6.5f),
        McsEntry(1, "QPSK", "1/2", 8f, 13.0f),
        McsEntry(2, "QPSK", "3/4", 11f, 19.5f),
        McsEntry(3, "16-QAM", "1/2", 14f, 26.0f),
        McsEntry(4, "16-QAM", "3/4", 17f, 39.0f),
        McsEntry(5, "64-QAM", "2/3", 20f, 52.0f),
        McsEntry(6, "64-QAM", "3/4", 23f, 58.5f),
        McsEntry(7, "64-QAM", "5/6", 26f, 65.0f)
    )

    /**
     * Calculate theoretical Shannon capacity.
     *
     * @param bandwidthMhz Channel bandwidth in MHz
     * @param snrDb Signal-to-noise ratio in dB
     * @return Theoretical maximum capacity in Mbps
     */
    fun calculateShannonCapacity(bandwidthMhz: Int, snrDb: Float): Float {
        // Convert SNR from dB to linear: SNR_linear = 10^(SNR_dB / 10)
        val snrLinear = 10.0.pow(snrDb / 10.0)

        // Shannon capacity: C = B × log₂(1 + SNR)
        // Convert bandwidth to Hz, calculate bits/sec, convert to Mbps
        val capacityBps = bandwidthMhz * 1_000_000.0 * log2(1 + snrLinear)
        return (capacityBps / 1_000_000).toFloat()
    }

    /**
     * Estimate MCS index based on current SNR.
     *
     * @param snrDb Signal-to-noise ratio in dB
     * @param wifiStandard Wi-Fi standard to use for MCS table selection
     * @return MCS index (0-11 for 802.11ax, 0-9 for 802.11ac, 0-7 for 802.11n)
     */
    fun estimateMcsIndex(snrDb: Float, wifiStandard: WifiStandard): Int {
        val mcsTable = getMcsTable(wifiStandard)

        // Find highest MCS that can be supported with current SNR
        // Add 3dB margin for stability
        val effectiveSnr = snrDb - 3f

        var bestMcs = 0
        for (entry in mcsTable) {
            if (effectiveSnr >= entry.minSnrDb) {
                bestMcs = entry.index
            } else {
                break
            }
        }
        return bestMcs
    }

    /**
     * Predict throughput based on network parameters.
     *
     * @param snrDb Signal-to-noise ratio in dB
     * @param wifiStandard Wi-Fi standard
     * @param channelWidth Channel width in MHz (20, 40, 80, 160)
     * @param spatialStreams Number of spatial streams (MIMO)
     * @return ThroughputPrediction with detailed breakdown
     */
    fun predictThroughput(
        snrDb: Float,
        wifiStandard: WifiStandard,
        channelWidth: Int,
        spatialStreams: Int = 1
    ): ThroughputPrediction {
        // Calculate Shannon capacity (theoretical maximum)
        val shannonCapacity = calculateShannonCapacity(channelWidth, snrDb)

        // Get appropriate MCS table and estimate MCS
        val mcsTable = getMcsTable(wifiStandard)
        val mcsIndex = estimateMcsIndex(snrDb, wifiStandard)
        val mcsEntry = mcsTable.getOrNull(mcsIndex) ?: mcsTable.first()

        // Calculate MCS-based throughput
        // Scale by channel width: 40MHz = 2x, 80MHz = 4x, 160MHz = 8x
        val channelWidthMultiplier = when (channelWidth) {
            40 -> 2.1f   // Slightly less than 2x due to guard band
            80 -> 4.2f   // Slightly less than 4x
            160 -> 8.3f  // Slightly less than 8x
            else -> 1.0f // 20 MHz
        }

        val phyRate = mcsEntry.dataRateMbps20Mhz * channelWidthMultiplier * spatialStreams

        // Apply protocol overhead for real-world estimate
        val estimatedReal = phyRate * PROTOCOL_OVERHEAD

        // Calculate confidence based on SNR margin above MCS threshold
        val snrMargin = snrDb - mcsEntry.minSnrDb
        val confidence = when {
            snrMargin >= 10f -> 0.95f
            snrMargin >= 5f -> 0.85f
            snrMargin >= 3f -> 0.75f
            snrMargin >= 0f -> 0.6f
            else -> 0.4f
        }

        return ThroughputPrediction(
            shannonCapacity = shannonCapacity,
            mcsBasedThroughput = phyRate,
            estimatedReal = estimatedReal,
            mcsIndex = mcsIndex,
            modulation = "${mcsEntry.modulation} ${mcsEntry.codingRate}",
            confidence = confidence
        )
    }

    /**
     * Predict throughput from RSSI.
     *
     * @param rssi Received signal strength in dBm
     * @param noiseFloor Noise floor in dBm (default: -95 dBm)
     * @param wifiStandard Wi-Fi standard
     * @param channelWidth Channel width in MHz
     * @param spatialStreams Number of spatial streams
     * @return ThroughputPrediction
     */
    fun predictThroughputFromRssi(
        rssi: Int,
        noiseFloor: Int = DEFAULT_NOISE_FLOOR,
        wifiStandard: WifiStandard,
        channelWidth: Int,
        spatialStreams: Int = 1
    ): ThroughputPrediction {
        val snrDb = (rssi - noiseFloor).toFloat()
        return predictThroughput(snrDb, wifiStandard, channelWidth, spatialStreams)
    }

    /**
     * Get MCS table for the given Wi-Fi standard.
     */
    private fun getMcsTable(wifiStandard: WifiStandard): List<McsEntry> {
        return when (wifiStandard) {
            WifiStandard.WIFI_6, WifiStandard.WIFI_6E -> mcsTable11ax
            WifiStandard.WIFI_5 -> mcsTable11ac
            WifiStandard.WIFI_4 -> mcsTable11n
            WifiStandard.UNKNOWN -> mcsTable11n // Fallback to most conservative
        }
    }

    /**
     * Get MCS entry details for display.
     */
    fun getMcsDetails(mcsIndex: Int, wifiStandard: WifiStandard): McsEntry? {
        return getMcsTable(wifiStandard).getOrNull(mcsIndex)
    }
}
