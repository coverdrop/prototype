package com.coverdrop.lib

import android.content.Context


class CoverdropPublicDataImpl(
    val context: Context,
    val namespace: String
) : CoverdropPublicData {

    override fun setSgxPubKey(sgxPub: ByteArray) {
        TODO("stub")
    }

    override fun getSgxPubKey(): ByteArray {
        TODO("stub")
    }

    override fun setSgxSignPubKey(sgxSignPub: ByteArray) {
        TODO("stub")
    }

    override fun getSgxSignPubKey(): ByteArray {
        TODO("stub")
    }

    override fun setSyncIntervalMs(timeInMs: Long) {
        TODO("stub")
    }

    override fun getSyncIntervalMs(): Long {
        TODO("stub")
    }


    override fun setSyncNextTimeMs(timeInMs: Long) {
        TODO("stub")
    }

    override fun getSyncNextTimeMs(): Long {
        TODO("stub")
    }

    override fun addOrUpdateRemoteContact(remoteContact: RemoteContact) {
        TODO("stub")
    }

    override fun getRemoteContact(id: Long): RemoteContact {
        TODO("stub")
    }

    override fun getAllRemoteContacts(): List<RemoteContact> {
        TODO("stub")
    }

    override fun addIncomingMessages(blobs: List<ByteArray>) {
        TODO("stub")
    }

    override fun getPendingIncomingMessages(): List<ByteArray> {
        TODO("stub")
    }

    override fun clearIncomingMessages() {
        TODO("stub")
    }

    override fun addOutgoingMessage(blob: ByteArray) {
        TODO("stub")
    }

    override fun getPendingOutgoingMessages(): List<ByteArray> {
        TODO("stub")
    }

    override fun clearOutgoingMessages() {
        TODO("stub")
    }

    override fun syncRemoteContacts(downloadCallback: () -> List<RemoteContact>) {
        TODO("stub")
    }

    override fun updateSgxKeys(downloadCallback: () -> Pair<ByteArray, ByteArray>) {
        TODO("stub")
    }

}

