package br.com.jarvis.mobile.voice

import kotlin.math.sqrt

/** Bounded 20 ms PCM16 envelope. Stores levels only, never speech samples or text. */
class PcmEnvelope {
    private val levels = ArrayList<Float>()
    private var window = 441
    private var energy = 0.0
    private var count = 0
    private var supported = false
    private var pendingByte: Int? = null
    @Synchronized fun reset(sampleRate: Int = 0, channels: Int = 1, pcm16: Boolean = true) {
        levels.clear(); energy = 0.0; count = 0; pendingByte = null
        supported = pcm16 && sampleRate in 8000..192000 && channels in 1..2
        window = (sampleRate / 50 * channels).coerceAtLeast(1)
    }
    @Synchronized fun append(bytes: ByteArray) {
        if (!supported || levels.size >= 9000) return
        var i = 0
        while (i < bytes.size && levels.size < 9000) {
            val low = pendingByte
            if (low == null) { pendingByte = bytes[i].toInt() and 255; i++; continue }
            val sample = (low or (bytes[i].toInt() shl 8)).toShort().toInt() / 32768.0
            pendingByte = null; energy += sample * sample; count++; i++
            if (count >= window) {
                levels += (sqrt(energy / count).toFloat() * 4.5f).coerceIn(0f, 1f)
                count = 0; energy = 0.0
            }
        }
    }
    @Synchronized fun levelAt(elapsedMillis: Long): Float? {
        if (!supported || elapsedMillis < 0) return null
        return levels.getOrNull((elapsedMillis / 20).coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
    }
}
