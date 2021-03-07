package com.coverdrop.lib

import android.app.Application
import android.content.Context
import com.coverdrop.lib.crypto.MemorablePassphrase
import java.util.*

const val COVERDROP_NAMESPACE = "coverdroplib"

interface CoverdropApplication {
    fun downloadRemoteContacts(): List<RemoteContact>
    fun downloadSgxKeys(): Pair<ByteArray, ByteArray>
    fun downloadDeaddrop(): List<ByteArray>
    fun sendOutgoingMessage(message: ByteArray?)
}

class CoverdropLib private constructor(
    val context: Context,
    val application: CoverdropApplication,
    val namespace: String = COVERDROP_NAMESPACE
) {
    companion object {
        private var sInstance: CoverdropLib? = null

        @Synchronized
        fun onAppInit(applicationContext: Application, coverdropApplication: CoverdropApplication) {
            TODO("stub")
        }

        @Synchronized
        fun getInstance(): CoverdropLib {
            TODO("stub")
        }
    }

    fun getSyncIntervalInSeconds(): Long {
        TODO("stub")
    }

    fun getNextSyncTime(): Date {
        TODO("stub")
    }

    fun getCoverdropPublicData(): CoverdropPublicData {
        TODO("stub")
    }

    @Synchronized
    fun createOrUnlockCoverdropUserSession(passphrase: MemorablePassphrase) {
        TODO("stub")
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        TODO("stub")
    }

    @Synchronized
    fun getUnlockedCoverdropUserSession(): CoverdropPrivateDataImpl {
        TODO("stub")
    }

    @Synchronized
    private fun ensureCoverdropUserSession(): CoverdropPrivateDataImpl {
        TODO("stub")
    }

    fun sendOutgoingMessage() {
        TODO("stub")
    }

    fun generateDummyMessagesIfNone() {
        TODO("stub")
    }

    fun doRegularBackgroundOperation() {
        TODO("stub")
    }
}

/**
 * A [CoverdropPublicData] is used by the regular synchronisation service and does not require
 * the CoverDrop to be unlocked. Any and all data in here is must be non-sensitive if persisted.
 */
interface CoverdropPublicData {

    fun setSgxPubKey(publicKey: ByteArray)

    fun getSgxPubKey(): ByteArray

    fun setSgxSignPubKey(publicKey: ByteArray)

    fun getSgxSignPubKey(): ByteArray

    fun setSyncIntervalMs(timeInMs: Long)

    fun getSyncIntervalMs(): Long

    fun setSyncNextTimeMs(timeInMs: Long)

    /**
     * Time of the next sync operations as Unix time stamp in MS. The result might be wrong until
     * the next sync if the clock of the device updates.
     *
     * This is to be used by UI only. The actual scheduling is done properly using a monotonic clock.
     */
    fun getSyncNextTimeMs(): Long

    fun addOrUpdateRemoteContact(remoteContact: RemoteContact)

    fun getRemoteContact(id: Long): RemoteContact

    fun getAllRemoteContacts(): List<RemoteContact>

    fun addIncomingMessages(blobs: List<ByteArray>)

    fun getPendingIncomingMessages(): List<ByteArray>

    fun clearIncomingMessages()

    fun addOutgoingMessage(blob: ByteArray)

    fun getPendingOutgoingMessages(): List<ByteArray>

    fun clearOutgoingMessages()

    fun syncRemoteContacts(downloadCallback: () -> List<RemoteContact>)

    fun updateSgxKeys(downloadCallback: () -> Pair<ByteArray, ByteArray>)
}


