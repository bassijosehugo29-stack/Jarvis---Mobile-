package br.com.jarvis.mobile.voice

private val spokenBoundaries = Regex("""[.!?;]\s+""")
private val spokenAbbreviations = setOf("sr.", "sra.", "srta.", "dr.", "dra.", "prof.", "profa.", "av.", "art.", "p.ex.")
private fun abbreviatedWord(text: String): Boolean {
    val word = text.trimEnd().substringAfterLast(' ').trim('"', '\u201c', '\u201d').lowercase(java.util.Locale.ROOT)
    return word in spokenAbbreviations || (word.length == 2 && word[0].isLetter() && word[1] == '.')
}

/** Split long spoken replies at natural boundaries without breaking a title from its name.
 * No pitch processing, model changes, external voice service or changes to the displayed text.
 */
fun speechChunks(text: String, limit: Int = 420): List<String> {
    require(limit >= 40)
    val chunks = mutableListOf<String>(); var rest = text.trim()
    while (rest.length > limit) {
        val candidate = rest.take(limit + 1)
        val sentence = spokenBoundaries.findAll(candidate).map { it.range.first + 1 }
            .filter { it >= limit / 3 && !abbreviatedWord(candidate.take(it)) }.lastOrNull()
        var cut = sentence ?: candidate.lastIndexOf(' ').takeIf { it > 0 } ?: limit
        if (sentence == null && abbreviatedWord(candidate.take(cut))) {
            val beforeTitle = candidate.take(cut).trimEnd().lastIndexOf(' ')
            if (beforeTitle > 0) cut = beforeTitle
        }
        chunks += rest.take(cut).trim(); rest = rest.drop(cut).trimStart()
    }
    if (rest.isNotBlank()) chunks += rest
    return chunks.ifEmpty { listOf("Às suas ordens, senhor.") }
}
