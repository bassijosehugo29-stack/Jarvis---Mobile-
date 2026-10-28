package br.com.jarvis.mobile

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.net.Uri
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
import br.com.jarvis.mobile.conversation.LocalConversation
import br.com.jarvis.mobile.conversation.ModelStore
import br.com.jarvis.mobile.voice.AndroidSpeechInput
import br.com.jarvis.mobile.voice.SpeechInput
import br.com.jarvis.mobile.voice.SpeechOutput
import br.com.jarvis.mobile.voice.SpeechResult
import br.com.jarvis.mobile.voice.VoiceEngine
import br.com.jarvis.mobile.voice.VoiceOption
import br.com.jarvis.mobile.audio.AssistantAudioFocus
import br.com.jarvis.mobile.audio.Soundscape
import br.com.jarvis.mobile.audio.SoundCue
import br.com.jarvis.mobile.experience.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
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
    val reply: String = "Às suas ordens, senhor. Ative o núcleo para falar ou escreva uma mensagem.",
    val voiceInfo: String = "Microfone desligado • escuta apenas com o app visível",
    val notice: String = "",
    val audioLevel: Float = 0f,
    val thinking: Boolean = false,
    val speechInfo: String = "Use Testar voz nos ajustes para conferir o som.",
    val voiceIssue: String = "",
    val modelReady: Boolean = false,
    val modelBusy: Boolean = false,
    val modelInfo: String = "Prepare a conversa offline nos ajustes • download gratuito de 491 MB",
    val experience: ExperienceSettings = ExperienceSettings(),
    val preferencesReady: Boolean = false,
    val initializing: Boolean = true,
    val visualError: Boolean = false,
    val voicePlaying: Boolean = false,
    val speechProgress: Float = 0f,
    val speechPulse: Int = 0,
    val engines: List<VoiceEngine> = emptyList(),
    val voices: List<VoiceOption> = emptyList()
) {
    val corePhase get() = when {
        visualError -> CorePhase.ERROR
        initializing -> CorePhase.INITIALIZING
        listening -> CorePhase.LISTENING
        voicePlaying -> CorePhase.RESPONDING
        thinking || speaking -> CorePhase.PROCESSING
        else -> CorePhase.IDLE
    }
    val soundPhase get() = when {
        listening -> CorePhase.LISTENING
        speaking -> CorePhase.RESPONDING
        thinking -> CorePhase.PROCESSING
        else -> corePhase
    }
}

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
    private val models = ModelStore(application)
    private val conversation = LocalConversation(models)
    private var modelMonitor: Job? = null
    private var preparation: Job? = null
    private var permissionResumeVoice = false
    private val gate = ConversationGate(SystemClock::elapsedRealtime)
    private var input: SpeechInput? = null
    private var foreground = false
    private var failures = 0
    private var pending: PendingVoice? = null
    private var permissionEvent: CalendarEvent? = null
    private var permissionUntil = 0L
    private var work: Job? = null
    private var session = 0L
    private val preferences = ExperienceStore(application)
    private var pendingPreferenceWrites = 0
    private val audioFocus: AssistantAudioFocus = AssistantAudioFocus(application) {
        stop("Áudio pausado pelo Android")
        soundscape.stop()
        report("Outro áudio interrompeu o JARVIS. Toque no núcleo para retomar quando desejar.")
    }
    private val soundscape: Soundscape = Soundscape(audioFocus, onError = ::report)
    private val clearVisualError = Runnable { _state.update { it.copy(visualError = false) } }
    private val finishInitialization = Runnable { _state.update { it.copy(initializing = false) } }
    private val output: SpeechOutput = SpeechOutput(application, onStatus = { status ->
        _state.update { it.copy(speechInfo = status.message, voiceIssue = if (status.failure) status.message else "") }
        if (status.failure) signalError()
    }, audioSession = audioFocus,
        onVoices = { options -> _state.update { it.copy(voices = options) } },
        onPlayback = { playing -> _state.update { it.copy(voicePlaying = playing) } },
        onRange = { _, end, length -> _state.update { it.copy(speechPulse = it.speechPulse + 1,
            speechProgress = if (length > 0) end.toFloat() / length else 0f) } })
    init {
        viewModelScope.launch {
            preferences.settings.collect { value ->
                // An older disk emission must never re-enable sound after a local switch to Silent.
                if (pendingPreferenceWrites == 0) {
                    output.configure(value)
                    _state.update { it.copy(experience = value, preferencesReady = true) }
                }
            }
        }
        viewModelScope.launch {
            state.map { Triple(it.experience, it.soundPhase, it.preferencesReady) }.distinctUntilChanged().collect { (settings, phase, ready) ->
                soundscape.update(settings, phase, foreground && ready)
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(engines = output.engines()) }
            handler.postDelayed(finishInitialization, 1_200L)
        }
        viewModelScope.launch { soundscape.prepare() }
        viewModelScope.launch {
            models.state.collect { model ->
                _state.update { it.copy(modelReady = model.ready, modelBusy = model.downloading, modelInfo = model.message) }
            }
        }
    }
    private val restart = Runnable {
        if (foreground && _state.value.enabled && !_state.value.speaking && !_state.value.thinking) {
            _state.update { it.copy(status = "Preparando microfone", listening = false) }
            // This cue ends before recognition starts; never play effects into an active microphone.
            soundscape.update(_state.value.experience, CorePhase.LISTENING, foreground)
            input?.start()
        }
    }
    private val prepareListening = Runnable {
        if (foreground && _state.value.enabled && !_state.value.speaking && !_state.value.thinking &&
            _state.value.experience.mode != OperationMode.SILENT) {
            soundscape.cue(SoundCue.LISTEN)
            handler.postDelayed(restart, 240L)
        }
    }

    fun onForeground() {
        foreground = true
        audioFocus.allowAfterUserAction()
        syncSoundscape()
        modelMonitor?.cancel()
        modelMonitor = viewModelScope.launch {
            while (isActive && foreground) {
                try { models.refresh() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: RuntimeException) { models.reportFailure("Não foi possível conferir o modelo. Verifique armazenamento e Wi-Fi.") }
                delay(1_500L)
            }
        }
    }
    fun onBackground() { foreground = false; modelMonitor?.cancel(); stop(); soundscape.stop() }

    private fun syncSoundscape() = _state.value.let { soundscape.update(it.experience, it.soundPhase, foreground && it.preferencesReady) }
    private fun signalError() {
        handler.removeCallbacks(clearVisualError)
        _state.update { it.copy(visualError = true) }
        handler.postDelayed(clearVisualError, 3_500L)
    }
    fun updateExperience(change: (ExperienceSettings) -> ExperienceSettings) {
        val previous = _state.value.experience
        val next = change(previous).sanitized()
        if (next.mode != previous.mode || _state.value.speaking) {
            soundscape.stop(); stop("Preferências atualizadas")
        }
        if (next.mode != previous.mode) {
            handler.removeCallbacks(finishInitialization)
            _state.update { it.copy(initializing = next.mode == OperationMode.CINEMATIC) }
            if (next.mode == OperationMode.CINEMATIC) handler.postDelayed(finishInitialization, 1_200L)
        }
        audioFocus.allowAfterUserAction()
        output.configure(next)
        _state.update { it.copy(experience = next, preferencesReady = true, voiceIssue = if (next.voiceAllowed) it.voiceIssue else "") }
        syncSoundscape()
        pendingPreferenceWrites++
        viewModelScope.launch {
            try { preferences.update(change) }
            catch (_: java.io.IOException) { report("A escolha vale nesta sessão, mas não consegui salvá-la. Confira o armazenamento.") }
            finally { pendingPreferenceWrites-- }
        }
    }
    fun loadVoices() { if (foreground) output.loadVoices() }

    fun testVoice() {
        if (!foreground) return
        if (!_state.value.experience.voiceAllowed) { report("A voz está desativada. Escolha Assistente ou Cinema e ative Respostas faladas para testar."); return }
        audioFocus.allowAfterUserAction()
        work?.cancel(); conversation.cancel(); pending = null
        respond("Teste de voz do JARVIS. Às suas ordens, senhor.")
    }
    fun voiceSettings() { requestAction(AppAction.OpenVoiceSettings) }
    fun sendMessage(message: String) { audioFocus.allowAfterUserAction(); handleText(message.take(480), fromVoice = false) }
    fun downloadConversation() {
        if (!foreground || preparation?.isActive == true) return
        preparation = viewModelScope.launch {
            try { models.download() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { models.reportFailure(error.message ?: "Não foi possível iniciar o download. Confira o Wi-Fi e o armazenamento.") }
        }
    }
    fun cancelModelPreparation() {
        preparation?.cancel()
        preparation = viewModelScope.launch { models.cancelDownload() }
    }
    fun importConversation(uri: Uri) {
        if (preparation?.isActive == true) return
        preparation = viewModelScope.launch {
            try { models.importModel(uri) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { models.reportFailure(error.message ?: "Não foi possível importar o modelo oficial.") }
        }
    }

    fun start() {
        if (!foreground || _state.value.enabled || !_state.value.preferencesReady) return
        audioFocus.allowAfterUserAction()
        if (ContextCompat.checkSelfPermission(getApplication(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionDenied(); return
        }
        failures = 0
        if (_state.value.experience.mode != OperationMode.SILENT || !gate.isOpen()) { gate.reset(); pending = null }
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
        gate.extend()
        if (_state.value.experience.mode == OperationMode.SILENT) {
            _state.update { it.copy(reply = "Ouvindo uma solicitação. A resposta será exibida em texto.") }
            handler.post(restart)
        } else respond("Às suas ordens, senhor")
    }

    fun stop(message: String = "Microfone desligado") {
        session++
        work?.cancel(); work = null
        conversation.release()
        pending = null
        permissionEvent = null
        handler.removeCallbacks(restart)
        handler.removeCallbacks(prepareListening)
        input?.close(); input = null
        output.stop()
        gate.reset()
        _state.update { it.copy(enabled = false, listening = false, speaking = false, voicePlaying = false,
            speechProgress = 0f, audioLevel = 0f, thinking = false, status = message) }
    }
    fun permissionDenied() {
        stop("Permissão de microfone necessária")
        report("Permita o microfone nas configurações do aplicativo para pedir ações por voz.")
        signalError()
    }
    fun report(message: String) { _state.update { it.copy(notice = message) } }

    fun actionFailed(message: String) { if (foreground) respond(message) }

    private fun requestAction(action: AppAction) {
        if (!foreground) return
        stop("Abrindo aplicativo")
        if (!_actions.tryEmit(action)) report("Não consegui iniciar a ação. Peça novamente, senhor.")
    }

    private fun onRecognized(text: String) { handleText(text, fromVoice = true) }
    private fun handleText(text: String, fromVoice: Boolean) {
        if (!foreground || text.isBlank() || (fromVoice && !_state.value.enabled) || _state.value.speaking || work?.isActive == true) return
        failures = 0
        if (_state.value.experience.mode == OperationMode.SILENT && _state.value.enabled) {
            input?.close(); input = null
            _state.update { it.copy(enabled = false) }
        }
        _state.update { it.copy(heard = text, listening = false, audioLevel = 0f) }
        val parsed = parser.parse(text)
        val continuing = gate.isOpen()
        gate.extend()
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
            Command.Help -> respond("Você pode conversar comigo depois de preparar o modelo offline, abrir aplicativos pelo nome, agendar compromissos, definir alarmes ou pedir para parar.")
            Command.PrepareConversation -> { downloadConversation(); respond("Solicitei o modelo gratuito de conversa. São 491 megabytes, por Wi-Fi. Você pode acompanhar pelos ajustes, senhor.") }
            Command.Stop -> { stop(); respond("Escuta desativada, senhor.") }
            Command.Unknown -> converse(text)
        }
    }

    private fun converse(message: String) {
        if (models.readyFile() == null) {
            respond(if (_state.value.modelBusy) "O modelo de conversa ainda está sendo preparado. Acompanhe o progresso pelos ajustes, senhor."
                else "Para conversarmos sobre outros assuntos, prepare a conversa offline nos ajustes. O modelo é gratuito e ocupa 491 megabytes, senhor.")
            return
        }
        runWork("Pensando") {
            _state.update { it.copy(thinking = true) }
            val deadline = Runnable { conversation.cancel() }
            handler.postDelayed(deadline, 120_000L)
            try {
                val answer = conversation.answer(message)
                if (foreground) { _state.update { it.copy(thinking = false) }; respond(answer) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: LinkageError) {
                if (foreground) { _state.update { it.copy(thinking = false) }; respond("Não foi possível iniciar o motor de conversa nesta instalação. Instale o APK completo compatível com seu aparelho, senhor.") }
            }
            catch (error: Exception) {
                if (foreground) {
                    _state.update { it.copy(thinking = false) }
                    respond(if (error.message == "NOT_ENOUGH_MEMORY") "Faltou memória para conversar. Feche outros aplicativos e tente novamente, senhor."
                        else "Não consegui gerar a resposta local. Confira o modelo nos ajustes e tente uma pergunta curta, senhor.")
                }
            } finally { handler.removeCallbacks(deadline) }
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
            permissionResumeVoice = _state.value.enabled
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
        if (permissionResumeVoice) start()
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
        handler.removeCallbacks(prepareListening)
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
        handler.removeCallbacks(prepareListening)
        input?.stop()
        _state.update { it.copy(reply = text, listening = false, speaking = true, thinking = false, audioLevel = 0f,
            speechProgress = 0f, status = "JARVIS respondendo") }
        // Begin ducking before synthesis/playback starts, not after the first spoken sample.
        soundscape.update(_state.value.experience, CorePhase.RESPONDING, foreground && _state.value.preferencesReady)
        output.speak(text) { result ->
            _state.update { it.copy(speaking = false, status = when {
                result is SpeechResult.Failed -> "Falha na saída de voz"
                it.enabled -> "Aguardando sua fala"
                else -> "Microfone desligado"
            }) }
            if (foreground) {
                if (action == AppAction.RequestCalendarPermission) {
                    val event = permissionEvent
                    stop("Permissão da agenda necessária")
                    permissionEvent = event
                    permissionUntil = SystemClock.elapsedRealtime() + 60_000L
                    _actions.tryEmit(action)
                } else if (action != null) requestAction(action)
                else { gate.extend(); if (result is SpeechResult.Completed) soundscape.cue(SoundCue.CONFIRM); scheduleListening() }
            }
        }
    }

    private fun scheduleListening(delayMillis: Long = 800L) {
        handler.removeCallbacks(restart)
        handler.removeCallbacks(prepareListening)
        if (foreground && _state.value.enabled && _state.value.experience.mode != OperationMode.SILENT) {
            syncSoundscape()
            handler.postDelayed(prepareListening, delayMillis.coerceAtLeast(300L) - 240L)
        }
    }

    private fun onRecognitionError(error: Int) {
        if (!foreground || !_state.value.enabled) return
        _state.update { it.copy(listening = false, audioLevel = 0f) }
        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) { permissionDenied(); return }
        if (_state.value.experience.mode == OperationMode.SILENT) {
            input?.close(); input = null
            _state.update { it.copy(enabled = false, status = "Escuta concluída") }
            report("Não captei a solicitação. Toque no núcleo para tentar de novo ou escreva a mensagem.")
            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) signalError()
            return
        }
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
            stop("Escuta interrompida"); report(explanation); signalError()
        } else {
            report(explanation)
            _state.update { it.copy(status = "Reconectando ao serviço de voz ($failures/3)") }
            scheduleListening(1_500L * failures)
        }
    }

    override fun onCleared() { stop(); handler.removeCallbacks(clearVisualError); handler.removeCallbacks(finishInitialization);
        soundscape.close(); output.close(); audioFocus.close(); super.onCleared() }
}
