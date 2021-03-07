package com.coverdrop.lib.mocks

import com.coverdrop.lib.CoverdropPublicData
import com.coverdrop.lib.RemoteContact
import com.coverdrop.lib.crypto.hexToByteArray

/**
 * A mock for the [CoverdropPublicData] interface. Upon initialisation it is populated with dummy data. Used for tests.
 */
class CoverdropPublicDataMock : CoverdropPublicData {

    val mRemoteContacts = mutableListOf(
        RemoteContact(1, "Alice", "00112233".toByteArray()),
        RemoteContact(2, "Bob", "44556677".toByteArray())
    )

    val mIncomingMessages = mutableListOf<ByteArray>()
    val mOutgoingMessages = mutableListOf<ByteArray>()

    var mSyncIntervalMs = -1L
    var mSyncNextTimeMs = -1L

    var mSgxPubKey = hexToByteArray("AABBCC01")
    var mSgxSignPubKey = hexToByteArray("AABBCC02")

    override fun setSgxPubKey(publicKey: ByteArray) {
        mSgxPubKey = publicKey
    }

    override fun getSgxPubKey(): ByteArray = mSgxPubKey

    override fun setSgxSignPubKey(publicKey: ByteArray) {
        mSgxSignPubKey = publicKey
    }

    override fun getSgxSignPubKey(): ByteArray = mSgxSignPubKey

    override fun setSyncIntervalMs(timeInMs: Long) {
        mSyncIntervalMs = timeInMs
    }

    override fun getSyncIntervalMs(): Long {
        return mSyncIntervalMs
    }

    override fun setSyncNextTimeMs(timeInMs: Long) {
        mSyncNextTimeMs = timeInMs
    }

    override fun getSyncNextTimeMs(): Long {
        return mSyncNextTimeMs
    }

    override fun addOrUpdateRemoteContact(remoteContact: RemoteContact) {
        val index = mRemoteContacts.indexOfFirst { it.id == remoteContact.id }
        if (index >= 0) {
            mRemoteContacts[index] = remoteContact
        } else {
            mRemoteContacts.add(remoteContact)
        }
    }

    override fun getRemoteContact(id: Long): RemoteContact {
        return mRemoteContacts.first { it.id == id }
    }

    override fun getAllRemoteContacts(): List<RemoteContact> {
        return mRemoteContacts.toList()
    }

    override fun addIncomingMessages(blobs: List<ByteArray>) {
        mIncomingMessages.addAll(blobs)
    }

    override fun getPendingIncomingMessages(): List<ByteArray> = mIncomingMessages

    override fun clearIncomingMessages() = mIncomingMessages.clear()

    override fun addOutgoingMessage(blob: ByteArray) {
        mOutgoingMessages.add(blob)
    }

    override fun getPendingOutgoingMessages(): List<ByteArray> = mOutgoingMessages

    override fun popPendingOutgoingMessage(): ByteArray {
        val msg = mOutgoingMessages.get(0)
        mOutgoingMessages.removeAt(0)
        return msg
    }

    override fun clearOutgoingMessages() = mOutgoingMessages.clear()

    override fun syncRemoteContacts(downloadCallback: () -> List<RemoteContact>) {
        TODO("Not yet implemented")
    }

    override fun updateSgxKeys(downloadCallback: () -> Pair<ByteArray, ByteArray>) {
        TODO("Not yet implemented")
    }

}
