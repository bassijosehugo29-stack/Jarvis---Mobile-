package br.com.jarvis.mobile.brain

import org.junit.Assert.*
import org.junit.Test

class BrainModelsTest {
    private val goal=Note(title="Aprovação",category=NoteCategory.GOALS,content="Estudar matemática",updatedAt=10,tags=listOf("Educação"))
    @Test fun searchesAccentsContentAndTagsWithoutChangingCategory() {
        val health=Note(title="Consulta",category=NoteCategory.HEALTH,content="Revisão anual",updatedAt=20)
        assertEquals(listOf(goal),searchNotes(listOf(health,goal),"APROVACAO",null))
        assertEquals(listOf(goal),searchNotes(listOf(health,goal),"matematica",null))
        assertEquals(listOf(goal),searchNotes(listOf(health,goal),"educacao",NoteCategory.GOALS))
        assertTrue(searchNotes(listOf(health,goal),"educacao",NoteCategory.HEALTH).isEmpty())
    }
    @Test fun pinnedNotesComeFirstThenMostRecentlyUpdated() {
        val newer=goal.copy(id="new",updatedAt=30)
        val pinned=goal.copy(id="pin",updatedAt=1,pinned=true)
        assertEquals(listOf("pin","new",goal.id),searchNotes(listOf(goal,newer,pinned),"",null).map{it.id})
    }
    @Test fun manualLinksWinOverSharedTagsAndIgnoreSelfMissingOrDuplicateTargets() {
        val a=goal.copy(id="a",links=listOf("b","a","missing","b"))
        val b=goal.copy(id="b",links=listOf("a"),tags=listOf("educacao"))
        val c=goal.copy(id="c",tags=listOf("EDUCAÇÃO"))
        val links=noteLinks(listOf(a,b,c))
        assertEquals(3,links.size)
        assertEquals(1,links.count{it.manual})
        assertTrue(links.any{it.from=="a"&&it.to=="b"&&it.manual})
    }
    @Test fun mergesByUuidOnlyAndKeepsLocalOnTimestampTie() {
        val same=goal.copy(content="Alteração com mesma data")
        val older=goal.copy(content="Antiga",updatedAt=9)
        assertEquals(goal,mergeNotes(listOf(goal),listOf(same)).single())
        assertEquals(goal,mergeNotes(listOf(goal),listOf(older)).single())
        val updated=goal.copy(content="Atualizada",updatedAt=11)
        assertEquals(updated,mergeNotes(listOf(goal),listOf(updated)).single())
        assertEquals(2,mergeNotes(listOf(goal),listOf(goal.copy(id="another"))).size)
    }
    @Test(expected=IllegalArgumentException::class) fun rejectsAmbiguousDuplicateImportIds() {
        mergeNotes(emptyList(),listOf(goal,goal.copy(content="Outro conteúdo")))
    }
    @Test fun sensitiveCategoriesDoNotAuthorizeDigestByDefault() {
        assertEquals(8,NoteCategory.entries.size)
        NoteCategory.entries.forEach{assertFalse(Note(title="Opcional",category=it,content="Privado").includeInDigest)}
    }
    @Test fun filtersOneThousandNotesWithoutDroppingAccessToAnyCategory() {
        val notes=(0 until 1000).map{goal.copy(id="$it",title="Nota $it",category=NoteCategory.entries[it%8],tags=listOf("lote"))}
        assertEquals(1000,searchNotes(notes,"lote",null).size)
        NoteCategory.entries.forEach{assertEquals(125,searchNotes(notes,"",it).size)}
        assertEquals("997",searchNotes(notes,"Nota 997",null).single().id)
        assertTrue(noteLinks(notes.take(80)).size<=80*79/2)
    }
}
