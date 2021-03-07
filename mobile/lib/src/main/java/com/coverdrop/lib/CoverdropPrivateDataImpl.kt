package com.coverdrop.lib

import android.content.Context
import android.util.Log
import com.coverdrop.lib.crypto.*
import java.nio.charset.Charset
import java.time.Duration
import java.time.Instant
import javax.crypto.AEADBadTagException

/** The total amount of data that CoverDrop will store */
internal const val COVERDROP_BLOB_PADDED_LENGTH = 4 * 1024;

internal data class UnlockedPrivateDataState(
    val chatLogs: List<ChatLog>,
    val privateKeyPair: PrivateKeyPairHolder
) {
    companion object {
        fun empty() = UnlockedPrivateDataState(
            chatLogs = listOf(),
            privateKeyPair = PrivateKeyPairHolder(byteArrayOf(), byteArrayOf())
        )
    }

    fun assertInvariants() {
        // there must be no more than one `ChatLog` per `RemoteContact`
        val idSet = HashSet<Long>()
        for (chatLog in chatLogs) {
            val remoteId = chatLog.remoteContact.id
            if (remoteId in idSet) throw IllegalStateException("Bad invariant")
            idSet.add(remoteId)
        }
    }
}

/**
 * @param namespace The namespace can be changed for tests to avoid conflicts with the app state
 */
class CoverdropPrivateDataImpl(
    private val context: Context,
    private val coverdropPublicData: CoverdropPublicData,
    private val namespace: String,
) {
    private val coverdropBox = CoverdropBox(context, namespace)
    private val coverdropPrivateDataSerialiser = CoverdropPrivateDataSerialiser(coverdropPublicData)

    private var unlockedState: UnlockedPrivateDataState? = null
    private var unlockedPassphrase: MemorablePassphrase? = null

    internal fun ensureAndTouch() {
        if (!coverdropBox.exists()) {
            // save an empty private data blob with a randomly chosen passphrase
            val passphrase = MemorablePassphraseGenerator(context).generatePassphrase()
            createNew(passphrase)
        }
        coverdropBox.touch()
    }

    internal fun unlock(passphrase: MemorablePassphrase) {
        if (isUnlocked()) return  // unlocking is idempotent

        try {
            // load content
            val padded = coverdropBox.load(passphrase)
            val content = PaddingWithCompression().unpad(padded)
            unlockedState = coverdropPrivateDataSerialiser.load(content)
            unlockedPassphrase = passphrase

            // handle messages that have arrived in the mean time
            processPendingSyncData()
        } catch (badTag: AEADBadTagException) {
            // if the passphrase is wrong, we want to erase everything from disk and then start
            // a new session from that
            coverdropBox.wipe()
            createNew(passphrase)
            unlock(passphrase)
        }
    }

    private fun createNew(passphrase: MemorablePassphrase) {
        // gen public and private key
        val keypair = MessageCipher().generatePrivateKeyPair()

        // update public and private state accordingly
        val state = UnlockedPrivateDataState(
            chatLogs = emptyList(),
            privateKeyPair = keypair
        )

        save(state, passphrase);
    }

    internal fun lock() {
        unlockedState = null
        unlockedPassphrase = null
    }

    fun debugForceSync() {
        processPendingSyncData()
    }

    internal fun processPendingSyncData() {
        assertUnlocked()

        // new state
        val state = unlockedState!!
        val chatLogs = state.chatLogs.toMutableList()

        // get incoming messages
        val incomingMessages = coverdropPublicData.getPendingIncomingMessages()

        // handle them one-by-one
        val messageCipher = MessageCipher()
        for (blindedMessage in incomingMessages) {
            for (remoteContact in state.chatLogs.map { it.remoteContact }) {
                try {
                    val message = messageCipher.decrypt(
                        publicKeySignSgx = coverdropPublicData.getSgxSignPubKey(),
                        publicKeySender = remoteContact.pubkey,
                        privateKeyRecipient = state.privateKeyPair.private,
                        packet = blindedMessage
                    )
                    val chatMessage =
                        ChatMessage(remoteContact, message.toString(Charset.defaultCharset()), Instant.now())

                    // TODO: filter out duplicate messages
                    addMessageToMutableChatlogs(remoteContact.id, chatMessage, chatLogs)
                } catch (e: Exception) {
                    // fail silently
                } catch (e: AssertionError) {
                    // fail silently
                }
            }
        }
        coverdropPublicData.clearIncomingMessages()

        // persist new state
        unlockedState = UnlockedPrivateDataState(chatLogs, state.privateKeyPair)
        save(unlockedState!!, unlockedPassphrase!!)
    }

    fun getChatLogs(): List<ChatLog> {
        assertUnlocked()
        return unlockedState!!.chatLogs
    }

    fun sendMessage(remoteContactId: Long, chatMessage: ChatMessage) {
        assertUnlocked()

        // new state
        val state = unlockedState!!
        val chatLogs = state.chatLogs.toMutableList()

        // add message to the first matching one (or create a new one)
        addMessageToMutableChatlogs(remoteContactId, chatMessage, chatLogs)

        // encrypt and add to outgoing messages
        val messageCipher = MessageCipher()
        val remoteContact = coverdropPublicData.getRemoteContact(remoteContactId)
        val ciphertext = messageCipher.encrypt(
            publicKeySender = state.privateKeyPair.public,
            publicKeySgx = coverdropPublicData.getSgxPubKey(),
            publicKeyReceiver = remoteContact.pubkey,
            message = chatMessage.text.toByteArray(),
            realMessage = true
        )
        coverdropPublicData.addOutgoingMessage(ciphertext)

        // persist new state
        unlockedState = UnlockedPrivateDataState(chatLogs, state.privateKeyPair)
        save(unlockedState!!, unlockedPassphrase!!)
    }

    internal fun assertUnlocked() {
        if (!isUnlocked()) throw IllegalStateException("Must call unlock() first")
    }

    internal fun isUnlocked() = unlockedState != null

    private fun save(state: UnlockedPrivateDataState, passphrase: MemorablePassphrase) {
        val serialized = coverdropPrivateDataSerialiser.save(state)
        val padded = PaddingWithCompression().pad(serialized, COVERDROP_BLOB_PADDED_LENGTH)

        coverdropBox.store(padded, passphrase)
    }

    fun createOrGetChatLog(remoteContactId: Long): ChatLog {
        assertUnlocked()
        val chatLogs = getChatLogs().toMutableList()

        // return match if there's any
        for (chatLog in chatLogs) {
            if (chatLog.remoteContact.id == remoteContactId) {
                return chatLog
            }
        }

        val remoteContact = coverdropPublicData.getRemoteContact(remoteContactId)
        val newChatLog = ChatLog(remoteContact, emptyList(), Instant.MIN)
        chatLogs.add(newChatLog)

        // persist new state
        unlockedState = UnlockedPrivateDataState(chatLogs, unlockedState!!.privateKeyPair)
        save(unlockedState!!, unlockedPassphrase!!)

        return newChatLog
    }
//
//    fun addDummyMessages(remoteContactId: Long) {
//        assertUnlocked()
//        Log.e("CoverdropPrivateDataImpl", "A debug method has been called.")
//
//        // new state
//        val state = unlockedState!!
//        val chatLogs = state.chatLogs.toMutableList()
//
//        val remoteContact = coverdropPublicData.getRemoteContact(remoteContactId)
//        val dummyMessages = listOf(
//            ChatMessage(
//                null,
//                "Hello, I have information about money laundering at a large math company. How can I send you the documents?",
//                Instant.now() - Duration.ofHours(180)
//            ),
//            ChatMessage(
//                remoteContact,
//                "That sounds interesting. You can send us a copy via mail - details are on our website.",
//                Instant.now() - Duration.ofHours(178)
//            ),
//            ChatMessage(
//                null, "Have you received the documents?", Instant.now() - Duration.ofHours(75)
//            ),
//            ChatMessage(
//                remoteContact,
//                "We got your documents. Can you confirm that the name mentioned on page 12 is PYTHAGORAS?",
//                Instant.now() - Duration.ofHours(1)
//            ),
//            ChatMessage(
//                null, "Yes, it's the CEO of ALGEBRA-GEOMETRY Inc.", Instant.now() - Duration.ofMinutes(2)
//            )
//        )
//
//        for (message in dummyMessages) {
//            addMessageToMutableChatlogs(remoteContactId, message, chatLogs)
//        }
//
//        // persist new state
//        unlockedState = UnlockedPrivateDataState(chatLogs, state.privateKeyPair)
//        save(unlockedState!!, unlockedPassphrase!!)
//    }

    private fun addMessageToMutableChatlogs(
        remoteContactId: Long,
        message: ChatMessage,
        chatLogs: MutableList<ChatLog>
    ) {
        // if ChatLog does not exist yet, create it
        if (!chatLogs.any { it.remoteContact.id == remoteContactId }) {
            val remoteContact = coverdropPublicData.getRemoteContact(remoteContactId)
            chatLogs.add(ChatLog(remoteContact, emptyList(), Instant.MIN))
        }

        // add message to ChatLog
        val index = chatLogs.indexOfFirst { it.remoteContact.id == remoteContactId }
        chatLogs[index] = chatLogs[index].withAddedMessage(message)
    }
}
