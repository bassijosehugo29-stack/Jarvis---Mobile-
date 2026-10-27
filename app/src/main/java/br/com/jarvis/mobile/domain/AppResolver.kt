package br.com.jarvis.mobile.domain

data class LaunchableApp(val label: String, val packageName: String, val component: String)

/** Only launcher activities supplied by Android are eligible; never guess a package or a URL. */
class AppResolver {
    fun candidates(query: String, apps: List<LaunchableApp>): List<LaunchableApp> {
        val name = voiceText(query).removePrefix("o ").removePrefix("a ")
            .replace(Regex("^(?:aplicativo|app)\\s+"), "")
        if (name.isBlank()) return emptyList()
        val sorted = apps.distinctBy { it.component }.sortedWith(compareBy({ voiceText(it.label) }, { it.component }))
        val exact = sorted.filter { voiceText(it.label) == name }
        if (exact.isNotEmpty()) return exact
        val aliases = when (name) {
            "chatgpt", "chat gpt", "chat g p t" -> setOf("com.openai.chatgpt")
            "agenda", "calendario", "google agenda", "google calendario" ->
                setOf("com.google.android.calendar", "com.samsung.android.calendar")
            "relogio", "alarmes", "despertador" -> setOf("com.google.android.deskclock", "com.sec.android.app.clockpackage")
            "whatsapp", "whats app", "zap" -> setOf("com.whatsapp")
            "whatsapp business", "whats app business" -> setOf("com.whatsapp.w4b")
            "youtube", "you tube" -> setOf("com.google.android.youtube")
            "play store", "loja de aplicativos" -> setOf("com.android.vending")
            else -> emptySet()
        }
        val aliased = sorted.filter { it.packageName in aliases }
        if (aliased.isNotEmpty()) return aliased
        val words = name.split(' ')
        if (name.length < 3) return emptyList()
        return sorted.filter { app ->
            val label = voiceText(app.label).split(' ')
            words.all { word -> label.any { it == word } }
        }
    }
}

/** Number or an unambiguous complete label; ambiguous replies never choose an app/calendar. */
internal fun choiceIndex(text: String, labels: List<String>): Int? {
    val answer = voiceText(text).replace(Regex("\\b(jarvis|jarves|javis|javes)\\b"), "").trim()
        .replace(Regex("^(?:opcao|numero|agenda|aplicativo)\\s+"), "")
    spokenNumber(answer)?.let { return (it - 1).takeIf { index -> index in labels.indices } }
    val matches = labels.indices.filter { voiceText(labels[it]) == answer }
    return matches.singleOrNull()
}
