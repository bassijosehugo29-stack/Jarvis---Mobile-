package br.com.jarvis.mobile.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper

/** One focus request shared by TTS and the original soundscape. No competing in-app focus owners. */
class AssistantAudioFocus(context: Context, private val onInterruption: () -> Unit = {}) {
    enum class Owner { VOICE, SOUNDS }
    private val manager = context.getSystemService(AudioManager::class.java)
    private val owners = mutableSetOf<Owner>()
    private var blocked = false
    private var granted = false
    private val request: AudioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        .setWillPauseWhenDucked(true)
        .setOnAudioFocusChangeListener({ change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
                blocked = true; granted = false; owners.clear()
                manager.abandonAudioFocusRequest(requestValue())
                onInterruption()
            }
        }, Handler(Looper.getMainLooper())).build()

    private fun requestValue(): AudioFocusRequest = request
    fun allowAfterUserAction() { blocked = false }
    fun acquire(owner: Owner): Boolean {
        if (blocked) return false
        if (!granted) granted = manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (granted) owners += owner
        return granted
    }
    fun release(owner: Owner) {
        owners -= owner
        if (owners.isEmpty() && granted) { manager.abandonAudioFocusRequest(request); granted = false }
    }
    fun close() { owners.clear(); manager.abandonAudioFocusRequest(request); granted = false }
}
