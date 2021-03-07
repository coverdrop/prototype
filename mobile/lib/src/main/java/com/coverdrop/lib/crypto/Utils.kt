package com.coverdrop.lib.crypto

import java.nio.ByteBuffer


internal fun ByteBuffer.readAsByteArray(): ByteArray {
    val bytes = ByteArray(capacity())
    position(0)
    get(bytes)
    return bytes
}

internal fun assertOrThrow(success: Boolean, message: String = "Assertion failed") {
    if (!success) {
        throw AssertionError(message)
    }
}

internal fun byteArrayToHex(byteArray: ByteArray): String {
    val sb = java.lang.StringBuilder(byteArray.size * 2)
    for (b in byteArray) {
        sb.append(String.format("%02x", b))
    }
    return sb.toString()
}

internal fun hexToByteArray(string: String): ByteArray {
    return ByteArray(string.length / 2) {
        string.substring(2 * it, 2 * it + 2).toInt(radix = 16).toByte()
    }
}
