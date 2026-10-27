package br.com.jarvis.mobile.domain

import java.time.LocalTime

internal const val SPOKEN_NUMBER = "(?:[0-9]{1,3}|(?:vinte|trinta|quarenta|cinquenta)(?:\\s+e\\s+(?:um|uma|dois|duas|tres|quatro|cinco|seis|sete|oito|nove))?|sessenta|dezesseis|dezasseis|dezessete|dezoito|dezenove|quatorze|catorze|quinze|treze|doze|onze|zero|uma|um|duas|dois|tres|quatro|cinco|seis|sete|oito|nove|dez)"

internal data class ClockReading(val time: LocalTime?, val range: IntRange?, val error: Boolean = false)

/** Small deterministic pt-BR grammar, shared by alarm and calendar; no invented time on failure. */
internal fun readClock(text: String, standalone: Boolean = false): ClockReading {
    val marker = Regex("\\b(?:as|pelas)\\s+").find(text)
    val explicit = Regex("(?<![0-9/])(?:[0-9]{1,3}\\s*[:h]|$SPOKEN_NUMBER\\s+horas?\\b)").find(text)
    val start = marker?.range?.last?.plus(1) ?: explicit?.range?.first ?: if (standalone) 0 else return ClockReading(null, null)
    val number = "(?:meio\\s+dia|meia\\s+noite|$SPOKEN_NUMBER)"
    val minute = "(?:meia|um\\s+quarto|$SPOKEN_NUMBER)"
    val pattern = Regex("^($number)(?:\\s*[:h]\\s*([0-9]{1,3})|\\s*(?:horas?\\b|h\\b)(?:\\s*(?:e\\s+)?($minute)(?:\\s+minutos?)?)?|\\s+e\\s+($minute)(?:\\s+minutos?)?)?(?:\\s+(da\\s+manha|da\\s+tarde|da\\s+noite))?(?![a-z0-9])")
    val match = pattern.find(text.substring(start)) ?: return ClockReading(null, marker?.range, true)
    val end = start + match.value.length
    val rest = text.substring(end).trimStart()
    // Reject partial matches such as 7 30, 7:300, or 7 e quarenta e cinco e dez.
    if (Regex("^(?:[0-9:]|e\\b|minutos?\\b|meia\\b)").containsMatchIn(rest)) return ClockReading(null, null, true)
    var hour = when (match.groupValues[1]) {
        "meio dia" -> 12
        "meia noite" -> 0
        else -> spokenNumber(match.groupValues[1]) ?: return ClockReading(null, null, true)
    }
    val minuteText = match.groupValues.slice(2..4).firstOrNull { it.isNotBlank() }
    val minutes = when (minuteText) {
        null -> 0
        "meia" -> 30
        "um quarto" -> 15
        else -> spokenNumber(minuteText) ?: return ClockReading(null, null, true)
    }
    val period = match.groupValues[5].replace(Regex("\\s+"), " ")
    if (hour !in 0..23 || minutes !in 0..59 || (period.isNotEmpty() && hour !in 1..12)) return ClockReading(null, null, true)
    if (period == "da tarde" || period == "da noite") { if (hour in 1..11) hour += 12 }
    else if (period == "da manha" && hour == 12) hour = 0
    return ClockReading(LocalTime.of(hour, minutes), (marker?.range?.first ?: start)..(end - 1))
}
