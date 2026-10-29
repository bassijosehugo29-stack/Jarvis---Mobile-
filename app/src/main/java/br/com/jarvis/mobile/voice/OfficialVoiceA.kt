package br.com.jarvis.mobile.voice

import android.content.Context
import android.content.pm.PackageManager
import br.com.jarvis.mobile.BuildConfig

/** Identity of the user-approved model, not a replacement system voice with a lowered pitch. */
enum class VoiceProfile(val title:String){OFFICIAL_A("A · Executivo clássico"),ANDROID("Voz do Android")}
enum class VoiceAInstallation{READY,MISSING,INCOMPATIBLE}
object OfficialVoiceA {
    const val PACKAGE=BuildConfig.VOICE_ENGINE_PACKAGE
    const val VOICE="jarvis-faber-pt-BR"
    const val PROFILE="executive-a-v1"
    const val MODEL_SHA256="858555e3a064209c57088fe6bd70c4c3dc54d03eaa00c45d5ecaf43a33f95aa7"
    const val GUIDE="https://github.com/bassijosehugo29-stack/Jarvis---Mobile-/blob/fix/escuta-inteligente-20261029/docs/ESCUTA-INTELIGENTE.md"
    @Suppress("DEPRECATION")
    fun installation(context:Context):VoiceAInstallation=try{
        val pm=context.packageManager
        val info=pm.getPackageInfo(PACKAGE,PackageManager.GET_META_DATA or PackageManager.GET_PERMISSIONS)
        val meta=info.applicationInfo?.metaData
        val sameCertificate=pm.checkSignatures(context.packageName,PACKAGE)==PackageManager.SIGNATURE_MATCH
        val noCaptureOrNetwork=info.requestedPermissions.orEmpty().none{it in setOf("android.permission.INTERNET","android.permission.RECORD_AUDIO")}
        if(sameCertificate&&noCaptureOrNetwork&&meta?.getString("jarvis.voice.profile")==PROFILE&&meta.getString("jarvis.voice.model.sha256")==MODEL_SHA256&&meta.getString("jarvis.voice.log.policy")=="no-text-v1")
            VoiceAInstallation.READY else VoiceAInstallation.INCOMPATIBLE
    }catch(_:PackageManager.NameNotFoundException){VoiceAInstallation.MISSING}catch(_:RuntimeException){VoiceAInstallation.INCOMPATIBLE}
    fun unavailableMessage(context:Context)=when(installation(context)){
        VoiceAInstallation.MISSING->"Voz A em texto: instale o APK gratuito JARVIS Voz A junto desta versão do aplicativo."
        VoiceAInstallation.INCOMPATIBLE->"O motor da voz A tem assinatura ou versão incompatível. Instale os dois APKs da mesma entrega."
        VoiceAInstallation.READY->"A voz A não confirmou a síntese local. Reabra o motor e use Testar voz; a resposta permanece em texto."
    }
}
