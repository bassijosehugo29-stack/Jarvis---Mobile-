package br.com.jarvis.mobile.domain

import java.text.Normalizer
import java.util.Locale

internal fun voiceText(text: String): String = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "")
    .replace(Regex("[^a-z0-9:/ ]"), " ")
    .replace(Regex("\\s+"), " ").trim()

internal fun spokenNumber(text: String): Int? {
    val value = voiceText(text)
    value.toIntOrNull()?.let { return it }
    val small = mapOf("zero" to 0, "um" to 1, "uma" to 1, "dois" to 2, "duas" to 2,
        "tres" to 3, "quatro" to 4, "cinco" to 5, "seis" to 6, "sete" to 7,
        "oito" to 8, "nove" to 9, "dez" to 10, "onze" to 11, "doze" to 12,
        "treze" to 13, "catorze" to 14, "quatorze" to 14, "quinze" to 15,
        "dezesseis" to 16, "dezasseis" to 16, "dezessete" to 17, "dezoito" to 18, "dezenove" to 19,
        "vinte" to 20, "trinta" to 30, "quarenta" to 40, "cinquenta" to 50, "sessenta" to 60)
    small[value]?.let { return it }
    val parts = value.split(" e ")
    if (parts.size != 2) return null
    val tens = small[parts[0]] ?: return null
    val units = small[parts[1]] ?: return null
    return if (tens in setOf(20, 30, 40, 50) && units in 1..9) tens + units else null
}
