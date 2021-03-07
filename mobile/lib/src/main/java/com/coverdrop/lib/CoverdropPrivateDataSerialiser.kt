package com.coverdrop.lib

import java.nio.ByteBuffer
import java.time.Instant

private const val SERIALIZER_VERSION: Int = 0x02

/**
 * Class to map between [UnlockedPrivateDataState] objects and a [ByteArray] representation.
 *
 * The data format is as follows.
 *
 * ```
 * version: Int
 * priv_key_length: Int
 * priv_key_bytes: ByteArray(priv_key_length)
 * pub_key_length: Int
 * pub_key_bytes: ByteArray(pub_key_length)
 * num_logs: Int
 *   log0_remote_contact_id: Int
 *   log0_timestamp_last_opened: Long
 *   log0_message_count: Int
 *     log0_message0_sender_id: Int
 *     log0_message0_timestamp: Long
 *     log0_message0_text_length: Int
 *     log0_message0_text: ByteArray(log0_message0_text_length)
 *     ...
 *   ...
 * ```
 *
 * TODO: This should be updated to use Protobufs or similar
 */
internal class CoverdropPrivateDataSerialiser(
    private val publicData: CoverdropPublicData,
    private val maxSize: Int = COVERDROP_BLOB_PADDED_LENGTH
) {

    fun load(byteArray: ByteArray): UnlockedPrivateDataState {
        if (byteArray.size > maxSize) throw IllegalArgumentException("ByteArray too long")

        val byteBuffer = ByteBuffer.wrap(byteArray)
        if (byteBuffer.int != SERIALIZER_VERSION) throw IllegalArgumentException("Bad version header")

        val privateKeyPair = readPrivateKeyPairHolder(byteBuffer)
        val chatLogs = readChatlogs(byteBuffer)
        return UnlockedPrivateDataState(
            chatLogs,
            privateKeyPair
        )
    }

    fun save(state: UnlockedPrivateDataState): ByteArray {
        val byteBuffer = ByteBuffer.allocate(maxSize)

        byteBuffer.putInt(SERIALIZER_VERSION)
        writePrivateKeyPairHolder(byteBuffer, state.privateKeyPair)
        writeChatlogs(byteBuffer, state.chatLogs)

        return byteBuffer.array()
    }

    //
    // PrivateKeyPairHolder
    //

    private fun writePrivateKeyPairHolder(
        byteBuffer: ByteBuffer,
        privateKeyPair: PrivateKeyPairHolder
    ) {
        writeByteArray(byteBuffer, privateKeyPair.private)
        writeByteArray(byteBuffer, privateKeyPair.public)
    }

    private fun readPrivateKeyPairHolder(byteBuffer: ByteBuffer): PrivateKeyPairHolder {
        return PrivateKeyPairHolder(
            readByteArray(byteBuffer),
            readByteArray(byteBuffer)
        )
    }

    //
    // List<ChatLog>
    //

    private fun writeChatlogs(byteBuffer: ByteBuffer, chatLogs: List<ChatLog>) {
        byteBuffer.putInt(chatLogs.size)
        for (chatLog in chatLogs) {
            writeChatLog(byteBuffer, chatLog)
        }
    }

    private fun readChatlogs(byteBuffer: ByteBuffer) =
        List(byteBuffer.int) { readChatlog(byteBuffer) }

    //
    // ChatLog
    //

    private fun readChatlog(byteBuffer: ByteBuffer): ChatLog {
        val remoteContact = readRemoteContact(byteBuffer)!!
        val dateLastOpened = Instant.ofEpochSecond(byteBuffer.long)
        val messageCount = byteBuffer.int

        return ChatLog(
            remoteContact = remoteContact,
            messages = List(messageCount) { readMessage((byteBuffer)) },
            lastOpened = dateLastOpened
        )
    }

    private fun writeChatLog(byteBuffer: ByteBuffer, chatLog: ChatLog) {
        writeRemoteContact(byteBuffer, chatLog.remoteContact)
        byteBuffer.putLong(chatLog.lastOpened.epochSecond)
        byteBuffer.putInt(chatLog.messages.size)

        for (message in chatLog.messages) {
            writeMessage(byteBuffer, message)
        }
    }

    //
    // ChatMessage
    //

    private fun readMessage(byteBuffer: ByteBuffer): ChatMessage {
        val remoteContact = readRemoteContact(byteBuffer)
        val timeStamp = Instant.ofEpochSecond(byteBuffer.long)

        val textBytes = readByteArray(byteBuffer)
        return ChatMessage(
            remoteContact = remoteContact,
            text = textBytes.toString(charset = Charsets.UTF_8),
            time = timeStamp
        )
    }

    private fun writeMessage(byteBuffer: ByteBuffer, message: ChatMessage) {
        writeRemoteContact(byteBuffer, message.remoteContact)
        byteBuffer.putLong(message.time.epochSecond)

        val textBytes = message.text.toByteArray(charset = Charsets.UTF_8)
        writeByteArray(byteBuffer, textBytes)
    }

    //
    // RemoteContact
    //

    private fun readRemoteContact(byteBuffer: ByteBuffer) =
        when (val remoteContactId = byteBuffer.long) {
            -1L -> null
            else -> publicData.getRemoteContact(remoteContactId)
        }

    private fun writeRemoteContact(byteBuffer: ByteBuffer, remoteContact: RemoteContact?) {
        val remoteId = when (remoteContact) {
            null -> -1
            else -> remoteContact.id
        }
        byteBuffer.putLong(remoteId)
    }

    //
    // ByteArray
    //

    private fun writeByteArray(byteBuffer: ByteBuffer, array: ByteArray) {
        byteBuffer.putInt(array.size)
        byteBuffer.put(array)
    }

    private fun readByteArray(byteBuffer: ByteBuffer): ByteArray {
        val array = ByteArray(byteBuffer.getInt())
        byteBuffer.get(array)
        return array
    }
}
