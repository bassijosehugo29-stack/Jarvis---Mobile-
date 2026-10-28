package br.com.jarvis.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import br.com.jarvis.mobile.brain.*
import br.com.jarvis.mobile.experience.OperationMode
import br.com.jarvis.mobile.ui.BrainPanel
import br.com.jarvis.mobile.ui.JarvisTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BrainPanelTest {
    @get:Rule val compose=createComposeRule()
    @Test fun editorRequiresExplicitSaveAndGraphAndSearchFollowConfirmedNote() {
        val state=mutableStateOf(BrainState(unlocked=true))
        compose.setContent{JarvisTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
            BrainPanel(state.value,OperationMode.SILENT,{},
                {note->state.value=state.value.copy(draft=note?:Note(title="",category=NoteCategory.YOU,content=""))},
                {note->state.value=state.value.copy(notes=(state.value.notes.filter{it.id!=note.id}+note),draft=null)},
                {id->state.value=state.value.copy(notes=state.value.notes.filter{it.id!=id},draft=null)},
                {state.value=state.value.copy(draft=null)},{},{},{},{},{},{})
        }}}
        compose.onNodeWithTag("brain-add").performScrollTo().performClick()
        compose.onNodeWithTag("note-title").performScrollTo().performTextInput("Projeto CI")
        compose.onNodeWithTag("note-content").performScrollTo().performTextInput("Conteúdo confirmado pelo usuário de teste")
        compose.runOnIdle{assertTrue(state.value.notes.isEmpty())}
        compose.onNodeWithTag("note-save").performScrollTo().performClick()
        compose.runOnIdle{assertEquals(1,state.value.notes.size);assertFalse(state.value.notes.single().includeInDigest)}
        compose.onNodeWithTag("brain-graph").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("brain-search").performScrollTo().performTextInput("NÃO EXISTE")
        compose.onNodeWithText("Projeto CI").assertDoesNotExist()
        compose.onNodeWithTag("brain-search").performTextClearance()
        compose.onNodeWithText("Projeto CI").performScrollTo().performClick()
        compose.onNodeWithText("Excluir nota").performScrollTo().performClick()
        compose.onNodeWithText("Excluir",useUnmergedTree=true).performClick()
        compose.runOnIdle{assertTrue(state.value.notes.isEmpty())}
    }
    @Test fun lockedMemoryOnlyOffersSystemUnlockAndDoesNotRenderNotes() {
        var requests=0
        val secret=Note(title="NOTA PROTEGIDA CI",category=NoteCategory.HEALTH,content="CONTEÚDO CI")
        compose.setContent{JarvisTheme{Column{BrainPanel(BrainState(notes=listOf(secret)),OperationMode.SILENT,
            {requests++},{},{},{},{},{},{},{},{},{},{})}}}
        compose.onNodeWithText(secret.title).assertDoesNotExist()
        compose.onNodeWithTag("brain-graph").assertDoesNotExist()
        compose.onNodeWithTag("brain-unlock").performClick()
        compose.runOnIdle{assertEquals(1,requests)}
    }
    @Test fun thousandNotesUseBoundedGraphAndPaginatedListWithAllNotesSearchable() {
        val notes=(0 until 1000).map{Note(title="Nota CI $it",category=NoteCategory.entries[it%8],content="Conteúdo $it",updatedAt=it.toLong())}
        compose.setContent{JarvisTheme{Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())){
            BrainPanel(BrainState(unlocked=true,notes=notes),OperationMode.SILENT,{},{},{},{},{},{},{},{},{},{},{})
        }}}
        compose.onNodeWithTag("brain-graph").assertExists()
        compose.onNodeWithText("1/20 · 50 por página").assertExists()
        compose.onNodeWithTag("brain-search").performScrollTo().performTextInput("Nota CI 997")
        compose.onNodeWithText("Nota CI 997").performScrollTo().assertIsDisplayed()
    }
}
