package com.coverdrop.lib.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class MemorablePassphraseGeneratorTest {

    @Test
    fun testLoadingWordlist_whenLoaded_thenLengthMatchesRawFile() {
        val wordlist = MemorablePassphraseGenerator(context()).loadWordlist()
        assertThat(wordlist).hasSize(7776)
    }

    @Test
    fun testGeneratingPassphrase_whenProvidedNumber_thenHasGivenNumberOfWords() {
        val passphrase1 = MemorablePassphraseGenerator(context())
            .generatePassphrase(numberOfWords = 1)
        assertThat(passphrase1.value).matches("\\w+")

        val passphrase3 = MemorablePassphraseGenerator(context())
            .generatePassphrase(numberOfWords = 3)
        assertThat(passphrase3.value).matches("\\w+ \\w+ \\w+")
    }

    @Test(expected = IllegalArgumentException::class)
    fun testGeneratingPassphrase_whenNumberOfWordsSmaller1_thenThrows() {
        MemorablePassphraseGenerator(context()).generatePassphrase(numberOfWords = 0)
    }

    @Test
    fun testGeneratingPassphrase_whenGeneratingTwoPassphrasesOnSameInstance_thenDifferent() {
        val instance = MemorablePassphraseGenerator(context())
        val passphraseA = instance.generatePassphrase()
        val passphraseB = instance.generatePassphrase()
        assertThat(passphraseA).isNotEqualTo(passphraseB)
    }

    @Test
    fun testGeneratingPassphrase_whenGeneratingTwoPassphrasesOnDifferentInstances_thenDifferent() {
        val passphraseA = MemorablePassphraseGenerator(context())
            .generatePassphrase()
        val passphraseB = MemorablePassphraseGenerator(context())
            .generatePassphrase()
        assertThat(passphraseA).isNotEqualTo(passphraseB)
    }

    @Test
    fun testIsValidPassphrase_whenGivenValidInput_thenTrue() {
        val instance = MemorablePassphraseGenerator(context())
        val passphrase = instance.generatePassphrase()
        assertThat(instance.isValidPassphrase(passphrase)).isTrue()
    }

    @Test
    fun testIsValidPassphrase_whenGivenValidInputWithNoise_thenTrue() {
        val instance = MemorablePassphraseGenerator(context())
        var passphrase = instance.generatePassphrase()

        val passphraseModified = " \t${passphrase.value.toUpperCase(Locale.UK)}  \n"

        assertThat(instance.isValidPassphrase(passphraseModified)).isTrue()
    }

    @Test
    fun testIsValidPassphrase_whenGivenInvalidInput_thenFalse() {
        val instance = MemorablePassphraseGenerator(context())

        val passphraseModified = "wrongwrong"

        assertThat(instance.isValidPassphrase(passphraseModified)).isFalse()
    }

    @Test
    fun testPassphrasesMatch_whenDifferent_thenFalse() {
        val instance = MemorablePassphraseGenerator(context())
        assertThat(instance.passphrasesMatch("a", "b")).isFalse()
        assertThat(instance.passphrasesMatch("a b c", "a b d")).isFalse()
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext
}
