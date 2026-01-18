package com.ethersense.domain.analyzer

import com.ethersense.data.model.ChannelInfo
import com.ethersense.data.model.FrequencyBand
import com.ethersense.data.model.WifiNetwork
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow

@Singleton
class ChannelAnalyzer @Inject constructor() {

    fun calculateInterference(
        target: WifiNetwork,
        allNetworks: List<WifiNetwork>
    ): Float {
        val others = allNetworks.filter { it.bssid != target.bssid }
        if (others.isEmpty()) return 0f

        var interferenceSum = 0.0

        for (network in others) {
            val overlap = calculateChannelOverlap(
                targetChannel = target.channel,
                targetWidth = target.channelWidth,
                otherChannel = network.channel,
                otherWidth = network.channelWidth,
                band = target.frequencyBand
            )

            if (overlap > 0) {
                val relativeStrength = rssiToLinear(network.rssi) / rssiToLinear(target.rssi)
                interferenceSum += overlap * relativeStrength
            }
        }

        return sigmoid(interferenceSum).coerceIn(0f, 1f)
    }

    fun getChannelUtilization(
        channel: Int,
        allNetworks: List<WifiNetwork>
    ): Float {
        val networksOnChannel = allNetworks.filter { network ->
            val overlap = calculateChannelOverlap(
                targetChannel = channel,
                targetWidth = 20,
                otherChannel = network.channel,
                otherWidth = network.channelWidth,
                band = network.frequencyBand
            )
            overlap > 0
        }

        if (networksOnChannel.isEmpty()) return 0f

        val utilizationScore = networksOnChannel.size / MAX_EXPECTED_NETWORKS.toFloat()
        return utilizationScore.coerceIn(0f, 1f)
    }

    fun analyzeAllChannels(
        networks: List<WifiNetwork>,
        band: FrequencyBand
    ): List<ChannelInfo> {
        val channels = when (band) {
            FrequencyBand.BAND_2_4_GHZ -> ChannelInfo.CHANNELS_2_4_GHZ
            FrequencyBand.BAND_5_GHZ -> CHANNELS_5_GHZ
            FrequencyBand.BAND_6_GHZ -> CHANNELS_6_GHZ
        }

        return channels.map { channel ->
            val networksOnChannel = networks.filter { network ->
                network.frequencyBand == band && isChannelOverlapping(
                    channel, network.channel, network.channelWidth, band
                )
            }

            val avgRssi = if (networksOnChannel.isNotEmpty()) {
                networksOnChannel.map { it.rssi }.average().toInt()
            } else {
                -100
            }

            ChannelInfo(
                channelNumber = channel,
                frequencyMhz = ChannelInfo.channelToFrequency(channel, band),
                band = band,
                networkCount = networksOnChannel.size,
                utilization = getChannelUtilization(channel, networks),
                averageRssi = avgRssi
            )
        }
    }

    fun findLeastCongestedChannel(
        networks: List<WifiNetwork>,
        band: FrequencyBand
    ): Int {
        val channels = analyzeAllChannels(networks, band)
        val nonOverlapping = when (band) {
            FrequencyBand.BAND_2_4_GHZ -> channels.filter {
                it.channelNumber in ChannelInfo.NON_OVERLAPPING_2_4_GHZ
            }
            else -> channels
        }

        return nonOverlapping.minByOrNull { it.networkCount }?.channelNumber
            ?: channels.first().channelNumber
    }

    private fun calculateChannelOverlap(
        targetChannel: Int,
        targetWidth: Int,
        otherChannel: Int,
        otherWidth: Int,
        band: FrequencyBand
    ): Float {
        if (band == FrequencyBand.BAND_2_4_GHZ) {
            return calculate2_4GhzOverlap(targetChannel, otherChannel)
        }

        val targetStart = targetChannel - (targetWidth / 10)
        val targetEnd = targetChannel + (targetWidth / 10)
        val otherStart = otherChannel - (otherWidth / 10)
        val otherEnd = otherChannel + (otherWidth / 10)

        val overlapStart = maxOf(targetStart, otherStart)
        val overlapEnd = minOf(targetEnd, otherEnd)

        return if (overlapEnd > overlapStart) {
            (overlapEnd - overlapStart).toFloat() / (targetEnd - targetStart)
        } else {
            0f
        }
    }

    private fun calculate2_4GhzOverlap(channel1: Int, channel2: Int): Float {
        val separation = abs(channel1 - channel2)
        return when {
            separation == 0 -> 1.0f
            separation == 1 -> 0.75f
            separation == 2 -> 0.5f
            separation == 3 -> 0.25f
            separation == 4 -> 0.1f
            else -> 0f
        }
    }

    private fun isChannelOverlapping(
        channel: Int,
        networkChannel: Int,
        networkWidth: Int,
        band: FrequencyBand
    ): Boolean {
        return calculateChannelOverlap(
            channel, 20, networkChannel, networkWidth, band
        ) > 0
    }

    private fun rssiToLinear(rssi: Int): Double {
        return 10.0.pow(rssi / 10.0)
    }

    private fun sigmoid(x: Double): Float {
        return (2.0 / (1.0 + exp(-x)) - 1.0).toFloat()
    }

    companion object {
        private const val MAX_EXPECTED_NETWORKS = 10

        private val CHANNELS_5_GHZ = listOf(
            36, 40, 44, 48, 52, 56, 60, 64,
            100, 104, 108, 112, 116, 120, 124, 128, 132, 136, 140, 144,
            149, 153, 157, 161, 165
        )

        private val CHANNELS_6_GHZ = (1..233 step 4).toList()
    }
}
