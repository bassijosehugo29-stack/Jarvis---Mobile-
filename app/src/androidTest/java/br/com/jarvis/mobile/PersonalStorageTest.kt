package br.com.jarvis.mobile

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import br.com.jarvis.mobile.brain.*
import br.com.jarvis.mobile.hub.*
import br.com.jarvis.mobile.security.PrivateCipher
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

/** Only isolated synthetic notes/accounts; no real user data or credentials in reports. */
@RunWith(AndroidJUnit4::class)
class PersonalStorageTest {
    private val context=InstrumentationRegistry.getInstrumentation().targetContext
    private val brain=(context.applicationContext as JarvisApplication).data.brain
    private val storage=(context.applicationContext as JarvisApplication).data.storage
    @Before fun reset()=runBlocking {brain.erase();storage.eraseAll();brain.unlock()}
    @After fun clean()=runBlocking {brain.erase();storage.eraseAll()}
    private fun note()=Note(title="TÍTULO PRIVADO CI",category=NoteCategory.HEALTH,content="CONTEÚDO CONFIDENCIAL APENAS DE TESTE",updatedAt=100)

    @Test fun crudPersistsEncryptedPayloadAndLockedMemoryCannotBeRead()=runBlocking {
        val original=note();brain.save(original)
        val record=PersonalDatabase.get(context).records().records("note").single()
        assertFalse(String(record.payload,Charsets.UTF_8).contains(original.title))
        assertFalse(String(record.payload,Charsets.UTF_8).contains(original.content))
        assertEquals(original,brain.notes().single())
        val edited=original.copy(content="EDIÇÃO PRIVADA CI",updatedAt=101)
        brain.save(edited);assertEquals(edited,brain.notes().single())
        brain.lock()
        try{brain.notes();fail("Locked store returned private notes")}catch(_:IllegalStateException){}
        val reopened=BrainStore(context)
        assertFalse(reopened.isUnlocked());reopened.unlock();assertEquals(edited,reopened.notes().single());reopened.lock()
        brain.unlock();brain.delete(original.id);assertEquals(0,brain.count())
    }
    @Test fun backupRequiresReviewAndImportIsAtomicAndAuthenticated()=runBlocking {
        val original=note();brain.save(original)
        val password="senha de teste ci".toCharArray()
        val backup=brain.export(password)
        brain.delete(original.id);assertEquals(0,brain.count())
        val preview=brain.previewBackup(backup,password)
        assertEquals(0,brain.count()) // Preview never writes.
        brain.importConfirmed(preview);assertEquals(original,brain.notes().single())
        val newer=original.copy(title="VERSÃO LOCAL MAIS NOVA",updatedAt=200)
        brain.save(newer);brain.importConfirmed(preview);assertEquals(newer,brain.notes().single())
        val corrupted=backup.copyOf();corrupted[corrupted.lastIndex]=(corrupted.last().toInt() xor 1).toByte()
        try{brain.previewBackup(corrupted,password);fail("Tampered backup accepted")}catch(_:javax.crypto.AEADBadTagException){}
        try{brain.previewBackup(backup,"senha incorreta".toCharArray());fail("Wrong password accepted")}catch(_:javax.crypto.AEADBadTagException){}
        try{brain.importConfirmed(listOf(original.copy(id=java.util.UUID.randomUUID().toString()),original.copy(title="")));fail("Invalid import accepted")}catch(_:IllegalArgumentException){}
        assertEquals(listOf(newer),brain.notes())
    }
    @Test fun encryptedCacheCannotBeResurrectedByLateWorkerOrOAuthAfterDisconnectAndPurge()=runBlocking {
        val initial=storage.epoch()
        val connected=storage.connectMail("ci@example.invalid",initial)
        storage.put("mail",JSONObject().put("private","CACHE PRIVADO CI"),connected)
        assertEquals("CACHE PRIVADO CI",storage.get("mail")!!.getString("private"))
        storage.put("mail-overrides",JSONObject().put("CI1","ACTION"),connected)
        storage.connectMail("ci@example.invalid",connected)
        assertEquals("ACTION",storage.get("mail-overrides")!!.getString("CI1"))
        storage.disconnectMail()
        assertNull(storage.get("mail"));assertNull(storage.get("gmail-account"))
        try{storage.put("mail",JSONObject(),connected);fail("Late worker restored cache")}catch(_:IllegalStateException){}
        try{storage.connectMail("ci@example.invalid",initial);fail("Late OAuth restored account")}catch(_:IllegalStateException){}
        val active=storage.connectMail("ci2@example.invalid",storage.epoch())
        brain.save(note());brain.erase();storage.eraseAll()
        assertEquals(0,brain.count());assertFalse(brain.isUnlocked());assertNull(storage.get("gmail-account"))
        try{storage.put("mail",JSONObject(),active);fail("Late refresh survived purge")}catch(_:IllegalStateException){}
        brain.unlock();brain.save(note());assertEquals(1,brain.count()) // New keystore key works.
    }
    @Test fun deviceCipherUsesFreshIvAndAuthenticatesRecordPurpose() {
        val cipher=PrivateCipher("jarvis-ci-isolated-key")
        try{
            val bytes="PRIVADO CI".toByteArray();val one=cipher.encrypt(bytes,"note:1");val two=cipher.encrypt(bytes,"note:1")
            assertFalse(one.contentEquals(two));assertArrayEquals(bytes,cipher.decrypt(one,"note:1"))
            try{cipher.decrypt(one,"note:2");fail("Record substitution accepted")}catch(_:javax.crypto.AEADBadTagException){}
        }finally{cipher.destroy()}
    }
    @Test fun gmailMetadataClassificationUsesOnlyRealSuppliedFixtureAndNoNetwork() {
        val repository=GmailRepository(context,storage,NetworkClient())
        val message=JSONObject("""{"id":"CI1","internalDate":"123000","snippet":"Favor responder &amp; confirmar","labelIds":["INBOX","UNREAD"],"payload":{"headers":[{"name":"Subject","value":"Aprovação pendente"},{"name":"From","value":"CI <ci@example.invalid>"}]}}""")
        val parsed=repository.parseMessage(message,"test@example.invalid")
        assertEquals(EmailCategory.ACTION,parsed.category);assertEquals("test@example.invalid",parsed.account)
        assertEquals("Favor responder & confirmar",parsed.preview);assertEquals(123000L,parsed.receivedAt);assertTrue(parsed.unread)
    }
}
