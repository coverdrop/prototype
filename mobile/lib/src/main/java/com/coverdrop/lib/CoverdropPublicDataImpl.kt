package com.coverdrop.lib

import android.content.Context
import androidx.room.*
import com.coverdrop.lib.crypto.byteArrayToHex
import com.coverdrop.lib.crypto.hexToByteArray

private const val SYNC_INTERVAL_PREF_NAME = "coverdrop_sync_interval"
private const val SYNC_NEXT_RUN_PREF_NAME = "coverdrop_sync_next_run"
private const val SGX_PUB_PREF_NAME = "coverdrop_sgx_pub"
private const val SGX_SIGN_PUB_PREF_NAME = "coverdrop_sgx_sign_pub"

class CoverdropPublicDataImpl(
    val context: Context,
    val namespace: String
) : CoverdropPublicData {

    companion object {
        var currNamespace: String? = null
        var currDb: CoverdropPublicDatabase? = null

        fun getDbInstance(namespace: String, context: Context): CoverdropPublicDatabase {
            synchronized(this) {
                if (namespace != currNamespace) {
                    currDb = Room.databaseBuilder(
                        context.applicationContext,
                        CoverdropPublicDatabase::class.java,
                        "coverdrop_public_db_$namespace.sqlite"
                    ).build()

                    // TODO: add .shutdown() method to CoverDrop to avoid leaking the open sqlite db connection
                }
                return currDb!!
            }
        }
    }

    val db = getDbInstance(namespace, context)

    override fun setSgxPubKey(sgxPub: ByteArray) {
        getSharedPrefs().edit().apply {
            putString(SGX_PUB_PREF_NAME, byteArrayToHex(sgxPub))
            apply()
        }
    }

    override fun getSgxPubKey(): ByteArray =
        hexToByteArray(getSharedPrefs().getString(SGX_PUB_PREF_NAME, "")!!)

    override fun setSgxSignPubKey(sgxSignPub: ByteArray) {
        getSharedPrefs().edit().apply {
            putString(SGX_SIGN_PUB_PREF_NAME, byteArrayToHex(sgxSignPub))
            apply()
        }
    }

    override fun getSgxSignPubKey(): ByteArray =
        hexToByteArray(getSharedPrefs().getString(SGX_SIGN_PUB_PREF_NAME, "")!!)

    override fun setSyncIntervalMs(timeInMs: Long) {
        getSharedPrefs().edit().apply {
            putString(SYNC_INTERVAL_PREF_NAME, timeInMs.toString())
            apply()
        }
    }

    override fun getSyncIntervalMs(): Long {
        // awkward toString.toLong to support ListPreference in preference screens
        return getSharedPrefs().getString(
            SYNC_INTERVAL_PREF_NAME,
            COVERDROP_SYNC_INTERVAL_DEFAULT_MS.toString()
        )!!.toLong()
    }


    override fun setSyncNextTimeMs(timeInMs: Long) {
        getSharedPrefs().edit().apply {
            putLong(SYNC_NEXT_RUN_PREF_NAME, timeInMs)
            apply()
        }
    }

    override fun getSyncNextTimeMs(): Long = getSharedPrefs().getLong(
        SYNC_NEXT_RUN_PREF_NAME,
        -1
    )

    override fun addOrUpdateRemoteContact(remoteContact: RemoteContact) {
        val entry = RemoteContactEntry(remoteContact.id, remoteContact.name, remoteContact.pubkey)
        db.remoteContactDao().insert(entry)
    }

    override fun getRemoteContact(id: Long): RemoteContact {
        val entry = db.remoteContactDao().get(id)
        return RemoteContact(entry.id, entry.name, entry.pubkey)
    }

    override fun getAllRemoteContacts(): List<RemoteContact> {
        val entries = db.remoteContactDao().getAll()
        return entries.map { RemoteContact(it.id, it.name, it.pubkey) }
    }

    override fun addIncomingMessages(blobs: List<ByteArray>) =
        db.messageDao().insertAll(blobs.map { MessageEntry(it, "in") })

    override fun getPendingIncomingMessages(): List<ByteArray> =
        db.messageDao().getAll("in").map { it.blob }

    override fun clearIncomingMessages() =
        db.messageDao().deleteAllForDirection("in")

    override fun addOutgoingMessage(blob: ByteArray) =
        db.messageDao().insertAll(listOf(MessageEntry(blob, "out")))

    override fun getPendingOutgoingMessages(): List<ByteArray> =
        db.messageDao().getAll("out").map { it.blob }

    override fun popPendingOutgoingMessage(): ByteArray {
        val message = db.messageDao().getOldestForDirection("out")
        db.messageDao().removeMessageForId(message.id)
        return message.blob
    }

    override fun clearOutgoingMessages() =
        db.messageDao().deleteAllForDirection("out")

    override fun syncRemoteContacts(downloadCallback: () -> List<RemoteContact>) {
        val contacts = downloadCallback()
        contacts.forEach { addOrUpdateRemoteContact(it) }
    }

    override fun updateSgxKeys(downloadCallback: () -> Pair<ByteArray, ByteArray>) {
        val keys = downloadCallback()
        setSgxPubKey(keys.first)
        setSgxSignPubKey(keys.second)
    }

    internal fun deleteAll() {
        db.clearAllTables()
        getSharedPrefs().edit().apply {
            remove(SGX_PUB_PREF_NAME)
            remove(SGX_SIGN_PUB_PREF_NAME)
            apply()
        }
    }

    private fun getSharedPrefs() =
        context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
}

@Entity(tableName = "messages")
data class MessageEntry(
    @ColumnInfo(name = "blob") val blob: ByteArray,
    @ColumnInfo(name = "direction") val direction: String
) {
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0

    init {
        if (direction != "in" && direction != "out")
            throw IllegalArgumentException("Unexpected direction: '$direction'")
    }
}

@Entity(tableName = "contacts")
data class RemoteContactEntry(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "pubkey") val pubkey: ByteArray
)

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE direction=:direction")
    fun getAll(direction: String): List<MessageEntry>

    @Insert
    fun insertAll(messages: List<MessageEntry>)

    @Query("DELETE FROM messages WHERE direction=:direction")
    fun deleteAllForDirection(direction: String)

    @Query("SELECT * FROM messages WHERE direction=:direction ORDER BY id ASC LIMIT 1")
    fun getOldestForDirection(direction: String): MessageEntry

    @Query("DELETE FROM messages WHERE id=:id")
    fun removeMessageForId(id: Int)
}

@Dao
interface RemoteContactDao {

    @Query("SELECT * FROM contacts")
    fun getAll(): List<RemoteContactEntry>

    @Query("SELECT * FROM contacts WHERE id=:id")
    fun get(id: Long): RemoteContactEntry

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(contacts: RemoteContactEntry)
}

@Database(entities = [MessageEntry::class, RemoteContactEntry::class], version = 1)
abstract class CoverdropPublicDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun remoteContactDao(): RemoteContactDao
}
