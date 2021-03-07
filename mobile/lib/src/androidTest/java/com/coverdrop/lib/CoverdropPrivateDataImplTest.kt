import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coverdrop.lib.ChatMessage
import com.coverdrop.lib.CoverdropPrivateDataImpl
import com.coverdrop.lib.crypto.CoverdropBox
import com.coverdrop.lib.crypto.MemorablePassphraseGenerator
import com.coverdrop.lib.mocks.CoverdropPublicDataMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CoverdropPrivateDataImplTest {

    private val publicData = CoverdropPublicDataMock()
    private val namespace = "TEST"

    @Before
    fun setup() {
        // delete the persistence file (id any)
        CoverdropBox.getPersistedDataFile(context(), namespace).delete()
    }

    @Test
    fun testInit_whenCalled_thenNothingHappens() {
        CoverdropPrivateDataImpl(context(), publicData, namespace)
    }

    @Test
    fun ensureAndTouch_whenCalled_thenFileCreatedButNotUnlocked() {
        val instance = CoverdropPrivateDataImpl(context(), publicData, namespace)

        assertThat(CoverdropBox.getPersistedDataFile(context(), namespace)).doesNotExist()

        instance.ensureAndTouch()

        assertThat(instance.isUnlocked()).isFalse()
        assertThat(CoverdropBox.getPersistedDataFile(context(), namespace)).exists()
    }

    @Test
    fun testUnlockLock_whenCallSequence_thenInvariantsMaintained() {
        val instance = CoverdropPrivateDataImpl(context(), publicData, namespace)
        val passphrase = MemorablePassphraseGenerator(context()).generatePassphrase()
        instance.ensureAndTouch()

        assertThat(instance.isUnlocked()).isFalse()

        instance.unlock(passphrase)
        assertThat(instance.isUnlocked()).isTrue()

        instance.unlock(passphrase)
        assertThat(instance.isUnlocked()).isTrue()

        instance.lock()
        assertThat(instance.isUnlocked()).isFalse()

        instance.unlock(MemorablePassphraseGenerator(context()).generatePassphrase())
        assertThat(instance.isUnlocked()).isTrue()
    }

    @Test
    fun testModifyingState_whenLockedAndUnlockedWithSamePassphrase_thenContentRestored() {
        val instance = CoverdropPrivateDataImpl(context(), publicData, namespace)
        val passphrase = MemorablePassphraseGenerator(context()).generatePassphrase()

        instance.ensureAndTouch()
        instance.unlock(passphrase)

        val remoteContactA = publicData.mRemoteContacts[0]
        val remoteContactB = publicData.mRemoteContacts[1]

        val messageA1 = ChatMessage(remoteContactA, "Hello A", Instant.ofEpochSecond(100))
        val messageB1 = ChatMessage(remoteContactB, "Hello B", Instant.ofEpochSecond(101))
        val messageA2 = ChatMessage(remoteContactA, "Goodbye A", Instant.ofEpochSecond(102))

        for (message in listOf(messageA1, messageB1, messageA2))
            instance.sendMessage(message.remoteContact!!.id, message)

        // save and load under the hood
        instance.lock()
        instance.unlock(passphrase)

        // verify content
        val chatLogs = instance.getChatLogs()
        assertThat(chatLogs).hasSize(2)

        val chatLogA = chatLogs.first { it.remoteContact == remoteContactA }
        assertThat(chatLogA.messages).containsExactly(messageA1, messageA2)

        val chatLogB = chatLogs.first { it.remoteContact == remoteContactB }
        assertThat(chatLogB.messages).containsExactly(messageB1)
    }


    @Test
    fun testModifyingState_whenLockedAndUnlockedWithDifferentPassphrase_thenContentGone() {
        val instance = CoverdropPrivateDataImpl(context(), publicData, namespace)
        val passphrase = MemorablePassphraseGenerator(context()).generatePassphrase()

        instance.ensureAndTouch()
        instance.unlock(passphrase)

        val remoteContactA = publicData.mRemoteContacts[0]
        val remoteContactB = publicData.mRemoteContacts[1]

        val messageA1 = ChatMessage(remoteContactA, "Hello A", Instant.ofEpochSecond(100))
        val messageB1 = ChatMessage(remoteContactB, "Hello B", Instant.ofEpochSecond(101))
        val messageA2 = ChatMessage(remoteContactA, "Goodbye A", Instant.ofEpochSecond(102))

        for (message in listOf(messageA1, messageB1, messageA2))
            instance.sendMessage(message.remoteContact!!.id, message)

        // save and load under the hood
        instance.lock()
        instance.unlock(MemorablePassphraseGenerator(context()).generatePassphrase()) // DIFFERENT!

        // verify content
        val chatLogs = instance.getChatLogs()
        assertThat(chatLogs).isEmpty()
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext

}
