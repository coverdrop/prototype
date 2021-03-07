package com.coverdrop.lib.crypto

import android.content.Context
import com.coverdrop.lib.COVERDROP_BLOB_PADDED_LENGTH
import java.io.File


private const val AES_IV_PREF_NAME = "coverdrop_aes_key_iv"
private const val SE_IV_PREF_NAME = "coverdrop_se_key_iv"

/**
 * The [CoverdropBox] wraps the [KeyDerivation], [AesCtrCipher], and [SecureElement] as defined
 * in the design. It ensures that only the correct key and SE state can decrypt the content and that
 * no information that could aid brute-force attempts to confirm the presence of plaintext.
 *
 * @param namespace The namespace alles tests to avoid conflicts with the app state
 */
class CoverdropBox(private val context: Context, private val namespace: String) {

    fun store(data: Padded, passphrase: MemorablePassphrase) {
        // Derive key
        val key = KeyDerivation().deriveKey(passphrase.value, AES_KEY_LENGTH_BYTES)

        // Encrypt using SE
        val secureElementSealed = SecureElement(context, namespace).seal(data.byteArray)
        saveBytesAsPref(secureElementSealed.iv, SE_IV_PREF_NAME)

        // Encrypt using AES-CTR
        val aesCtrSealed = AesCtrCipher(context, key).seal(secureElementSealed.ciphertext)
        saveBytesAsPref(aesCtrSealed.iv, AES_IV_PREF_NAME)

        // Write to disk
        saveBytesToFile(aesCtrSealed.ciphertext)
    }

    /**
     * @throws javax.crypto.AEADBadTagException if the passphrase was not correct, the secure
     * element has been wiped, or data was corrupted on disk
     */
    fun load(passphrase: MemorablePassphrase): Padded {
        // Derive key
        val key = KeyDerivation().deriveKey(passphrase.value, AES_KEY_LENGTH_BYTES)

        // Load from disk
        val blob = loadBytesFromFile()

        // Decrypt using AES-CTR
        val aesCtrSealed = AesCtrSealed(blob, loadPrefAsBytes(AES_IV_PREF_NAME))
        val aesCtrUnsealed = AesCtrCipher(context, key).unseal(aesCtrSealed)

        // Decrypt using SE
        val secureElementSealed =
            SecureElementSealed(aesCtrUnsealed, loadPrefAsBytes(SE_IV_PREF_NAME))
        val secureElementUnsealed =
            SecureElement(context, namespace).unseal(secureElementSealed)

        return Padded(secureElementUnsealed, COVERDROP_BLOB_PADDED_LENGTH)
    }

    fun wipe() {
        // By deleting the secure element key, data cannot be restored any longer
        SecureElement(context, namespace).resetKey()
    }


    /**
     * @return true if the persisted data file exists. This should be the case regardless of whether
     * the CoverDrop feature has been used as it is created during app start
     */
    fun exists() = getPersistedDataFile().exists()

    fun touch() {
        getPersistedDataFile().setLastModified(System.currentTimeMillis())
    }

    private fun saveBytesAsPref(bytes: ByteArray, prefName: String) {
        val prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(prefName, byteArrayToHex(bytes))
            apply()
        }
    }

    private fun loadPrefAsBytes(prefName: String): ByteArray {
        val prefs = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
        return hexToByteArray(prefs.getString(prefName, "")!!)
    }

    private fun saveBytesToFile(bytes: ByteArray) {
        val file = getPersistedDataFile()
        file.outputStream().use {
            it.write(bytes)
        }
    }

    private fun loadBytesFromFile(): ByteArray {
        val file = getPersistedDataFile()
        file.inputStream().use {
            return it.readBytes()
        }
    }

    private fun getPersistedDataFile() = Companion.getPersistedDataFile(context, namespace)

    companion object {
        internal fun getPersistedDataFile(context: Context, namespace: String): File {
            val dir = context.noBackupFilesDir
            return File(dir, "coverdrop_$namespace.persist")
        }
    }
}
