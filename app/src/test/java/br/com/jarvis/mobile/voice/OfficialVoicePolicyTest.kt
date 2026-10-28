package br.com.jarvis.mobile.voice

import br.com.jarvis.mobile.experience.*
import org.junit.Assert.*
import org.junit.Test

class OfficialVoicePolicyTest {
    @Test fun approvalSelectsExactIdentityWithoutArtificialPitchReduction(){
        val selected=ExperienceSettings().sanitized()
        assertEquals(VoiceProfile.OFFICIAL_A,selected.voiceProfile)
        assertEquals(1f,selected.pitch,0f);assertEquals(1f,selected.speechRate,0f)
        assertEquals("jarvis-faber-pt-BR",OfficialVoiceA.VOICE)
        assertEquals("858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7",OfficialVoiceA.MODEL_SHA256)
        assertEquals(1f,selected.copy(pitch=0.7f).sanitized().pitch,0f)
    }
    @Test fun alternativeAndroidVoiceKeepsOwnControlsButSilentOverridesBothProfiles(){
        val alternative=ExperienceSettings(voiceProfile=VoiceProfile.ANDROID,pitch=0.83f).sanitized()
        assertEquals(0.83f,alternative.pitch,0f)
        VoiceProfile.entries.forEach{assertFalse(ExperienceSettings(voiceProfile=it,mode=OperationMode.SILENT).voiceAllowed)}
    }
}
