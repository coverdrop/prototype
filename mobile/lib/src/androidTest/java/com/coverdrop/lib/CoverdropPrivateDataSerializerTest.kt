package com.coverdrop.lib

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coverdrop.lib.crypto.MessageCipher
import com.coverdrop.lib.mocks.CoverdropPublicDataMock
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class CoverdropPrivateDataSerializerTest {

    private val publicData = CoverdropPublicDataMock()

    @Test
    fun testSaveLoad_whenEmptyBox_thenRestored() {
        val box = CoverdropPrivateDataSerialiser(publicData)

        val bytes = box.save(UnlockedPrivateDataState.empty())
        assertThat(bytes.size).isEqualTo(COVERDROP_BLOB_PADDED_LENGTH)

        val actual = box.load(bytes)
        assertThat(actual.chatLogs).isEmpty()
    }

    @Test
    fun testSaveLoad_whenBoxWithData_thenRestored() {
        val box = CoverdropPrivateDataSerialiser(publicData)

        val alice = publicData.getRemoteContact(1)

        val chatLogs = listOf(
            ChatLog(
                remoteContact = alice,
                lastOpened = Instant.ofEpochSecond(1587300000),
                messages = listOf(
                    ChatMessage(
                        remoteContact = null,
                        text = "Hello there",
                        time = Instant.ofEpochSecond(1587300000 + 1)
                    ),
                    ChatMessage(
                        remoteContact = null,
                        text = "I have a question",
                        time = Instant.ofEpochSecond(1587300000 + 2)
                    ),
                    ChatMessage(
                        remoteContact = alice,
                        text = "Sure no problem",
                        time = Instant.ofEpochSecond(1587300000 + 3)
                    ),
                    ChatMessage(
                        remoteContact = alice,
                        text = "What's the matter?",
                        time = Instant.ofEpochSecond(1587300000 + 4)
                    ),
                    ChatMessage(
                        remoteContact = null,
                        text = "Is it possible to use umlaute such as äöüß?",
                        time = Instant.ofEpochSecond(1587300000 + 5)
                    )
                )
            )
        )
        val privateKeyPair = MessageCipher().generatePrivateKeyPair()
        val state = UnlockedPrivateDataState(chatLogs, privateKeyPair)

        val bytes = box.save(state)
        assertThat(bytes.size).isEqualTo(COVERDROP_BLOB_PADDED_LENGTH)

        val actual = box.load(bytes)
        assertThat(actual.chatLogs).isEqualTo(chatLogs)
        assertThat(actual.privateKeyPair).isEqualTo(privateKeyPair)
    }

}
