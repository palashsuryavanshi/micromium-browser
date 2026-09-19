package com.example.data

import android.content.Context
import android.util.Base64
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Vault cryptography + master-password verification.
 *
 * - Login passwords: encrypted with Tink AES-256-GCM. The keyset lives in
 *   SharedPreferences, wrapped by a master key in Android Keystore, so the
 *   ciphertext is bound to this device.
 * - Master password: never stored. Only a salted PBKDF2-SHA256 verifier is
 *   kept (in the vault DB meta table). Verification is constant-time.
 */
object VaultCrypto {

    private const val KEYSET_PREFS = "vault_keyset_prefs"
    private const val KEYSET_NAME = "vault_keyset"
    private const val MASTER_KEY_URI = "android-keystore://vault_master_key"
    private const val PBKDF2_ITERATIONS = 210_000
    private const val PBKDF2_KEY_BITS = 256
    private const val SALT_BYTES = 16

    @Volatile
    private var aead: Aead? = null

    private fun aead(context: Context): Aead {
        return aead ?: synchronized(this) {
            aead ?: run {
                AeadConfig.register()
                val handle = AndroidKeysetManager.Builder()
                    .withSharedPref(context.applicationContext, KEYSET_NAME, KEYSET_PREFS)
                    .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
                    .withMasterKeyUri(MASTER_KEY_URI)
                    .build()
                    .keysetHandle
                handle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
                    .also { aead = it }
            }
        }
    }

    fun encryptToBase64(context: Context, plaintext: String): String {
        val ciphertext = aead(context).encrypt(plaintext.toByteArray(Charsets.UTF_8), null)
        return Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    fun decryptFromBase64(context: Context, encoded: String): String {
        val ciphertext = Base64.decode(encoded, Base64.NO_WRAP)
        val plaintext = aead(context).decrypt(ciphertext, null)
        return String(plaintext, Charsets.UTF_8)
    }

    /** Returns "base64(salt):base64(verifier)" to store. */
    fun hashPassword(password: String): String {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password, salt)
        val encoder = Base64.NO_WRAP
        return Base64.encodeToString(salt, encoder) + ":" + Base64.encodeToString(hash, encoder)
    }

    fun verifyPassword(password: String, stored: String): Boolean {
        return try {
            val parts = stored.split(":")
            if (parts.size != 2) return false
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expected = Base64.decode(parts[1], Base64.NO_WRAP)
            val actual = pbkdf2(password, salt)
            java.security.MessageDigest.isEqual(expected, actual)
        } catch (e: Exception) {
            false
        }
    }

    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_BITS)
        return try {
            factory.generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
