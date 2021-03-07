package com.coverdrop.lib.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyProperties.KEY_ALGORITHM_AES
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec

private const val SE_KEY_STORE = "AndroidKeyStore"
private const val SE_KEY_ALIAS = "coverdrop_se_key"

data class SecureElementSealed(val ciphertext: ByteArray, val iv: ByteArray)

/**
 * A sealed box implementation where the key is generated and stored inside the secure element.
 * This promises that its extraction is difficult to impossible. Hence any key cracking operation
 * is limited by the throughput of the secure element which is around 10 KiB per second.
 */
class SecureElement(private val context: Context, private val namespace: String) {

    private val keyAlias = "${namespace}_${SE_KEY_ALIAS}"

    init {
        ensureKey()
    }

    /**
     * Resets the key stored in the secure element. Afterwards any sealed entity can no longer be
     * opened.
     */
    fun resetKey() {
        val keyGenSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).run {
            setIsStrongBoxBacked(true) // Forces use of secure element
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(256)
            setUnlockedDeviceRequired(true)
            build()
        }

        val keyGen = KeyGenerator.getInstance(KEY_ALGORITHM_AES, SE_KEY_STORE)
        keyGen.init(keyGenSpec)
        keyGen.generateKey()
    }

    /**
     * Seals a ByteArray with the current key into a [SecureElementSealed].
     */
    fun seal(plaintext: ByteArray): SecureElementSealed {
        val keyHandle = getKeyHandle()
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, keyHandle)
            SecureElementSealed(doFinal(plaintext), iv)
        }
    }

    /**
     * Unseals a [SecureElementSealed] with the current key.
     *
     * @throws [AEADBadTagException::class] is the ciphertext does not decrypt with the current key
     */
    fun unseal(sealed: SecureElementSealed): ByteArray {
        val keyHandle = getKeyHandle()
        return Cipher.getInstance("AES/GCM/NoPadding").run {
            init(Cipher.DECRYPT_MODE, keyHandle, GCMParameterSpec(128, sealed.iv))
            doFinal(sealed.ciphertext)
        }
    }

    private fun ensureKey() {
        if (getKeyHandle() == null) resetKey()
    }

    private fun getKeyHandle() = KeyStore.getInstance(SE_KEY_STORE).run {
        load(null)
        getKey(keyAlias, null)
    }

}
