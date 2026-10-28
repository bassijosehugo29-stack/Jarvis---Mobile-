package br.com.jarvis.mobile.brain

import java.text.Normalizer
import java.util.Locale
import java.util.UUID

enum class NoteCategory(val title: String, val color: Long) {
    YOU("Você", 0xFF53E8FF), GOALS("Metas", 0xFFD6BA7A), CAREER("Carreira", 0xFF729FFF),
    PROJECTS("Projetos", 0xFF7DE5BC), FINANCE("Finanças", 0xFFEBC881),
    LEARNING("Aprendizado", 0xFFB49AFF), HEALTH("Saúde", 0xFFFF9DAD), RELATIONS("Relações", 0xFFFFB77C)
}

data class Note(
    val id: String = UUID.randomUUID().toString(), val title: String, val category: NoteCategory,
    val content: String, val updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(), val links: List<String> = emptyList(),
    val includeInDigest: Boolean = false, val pinned: Boolean = false
)

data class NoteLink(val from: String, val to: String, val manual: Boolean)

fun normalizedSearch(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT).trim()

fun searchNotes(notes: List<Note>, query: String, category: NoteCategory?): List<Note> {
    val search = normalizedSearch(query)
    return notes.filter { (category == null || it.category == category) &&
        (search.isBlank() || normalizedSearch(it.title + " " + it.content + " " + it.tags.joinToString(" ")).contains(search)) }
        .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })
}

fun noteLinks(notes: List<Note>): List<NoteLink> = buildList {
    val ids = notes.map { it.id }.toSet()
    val seen = mutableSetOf<Pair<String,String>>()
    for (note in notes) for (target in note.links) {
        val key = listOf(note.id, target).sorted().let { it[0] to it[1] }
        if (target in ids && target != note.id && seen.add(key)) add(NoteLink(key.first,key.second,true))
    }
    for (i in notes.indices) for (j in i+1 until notes.size) {
        val a=notes[i]; val b=notes[j]; val key=listOf(a.id,b.id).sorted().let { it[0] to it[1] }
        if (a.tags.map(::normalizedSearch).filter { it.isNotBlank() }.intersect(b.tags.map(::normalizedSearch).toSet()).isNotEmpty() && seen.add(key))
            add(NoteLink(a.id,b.id,false))
    }
}

/** Pure merge policy: same UUID only changes when the imported version is newer. */
fun mergeNotes(existing: List<Note>, incoming: List<Note>): List<Note> {
    require(incoming.map { it.id }.distinct().size == incoming.size) { "Identificadores duplicados no backup." }
    val result = existing.associateBy { it.id }.toMutableMap()
    incoming.forEach { note -> if (result[note.id]?.let { it.updatedAt >= note.updatedAt } != true) result[note.id] = note }
    return result.values.toList()
}
