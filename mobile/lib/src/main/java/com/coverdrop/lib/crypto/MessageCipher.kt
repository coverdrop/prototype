package com.coverdrop.lib.crypto

import com.coverdrop.lib.PrivateKeyPairHolder
import com.goterl.lazycode.lazysodium.interfaces.Box
import com.goterl.lazycode.lazysodium.interfaces.Sign
import java.nio.ByteBuffer


internal const val MESSAGE_MAX_LENGTH = 255

/**
 * Class for encrypting and decrypting blinded messages that are sent between the device and the
 * webserver. All messages are padded and encrypted in layers such that in order to read them you
 * must have access to the SGX private key and the receiver's private key.
 */
class MessageCipher {

    val lazySodium = CoverdropSodiumProvider.instance

    /**
     * Encrypts the padded payload for a two hop routing with the inner encryption for the receiver
     * and the outer encryption for the SGX
     */
    fun encrypt(
        publicKeySender: ByteArray,
        publicKeySgx: ByteArray,
        publicKeyReceiver: ByteArray,
        message: ByteArray,
        realMessage: Boolean
    ): ByteArray {
        if (message.size > MESSAGE_MAX_LENGTH)
            throw IllegalArgumentException("Message text too long")
        if (publicKeySender.size != Box.PUBLICKEYBYTES)
            throw IllegalArgumentException("Wrong publicKeySender size")

        // create inner plaintext bytes
        val innerPlaintext = ByteBuffer.allocate(Box.PUBLICKEYBYTES + 1 + MESSAGE_MAX_LENGTH)
        innerPlaintext.put(publicKeySender)
        innerPlaintext.put(message.size.toByte())
        innerPlaintext.put(message) // not more than MESSAGE_MAX_LENGTH bytes

        // pad to maximum size
        while (innerPlaintext.remaining() > 0)
            innerPlaintext.put(0x00)

        // encrypt inner plaintext with receiver public key
        val innerCiphertext = asymmetricEnc(publicKeyReceiver, innerPlaintext.readAsByteArray())

        // build outer plaintext
        val outerPlaintext = ByteBuffer.allocate(1 + innerCiphertext.size)
        outerPlaintext.put(if (realMessage) 0x01 else 0x00)
        outerPlaintext.put(innerCiphertext)

        // encrypt outer plaintext with SGX public key as final ciphertext
        return asymmetricEnc(publicKeySgx, outerPlaintext.readAsByteArray())
    }

    /**
     * Decrypts an incoming message. As it will already have passed the SGX decryption, it will
     * contain the "inner ciphertext" and the signature of the SGX.
     */
    fun decrypt(
        publicKeySignSgx: ByteArray,
        publicKeySender: ByteArray,
        privateKeyRecipient: ByteArray,
        packet: ByteArray,
    ): ByteArray {
        // verify signature by the SGX
        val innerMessage = signatureVerify(publicKeySignSgx, packet)

        // decrypt the inner ciphertext
        val plaintext = asymmetricDec(publicKeySender, privateKeyRecipient, innerMessage)

        val buffer = ByteBuffer.wrap(plaintext)
        val messageSize = buffer.get()

        val message = ByteArray(messageSize.toInt())
        buffer.get(message)

        return message
    }

    fun createDummyMessage(publicKeySgx: ByteArray): ByteArray {
        val tempKeys = generatePrivateKeyPair()
        return encrypt(tempKeys.public, publicKeySgx, tempKeys.public, "".encodeToByteArray(), realMessage = false)
    }

    internal fun asymmetricEnc(publicKey: ByteArray, messageBytes: ByteArray): ByteArray {
        val ciphertext = ByteArray(Box.SEALBYTES + messageBytes.size)
        val success = lazySodium.cryptoBoxSeal(ciphertext, messageBytes, messageBytes.size.toLong(), publicKey)
        assertOrThrow(success, "encryption failed")
        return ciphertext
    }

    internal fun asymmetricDec(
        publicKeySender: ByteArray,
        privateKeyRecipient: ByteArray,
        cryptogram: ByteArray
    ): ByteArray {
        val plaintext = ByteArray(cryptogram.size - Box.MACBYTES - Box.NONCEBYTES)

        val nonce = cryptogram.copyOfRange(0, Box.NONCEBYTES)
        val ciphertext = cryptogram.copyOfRange(Box.NONCEBYTES, cryptogram.size)

        val success = lazySodium.cryptoBoxOpenEasy(
            plaintext,
            ciphertext,
            ciphertext.size.toLong(),
            nonce,
            publicKeySender,
            privateKeyRecipient
        )
        assertOrThrow(success, "decryption failed")
        return plaintext
    }

    internal fun signatureVerify(publicSignKey: ByteArray, signedMessage: ByteArray): ByteArray {
        val signature = signedMessage.copyOfRange(0, Sign.BYTES)
        val message = signedMessage.copyOfRange(Sign.BYTES, signedMessage.size)

        val success = lazySodium.cryptoSignVerifyDetached(
            signature,
            message,
            message.size,
            publicSignKey
        )
        assertOrThrow(success, "verify failed")
        return message
    }

    fun generatePrivateKeyPair(): PrivateKeyPairHolder {
        val keyPair = lazySodium.cryptoBoxKeypair()
        return PrivateKeyPairHolder(
            private = keyPair.secretKey.asBytes,
            public = keyPair.publicKey.asBytes
        )
    }

}
