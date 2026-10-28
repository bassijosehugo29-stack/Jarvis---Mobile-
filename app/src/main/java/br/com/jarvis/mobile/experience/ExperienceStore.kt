package br.com.jarvis.mobile.experience

import android.content.Context
import br.com.jarvis.mobile.voice.VoiceProfile
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.experienceDataStore by preferencesDataStore(name = "jarvis_experience")

class ExperienceStore(context: Context, private val store: DataStore<Preferences> = context.applicationContext.experienceDataStore) {
    val settings = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map(::decode)

    suspend fun update(change: (ExperienceSettings) -> ExperienceSettings) {
        store.edit { p ->
            val s = change(decode(p)).sanitized()
            p[mode] = s.mode.name; p[voice] = s.voiceEnabled; p[effects] = s.effectsEnabled
            p[assistantMusic] = s.assistantMusic; p[cinemaMusic] = s.cinematicMusic
            p[voiceVolume] = s.voiceVolume; p[musicVolume] = s.musicVolume; p[effectsVolume] = s.effectsVolume
            p[profile] = s.voiceProfile.name; p[rate] = s.speechRate; p[pitch] = s.pitch; p[engine] = s.enginePackage; p[voiceName] = s.voiceName
        }
    }

    private fun decode(p: Preferences): ExperienceSettings {
        val defaults = ExperienceSettings()
        val selected=VoiceProfile.entries.firstOrNull{it.name==p[profile]}?:VoiceProfile.OFFICIAL_A
        val migrated=p[profile]==null
        return ExperienceSettings(
            mode = OperationMode.entries.firstOrNull { it.name == p[mode] } ?: defaults.mode,
            voiceEnabled = p[voice] ?: defaults.voiceEnabled, effectsEnabled = p[effects] ?: defaults.effectsEnabled,
            assistantMusic = p[assistantMusic] ?: defaults.assistantMusic, cinematicMusic = p[cinemaMusic] ?: defaults.cinematicMusic,
            voiceVolume = p[voiceVolume] ?: defaults.voiceVolume, musicVolume = p[musicVolume] ?: defaults.musicVolume,
            effectsVolume = p[effectsVolume] ?: defaults.effectsVolume, speechRate = p[rate] ?: defaults.speechRate,
            pitch = p[pitch] ?: defaults.pitch, enginePackage = p[engine].orEmpty(), voiceName = p[voiceName].orEmpty()
        ).sanitized()
    }
    private companion object {
        val profile = stringPreferencesKey("voiceProfile"); val mode = stringPreferencesKey("mode"); val voice = booleanPreferencesKey("voice")
        val effects = booleanPreferencesKey("effects"); val assistantMusic = booleanPreferencesKey("assistant_music")
        val cinemaMusic = booleanPreferencesKey("cinema_music"); val voiceVolume = floatPreferencesKey("voice_volume")
        val musicVolume = floatPreferencesKey("music_volume"); val effectsVolume = floatPreferencesKey("effects_volume")
        val rate = floatPreferencesKey("speech_rate"); val pitch = floatPreferencesKey("pitch")
        val engine = stringPreferencesKey("engine"); val voiceName = stringPreferencesKey("voice_name")
    }
}
