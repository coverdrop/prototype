package com.coverdrop.lib.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.experimental.xor

@RunWith(AndroidJUnit4::class)
class AesCtrCipherTest {

    private val dummyKey1 = "testtesttesttesttesttesttesttest".toByteArray()
    private val dummyKey1tooShort = "testtesttesttesttesttest".toByteArray()
    private val dummyKey2 = "testtesttesttesttesttesttestXXXX".toByteArray()

    @Test
    fun testInit_whenProperKey_thenNothingThrows() {
        AesCtrCipher(context(), dummyKey1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInit_whenTooShortKey_thenNothingThrows() {
        AesCtrCipher(context(), dummyKey1tooShort)
    }

    @Test
    fun testSealUnseal_whenGivenEmptyPlaintext_thenUnsealsFine() {
        val instance = AesCtrCipher(context(), dummyKey1)

        val plaintext = ByteArray(0)
        val sealed = instance.seal(plaintext)
        assertThat(sealed.ciphertext.size).isEqualTo(0)

        val actual = instance.unseal(sealed)
        assertThat(actual).isEqualTo(plaintext)
    }

    @Test
    fun testSealUnseal_when1KilobytePlaintext_thenUnsealsFine() {
        val instance = AesCtrCipher(context(), dummyKey1)

        val plaintext = ByteArray(1024)
        val sealed = instance.seal(plaintext)
        assertThat(sealed.ciphertext.size).isEqualTo(1024)

        val actual = instance.unseal(sealed)
        assertThat(actual).isEqualTo(plaintext)
    }

    @Test
    fun testSealUnseal_whenFlippingBitInCiphertext_thenDecryptsWithFlippedBit() {
        val instance = AesCtrCipher(context(), dummyKey1)

        val plaintext = byteArrayOf(0, 1, 2, 3, 4)
        val sealed = instance.seal(plaintext)
        sealed.ciphertext[0] = sealed.ciphertext[0] xor 0x01

        val actual = instance.unseal(sealed)
        assertThat(actual).isEqualTo(byteArrayOf(0 xor 0x01, 1, 2, 3, 4))
    }

    @Test
    fun testSealUnseal_whenChangingKey_thenDecryptsToSomethingDifferent() {
        val instanceEnc = AesCtrCipher(context(), dummyKey1)
        val plaintext = byteArrayOf(0, 1, 2, 3, 4)
        val ciphertext = instanceEnc.seal(plaintext)

        val instanceDec = AesCtrCipher(context(), dummyKey2)
        val actual = instanceDec.unseal(ciphertext)
        assertThat(actual).isNotEqualTo(plaintext)
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext
}
