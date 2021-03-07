package com.coverdrop.lib

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coverdrop.lib.crypto.hexToByteArray
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverdropPublicDataImplTest {

    private val namespace = "TEST"

    @Before
    fun setup() {
        CoverdropPublicDataImpl(context(), namespace).deleteAll()
    }

    @Test
    fun testInit_whenCalled_thenNothingHappens() {
        CoverdropPublicDataImpl(context(), namespace)
    }

    @Test
    fun testMessages_whenAddedAndRemove_thenReturnedStatesCorrect() {
        val instance = CoverdropPublicDataImpl(context(), namespace)

        val in1 = "Hello Alice".toByteArray()
        val out2 = "Hello Bob".toByteArray()
        val out3 = "What is the status of the situation?".toByteArray()
        val in4 = "Not terrible, not great...".toByteArray()

        instance.addIncomingMessages(listOf(in1, in4))
        instance.addOutgoingMessage(out2)
        instance.addOutgoingMessage(out3)

        assertThat(instance.getPendingIncomingMessages()).containsExactlyInAnyOrder(in1, in4)
        assertThat(instance.getPendingOutgoingMessages()).containsExactlyInAnyOrder(out2, out3)

        instance.clearIncomingMessages()
        assertThat(instance.getPendingIncomingMessages()).isEmpty()

        instance.clearOutgoingMessages()
        assertThat(instance.getPendingOutgoingMessages()).isEmpty()
    }

    @Test
    fun testContact_whenAddedAndUpdated_thenReflectedInState() {
        val instance = CoverdropPublicDataImpl(context(), namespace)

        val contact1 = RemoteContact(10, "Hans", hexToByteArray("F0F1"))
        val contact2 = RemoteContact(11, "Grber", hexToByteArray("F1F2"))
        val contact2Updated = RemoteContact(11, "Gruber", hexToByteArray("F1F2F3"))

        instance.addOrUpdateRemoteContact(contact1)
        instance.addOrUpdateRemoteContact(contact2)
        assertThat(instance.getRemoteContact(10)).isEqualTo(contact1)
        assertThat(instance.getRemoteContact(11)).isEqualTo(contact2)

        instance.addOrUpdateRemoteContact(contact2Updated)
        assertThat(instance.getRemoteContact(11)).isNotEqualTo(contact2)
        assertThat(instance.getRemoteContact(11)).isEqualTo(contact2Updated)
    }

    @Test
    fun testSgxPubKey_whenSet_thenReturned() {
        val instance = CoverdropPublicDataImpl(context(), namespace)
        val pubkey = "sgx_pub_key".toByteArray()

        instance.setSgxPubKey(pubkey)
        assertThat(instance.getSgxPubKey()).isEqualTo(pubkey)
    }

    @Test
    fun testSgxSignPubKey_whenSet_thenReturned() {
        val instance = CoverdropPublicDataImpl(context(), namespace)
        val pubkey = "sgx_sign_pub_key".toByteArray()

        instance.setSgxSignPubKey(pubkey)
        assertThat(instance.getSgxSignPubKey()).isEqualTo(pubkey)
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext

}
