package com.coverdrop.lib.crypto

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessageCipherTest {

    private val PLAINTEXT_SHORT = "Hello World"
    private val PLAINTEXT_LONG = "Text that is quite long but shorter than the maximum message size"

    private val cipher = MessageCipher()

    private val keyPairUser = cipher.generatePrivateKeyPair()
    private val keyPairSgx = cipher.generatePrivateKeyPair()
    private val keyPairReceiver = cipher.generatePrivateKeyPair()

    @Test
    fun testGenerateKeypair_whenRun_thenNonEmptyKeypairsReturned() {
        assertThat(keyPairUser.public).isNotEmpty()
        assertThat(keyPairUser.private).isNotEmpty()
    }

    @Test
    fun testEnc_whenGivenPlaintextOfDifferentLength_thenSameCiphertextLength() {
        val ciphertextShort = cipher.encrypt(
            publicKeySender = keyPairUser.public,
            publicKeySgx = keyPairSgx.public,
            publicKeyReceiver = keyPairReceiver.public,
            message = PLAINTEXT_SHORT.encodeToByteArray(),
            realMessage = true,
        )

        val ciphertextLong = cipher.encrypt(
            publicKeySender = keyPairUser.public,
            publicKeySgx = keyPairSgx.public,
            publicKeyReceiver = keyPairReceiver.public,
            message = PLAINTEXT_LONG.encodeToByteArray(),
            realMessage = true,
        )

        assertThat(ciphertextShort.size).isEqualTo(ciphertextLong.size)
        assertThat(ciphertextShort).isNotEqualTo(ciphertextLong)
    }

    @Test
    fun testDecrypt_withPythonTestVector() {
        val sgxSignPub = hexToByteArray("26ff3007f5c0f421c8483f3dbb46a42b8826b51502e3199e2dcee13b0700f473")
        val reporterPub = hexToByteArray("d67599ef7b6119dc33058701a8c484dd47a53394323c783652898b7e853cfc69")
        val userPriv = hexToByteArray("146e93b42448d4aae7846efce0749d6898892ba796952d204d7aba869729828b")
        val packetSgxToUser = hexToByteArray(
            "eda471afdf2d753d5099bdc897c39330c73736157ee8af45c968ea2ba23970f" +
                    "3ddbe67683864f8116e1f266cb03edc5c48d5015f83ab75cced4654202ffb89061724e04549ca0020c90e61502cdcf5494" +
                    "8322f452a2ad1d91094e0923d0806dfc96d6f90a6c4cc7a89bf818e33bc12b9f862d685620c6baf77c09af2eb509bb5a30" +
                    "7594d15b7494e8174f0bc451e9c19105f6acc095d94691de59726437a256e257eb12522bc24557d01dc9ddd4abb9db798f" +
                    "5457f4de11ae2593a35ebd8969b40b78dd507f7dffbde29bf5e171047279810a4b915221953a9f2fe85abc4d6f61ba316a" +
                    "c9ee2d3a7b33d6f6f632088007ec78ac2b00235c2c9adf854acdad2e14f807ffc1f8ec3a39ab4b48f90bf0f7854f8d9182" +
                    "47cb18c1cd6fa73709c4e62a44218755d581dc64feda81c7113876264cbf22d6a95e9651035387edb6d599f7ad54735b07" +
                    "ab67e6426f8feb25dbda6f0ba8753065ee15b0bbed3fa71a4db64cbac3acb1c17d240"
        )

        val actual = cipher.decrypt(sgxSignPub, reporterPub, userPriv, packetSgxToUser)
        assertThat(actual.decodeToString()).isEqualTo("Wassup?")
    }

    @Test
    fun testAsymmetricEnc_whenGivenSamePlaintext_thenDifferentCryptograms() {
        val actual1 = cipher.asymmetricEnc(keyPairUser.public, PLAINTEXT_SHORT.toByteArray())
        val actual2 = cipher.asymmetricEnc(keyPairUser.public, PLAINTEXT_SHORT.toByteArray())

        assertThat(actual1.size).isEqualTo(actual2.size)
        assertThat(actual1).isNotEqualTo(actual2)
    }
}
