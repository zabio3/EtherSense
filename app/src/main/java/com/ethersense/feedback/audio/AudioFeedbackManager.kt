package com.ethersense.feedback.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.sin

@Singleton
class AudioFeedbackManager @Inject constructor(
    private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var audioTrack: AudioTrack? = null
    private var continuousJob: Job? = null
    private var isEnabled = true

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
        if (!enabled) {
            stop()
        }
    }

    fun playSignalTone(rssi: Int, durationMs: Int = 100) {
        if (!isEnabled || !canPlayAudio()) return

        val frequency = mapRssiToFrequency(rssi)
        scope.launch {
            playTone(frequency, durationMs)
        }
    }

    fun startContinuousFeedback(rssiFlow: Flow<Int>) {
        if (!isEnabled) return

        continuousJob?.cancel()
        continuousJob = scope.launch {
            initAudioTrack()
            audioTrack?.play()

            rssiFlow.collect { rssi ->
                if (!isActive) return@collect
                val frequency = mapRssiToFrequency(rssi)
                val interval = mapRssiToInterval(rssi)

                generateToneBuffer(frequency, interval)?.let { buffer ->
                    audioTrack?.write(buffer, 0, buffer.size)
                }

                delay(interval.toLong())
            }
        }
    }

    fun startGeigerMode(rssiProvider: () -> Int) {
        if (!isEnabled) return

        continuousJob?.cancel()
        continuousJob = scope.launch {
            while (isActive && isEnabled) {
                val rssi = rssiProvider()
                val frequency = mapRssiToFrequency(rssi)
                val interval = mapRssiToInterval(rssi)

                playTone(frequency, CLICK_DURATION_MS)
                delay(interval.toLong())
            }
        }
    }

    fun stop() {
        continuousJob?.cancel()
        continuousJob = null
        audioTrack?.apply {
            stop()
            release()
        }
        audioTrack = null
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun mapRssiToFrequency(rssi: Int): Int {
        val normalized = ((rssi + 90).coerceIn(0, 60)) / 60f
        return (MIN_FREQUENCY + normalized * (MAX_FREQUENCY - MIN_FREQUENCY)).toInt()
    }

    private fun mapRssiToInterval(rssi: Int): Int {
        val normalized = ((rssi + 90).coerceIn(0, 60)) / 60f
        return (MAX_INTERVAL - normalized * (MAX_INTERVAL - MIN_INTERVAL)).toInt()
    }

    private fun canPlayAudio(): Boolean {
        return audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT
    }

    private fun initAudioTrack() {
        if (audioTrack != null) return

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private suspend fun playTone(frequency: Int, durationMs: Int) {
        val buffer = generateToneBuffer(frequency, durationMs) ?: return

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()

        delay(durationMs.toLong())

        track.stop()
        track.release()
    }

    private fun generateToneBuffer(frequency: Int, durationMs: Int): ShortArray? {
        val numSamples = (SAMPLE_RATE * durationMs / 1000)
        if (numSamples <= 0) return null

        val buffer = ShortArray(numSamples)
        val twoPi = 2.0 * PI

        for (i in 0 until numSamples) {
            val angle = twoPi * i * frequency / SAMPLE_RATE
            val amplitude = calculateAmplitudeWithEnvelope(i, numSamples)
            buffer[i] = (sin(angle) * amplitude * Short.MAX_VALUE).toInt().toShort()
        }

        return buffer
    }

    private fun calculateAmplitudeWithEnvelope(sample: Int, total: Int): Double {
        val position = sample.toDouble() / total
        val attackEnd = 0.1
        val releaseStart = 0.8

        return when {
            position < attackEnd -> position / attackEnd * VOLUME
            position > releaseStart -> (1 - (position - releaseStart) / (1 - releaseStart)) * VOLUME
            else -> VOLUME
        }
    }

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val MIN_FREQUENCY = 220
        private const val MAX_FREQUENCY = 880
        private const val MIN_INTERVAL = 100
        private const val MAX_INTERVAL = 1000
        private const val CLICK_DURATION_MS = 50
        private const val VOLUME = 0.5
    }
}
