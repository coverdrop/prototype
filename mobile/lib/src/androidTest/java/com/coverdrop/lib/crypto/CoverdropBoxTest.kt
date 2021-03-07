package com.coverdrop.lib.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coverdrop.lib.COVERDROP_BLOB_PADDED_LENGTH
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverdropBoxTest {

    private val content = "Hello World".toByteArray()
    private val passphrase1 = MemorablePassphraseGenerator(context()).generatePassphrase()
    private val passphrase2 = MemorablePassphraseGenerator(context()).generatePassphrase()
    private val namespace = "test"

    @Test
    fun testInit_whenInit_thenNothingHappens() {
        CoverdropBox(context(), namespace)
    }

    @Test
    fun testStoreLoad_whenSamePassphrase_thenRestored() {
        val instance = CoverdropBox(context(), namespace)

        val padded = PaddingWithCompression().pad(content, COVERDROP_BLOB_PADDED_LENGTH)
        instance.store(padded, passphrase1)

        val actual = instance.load(passphrase1)
        val actualContent = PaddingWithCompression().unpad(actual)
        assertThat(actualContent).isEqualTo(content)
    }

    @Test(expected = javax.crypto.AEADBadTagException::class)
    fun testStoreLoad_whenDifferentPassphrase_thenFails() {
        val instance = CoverdropBox(context(), namespace)

        val padded = PaddingWithCompression().pad(content, COVERDROP_BLOB_PADDED_LENGTH)
        instance.store(padded, passphrase1)

        val actual = instance.load(passphrase2)
        val actualContent = PaddingWithCompression().unpad(actual)
        assertThat(actualContent).isEqualTo(content)

    }

    @Test(expected = javax.crypto.AEADBadTagException::class)
    fun testStoreLoad_whenWipeInBetween_thenFails() {
        val instance = CoverdropBox(context(), namespace)

        val padded = PaddingWithCompression().pad(content, COVERDROP_BLOB_PADDED_LENGTH)
        instance.store(padded, passphrase1)

        instance.wipe()

        val actual = instance.load(passphrase1)
        val actualContent = PaddingWithCompression().unpad(actual)
        assertThat(actualContent).isEqualTo(content)
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext
}
