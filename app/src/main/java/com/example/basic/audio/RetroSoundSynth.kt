package com.example.basic.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Waveform types for retro 8-bit sound generation.
 */
enum class WaveType {
    SQUARE,
    TRIANGLE,
    SAWTOOTH,
    SINE,
    NOISE
}

/**
 * Low-latency real-time 8-bit sound synthesizer using Android AudioTrack.
 */
class RetroSoundSynth(private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {

    companion object {
        const val SAMPLE_RATE = 22050
    }

    /**
     * Plays a synthesized tone with frequency, duration in milliseconds, and wave type.
     */
    fun playTone(freqHz: Double, durationMs: Long, waveType: WaveType = WaveType.SQUARE) {
        if (freqHz <= 10.0 || durationMs <= 0) return

        scope.launch {
            try {
                val clampedMs = durationMs.coerceIn(5, 5000)
                val totalSamples = ((SAMPLE_RATE * clampedMs) / 1000).toInt()
                val buffer = ShortArray(totalSamples)

                val period = SAMPLE_RATE / freqHz
                var phase = 0.0

                for (i in 0 until totalSamples) {
                    // Attack and decay envelope to prevent popping
                    val attackSamples = (SAMPLE_RATE * 0.005).toInt()
                    val decaySamples = (SAMPLE_RATE * 0.010).toInt()
                    val env = when {
                        i < attackSamples -> i.toDouble() / attackSamples
                        i > totalSamples - decaySamples -> (totalSamples - i).toDouble() / decaySamples
                        else -> 1.0
                    }

                    val sampleVal: Double = when (waveType) {
                        WaveType.SQUARE -> {
                            val p = (i % period) / period
                            if (p < 0.5) 1.0 else -1.0
                        }
                        WaveType.TRIANGLE -> {
                            val p = (i % period) / period
                            if (p < 0.5) (4.0 * p) - 1.0 else 3.0 - (4.0 * p)
                        }
                        WaveType.SAWTOOTH -> {
                            val p = (i % period) / period
                            (2.0 * p) - 1.0
                        }
                        WaveType.SINE -> {
                            sin(2.0 * PI * i / period)
                        }
                        WaveType.NOISE -> {
                            (Random.nextDouble() * 2.0) - 1.0
                        }
                    }

                    buffer[i] = (sampleVal * 16000 * env).toInt().toShort()
                }

                playBuffer(buffer)
            } catch (e: Exception) {
                // Audio synthesis error fallback
            }
        }
    }

    /**
     * Built-in retro sound effect presets (Laser, Jump, Explosion, Coin, Powerup).
     */
    fun playSfx(preset: Int) {
        scope.launch {
            try {
                when (preset) {
                    0 -> playLaser()
                    1 -> playJump()
                    2 -> playExplosion()
                    3 -> playCoin()
                    4 -> playPowerup()
                    else -> playLaser()
                }
            } catch (e: Exception) {
                // Ignore audio play errors
            }
        }
    }

    private fun playLaser() {
        val durationMs = 150
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 1200.0 - (progress * 900.0) // 1200Hz down to 300Hz
            val period = SAMPLE_RATE / freq
            val p = (i % period) / period
            val sample = if (p < 0.5) 1.0 else -1.0
            val env = 1.0 - progress
            buffer[i] = (sample * 18000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playJump() {
        val durationMs = 180
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 200.0 + (progress * 600.0) // 200Hz up to 800Hz
            val period = SAMPLE_RATE / freq
            val p = (i % period) / period
            val sample = if (p < 0.5) (4.0 * p) - 1.0 else 3.0 - (4.0 * p) // Triangle
            val env = 1.0 - (progress * 0.5)
            buffer[i] = (sample * 18000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playExplosion() {
        val durationMs = 350
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val noise = (Random.nextDouble() * 2.0) - 1.0
            val env = (1.0 - progress) * (1.0 - progress)
            buffer[i] = (noise * 20000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playCoin() {
        // High dual ping: B5 (987Hz) then E6 (1318Hz)
        val part1Samples = (SAMPLE_RATE * 80) / 1000
        val part2Samples = (SAMPLE_RATE * 200) / 1000
        val totalSamples = part1Samples + part2Samples
        val buffer = ShortArray(totalSamples)

        for (i in 0 until part1Samples) {
            val period = SAMPLE_RATE / 987.0
            val p = (i % period) / period
            val sample = if (p < 0.5) 1.0 else -1.0
            buffer[i] = (sample * 16000).toInt().toShort()
        }
        for (i in 0 until part2Samples) {
            val period = SAMPLE_RATE / 1318.0
            val p = (i % period) / period
            val sample = if (p < 0.5) 1.0 else -1.0
            val env = 1.0 - (i.toDouble() / part2Samples)
            buffer[part1Samples + i] = (sample * 16000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playPowerup() {
        // Arpeggio C4, E4, G4, C5
        val notes = doubleArrayOf(261.63, 329.63, 392.00, 523.25)
        val noteSamples = (SAMPLE_RATE * 70) / 1000
        val totalSamples = noteSamples * notes.size
        val buffer = ShortArray(totalSamples)

        for (n in notes.indices) {
            val freq = notes[n]
            val period = SAMPLE_RATE / freq
            for (i in 0 until noteSamples) {
                val p = (i % period) / period
                val sample = if (p < 0.5) 1.0 else -1.0
                buffer[n * noteSamples + i] = (sample * 15000).toInt().toShort()
            }
        }
        playBuffer(buffer)
    }

    private fun playBuffer(buffer: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
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
            .setBufferSizeInBytes(buffer.size.coerceAtLeast(minBufferSize) * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        // Release track after playback completes
        scope.launch {
            kotlinx.coroutines.delay((buffer.size * 1000L / SAMPLE_RATE) + 100)
            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                // Ignore cleanup error
            }
        }
    }
}
