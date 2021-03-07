package com.coverdrop.lib

import java.time.Instant

/**
 * A remote contact is identified by its [id]. Where [name] and [pubkey] change, the original
 * record with the given id is meant to be updated.
 */
data class RemoteContact(val id: Long, val name: String, val pubkey: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RemoteContact

        if (id != other.id) return false
        if (name != other.name) return false
        if (!pubkey.contentEquals(other.pubkey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + pubkey.contentHashCode()
        return result
    }
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
    fun hasUnreadMessage(): Boolean = messages.any { it.time.isAfter(lastOpened) }

    fun withAddedMessage(message: ChatMessage) = ChatLog(
        remoteContact = remoteContact,
        messages = messages.toMutableList().run { add(message); toList() },
        lastOpened = lastOpened
    )

    fun withMarkedOpened(timeStamp: Instant) = ChatLog(remoteContact, messages, timeStamp)
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
    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false

        other as PublicKeyHolder
        if (!public.contentEquals(other.public)) return false

        return true
    }

    override fun hashCode(): Int = public.contentHashCode()
}

/**
 * A private and public key pair
 */
data class PrivateKeyPairHolder(val private: ByteArray, val public: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (javaClass != other?.javaClass) return false

        other as PrivateKeyPairHolder
        if (!private.contentEquals(other.private)) return false
        if (!public.contentEquals(other.public)) return false

        return true
    }

    override fun hashCode(): Int = 31 * private.contentHashCode() + public.contentHashCode()
}
