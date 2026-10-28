package br.com.jarvis.mobile

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.SideEffect
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.withResumed
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.jarvis.mobile.actions.AndroidActions
import br.com.jarvis.mobile.actions.AppAction
import br.com.jarvis.mobile.brain.BrainViewModel
import br.com.jarvis.mobile.domain.Command
import br.com.jarvis.mobile.hub.*
import br.com.jarvis.mobile.ui.*
import com.google.android.gms.auth.api.identity.Identity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : FragmentActivity() {
    private val model: JarvisViewModel by viewModels()
    private val hub: HubViewModel by viewModels()
    private val brain: BrainViewModel by viewModels()
    private val actions = AndroidActions()
    private var noteMicrophone = false
    private var readCommand: Command = Command.Agenda()
    private var unlockAction: (() -> Unit)? = null
    private var unlockCancelled: (() -> Unit)? = null
    private var authEpoch = -1L
    private var encryptedExport: ByteArray? = null
    private val requestMicrophone = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) resumed { if (noteMicrophone) model.dictateNote() else model.start() } else model.permissionDenied()
    }
    private val requestCalendar = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resumed { model.onCalendarPermissionResult() } }
    private val requestCalendarRead = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> resumed {
        if (granted) { model.moduleStarted(); hub.handle(readCommand) }
        else { model.moduleFinished(); hub.notice("Leitura do calendário não autorizada. Nenhum compromisso foi inventado.") }
    } }
    private val importConversation = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::importConversation) }
    private val importBrain = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if (uri!=null) resumed {
        unlock({ brain.unlocked(); brain.selectImport(uri); hub.navigate(HubPanel.BRAIN) })
    } }
    private val exportBrain = registerForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val bytes=encryptedExport; encryptedExport=null
        if (uri!=null && bytes!=null) lifecycleScope.launch {
            try { withContext(Dispatchers.IO) { requireNotNull(contentResolver.openOutputStream(uri,"w")).use { it.write(bytes) } }; hub.notice("Backup criptografado exportado. Guarde a senha fora do JARVIS.") }
            catch (_: Exception) { hub.notice("Não foi possível gravar o backup no destino escolhido.") }
            finally { bytes.fill(0) }
        } else bytes?.fill(0)
    }
    private val confirmCredential = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> resumed {
        if (result.resultCode==RESULT_OK) authenticationSucceeded() else authenticationCancelled()
    } }
    private val gmailAuthorization = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result -> resumed {
        try {
            if (result.resultCode!=RESULT_OK) { hub.notice("Autorização do Gmail cancelada. A conta não foi conectada."); return@resumed }
            val authorization=Identity.getAuthorizationClient(this).getAuthorizationResultFromIntent(result.data)
            hub.acceptAuthorization(authorization,authEpoch)
        } catch (_: Exception) { hub.notice("Não foi possível autorizar. Confira a configuração OAuth e a assinatura do APK no guia do Gmail.") }
    } }
    private fun resumed(action:()->Unit) { lifecycleScope.launch { lifecycle.withResumed { action() } } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        volumeControlStream = AudioManager.STREAM_MUSIC
        enableEdgeToEdge(statusBarStyle=SystemBarStyle.dark(Color.TRANSPARENT),navigationBarStyle=SystemBarStyle.dark(Color.TRANSPARENT))
        model.installDigestPolicy(hub::shouldAutoDigest)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { model.actions.collect { action ->
                    if(action==AppAction.RequestCalendarPermission) requestCalendar.launch(arrayOf(Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR))
                    else actions.execute(this@MainActivity,action)?.let(model::actionFailed)
                } }
                launch { model.modules.collect(hub::handle) }
                launch { model.noteDrafts.collect { text -> unlock({brain.unlocked();brain.suggest(text);hub.navigate(HubPanel.BRAIN)}) } }
                launch { brain.exports.collect { bytes -> encryptedExport=bytes;exportBrain.launch("JARVIS-SecondBrain-${java.time.LocalDate.now()}.jarvisbackup") } }
                launch { hub.events.collect { event -> when(event){
                    is HubEvent.Speak -> model.moduleReply(event.text,event.privateData)
                    is HubEvent.CalendarPermission -> { model.stop();readCommand=event.command;requestCalendarRead.launch(Manifest.permission.READ_CALENDAR) }
                    is HubEvent.Unlock -> {
                        model.stop()
                        unlock({brain.unlocked();when(event.command){
                            Command.OpenBrain -> hub.navigate(HubPanel.BRAIN)
                            Command.AddNote -> {hub.navigate(HubPanel.BRAIN);brain.edit()}
                            else -> {model.moduleStarted();hub.handle(event.command)}
                        }}, {if(event.command is Command.Digest)hub.digestWithoutNotes()else hub.notice("Memória bloqueada. Nenhuma nota foi consultada.")})
                    }
                    HubEvent.BrainChanged -> brain.reload()
                    HubEvent.DataErased -> {model.erasePrivateState();brain.background();brain.foreground()}
                    HubEvent.Finished -> model.moduleFinished()
                } } }
            }
        }
        setContent {
            JarvisTheme {
                val state=model.state.collectAsStateWithLifecycle().value
                val central=hub.state.collectAsStateWithLifecycle().value
                val memory=brain.state.collectAsStateWithLifecycle().value
                val secure=central.panel in setOf(HubPanel.BRAIN,HubPanel.EMAIL,HubPanel.DIGEST) || state.privateReply || state.privateDictation
                SideEffect { if(secure)window.addFlags(WindowManager.LayoutParams.FLAG_SECURE) else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
                val back={model.stop();hub.cancelWork();if(memory.draft!=null)brain.closeEditor()else hub.navigate(HubPanel.HOME)}
                BackHandler(central.panel!=HubPanel.HOME,onBack=back)
                val toggle={
                    if(state.enabled||state.thinking||state.speaking||state.working){model.stop();hub.cancelWork()}
                    else {noteMicrophone=false;if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)model.start()else requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)}
                }
                val modeChange: (br.com.jarvis.mobile.experience.ExperienceSettings)->Unit={value->
                    if(value.mode!=state.experience.mode)hub.cancelWork()
                    model.updateExperience{value}
                }
                if(central.panel==HubPanel.HOME) JarvisScreen(
                    state=state,onToggle=toggle,
                    onSettings={startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:$packageName")))},
                    onVoiceTest=model::testVoice,onVoiceSettings=model::voiceSettings,onDownloadModel=model::downloadConversation,
                    onImportModel={importConversation.launch(arrayOf("*/*"))},onSendMessage=model::sendMessage,onCancelModel=model::cancelModelPreparation,
                    onExperienceChange=modeChange,onLoadVoices=model::loadVoices,
                    onOpenHub={model.stop();hub.navigate(HubPanel.MENU)}
                ) else HubScreen(central,state,memory,hub,
                    BrainActions(
                        unlock={unlock({brain.unlocked()})},edit=brain::edit,save=brain::save,delete=brain::delete,close=brain::closeEditor,
                        export=brain::export,importBackup={importBrain.launch(arrayOf("*/*"))},preview=brain::preview,confirm=brain::confirmImport,cancel=brain::cancelImport,
                        dictate={if(hub.data.brain.isUnlocked()) {noteMicrophone=true;if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)model.dictateNote()else requestMicrophone.launch(Manifest.permission.RECORD_AUDIO)}}),
                    onBack=back,onNavigate={panel->model.stop();hub.navigate(panel)},onToggle=toggle,onMode=modeChange,
                    onGmail=::connectGmail,onArticle={url->model.stop();try{val uri=Uri.parse(url);require(uri.scheme=="https"&&uri.host!=null);startActivity(Intent(Intent.ACTION_VIEW,uri))}catch(_:Exception){hub.notice("Não foi possível abrir a matéria original.")}},
                    onErase={model.stop();unlock({hub.erasePersonalData()},force=true)})
            }
        }
    }
    private fun connectGmail(){
        model.stop();authEpoch=hub.data.storage.epoch()
        lifecycleScope.launch{
            try{
                val request=hub.data.gmail.request(hub.data.gmail.connectedAccount())
                Identity.getAuthorizationClient(this@MainActivity).authorize(request)
                    .addOnSuccessListener { result -> if(result.hasResolution()) {
                        val intent=result.pendingIntent
                        if(intent!=null)gmailAuthorization.launch(IntentSenderRequest.Builder(intent.intentSender).build())
                        else hub.notice("O Google não forneceu a autorização necessária.")
                    }else hub.acceptAuthorization(result,authEpoch) }
                    .addOnFailureListener { hub.notice("Gmail ainda não autorizado. Registre o cliente OAuth com o pacote e SHA-1 deste APK; consulte o guia no repositório. Não é necessário informar sua senha ao JARVIS.") }
            }catch(_:Exception){hub.notice("Serviços Google indisponíveis ou configuração OAuth pendente.")}
        }
    }
    private fun unlock(success:()->Unit,cancel:()->Unit={},force:Boolean=false){
        if(!force&&hub.data.brain.isUnlocked()){success();return}
        model.stop();unlockAction=success;unlockCancelled=cancel
        val keyguard=getSystemService(KeyguardManager::class.java)
        if(!keyguard.isDeviceSecure){brain.error("Configure PIN, padrão ou senha de bloqueio no Android para proteger sua memória.");hub.notice("Configure o bloqueio de tela do Android antes de abrir dados protegidos.");authenticationCancelled();return}
        if(Build.VERSION.SDK_INT>=30){
            val prompt=BiometricPrompt(this,ContextCompat.getMainExecutor(this),object:BiometricPrompt.AuthenticationCallback(){
                override fun onAuthenticationSucceeded(result:BiometricPrompt.AuthenticationResult){resumed{authenticationSucceeded()}}
                override fun onAuthenticationError(errorCode:Int,errString:CharSequence){authenticationCancelled()}
            })
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle("Desbloquear dados do JARVIS")
                .setSubtitle("Use sua biometria ou a credencial do celular")
                .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL).build())
        }else{
            @Suppress("DEPRECATION") val intent=keyguard.createConfirmDeviceCredentialIntent("JARVIS · dados pessoais","Confirme o bloqueio do celular")
            if(intent!=null)confirmCredential.launch(intent)else authenticationCancelled()
        }
    }
    private fun authenticationSucceeded(){val action=unlockAction;unlockAction=null;unlockCancelled=null;hub.data.brain.unlock();action?.invoke()}
    private fun authenticationCancelled(){val cancel=unlockCancelled;unlockAction=null;unlockCancelled=null;cancel?.invoke()}
    override fun onStart(){super.onStart();model.onForeground();hub.onForeground();brain.foreground()}
    override fun onStop(){model.onBackground();hub.onBackground();brain.background();super.onStop()}
}
