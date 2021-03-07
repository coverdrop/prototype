package com.coverdrop.lib

import android.content.Context

internal data class UnlockedPrivateDataState(
    val chatLogs: List<ChatLog>,
    val privateKeyPair: PrivateKeyPairHolder
) {
    companion object {
        fun empty() {
            TODO("stub")
        }
    }

    fun assertInvariants() {
        TODO("stub")
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

    fun getChatLogs(): List<ChatLog> {
        TODO("stub")
    }

    fun markOpened(chatLog: ChatLog) {
        TODO("stub")
    }

    fun sendMessage(remoteContactId: Long, chatMessage: ChatMessage) {
        TODO("stub")
    }

    fun createOrGetChatLog(remoteContactId: Long): ChatLog {
        TODO("stub")
    }

    fun addDummyMessages(remoteContactId: Long) {
        TODO("stub")
    }

    fun debugForceSync() {
        TODO("stub")
    }
}
