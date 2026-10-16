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

data class SfxPresetInfo(
    val id: Int,
    val name: String,
    val description: String
)

/**
 * Low-latency real-time 8-bit sound synthesizer using Android AudioTrack.
 */
class RetroSoundSynth(private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {

    companion object {
        const val SAMPLE_RATE = 22050

        val PRESETS = listOf(
            SfxPresetInfo(0, "Laser Blaster", "Rapid downwards pitch chirp (1200 Hz down to 300 Hz)."),
            SfxPresetInfo(1, "Platform Jump", "Upwards triangle wave pitch sweep (200 Hz up to 800 Hz)."),
            SfxPresetInfo(2, "Explosion", "Heavy white noise burst with low-frequency quadratic decay rumble."),
            SfxPresetInfo(3, "Coin Pickup", "High two-tone major interval chime (987 Hz -> 1318 Hz)."),
            SfxPresetInfo(4, "Power-up", "Four-step ascending major arpeggio fanfare (C4, E4, G4, C5)."),
            SfxPresetInfo(5, "Hurt / Damage", "Crunchy descending pitch drop (380 Hz -> 60 Hz) blended with impact noise."),
            SfxPresetInfo(6, "Game Over", "Sad descending minor run (D4, Bb3, G3, Eb3) with slow tragic fade-out."),
            SfxPresetInfo(7, "Victory / Fanfare", "Triumphant arcade victory jingle (G4, C5, E5, G5, final held C6)."),
            SfxPresetInfo(8, "Blip / Select", "Snappy high-frequency micro-chirp (1600 Hz, 35ms) for UI clicks and menus."),
            SfxPresetInfo(9, "Warp / Teleport", "Sci-fi frequency-modulated vibrato sweep (200 Hz to 1100 Hz with 80 Hz LFO)."),
            SfxPresetInfo(10, "Alarm / Siren", "Piercing alternating two-tone emergency klaxon (880 Hz / 659 Hz)."),
            SfxPresetInfo(11, "Bounce / Spring", "Resonant upward spring boing (150 Hz to 550 Hz with warble envelope)."),
            SfxPresetInfo(12, "Zap / Electric Shock", "Jagged buzzing high-frequency sawtooth electric spark with jitter.")
        )
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
     * Built-in retro sound effect presets (0 to 12).
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
                    5 -> playHurt()
                    6 -> playGameOver()
                    7 -> playVictory()
                    8 -> playBlip()
                    9 -> playWarp()
                    10 -> playAlarm()
                    11 -> playBounce()
                    12 -> playZap()
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
        // Ascending major arpeggio: C4 (261.63), E4 (329.63), G4 (392.00), C5 (523.25)
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

    private fun playHurt() {
        // Crunchy descending pitch drop with noise punch
        val durationMs = 130
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val freq = 380.0 - (progress * 320.0) // 380Hz down to 60Hz
            val period = SAMPLE_RATE / freq
            val p = (i % period) / period
            val square = if (p < 0.5) 1.0 else -1.0
            val noise = (Random.nextDouble() * 2.0) - 1.0
            val combined = (square * 0.65) + (noise * 0.35)
            val env = 1.0 - progress
            buffer[i] = (combined * 19000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playGameOver() {
        // Descending tragic minor sequence: D4 (293.66), Bb3 (233.08), G3 (196.00), Eb3 (155.56)
        val notes = doubleArrayOf(293.66, 233.08, 196.00, 155.56)
        val noteDurationMs = 110
        val noteSamples = (SAMPLE_RATE * noteDurationMs) / 1000
        val totalSamples = noteSamples * notes.size
        val buffer = ShortArray(totalSamples)

        for (n in notes.indices) {
            val freq = notes[n]
            val period = SAMPLE_RATE / freq
            for (i in 0 until noteSamples) {
                val progress = i.toDouble() / noteSamples
                val p = (i % period) / period
                // Sawtooth for melancholy retro feel
                val sample = (2.0 * p) - 1.0
                val env = 1.0 - (progress * 0.7)
                buffer[n * noteSamples + i] = (sample * 16000 * env).toInt().toShort()
            }
        }
        playBuffer(buffer)
    }

    private fun playVictory() {
        // Triumphant fanfare: G4 (392.0), C5 (523.25), E5 (659.25), G5 (783.99), then held high C6 (1046.5)
        val notes = doubleArrayOf(392.00, 523.25, 659.25, 783.99, 1046.50)
        val durations = intArrayOf(70, 70, 70, 90, 200)
        val totalSamples = durations.sumOf { (SAMPLE_RATE * it) / 1000 }
        val buffer = ShortArray(totalSamples)

        var offset = 0
        for (n in notes.indices) {
            val freq = notes[n]
            val count = (SAMPLE_RATE * durations[n]) / 1000
            val period = SAMPLE_RATE / freq
            for (i in 0 until count) {
                val progress = i.toDouble() / count
                val p = (i % period) / period
                val sample = if (p < 0.5) 1.0 else -1.0
                val env = if (n == notes.lastIndex) (1.0 - (progress * 0.8)) else 1.0
                buffer[offset + i] = (sample * 16000 * env).toInt().toShort()
            }
            offset += count
        }
        playBuffer(buffer)
    }

    private fun playBlip() {
        // High crisp micro-chirp for UI clicks, selection, and typing
        val durationMs = 35
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        val freq = 1600.0
        val period = SAMPLE_RATE / freq
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val p = (i % period) / period
            val sample = if (p < 0.5) 1.0 else -1.0
            val env = 1.0 - progress
            buffer[i] = (sample * 14000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playWarp() {
        // Sci-Fi resonant frequency-modulated portal/warp sweep
        val durationMs = 320
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            // Carrier 200Hz to 1100Hz with 80Hz vibrato modulation
            val lfo = sin(2.0 * PI * 80.0 * i / SAMPLE_RATE) * 90.0
            val freq = (200.0 + (progress * 900.0) + lfo).coerceAtLeast(60.0)
            val period = SAMPLE_RATE / freq
            val p = (i % period) / period
            val sample = if (p < 0.5) (4.0 * p) - 1.0 else 3.0 - (4.0 * p) // Triangle
            val env = (1.0 - progress * 0.4)
            buffer[i] = (sample * 17000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playAlarm() {
        // Emergency alternating two-tone siren (880 Hz and 659 Hz in rapid 50ms intervals)
        val durationMs = 350
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        val cycleSamples = (SAMPLE_RATE * 50) / 1000
        for (i in 0 until totalSamples) {
            val isHighTone = ((i / cycleSamples) % 2) == 0
            val freq = if (isHighTone) 880.0 else 659.25
            val period = SAMPLE_RATE / freq
            val p = (i % period) / period
            val sample = if (p < 0.5) 1.0 else -1.0
            val env = 0.95
            buffer[i] = (sample * 17000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playBounce() {
        // Resonant boing / spring sound
        val durationMs = 220
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            // Pitch rises from 150 Hz to 550 Hz with warble
            val warble = sin(2.0 * PI * 35.0 * i / SAMPLE_RATE) * 40.0
            val freq = (150.0 + (progress * 400.0) + warble).coerceAtLeast(80.0)
            val period = SAMPLE_RATE / freq
            val p = (i % period) / period
            val sample = sin(2.0 * PI * i / period)
            val env = 1.0 - (progress * 0.8)
            buffer[i] = (sample * 18000 * env).toInt().toShort()
        }
        playBuffer(buffer)
    }

    private fun playZap() {
        // Jagged buzzing high-frequency sawtooth electric spark with jitter
        val durationMs = 180
        val totalSamples = (SAMPLE_RATE * durationMs) / 1000
        val buffer = ShortArray(totalSamples)
        for (i in 0 until totalSamples) {
            val progress = i.toDouble() / totalSamples
            val jitter = (Random.nextDouble() * 120.0) - 60.0
            val freq = (750.0 - (progress * 550.0) + jitter).coerceAtLeast(80.0)
            val period = SAMPLE_RATE / freq
            val p = (i % period) / period
            val saw = (2.0 * p) - 1.0
            val noise = (Random.nextDouble() * 2.0) - 1.0
            val combined = (saw * 0.7) + (noise * 0.3)
            val env = (1.0 - progress) * (1.0 - progress * 0.5)
            buffer[i] = (combined * 18000 * env).toInt().toShort()
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
