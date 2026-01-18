package com.ethersense.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Long.toFormattedTime(): String {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(this))
}

fun Int.toSignalBars(): Int {
    return when {
        this >= -50 -> 4
        this >= -60 -> 3
        this >= -70 -> 2
        else -> 1
    }
}

fun Float.toPercentageString(): String {
    return "${(this * 100).toInt()}%"
}

fun Int.toFrequencyBandString(): String {
    return when {
        this < 3000 -> "2.4 GHz"
        this < 6000 -> "5 GHz"
        else -> "6 GHz"
    }
}

fun Int.toChannelWidthString(): String {
    return "${this}MHz"
}
