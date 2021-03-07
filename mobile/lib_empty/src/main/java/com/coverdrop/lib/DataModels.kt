package com.coverdrop.lib

import java.time.Instant

/**
 * A remote contact is identified by its [id]. Where [name] and [pubkey] change, the original
 * record with the given id is meant to be updated.
 */
data class RemoteContact(val id: Long, val name: String, val pubkey: ByteArray) {
}

/**
 * A [ChatLog] is associated with a remote contact and has a list of messages. There should
 * only be at most one [ChatLog] per available [RemoteContact].
 */
data class ChatLog(
    val remoteContact: RemoteContact,
    val messages: List<ChatMessage>,
    val lastOpened: Instant
) {
    fun hasUnreadMessage(): Boolean {
        TODO("stub")
    }

    fun withAddedMessage(message: ChatMessage) {
        TODO("stub")
    }

    fun withMarkedOpened(timeStamp: Instant) {
        TODO("stub")
    }
}

/**
 * A [ChatMessage] is either send by a [RemoteContact] or by the user. In latter case [remoteContact] == null.
 */
data class ChatMessage(
    val remoteContact: RemoteContact?,
    val text: String,
    val time: Instant
)

/**
 * The UUID associated with this device
 */
data class UidHolder(val uid: Long)

/**
 * A public key
 */
data class PublicKeyHolder(val public: ByteArray) {
}

/**
 * A private and public key pair
 */
data class PrivateKeyPairHolder(val private: ByteArray, val public: ByteArray) {
}
