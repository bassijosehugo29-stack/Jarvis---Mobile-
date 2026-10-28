package br.com.jarvis.mobile.security

import org.junit.Assert.*
import org.junit.Test

class BackupCipherTest {
    @Test fun authenticatedPasswordRoundTripUsesFreshSaltAndIv() {
        val text="Memória privada com acentuação".toByteArray()
        val password="senha de teste longa".toCharArray()
        val first=BackupCipher.encrypt(text,password);val second=BackupCipher.encrypt(text,password)
        assertFalse(first.contentEquals(second))
        assertArrayEquals(text,BackupCipher.decrypt(first,password))
        assertArrayEquals(text,BackupCipher.decrypt(second,password))
        assertFalse(String(first).contains("Memória privada"))
    }
    @Test fun wrongPasswordAndTamperingAreRejectedBeforeImport() {
        val password="senha correta".toCharArray()
        val bytes=BackupCipher.encrypt("notas".toByteArray(),password)
        assertThrows(Exception::class.java){BackupCipher.decrypt(bytes,"senha errada".toCharArray())}
        for(index in listOf(8,24,bytes.lastIndex)) {
            val changed=bytes.copyOf();changed[index]=(changed[index].toInt() xor 1).toByte()
            assertThrows(Exception::class.java){BackupCipher.decrypt(changed,password)}
        }
    }
    @Test fun malformedEnvelopeAndShortPasswordsAreRejected() {
        assertThrows(IllegalArgumentException::class.java){BackupCipher.encrypt(byteArrayOf(1),"curta".toCharArray())}
        assertThrows(IllegalArgumentException::class.java){BackupCipher.decrypt(ByteArray(52),"senha longa".toCharArray())}
        assertThrows(IllegalArgumentException::class.java){BackupCipher.decrypt(ByteArray(3),"senha longa".toCharArray())}
    }
}
