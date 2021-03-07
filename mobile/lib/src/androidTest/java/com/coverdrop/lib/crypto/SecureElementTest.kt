package com.coverdrop.lib.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown
import org.assertj.core.data.Percentage
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import kotlin.experimental.xor

@RunWith(AndroidJUnit4::class)
class SecureElementTest {

    private val namespace = "seboxtest"

    @Test
    fun testResetKey_whenCalled_thenNothingThrows() {
        SecureElement(context(), namespace).resetKey()
    }

    @Test
    fun testSealUnseal_whenGivenEmptyPlaintext_thenUnsealsFine() {
        val instance = SecureElement(context(), namespace)

        val plaintext = ByteArray(0)
        val sealed = instance.seal(plaintext)

        val actual = instance.unseal(sealed)
        assertThat(plaintext).isEqualTo(actual)
    }

    @Test
    fun testSealUnseal_when1KilobytePlaintext_thenUnsealsFine() {
        val instance = SecureElement(context(), namespace)

        val plaintext = ByteArray(1 * 1024)
        SecureRandom().nextBytes(plaintext)

        val sealed = instance.seal(plaintext)
        val actual = instance.unseal(sealed)
        assertThat(plaintext).isEqualTo(actual)
    }

    @Test(expected = javax.crypto.AEADBadTagException::class)
    fun testSealUnseal_whenFlippingBitInCiphertext_thenThrowsInvalidTag() {
        val instance = SecureElement(context(), namespace)

        val plaintext = ByteArray(0)
        val sealed = instance.seal(plaintext)
        sealed.ciphertext[0] = sealed.ciphertext[0] xor 0x01

        val actual = instance.unseal(sealed)
        assertThat(plaintext).isEqualTo(actual)
    }

    @Test(expected = javax.crypto.AEADBadTagException::class)
    fun testSealUnseal_whenResettingKey_thenThrowsInvalidTag() {
        val instance = SecureElement(context(), namespace)

        val plaintext = ByteArray(0)
        val ciphertext = instance.seal(plaintext)

        instance.resetKey()

        val actual = instance.unseal(ciphertext)
        assertThat(plaintext).isEqualTo(actual)
    }

    @Test
    fun testSealUnseal_whenCorrectAndIncorrect_thenRoughlySameDuration() {
        val instance = SecureElement(context(), namespace)
        val iterations = 20

        val plaintext = ByteArray(512)
        SecureRandom().nextBytes(plaintext)

        // measure correct decryption
        val sealed = instance.seal(plaintext)
        val startCorrectTimeNs = System.nanoTime()
        for (i in 1..iterations) instance.unseal(sealed)
        val timeCorrectNs = System.nanoTime() - startCorrectTimeNs

        // measure failing decryption
        val sealedBad = instance.seal(plaintext)
        sealedBad.ciphertext[0] = sealedBad.ciphertext[0] xor 0x01
        val startBadTimeNs = System.nanoTime()
        for (i in 1..iterations) {
            try {
                instance.unseal(sealedBad)
                failBecauseExceptionWasNotThrown<Unit>(AEADBadTagException::class.java)
            } catch (ignore: AEADBadTagException) {
                // ignore
            }
        }
        val timeBadNs = System.nanoTime() - startBadTimeNs

        assertThat(timeBadNs).isCloseTo(timeCorrectNs, Percentage.withPercentage(20.0))
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext
}
