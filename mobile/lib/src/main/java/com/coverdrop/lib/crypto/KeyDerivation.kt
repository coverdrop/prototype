package com.coverdrop.lib.crypto

import com.goterl.lazycode.lazysodium.interfaces.PwHash

private const val COVERDROP_KDF_SALT = "COVERDROPKDF_SALT"

/**
 * The main key derivation function (KDF) mapping the passphrase to a key. Uses Argon2 of libsodium under the hood.
 */
class KeyDerivation {

    val lazySodium = CoverdropSodiumProvider.instance

    fun deriveKey(passphrase: String, keyLengthInBytes: Int): ByteArray {
        if (passphrase.isEmpty()) throw IllegalArgumentException("Passphrase must not be empty")
        if (keyLengthInBytes <= 0) throw IllegalArgumentException("Key length must be positive")

        val pwBytes = passphrase.toByteArray()

        val hash = ByteArray(keyLengthInBytes)
        val result = lazySodium.cryptoPwHash(
            hash,
            hash.size,
            pwBytes,
            pwBytes.size,
            COVERDROP_KDF_SALT.toByteArray(),
            PwHash.ARGON2ID_OPSLIMIT_MODERATE,
            PwHash.MEMLIMIT_MODERATE,
            PwHash.Alg.PWHASH_ALG_ARGON2ID13
        )
        assertOrThrow(result, "KDF failed")
        return hash
    }
}
