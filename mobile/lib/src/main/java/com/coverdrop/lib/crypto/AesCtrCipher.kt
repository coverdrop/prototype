package com.coverdrop.lib.crypto

import android.content.Context
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

internal const val AES_KEY_LENGTH_BYTES = 256 / 8

data class AesCtrSealed(val ciphertext: ByteArray, val iv: ByteArray)

/**
 * An encryption box that is using a user defined key for sealing/unsealing the items. It does not
 * use authenticated encryption.
 */
class AesCtrCipher(private val context: Context, private val key: ByteArray) {

    init {
        if (key.size != AES_KEY_LENGTH_BYTES)
            throw IllegalArgumentException("invalid key length: ${key.size}")
    }

    /**
     * Seals a ByteArray with the current key into a [AesCtrSealed].
     */
    fun seal(plaintext: ByteArray): AesCtrSealed {
        val key = SecretKeySpec(key, "AES")
        return Cipher.getInstance("AES/CTR/NoPadding").run {
            init(Cipher.ENCRYPT_MODE, key)
            AesCtrSealed(doFinal(plaintext), iv)
        }
    }

    /**
     * Unseals a [AesCtrSealed] with the current key.
     *
     * @throws [AEADBadTagException::class] is the ciphertext does not decrypt with the current key
     */
    fun unseal(sealed: AesCtrSealed): ByteArray {
        val key = SecretKeySpec(key, "AES")
        return Cipher.getInstance("AES/CTR/NoPadding").run {
            init(Cipher.DECRYPT_MODE, key, IvParameterSpec(sealed.iv))
            doFinal(sealed.ciphertext)
        }
    }

}
