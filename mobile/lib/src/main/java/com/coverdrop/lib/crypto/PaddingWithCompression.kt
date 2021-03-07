package com.coverdrop.lib.crypto

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream


internal const val PADDED_BOX_HEADER_LENGTH = 4

data class Padded(val byteArray: ByteArray, val expectedLength: Int) {
    init {
        if (byteArray.size != expectedLength)
            throw IllegalArgumentException("bad pad box length! Got ${byteArray.size} but expected $expectedLength")
    }
}

/**
 * Creates bytesarrays of constant size of up to [Int.MAX_VALUE] length by first compressing the payload and then
 * adding `0x00` bytes.
 */
class PaddingWithCompression {

    fun pad(content: ByteArray, outputLength: Int): Padded {
        val compressedBytes = compress(content)
        if (compressedBytes.size > outputLength - PADDED_BOX_HEADER_LENGTH)
            throw IllegalArgumentException("content too large")

        val bytes = ByteBuffer.allocateDirect(outputLength)
        bytes.rewind()
        bytes.putInt(compressedBytes.size)
        bytes.put(compressedBytes)

        return Padded(bytes.readAsByteArray(), outputLength)
    }

    fun unpad(padded: Padded): ByteArray {
        val bytes = ByteBuffer.wrap(padded.byteArray)

        val compressedBytesSize = bytes.getInt(0)
        val compressedBytes = ByteArray(compressedBytesSize)

        bytes.position(4)
        bytes.get(compressedBytes)

        return uncompress(compressedBytes)
    }

    private fun compress(content: ByteArray): ByteArray {
        ByteArrayOutputStream().use { outStream ->
            GZIPOutputStream(outStream).use { gzip ->
                gzip.write(content)
            }
            Log.w("PaddedCompressedBox", "${content.size} -> ${outStream.toByteArray().size}")
            return outStream.toByteArray()
        }
    }

    private fun uncompress(compressed: ByteArray): ByteArray {
        ByteArrayInputStream(compressed).use { inStream ->
            GZIPInputStream(inStream).use { gzip ->
                return gzip.readBytes()
            }
        }
    }
}
