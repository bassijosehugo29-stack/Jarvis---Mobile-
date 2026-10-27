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
import androidx.lifecycle.viewModelScope
import br.com.jarvis.mobile.actions.AppAction
import br.com.jarvis.mobile.actions.CalendarSaveResult
import br.com.jarvis.mobile.actions.CalendarStore
import br.com.jarvis.mobile.actions.InstalledApps
import br.com.jarvis.mobile.actions.WritableCalendar
import br.com.jarvis.mobile.domain.*
import br.com.jarvis.mobile.voice.AndroidSpeechInput
import br.com.jarvis.mobile.voice.SpeechInput
import br.com.jarvis.mobile.voice.SpeechOutput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class JarvisState(
    val enabled: Boolean = false,
    val listening: Boolean = false,
    val speaking: Boolean = false,
    val status: String = "Sistema pronto",
    val heard: String = "",
    val reply: String = "Toque no núcleo e diga: Jarvis.",
    val voiceInfo: String = "Microfone desligado • escuta apenas com o app visível",
    val notice: String = "",
    val audioLevel: Float = 0f
)

private sealed interface PendingVoice {
    data object Alarm : PendingVoice
    data class Event(val draft: EventDraft) : PendingVoice
    data class AppChoice(val options: List<LaunchableApp>) : PendingVoice
    data class CalendarChoice(val event: CalendarEvent, val options: List<WritableCalendar>) : PendingVoice
}

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(JarvisState())
    val state = _state.asStateFlow()
    private val _actions = MutableSharedFlow<AppAction>(extraBufferCapacity = 4)
    val actions = _actions.asSharedFlow()
    private val handler = Handler(Looper.getMainLooper())
    private val parser = CommandParser()
    private val eventParser = EventParser()
    private val appResolver = AppResolver()
    private val installedApps = InstalledApps(application)
    private val calendar = CalendarStore(application)
    private val gate = ConversationGate(SystemClock::elapsedRealtime)
    private var input: SpeechInput? = null
    private var foreground = false
    private var failures = 0
    private var pending: PendingVoice? = null
    private var permissionEvent: CalendarEvent? = null
    private var permissionUntil = 0L
    private var work: Job? = null
    private var session = 0L
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
        pending = null
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
        session++
        work?.cancel(); work = null
        pending = null
        permissionEvent = null
        handler.removeCallbacks(restart)
        input?.close(); input = null
        output.stop()
        gate.reset()
        _state.update { it.copy(enabled = false, listening = false, speaking = false, audioLevel = 0f, status = message) }
    }
    fun permissionDenied() {
        stop("Permissão de microfone necessária")
        report("Permita o microfone nas configurações do aplicativo para pedir ações por voz.")
    }
    fun report(message: String) { _state.update { it.copy(notice = message) } }

    fun actionFailed(message: String) { if (foreground) respond(message) }

    private fun requestAction(action: AppAction) {
        if (!foreground) return
        stop("Abrindo aplicativo")
        if (!_actions.tryEmit(action)) report("Não consegui iniciar a ação. Peça novamente, senhor.")
    }

    private fun onRecognized(text: String) {
        if (!foreground || !_state.value.enabled || _state.value.speaking || work?.isActive == true) return
        failures = 0
        _state.update { it.copy(heard = text, listening = false, audioLevel = 0f) }
        val parsed = parser.parse(text)
        val continuing = gate.isOpen()
        if (!gate.accept(parsed)) { scheduleListening(); return }
        if (!continuing) pending = null
        if (parsed.command == Command.Unknown && pending != null) { continueRequest(text); return }
        // An explicit new request replaces an incomplete request; nothing is left queued.
        pending = null
        when (val command = parsed.command) {
            Command.Wake -> respond("Às suas ordens, senhor")
            Command.OpenChatGpt -> respond("Abrindo o ChatGPT, senhor.", AppAction.OpenChatGpt)
            is Command.OpenApp -> findApp(command.name)
            is Command.CreateEvent -> prepareEvent(command.request)
            is Command.SetAlarm -> setAlarm(command)
            Command.AskAlarmTime -> { pending = PendingVoice.Alarm; respond("Para qual horário, senhor? Diga, por exemplo, sete e trinta.") }
            Command.InvalidAlarmTime -> { pending = PendingVoice.Alarm; respond("Horário inválido, senhor. Diga, por exemplo, sete e trinta.") }
            Command.Help -> respond("Peça abrir um aplicativo pelo nome, agendar um compromisso amanhã às quinze horas, alarme para sete e trinta, ou parar.")
            Command.Stop -> { stop(); respond("Escuta desativada, senhor.") }
            Command.Unknown -> respond("Não entendi, senhor. Peça abrir um aplicativo, agendar um compromisso ou diga ajuda.")
        }
    }

    private fun continueRequest(text: String) {
        when (val request = pending) {
            PendingVoice.Alarm -> when (val command = parser.parse("alarme para $text").command) {
                is Command.SetAlarm -> { pending = null; setAlarm(command) }
                else -> respond("Não entendi o horário. Diga, por exemplo, sete e trinta, ou cancelar.")
            }
            is PendingVoice.Event -> prepareEvent(text, request.draft)
            is PendingVoice.AppChoice -> {
                val choice = choiceIndex(text, request.options.map { it.label })
                if (choice == null) respond("Diga o número de uma das opções, ou cancelar, senhor.")
                else { pending = null; val app = request.options[choice]; respond("Abrindo ${app.label}, senhor.", AppAction.OpenApp(app)) }
            }
            is PendingVoice.CalendarChoice -> {
                val choice = choiceIndex(text, request.options.map { it.label })
                if (choice == null) respond("Diga o número da agenda que deseja usar, ou cancelar, senhor.")
                else { pending = null; saveEvent(request.event, request.options[choice]) }
            }
            null -> scheduleListening()
        }
    }

    private fun setAlarm(command: Command.SetAlarm) {
        respond("Solicitando ao relógio um alarme para %02d:%02d, senhor.".format(command.hour, command.minute),
            AppAction.SetAlarm(command.hour, command.minute))
    }

    private fun findApp(name: String) = runWork("Procurando aplicativo") {
        val apps = withContext(Dispatchers.IO) { appResolver.candidates(name, installedApps.list()) }
        when (apps.size) {
            0 -> respond("Não encontrei $name entre os aplicativos disponíveis. Diga o nome que aparece no celular, senhor.")
            1 -> respond("Abrindo ${apps.single().label}, senhor.", AppAction.OpenApp(apps.single()))
            else -> {
                val options = apps.take(5)
                pending = PendingVoice.AppChoice(options)
                respond("Qual aplicativo, senhor? " + options.mapIndexed { index, app -> "${index + 1} para ${app.label}" }.joinToString(". "))
            }
        }
    }

    private fun prepareEvent(text: String, previous: EventDraft = EventDraft()) {
        val result = eventParser.parse(text, previous)
        val draft = result.draft
        pending = PendingVoice.Event(draft)
        when {
            result.error != null -> respond(result.error)
            draft.title.isBlank() -> respond("Qual é o nome do compromisso, senhor?")
            draft.date == null -> respond("Em qual dia, senhor? Diga amanhã ou dia 30 de outubro.")
            draft.time == null -> respond("A que horas, senhor? Diga, por exemplo, às quinze e trinta.")
            else -> {
                val event = eventParser.toEvent(draft)
                if (event == null) {
                    pending = PendingVoice.Event(draft.copy(date = null, time = null))
                    respond("Esse horário já passou ou é ambíguo no fuso do celular. Diga outra data e horário, senhor.")
                } else { pending = null; chooseCalendar(event) }
            }
        }
    }

    private fun chooseCalendar(event: CalendarEvent) {
        if (!calendar.hasPermission()) {
            permissionEvent = event
            respond("Para salvar na sua agenda, permita o acesso ao calendário do Android, senhor.",
                AppAction.RequestCalendarPermission)
            return
        }
        runWork("Consultando agendas") {
            val options = withContext(Dispatchers.IO) { calendar.calendars() }
            val selected = options.firstOrNull { it.id == calendar.preferredId() } ?: options.singleOrNull()
            when {
                options.isEmpty() -> respond("Não há uma agenda visível que permita gravar. Configure uma agenda no celular e peça novamente, senhor.")
                selected != null -> saveEventNow(event, selected)
                else -> {
                    val choices = options.take(5)
                    pending = PendingVoice.CalendarChoice(event, choices)
                    respond("Em qual agenda, senhor? " + choices.mapIndexed { index, item -> "${index + 1} para ${item.label}" }.joinToString(". "))
                }
            }
        }
    }

    fun onCalendarPermissionResult() {
        val event = permissionEvent
        permissionEvent = null
        if (!foreground || event == null || SystemClock.elapsedRealtime() >= permissionUntil) return
        if (!calendar.hasPermission()) {
            respond("Acesso à agenda não permitido. Não salvei o compromisso, senhor. Você pode permitir nas configurações do aplicativo.")
            return
        }
        start()
        gate.extend()
        chooseCalendar(event)
    }

    private fun saveEvent(event: CalendarEvent, target: WritableCalendar) = runWork("Salvando compromisso") {
        saveEventNow(event, target)
    }

    private suspend fun saveEventNow(event: CalendarEvent, target: WritableCalendar) {
        if (!foreground || event.startMillis <= System.currentTimeMillis()) {
            respond("O horário já passou. Peça uma nova data e horário, senhor."); return
        }
        val result = withContext(Dispatchers.IO) { calendar.save(event, target.id) }
        if (result is CalendarSaveResult.Saved) {
            calendar.remember(target.id)
            val formatter = DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm", Locale.forLanguageTag("pt-BR"))
                .withZone(ZoneId.of(event.timeZone))
            val minutes = (event.endMillis - event.startMillis) / 60_000
            respond("Agendado: ${event.title}, ${formatter.format(Instant.ofEpochMilli(event.startMillis))}, por $minutes minutos, senhor.")
        } else respond("Não consegui confirmar o salvamento. Confira sua agenda antes de repetir, para evitar duplicação, senhor.")
    }

    private fun runWork(status: String, block: suspend () -> Unit) {
        handler.removeCallbacks(restart)
        input?.stop()
        _state.update { it.copy(listening = false, audioLevel = 0f, status = status, reply = "$status…") }
        val current = session
        work = viewModelScope.launch {
            try { if (foreground && current == session) block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: SecurityException) { if (foreground && current == session) respond("O Android bloqueou o acesso. Confira as permissões nas configurações, senhor.") }
            catch (_: RuntimeException) { if (foreground && current == session) respond("Não consegui confirmar a ação. Confira o aplicativo de destino antes de repetir, senhor.") }
        }
    }

    private fun respond(text: String, action: AppAction? = null) {
        handler.removeCallbacks(restart)
        input?.stop()
        _state.update { it.copy(reply = text, listening = false, speaking = true, audioLevel = 0f, status = "JARVIS respondendo") }
        output.speak(text) {
            _state.update { it.copy(speaking = false, status = if (it.enabled) "Aguardando comando" else "Microfone desligado") }
            if (foreground) {
                if (action == AppAction.RequestCalendarPermission) {
                    val event = permissionEvent
                    stop("Permissão da agenda necessária")
                    permissionEvent = event
                    permissionUntil = SystemClock.elapsedRealtime() + 60_000L
                    _actions.tryEmit(action)
                } else if (action != null) requestAction(action)
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
            scheduleListening(1_000L); return
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
            stop("Escuta interrompida"); report(explanation)
        } else {
            report(explanation)
            _state.update { it.copy(status = "Reconectando ao serviço de voz ($failures/3)") }
            scheduleListening(1_500L * failures)
        }
    }

    override fun onCleared() { stop(); output.close(); super.onCleared() }
}
