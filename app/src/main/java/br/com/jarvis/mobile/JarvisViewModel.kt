package br.com.jarvis.mobile

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import br.com.jarvis.mobile.actions.AppAction
import br.com.jarvis.mobile.domain.Command
import br.com.jarvis.mobile.domain.CommandParser
import br.com.jarvis.mobile.domain.ConversationGate
import br.com.jarvis.mobile.voice.AndroidSpeechInput
import br.com.jarvis.mobile.voice.SpeechInput
import br.com.jarvis.mobile.voice.SpeechOutput
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class JarvisState(
    val enabled: Boolean = false,
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val status: String = "Sistema pronto",
    val heard: String = "",
    val reply: String = "Toque em ativar e diga: Jarvis.",
    val voiceInfo: String = "Microfone desligado • escuta apenas com o app visível",
    val notice: String = "",
    val audioLevel: Float = 0f
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(JarvisState())
    val state = _state.asStateFlow()
    private val _actions = MutableSharedFlow<AppAction>(extraBufferCapacity = 1)
    val actions = _actions.asSharedFlow()
    private val handler = Handler(Looper.getMainLooper())
    private val parser = CommandParser()
    private val gate = ConversationGate(SystemClock::elapsedRealtime)
    private var input: SpeechInput? = null
    private var foreground = false
    private var failures = 0
    private val output = SpeechOutput(application) { message -> _state.update { it.copy(notice = message) } }
    private val restart = Runnable {
        if (foreground && _state.value.enabled && !_state.value.speaking) {
            _state.update { it.copy(status = "Preparando microfone", listening = false) }
            input?.start()
        }
    }

    fun onForeground() { foreground = true }
    fun onBackground() { foreground = false; stop() }

    fun start() {
        if (!foreground || _state.value.enabled) return
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionDenied(); return
        }
        failures = 0
        gate.reset()
        try {
            input?.close()
            input = AndroidSpeechInput(getApplication(),
                onReady = { _state.update { it.copy(listening = true, audioLevel = 0f, status = "Ouvindo • diga Jarvis") } },
                onResult = ::onRecognized,
                onFailure = ::onRecognitionError,
                onLevel = { rms ->
                    if (foreground && _state.value.enabled && _state.value.listening && !_state.value.speaking) {
                        val level = if (rms.isFinite()) ((rms + 2f) / 12f).coerceIn(0f, 1f) else 0f
                        _state.update { it.copy(audioLevel = level) }
                    }
                }
            )
        } catch (_: RuntimeException) {
            stop("Falha ao iniciar o serviço de voz")
            report("Verifique o serviço de reconhecimento de voz nas configurações do Android.")
            return
        }
        if (input?.available != true) {
            stop("Reconhecimento indisponível")
            report("Instale ou habilite um serviço de reconhecimento de voz compatível com pt-BR.")
            return
        }
        _state.update { it.copy(enabled = true, notice = "", voiceInfo =
            if (input?.onDevice == true) "Reconhecimento no dispositivo • pt-BR depende do modelo instalado"
            else "Serviço do Android • offline preferencial; o provedor pode usar internet") }
        scheduleListening(0)
    }

    fun stop(message: String = "Microfone desligado") {
        handler.removeCallbacks(restart)
        input?.close()
        input = null
        output.stop()
        gate.reset()
        _state.update { it.copy(enabled = false, listening = false, speaking = false, audioLevel = 0f, status = message) }
    }
    fun permissionDenied() {
        stop("Permissão de microfone necessária")
        report("Permita o microfone nas configurações do aplicativo para usar a voz. Os botões continuam disponíveis.")
    }
    fun report(message: String) { _state.update { it.copy(notice = message) } }

    fun requestAction(action: AppAction) {
        if (!foreground) return
        stop("Abrindo aplicativo")
        _actions.tryEmit(action)
    }

    private fun onRecognized(text: String) {
        if (!foreground || !_state.value.enabled) return
        failures = 0
        _state.update { it.copy(heard = text, listening = false, audioLevel = 0f) }
        val parsed = parser.parse(text)
        if (!gate.accept(parsed)) { scheduleListening(); return }
        when (val command = parsed.command) {
            Command.Wake -> respond("Às suas ordens, senhor")
            Command.OpenChatGpt -> respond("Abrindo o ChatGPT, senhor.", AppAction.OpenChatGpt)
            is Command.SetAlarm -> respond(
                "Confirme no relógio o alarme para %02d:%02d, senhor.".format(command.hour, command.minute),
                AppAction.SetAlarm(command.hour, command.minute)
            )
            Command.AskAlarmTime -> respond("Escolha o horário do alarme no relógio, senhor.", AppAction.SetAlarm())
            Command.InvalidAlarmTime -> respond("Horário inválido, senhor. Diga, por exemplo: alarme para 7 e 30.")
            Command.Help -> respond("Diga abrir ChatGPT, alarme para 7 e 30, ou parar.")
            Command.Stop -> { stop(); respond("Escuta desativada, senhor.") }
            Command.Unknown -> respond("Não entendi, senhor. Diga ajuda para ouvir os comandos.")
        }
    }

    private fun respond(text: String, action: AppAction? = null) {
        handler.removeCallbacks(restart)
        input?.stop()
        _state.update { it.copy(reply = text, listening = false, speaking = true, audioLevel = 0f, status = "JARVIS respondendo") }
        output.speak(text) {
            _state.update { it.copy(speaking = false, status = if (it.enabled) "Aguardando comando" else "Microfone desligado") }
            if (foreground) {
                if (action != null) requestAction(action)
                else { gate.extend(); scheduleListening() }
            }
        }
    }

    private fun scheduleListening(delayMillis: Long = 800L) {
        handler.removeCallbacks(restart)
        if (foreground && _state.value.enabled) handler.postDelayed(restart, delayMillis)
    }

    private fun onRecognitionError(error: Int) {
        if (!foreground || !_state.value.enabled) return
        _state.update { it.copy(listening = false, audioLevel = 0f) }
        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            _state.update { it.copy(status = "Aguardando voz • diga Jarvis") }
            scheduleListening(1_000L)
            return
        }
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) { permissionDenied(); return }
        val explanation = when (error) {
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED, SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
                "O serviço não tem o modelo pt-BR. Baixe o idioma português nas configurações de voz ou selecione outro serviço."
            SpeechRecognizer.ERROR_AUDIO -> "O microfone está indisponível. Feche outros aplicativos que estejam usando áudio."
            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                "O serviço de voz solicitou conexão. Verifique a internet ou instale o modelo pt-BR offline."
            else -> "Serviço de voz indisponível (código $error). Aguarde e ative novamente."
        }
        failures++
        if (failures >= 3 || error in setOf(SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE, SpeechRecognizer.ERROR_AUDIO)) {
            stop("Escuta interrompida")
            report(explanation)
        } else {
            report(explanation)
            _state.update { it.copy(status = "Reconectando ao serviço de voz ($failures/3)") }
            scheduleListening(1_500L * failures)
        }
    }

    override fun onCleared() { stop(); output.close(); super.onCleared() }
}
