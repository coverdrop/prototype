package com.coverdrop.lib

import android.app.Application
import android.content.Context
import android.util.Log
import com.coverdrop.lib.background.CoverdropBackgroundService
import com.coverdrop.lib.crypto.MemorablePassphrase
import com.coverdrop.lib.crypto.MessageCipher
import com.coverdrop.lib.crypto.byteArrayToHex
import java.time.Instant
import java.util.*

const val COVERDROP_NAMESPACE = "coverdroplib"
internal const val COVERDROP_SYNC_INTERVAL_DEFAULT_MS = 60 * 60 * 1000L // 1 hour
internal const val COVERDROP_SYNC_INTERVAL_OFF = -1L

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
            if (sInstance == null) sInstance =
                CoverdropLib(applicationContext, coverdropApplication)

            // TODO: Move to background threads (or even service)
            getInstance().ensureCoverdropUserSession()

            getInstance().getCoverdropPublicData().syncRemoteContacts {
                coverdropApplication.downloadRemoteContacts()
            }

            getInstance().getCoverdropPublicData().updateSgxKeys {
                coverdropApplication.downloadSgxKeys()
            }

            // during the first start this will set the default sync interval
            getInstance().setBackgroundServiceEnabled(true)
        }

        @Synchronized
        fun getInstance(): CoverdropLib {
            if (sInstance == null) throw IllegalStateException("Coverdrop must be initialised through `onAppInit` before usage")
            return sInstance!!
        }
    }

    private var coverdropPrivateDataImpl: CoverdropPrivateDataImpl? = null

    fun getSyncIntervalInSeconds() = getCoverdropPublicData().getSyncIntervalMs() / 1000

    fun getNextSyncTime() = Date.from(Instant.ofEpochMilli(getCoverdropPublicData().getSyncNextTimeMs()))

    fun getCoverdropPublicData(): CoverdropPublicData {
        return CoverdropPublicDataImpl(context, namespace = namespace)
    }

    @Synchronized
    fun createOrUnlockCoverdropUserSession(passphrase: MemorablePassphrase) {
        ensureCoverdropUserSession().also {
            it.lock()
            it.unlock(passphrase)
        }
    }

    fun setBackgroundServiceEnabled(enabled: Boolean) {
        if (enabled) {
            if (getCoverdropPublicData().getSyncIntervalMs() > 0) {
                // do not do anything if there's already an alarm
                return
            }
            getCoverdropPublicData().setSyncIntervalMs(COVERDROP_SYNC_INTERVAL_DEFAULT_MS)
            CoverdropBackgroundService.schedule(context)
        } else {
            getCoverdropPublicData().setSyncIntervalMs(COVERDROP_SYNC_INTERVAL_OFF)
            CoverdropBackgroundService.schedule(context)
        }
    }

    @Synchronized
    fun getUnlockedCoverdropUserSession(): CoverdropPrivateDataImpl {
        val session = coverdropPrivateDataImpl
        if (session == null || !session.isUnlocked()) {
            throw IllegalStateException("Must call createOrUnlockCoverdropUserSession() first")
        }
        return session
    }

    @Synchronized
    private fun ensureCoverdropUserSession(): CoverdropPrivateDataImpl {
        coverdropPrivateDataImpl?.let { return it }

        CoverdropPrivateDataImpl(context, getCoverdropPublicData(), namespace).also {
            it.ensureAndTouch()
            coverdropPrivateDataImpl = it
            return it
        }
    }

    /** Called from the background service independent of any user actions */
    fun doRegularBackgroundOperation() {
        generateDummyMessagesIfNone()
        sendOutgoingMessage()
        downloadDeaddrop()
    }

    private fun generateDummyMessagesIfNone() {
        val publicData = getCoverdropPublicData()
        if (publicData.getPendingOutgoingMessages().isEmpty()) {
            Log.d("CoverdropLib", "Creating a new dummy message")
            val publicKeySgx = getCoverdropPublicData().getSgxPubKey()
            publicData.addOutgoingMessage(MessageCipher().createDummyMessage(publicKeySgx))
        }
    }

    private fun sendOutgoingMessage() {
        val message = getCoverdropPublicData().popPendingOutgoingMessage()
        Log.d("CoverdropLib", "Sending message: " + byteArrayToHex(message))
        application.sendOutgoingMessage(
            message
        )
    }

    private fun downloadDeaddrop() {
        val publicData = getCoverdropPublicData()
        val blindedMessages = application.downloadDeaddrop()
        Log.d("CoverdropLib", "Downloaded ${blindedMessages.size} messages from /deaddrop")
        publicData.addIncomingMessages(blindedMessages)
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

    fun popPendingOutgoingMessage(): ByteArray

    fun clearOutgoingMessages()

    fun syncRemoteContacts(downloadCallback: () -> List<RemoteContact>)

    fun updateSgxKeys(downloadCallback: () -> Pair<ByteArray, ByteArray>)
}


