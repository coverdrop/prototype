package com.coverdrop.lib.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyDerivationTest {

    @Test
    fun testInit_whenInit_thenNothingBadHappens() {
        KeyDerivation()
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeriveKey_whenGivenTooShortKeyLength_thenThrows() {
        KeyDerivation().deriveKey("passphrase", keyLengthInBytes = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDeriveKey_whenEmptyPassphrase_thenThrows() {
        KeyDerivation().deriveKey("", keyLengthInBytes = 15)
    }

    @Test
    fun testDeriveKey_whenDerivingKey_thenMatchesLength() {
        val key = KeyDerivation().deriveKey("passphrase", keyLengthInBytes = 32)
        assertThat(key).hasSize(32)
    }

    @Test
    fun testDeriveKey_whenSamePassphrase_thenKeySame() {
        val instance = KeyDerivation()
        val keyA = instance.deriveKey("passphrase", keyLengthInBytes = 32)
        val keyB = instance.deriveKey("passphrase", keyLengthInBytes = 32)
        assertThat(keyA).isEqualTo(keyB)
    }

    @Test
    fun testDeriveKey_whenDifferentPassphrase_thenKeyDifferent() {
        val instance = KeyDerivation()
        val keyA = instance.deriveKey("passphraseA", keyLengthInBytes = 32)
        val keyB = instance.deriveKey("passphraseB", keyLengthInBytes = 32)
        assertThat(keyA).isNotEqualTo(keyB)
    }
}
