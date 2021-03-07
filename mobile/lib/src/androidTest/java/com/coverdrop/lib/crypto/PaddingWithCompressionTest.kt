package com.coverdrop.lib.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaddingWithCompressionTest {

    private val PADDED_LENGTH = 1024

    @Test
    fun testInit_whenInit_thenNothing() {
        PaddingWithCompression()
    }

    @Test
    fun testGetBytes_whenEmptyContent_thenMatchesBoxSize() {
        val padded = PaddingWithCompression().pad(byteArrayOf(), PADDED_LENGTH)
        assertThat(padded.byteArray).hasSize(PADDED_LENGTH)
    }

    @Test
    fun testGetBytes_whenSomeContent_thenMatchesBoxSize() {
        val padded = PaddingWithCompression().pad("Hello World".toByteArray(), PADDED_LENGTH)
        assertThat(padded.byteArray).hasSize(PADDED_LENGTH)
    }

    @Test
    fun testSetGetContentCompressed_whenSomeContentLargerThanLimit_thenStillFits() {
        // create content that is 3x too large, but that will compress well
        val stringBuilder = StringBuilder()
        while (stringBuilder.length < 3 * PADDED_LENGTH) {
            stringBuilder.append("Hello World ")
        }
        val content = stringBuilder.toString().toByteArray()


        val padded = PaddingWithCompression().pad(content, PADDED_LENGTH)
        assertThat(padded.byteArray).hasSize(PADDED_LENGTH)

        val unpadded = PaddingWithCompression().unpad(padded)
        assertThat(unpadded).isEqualTo(content)
    }

}
