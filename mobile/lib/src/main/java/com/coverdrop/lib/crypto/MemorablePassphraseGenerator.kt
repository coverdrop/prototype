package com.coverdrop.lib.crypto

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.coverdrop.lib.R
import java.security.SecureRandom
import java.util.*
import kotlin.collections.HashSet

data class MemorablePassphrase(val value: String)

/**
 * Class for generating and verifying memorable passphrase based on the EFF's large word list.
 *
 * See: https://www.eff.org/deeplinks/2016/07/new-wordlists-random-passphrases (CC-BY)
 */
class MemorablePassphraseGenerator(private val context: Context) {

    private var mWordlist: Array<String> = emptyArray()

    @VisibleForTesting
    internal fun loadWordlist(): Array<String> {
        if (mWordlist.isEmpty()) {
            val inputStream = context.resources.openRawResource(R.raw.eff_large_wordlist)
            mWordlist = inputStream.use {
                it.bufferedReader()
                    .lines()
                    .map { line -> line.split("\t")[1] }
                    .toArray { size -> arrayOfNulls<String>(size) }
            }
        }
        return mWordlist
    }

    /**
     * @return A randomly generated passphrase using the underlying word list
     */
    fun generatePassphrase(numberOfWords: Int = 3): MemorablePassphrase {
        if (numberOfWords < 1) throw IllegalArgumentException("numberOfWords must be >= 1")

        val wordlist = loadWordlist()
        val secureRandom = SecureRandom()

        val words = Array(numberOfWords) { wordlist[secureRandom.nextInt(wordlist.size)] }
        return normalize(words.joinToString(separator = " "))
    }

    /**
     * Checks if a passphrase could have been generated by this class. This is helpful to detect
     * user errors such as typos without compromising security.
     *
     * @return True iff the passphrase could have been generated by the [generatePassphrase] method.
     */
    fun isValidPassphrase(passphrase: String): Boolean {
        if (passphrase.isEmpty()) throw IllegalArgumentException("passphrase must not be empty")

        val wordset = HashSet<String>()
        wordset.addAll(loadWordlist())

        val normalizedPassphrase = normalize(passphrase)
        val words = normalizedPassphrase.value.split(" ")

        return words.all { word -> wordset.contains(word) }
    }

    fun isValidPassphrase(passphrase: MemorablePassphrase): Boolean =
        isValidPassphrase(passphrase.value)

    /**
     * @return True iff the two given passphrases are equal after being normalized.
     */
    fun passphrasesMatch(
        passphraseA: String,
        passphraseB: String
    ): Boolean {
        return normalize(passphraseA).equals(normalize(passphraseB))
    }

    /**
     * Normalizes the given passphrase by (a) removing leading and trailing whitespace and (b) by
     * setting to lower case.
     */
    fun normalize(passphrase: String): MemorablePassphrase =
        MemorablePassphrase(passphrase.trim().toLowerCase(Locale.UK))
}

