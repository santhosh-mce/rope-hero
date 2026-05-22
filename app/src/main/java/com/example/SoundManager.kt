package com.example

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.VibrationEffect
import android.os.Vibrator
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import kotlin.math.sin

class SoundManager(private val context: Context) {

    var sfxEnabled = true
    var bgmEnabled = true
    var vibrateEnabled = true

    private val poolScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var bgmJob: Job? = null
    private var isPlayingBgm = false

    // Cyberpunk synth arpeggio patterns
    private val bgmNotes = intArrayOf(
        220, 220, 261, 293, 220, 220, 329, 293, // A3, A3, C4, D4, A3, A3, E4, D4
        220, 220, 261, 293, 392, 349, 329, 261  // A3, A3, C4, D4, G4, F4, E4, C4
    )

    init {
        // Load preferences if any, but default to true
        val prefs = context.getSharedPreferences("RopeHeroPrefs", Context.MODE_PRIVATE)
        sfxEnabled = prefs.getBoolean("sfx", true)
        bgmEnabled = prefs.getBoolean("bgm", true)
        vibrateEnabled = prefs.getBoolean("vibrate", true)
    }

    fun saveSettings() {
        val prefs = context.getSharedPreferences("RopeHeroPrefs", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("sfx", sfxEnabled)
            putBoolean("bgm", bgmEnabled)
            putBoolean("vibrate", vibrateEnabled)
            apply()
        }
    }

    // Vibrator Helper with complete backwards compatibility
    @Suppress("DEPRECATION")
    fun vibrate(durationMs: Long) {
        if (!vibrateEnabled) return
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            durationMs,
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    vibrator.vibrate(durationMs)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Dynamic Tone Synthesizer using AudioTrack
    // This allows generating 100% pure audio effects at runtime!
    private fun synthesizeTone(
        frequencyStart: Float,
        frequencyEnd: Float,
        durationMs: Int,
        amplitude: Short = 8000,
        isWaveformSquare: Boolean = false
    ) {
        if (!sfxEnabled) return
        poolScope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val signal = ShortArray(numSamples)

                var phase = 0.0
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val currentFrequency = frequencyStart + (frequencyEnd - frequencyStart) * progress
                    val phaseIncrement = 2.0 * Math.PI * currentFrequency / sampleRate
                    phase += phaseIncrement

                    val value = if (isWaveformSquare) {
                        if (sin(phase) >= 0) amplitude else (-amplitude).toShort()
                    } else {
                        (sin(phase) * amplitude).toInt().toShort()
                    }
                    signal[i] = value
                }

                val bufferSize = numSamples * 2
                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize,
                    AudioTrack.MODE_STATIC
                )

                audioTrack.write(signal, 0, signal.size)
                audioTrack.play()
                
                // Allow the track to play, then release it
                delay(durationMs + 100L)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Play classic swing/rope shoot sound (rising pitch)
    fun playSwingShoot() {
        synthesizeTone(300f, 900f, 120, 6000, isWaveformSquare = false)
        vibrate(30)
    }

    // Play swing release sound (falling pitch)
    fun playSwingRelease() {
        synthesizeTone(600f, 200f, 100, 5000, isWaveformSquare = false)
        vibrate(15)
    }

    // Play coin grab sound (musical double plink)
    fun playCoinCollect() {
        poolScope.launch {
            synthesizeTone(987.77f, 987.77f, 70, 7000, isWaveformSquare = false) // B5
            delay(75)
            synthesizeTone(1318.51f, 1318.51f, 120, 7000, isWaveformSquare = false) // E6
        }
        vibrate(20)
    }

    // Play powerup collect sound (arpeggio loop)
    fun playPowerupCollect() {
        poolScope.launch {
            synthesizeTone(523.25f, 523.25f, 60, 6000) // C5
            delay(60)
            synthesizeTone(659.25f, 659.25f, 60, 6000) // E5
            delay(60)
            synthesizeTone(783.99f, 783.99f, 60, 6000) // G5
            delay(60)
            synthesizeTone(1046.50f, 1046.50f, 120, 6000) // C6
        }
        vibrate(40)
    }

    // Play hit / explosion sound (deep crash / decaying white noise wave)
    fun playHitExplosion() {
        // Deep decaying explosion tone
        synthesizeTone(200f, 40f, 350, 12000, isWaveformSquare = true)
        vibrate(150)
    }

    // Start Retro Synthesized Cyberpunk Background Music Loop
    fun startBgm() {
        if (isPlayingBgm) return
        isPlayingBgm = true
        bgmJob = poolScope.launch {
            while (isActive) {
                if (!bgmEnabled) {
                    delay(500)
                    continue
                }
                for (note in bgmNotes) {
                    if (!bgmEnabled || !isActive) break
                    // Synthesize short synth bass notes for background music
                    val noteFreq = note.toFloat()
                    playSimpleSynthNote(noteFreq, 140, 4500)
                    delay(180) // Tempo timing between steps
                }
            }
        }
    }

    // Stop Background Music
    fun stopBgm() {
        isPlayingBgm = false
        bgmJob?.cancel()
        bgmJob = null
    }

    private fun playSimpleSynthNote(frequency: Float, durationMs: Int, amplitude: Short) {
        if (!bgmEnabled) return
        try {
            val sampleRate = 11025
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val signal = ShortArray(numSamples)

            var phase = 0.0
            for (i in 0 until numSamples) {
                val phaseIncrement = 2.0 * Math.PI * frequency / sampleRate
                phase += phaseIncrement
                
                // Pure synth retro sound (mix of triangle/sub-sine)
                val rawValue = sin(phase)
                // Add soft distortion for retro feel
                val value = (rawValue * amplitude).toInt().toShort()
                signal[i] = value
            }

            val bufferSize = numSamples * 2
            val audioTrack = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STATIC
            )

            audioTrack.write(signal, 0, signal.size)
            audioTrack.play()
            
            CoroutineScope(Dispatchers.Default).launch {
                delay(durationMs + 50L)
                try {
                    audioTrack.stop()
                    audioTrack.release()
                } catch (e: Exception) {}
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
