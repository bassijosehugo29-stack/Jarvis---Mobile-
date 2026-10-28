package br.com.jarvis.mobile.audio

import kotlin.math.*

enum class SoundCue { BOOT, LISTEN, PROCESS, CONFIRM, ERROR }

/** Original synthesis written for this project. No samples, recordings or reference-video audio. */
object OriginalScore {
    const val SAMPLE_RATE = 22050
    const val LOOP_SECONDS = 16

    fun ambient(): ShortArray = render(LOOP_SECONDS.toDouble()) { t, duration ->
        val swell = 0.55 + 0.45 * sin(PI * t / duration).pow(2)
        val pad = sin(2 * PI * 110 * t) * 0.20 + sin(2 * PI * 165 * t) * 0.12 +
            sin(2 * PI * 220 * t + 0.3 * sin(2 * PI * t / 8)) * 0.08
        val beat = t % 1.0
        val pulse = sin(2 * PI * 440 * t) * exp(-beat * 12) * 0.06
        val shimmer = sin(2 * PI * 880 * t) * (0.5 + 0.5 * sin(2 * PI * t / 4)) * 0.015
        (pad * swell + pulse + shimmer) * 0.65
    }

    fun cue(cue: SoundCue): ShortArray {
        val duration = when (cue) { SoundCue.BOOT -> 0.80; SoundCue.LISTEN -> 0.18
            SoundCue.PROCESS -> 1.60; SoundCue.CONFIRM -> 0.36; SoundCue.ERROR -> 0.38 }
        return render(duration) { t, d ->
            val f = when (cue) {
                SoundCue.BOOT -> 220.0 + 440 * t / d
                SoundCue.LISTEN -> 660.0
                SoundCue.PROCESS -> 330.0
                SoundCue.CONFIRM -> if (t < d / 2) 523.25 else 783.99
                SoundCue.ERROR -> if (t < d / 2) 246.94 else 220.0
            }
            val envelope = if (cue == SoundCue.PROCESS) 0.20 * (0.5 + 0.5 * cos(2 * PI * t / 0.4)).pow(4)
                else 0.32 * sin(PI * t / d).pow(2)
            val cycles = when (cue) {
                SoundCue.BOOT -> 220 * t + 220 * t * t / d
                SoundCue.CONFIRM -> if (t < d / 2) 523.25 * t else 523.25 * d / 2 + 783.99 * (t - d / 2)
                SoundCue.ERROR -> if (t < d / 2) 246.94 * t else 246.94 * d / 2 + 220 * (t - d / 2)
                else -> f * t
            }
            (sin(2 * PI * cycles) + 0.15 * sin(4 * PI * cycles)) * envelope
        }
    }

    private fun render(seconds: Double, sample: (Double, Double) -> Double): ShortArray {
        val count = (seconds * SAMPLE_RATE).roundToInt()
        return ShortArray(count) { index ->
            val t = index.toDouble() / SAMPLE_RATE
            // Edges reach exactly zero, avoiding clicks at start, stop and loop boundaries.
            val edge = min(index, count - 1 - index).toDouble() / (SAMPLE_RATE * 0.025)
            val fade = (0.5 - 0.5 * cos(PI * edge.coerceIn(0.0, 1.0)))
            (sample(t, seconds) * fade * Short.MAX_VALUE).roundToInt().coerceIn(-32767, 32767).toShort()
        }
    }
}
