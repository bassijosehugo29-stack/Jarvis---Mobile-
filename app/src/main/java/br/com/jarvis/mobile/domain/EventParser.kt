package br.com.jarvis.mobile.domain

import java.text.Normalizer
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

data class CalendarEvent(val title: String, val startMillis: Long, val endMillis: Long, val timeZone: String)
data class EventDraft(val title: String = "", val date: LocalDate? = null, val time: LocalTime? = null,
    val durationMinutes: Int = 60)
data class EventParseResult(val draft: EventDraft, val error: String? = null)

/** Explicit dates/times only. Questions fill missing fields; unsupported recurrence is never saved. */
class EventParser(private val clock: Clock = Clock.systemDefaultZone()) {
    fun parse(text: String, previous: EventDraft = EventDraft()): EventParseResult {
        val raw = Normalizer.normalize(text.take(512), Normalizer.Form.NFC)
        val folded = Normalizer.normalize(raw.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
        val ranges = mutableListOf<IntRange>()
        if (Regex("\\b(todo|toda|todos|todas|recorrente|semanalmente|mensalmente|diariamente|daqui|fuso)\\b|a cada").containsMatchIn(folded))
            return EventParseResult(previous, "Nesta versão, agendo um compromisso por vez. Diga uma data e um horário, senhor.")
        var date = previous.date
        val today = LocalDate.now(clock)
        val relative = Regex("\\b(?:(?:para|em)\\s+)?(depois de amanha|amanha|hoje)\\b").find(folded)
        val numeric = Regex("\\b(?:para\\s+)?(?:dia\\s+)?([0-9]{1,2})\\s*/\\s*([0-9]{1,2})(?:\\s*/\\s*([0-9]{2,4}))?\\b").find(folded)
        val months = listOf("janeiro", "fevereiro", "marco", "abril", "maio", "junho", "julho", "agosto", "setembro", "outubro", "novembro", "dezembro")
        val named = Regex("\\b(?:para\\s+)?(?:dia\\s+)?([0-9]{1,2})\\s+de\\s+(" + months.joinToString("|") + ")(?:\\s+de\\s+([0-9]{4}))?\\b").find(folded)
        val weekdays = listOf("segunda", "terca", "quarta", "quinta", "sexta", "sabado", "domingo")
        val weekday = Regex("\\b(?:(?:na|no|para)\\s+)?(?:proxim[ao]\\s+)?(" + weekdays.joinToString("|") + ")(?:[ -]feira)?\\b").find(folded)
        if (Regex("\\b(depois de amanha|amanha|hoje)\\b").findAll(folded).count() > 1 ||
            Regex("\\b(?:as|pelas)\\s+").findAll(folded).count() > 1 ||
            listOf(relative, numeric, named, weekday).count { it != null } > 1)
            return EventParseResult(previous, "Ouvi mais de uma data ou horário. Diga apenas uma data e um horário, senhor.")
        try {
            when {
                relative != null -> {
                    date = today.plusDays(when (relative.groupValues[1]) { "hoje" -> 0L; "amanha" -> 1L; else -> 2L })
                    ranges += relative.range
                }
                numeric != null || named != null -> {
                    val m = numeric ?: named!!
                    val month = if (numeric != null) m.groupValues[2].toInt() else months.indexOf(m.groupValues[2]) + 1
                    val explicitYear = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()
                    if (explicitYear != null && explicitYear < 100) return EventParseResult(previous, "Diga o ano com quatro números, senhor.")
                    var candidate = LocalDate.of(explicitYear ?: today.year, month, m.groupValues[1].toInt())
                    if (explicitYear == null && candidate < today) candidate = candidate.plusYears(1)
                    date = candidate; ranges += m.range
                }
                weekday != null -> {
                    val target = DayOfWeek.of(weekdays.indexOf(weekday.groupValues[1]) + 1)
                    var days = (target.value - today.dayOfWeek.value + 7) % 7
                    if (days == 0) days = 7
                    date = today.plusDays(days.toLong()); ranges += weekday.range
                }
                Regex("\\b(dia|semana|mes|ano)\\b").containsMatchIn(folded) ->
                    return EventParseResult(previous, "Diga a data como amanhã ou dia 30 de outubro, senhor.")
            }
        } catch (_: java.time.DateTimeException) {
            return EventParseResult(previous, "Essa data não existe. Diga uma data válida, senhor.")
        }
        val duration = Regex("\\b(?:por|com duracao de)\\s+(meia|$SPOKEN_NUMBER)\\s+(minutos?|horas?)(?:\\s+e\\s+(meia|$SPOKEN_NUMBER)(?:\\s+minutos?)?)?\\b").find(folded)
        var durationMinutes = previous.durationMinutes
        if (duration != null) {
            val hours = duration.groupValues[2].startsWith("hora")
            val half = duration.groupValues[1] == "meia"
            val extra = when (val value = duration.groupValues[3]) { "" -> 0; "meia" -> 30; else -> spokenNumber(value) ?: -1 }
            if ((half && (!hours || extra != 0)) || extra !in 0..59 || (!hours && extra != 0))
                return EventParseResult(previous, "Diga a duração em minutos, entre 1 e 720, senhor.")
            val amount = spokenNumber(duration.groupValues[1]) ?: 0
            durationMinutes = if (half) 30 else amount * if (hours) 60 else 1
            durationMinutes += extra
            if (durationMinutes !in 1..720 || Regex("^\\s+e\\b").containsMatchIn(folded.substring(duration.range.last + 1)))
                return EventParseResult(previous, "Diga a duração em minutos, entre 1 e 720, senhor.")
            ranges += duration.range
        } else if (Regex("\\bduracao\\b|\\bpor\\s+(?:[-+]?\\s*[0-9]|menos\\b|meia\\b|$SPOKEN_NUMBER\\b)").containsMatchIn(folded)) {
            return EventParseResult(previous, "Diga a duração, por exemplo: por 30 minutos, senhor.")
        }
        // Hide the duration and date while searching so they cannot become an appointment time.
        val searchable = folded.toCharArray()
        for (range in ranges) for (index in range) searchable[index] = ' '
        val timeText = String(searchable)
        val time = readClock(timeText, standalone = previous.title.isNotBlank() && previous.date != null && previous.time == null)
        if (time.error) return EventParseResult(previous, "Não entendi o horário. Diga, por exemplo, às quinze e trinta, senhor.")
        time.range?.let { ranges += it }
        val titleChars = raw.toCharArray()
        for (range in ranges) for (index in range) titleChars[index] = ' '
        var title = String(titleChars)
            .replace(Regex("\\bjarvis\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\s*[,.:; -]*\\s*(?:por favor\\s+)?(?:agende|agendar|marque|marcar|anote|anotar|crie|criar|coloque|colocar)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\b(?:na|em minha|na minha)\\s+agenda\\b", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\s*(?:um|uma|o|a)\\s+", RegexOption.IGNORE_CASE), "")
            .replace(Regex("^\\s*(?:compromisso|evento)(?:\\s+(?:chamado|de))?\\s*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\s+"), " ").trim(' ', ',', '.', ':', ';', '-', '!', '?')
            .replace(Regex("(?:\\s+)?por favor$", RegexOption.IGNORE_CASE), "").trim()
        if (voiceText(title) in setOf("", "as", "para", "para as", "na agenda", "agende", "agendar")) title = previous.title
        else if (previous.title.isNotBlank()) title = previous.title
        return EventParseResult(EventDraft(title.take(150), date, time.time ?: previous.time, durationMinutes))
    }

    fun toEvent(draft: EventDraft): CalendarEvent? {
        if (draft.title.isBlank() || draft.date == null || draft.time == null) return null
        val zone = clock.zone
        val local = LocalDateTime.of(draft.date, draft.time)
        val offsets = zone.rules.getValidOffsets(local)
        if (offsets.size != 1) return null
        val start = local.toInstant(offsets.single()).toEpochMilli()
        if (start <= clock.millis()) return null
        return CalendarEvent(draft.title, start, start + draft.durationMinutes * 60_000L, zone.id)
    }
}
