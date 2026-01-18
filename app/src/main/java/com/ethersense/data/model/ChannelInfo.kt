package com.ethersense.data.model

data class ChannelInfo(
    val channelNumber: Int,
    val frequencyMhz: Int,
    val band: FrequencyBand,
    val networkCount: Int,
    val utilization: Float,
    val averageRssi: Int
) {
    val isCongested: Boolean
        get() = networkCount > 3 || utilization > 0.6f

    val isRecommended: Boolean
        get() = networkCount <= 1 && utilization < 0.3f

    companion object {
        fun frequencyToChannel(frequencyMhz: Int): Int {
            return when {
                frequencyMhz in 2412..2484 -> {
                    if (frequencyMhz == 2484) 14
                    else (frequencyMhz - 2412) / 5 + 1
                }
                frequencyMhz in 5170..5825 -> (frequencyMhz - 5000) / 5
                frequencyMhz in 5955..7115 -> (frequencyMhz - 5950) / 5
                else -> 0
            }
        }

        fun channelToFrequency(channel: Int, band: FrequencyBand): Int {
            return when (band) {
                FrequencyBand.BAND_2_4_GHZ -> {
                    if (channel == 14) 2484
                    else 2412 + (channel - 1) * 5
                }
                FrequencyBand.BAND_5_GHZ -> 5000 + channel * 5
                FrequencyBand.BAND_6_GHZ -> 5950 + channel * 5
            }
        }

        val CHANNELS_2_4_GHZ = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        val NON_OVERLAPPING_2_4_GHZ = listOf(1, 6, 11)
    }
}
