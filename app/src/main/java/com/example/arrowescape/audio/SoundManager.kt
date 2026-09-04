package com.example.arrowescape.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    var soundEnabled: Boolean = true
    var hapticsEnabled: Boolean = true

    // Pentatonic scale frequencies for successive valid taps
    private val pentatonicNotes = doubleArrayOf(
        523.25, // C5
        587.33, // D5
        659.25, // E5
        783.99, // G5
        880.00, // A5
        1046.50 // C6
    )

    fun playValidTap(comboIndex: Int = 0) {
        if (hapticsEnabled) {
            triggerLightHaptic()
        }
        if (!soundEnabled) return

        scope.launch {
            // High-octane supersonic rocket escape "shurrr... whoosh!" launch sound
            val noteFreq = pentatonicNotes[comboIndex % pentatonicNotes.size]
            playRocketWhoosh(baseFreq = noteFreq)
        }
    }

    fun playInvalidTap() {
        if (hapticsEnabled) {
            triggerErrorHaptic()
        }
        if (!soundEnabled) return

        scope.launch {
            // Distinct sharp wooden/metallic "clack/collision" impact sound
            playCollisionClack()
        }
    }

    /**
     * Synthesizes a true supersonic rocket launch "Shuuuuurrrrr... Whoooosh!" audio.
     * Incorporates:
     * 1. Rocket motor ignition rumble & low burst (40-140Hz)
     * 2. Aerodynamic jet thrust air-friction sweep (dynamic bandpass noise for "SHURRR")
     * 3. Supersonic ascending Doppler frequency chirp for rocket shoot off!
     */
    private fun playRocketWhoosh(baseFreq: Double) {
        try {
            val sampleRate = 44100
            val durationMs = 280
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            var phaseTone = 0.0
            var phaseHarmonic = 0.0
            var phaseSubRumble = 0.0
            var noiseFilterState1 = 0.0
            var noiseFilterState2 = 0.0

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = t / (durationMs / 1000.0)

                // 1. Supersonic rocket pitch glide: accelerates exponentially upwards
                val freqMultiplier = 1.0 + 2.2 * (progress * progress)
                val currentFreq = baseFreq * freqMultiplier

                phaseTone += 2.0 * PI * currentFreq / sampleRate
                phaseHarmonic += 2.0 * PI * (currentFreq * 1.5) / sampleRate

                // 2. Rocket motor low-end ignition rumble (85 Hz -> 150 Hz)
                val rumbleFreq = 85.0 + 65.0 * progress
                phaseSubRumble += 2.0 * PI * rumbleFreq / sampleRate
                val rumbleWave = sin(phaseSubRumble) * (1.0 - progress).coerceAtLeast(0.0)

                // 3. Rocket thrust "SHUUUUURRRRR" aerodynamic friction noise
                val rawWhiteNoise = (Math.random() * 2.0 - 1.0)
                // 2-pole lowpass/bandpass filter tracking the rocket velocity
                val filterCutoff = 0.20 + 0.45 * progress
                noiseFilterState1 += filterCutoff * (rawWhiteNoise - noiseFilterState1)
                noiseFilterState2 += filterCutoff * (noiseFilterState1 - noiseFilterState2)

                // Thrust noise envelope: rapid attack in first 35ms, intense body, smooth fade
                val thrustNoiseEnv = when {
                    progress < 0.12 -> progress / 0.12
                    progress < 0.65 -> 1.0
                    else -> exp(-6.5 * (progress - 0.65))
                }

                // Tonal flight envelope
                val toneEnv = when {
                    progress < 0.10 -> progress / 0.10
                    progress < 0.50 -> 1.0
                    else -> exp(-5.0 * (progress - 0.50))
                }

                val synthSine = sin(phaseTone) * 0.52 + sin(phaseHarmonic) * 0.22
                val rocketAirNoise = noiseFilterState2 * thrustNoiseEnv * 0.68
                val rocketRumble = rumbleWave * (if (progress < 0.3) 0.35 else 0.0)

                val mixed = (synthSine * toneEnv + rocketAirNoise + rocketRumble) * Short.MAX_VALUE * 0.92
                buffer[i] = mixed.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            playBuffer(buffer, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    /**
     * Synthesizes a high-impact wooden block / snap collision ("ladne ki aawaz")
     * when an arrow hits an obstacle and rebounds.
     */
    private fun playCollisionClack() {
        try {
            val sampleRate = 44100
            val durationMs = 140
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val progress = t / (durationMs / 1000.0)
                // Fast dropping pitch simulating a physical knock/bonk (from 380Hz down to 110Hz)
                val currentFreq = 380.0 * exp(-18.0 * progress) + 110.0
                // High transient attack with steep exponential decay (crisp impact snap)
                val impactEnv = exp(-32.0 * t)
                // Low body resonance
                val bodyEnv = exp(-15.0 * t) * 0.45

                phase += 2.0 * PI * currentFreq / sampleRate
                val noisePop = if (i < (sampleRate * 0.008)) (Math.random() * 2.0 - 1.0) * 0.6 else 0.0
                val primaryTone = sin(phase) * impactEnv
                val overtone = 0.4 * sin(phase * 2.5) * bodyEnv

                val sample = ((primaryTone + overtone + noisePop) * Short.MAX_VALUE * 0.85).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            playBuffer(buffer, sampleRate, durationMs)
        } catch (_: Exception) {}
    }

    private fun playBuffer(buffer: ShortArray, sampleRate: Int, durationMs: Int) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            Thread {
                try {
                    Thread.sleep(durationMs.toLong() + 60)
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {}
    }

    fun playHeartLoss() {
        if (hapticsEnabled) {
            triggerErrorHaptic()
        }
        if (!soundEnabled) return

        scope.launch {
            playTone(320.0, durationMs = 80, decayRate = 20.0)
            kotlinx.coroutines.delay(70)
            playTone(220.0, durationMs = 150, decayRate = 15.0)
        }
    }

    fun playWinSound() {
        if (hapticsEnabled) {
            triggerSuccessHaptic()
        }
        if (!soundEnabled) return

        scope.launch {
            val arpeggio = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
            for (freq in arpeggio) {
                playTone(freq, durationMs = 180, decayRate = 10.0)
                kotlinx.coroutines.delay(110)
            }
        }
    }

    fun playStarSound(starIndex: Int) {
        if (hapticsEnabled) {
            triggerLightHaptic()
        }
        if (!soundEnabled) return

        val starFreq = when (starIndex) {
            1 -> 659.25  // E5
            2 -> 880.00  // A5
            3 -> 1318.51 // E6 (brilliant sparkle)
            else -> 1046.50
        }

        scope.launch {
            playTone(starFreq, durationMs = 150, decayRate = 8.0)
        }
    }

    fun playScoreTick() {
        if (!soundEnabled) return
        scope.launch {
            playTone(987.77, durationMs = 30, decayRate = 45.0)
        }
    }

    fun playShuffle() {
        if (hapticsEnabled) {
            triggerLightHaptic()
        }
        if (!soundEnabled) return

        scope.launch {
            playTone(440.0, durationMs = 60, decayRate = 25.0)
            kotlinx.coroutines.delay(40)
            playTone(550.0, durationMs = 80, decayRate = 20.0)
        }
    }

    fun playHint() {
        if (hapticsEnabled) {
            triggerLightHaptic()
        }
        if (!soundEnabled) return

        scope.launch {
            playTone(880.0, durationMs = 120, decayRate = 12.0)
            kotlinx.coroutines.delay(80)
            playTone(1174.66, durationMs = 200, decayRate = 10.0)
        }
    }

    private fun triggerLightHaptic() {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    it.vibrate(VibrationEffect.createOneShot(18, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(18)
                }
            }
        } catch (_: Exception) {}
    }

    private fun triggerErrorHaptic() {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 35, 40, 35)
                    val amplitudes = intArrayOf(0, 200, 0, 180)
                    it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(80)
                }
            }
        } catch (_: Exception) {}
    }

    private fun triggerSuccessHaptic() {
        try {
            vibrator?.let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 40, 50, 60, 50, 80)
                    val amplitudes = intArrayOf(0, 150, 0, 180, 0, 220)
                    it.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    it.vibrate(150)
                }
            }
        } catch (_: Exception) {}
    }

    private fun playTone(frequency: Double, durationMs: Int, decayRate: Double) {
        try {
            val sampleRate = 44100
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val buffer = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val envelope = exp(-decayRate * t)
                // Fundamental + soft harmonic for pleasant marimba resonance
                val fundamental = sin(2.0 * PI * frequency * t)
                val overtone = 0.25 * sin(2.0 * PI * (frequency * 2.0) * t)
                val sample = ((fundamental + overtone) * envelope * Short.MAX_VALUE * 0.7).toInt()
                buffer[i] = sample.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }

            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            // Release after playing
            Thread {
                try {
                    Thread.sleep(durationMs.toLong() + 50)
                    audioTrack.stop()
                    audioTrack.release()
                } catch (_: Exception) {}
            }.start()
        } catch (_: Exception) {}
    }
}
