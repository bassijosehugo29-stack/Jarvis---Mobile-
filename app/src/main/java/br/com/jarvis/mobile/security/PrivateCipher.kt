package br.com.jarvis.mobile.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Device-only non-exportable key. Access to note operations is additionally gated by system auth. */
class PrivateCipher(private val alias: String) {
    private fun key(): SecretKey = synchronized(this) {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build())
            generateKey()
        }
    }
    fun encrypt(bytes: ByteArray, purpose: String): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE,key()); updateAAD(purpose.toByteArray(Charsets.UTF_8)) }
        return cipher.iv + cipher.doFinal(bytes)
    }
    fun decrypt(bytes: ByteArray, purpose: String): ByteArray {
        require(bytes.size >= 28)
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,bytes.copyOfRange(0,12)))
            updateAAD(purpose.toByteArray(Charsets.UTF_8)); doFinal(bytes,12,bytes.size-12)
        }
    }
    fun destroy() { KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(alias)) deleteEntry(alias) } }
}

/** Portable password backup: fixed, versioned envelope; authenticated before JSON is imported. */
object BackupCipher {
    private val magic = "JARVISB1".toByteArray(Charsets.US_ASCII)
    private const val iterations = 600_000
    const val MAX_BYTES = 50 * 1024 * 1024
    fun encrypt(bytes: ByteArray, password: CharArray): ByteArray {
        require(password.size >= 8) { "Use uma senha de pelo menos oito caracteres." }
        require(bytes.size <= MAX_BYTES-64) { "Backup excede 50 MB." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, derive(password,salt),GCMParameterSpec(128,iv))
        cipher.updateAAD(magic + salt)
        return magic + salt + iv + cipher.doFinal(bytes)
    }
    fun decrypt(bytes: ByteArray, password: CharArray): ByteArray {
        require(bytes.size in 52..MAX_BYTES && bytes.copyOfRange(0,8).contentEquals(magic)) { "Formato de backup não reconhecido." }
        val salt=bytes.copyOfRange(8,24); val iv=bytes.copyOfRange(24,36)
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE,derive(password,salt),GCMParameterSpec(128,iv)); updateAAD(magic+salt)
            doFinal(bytes,36,bytes.size-36)
        }
    }
    private fun derive(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec=PBEKeySpec(password,salt,iterations,256)
        return try { SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,"AES") }
        finally { spec.clearPassword() }
    }
}
