package com.coverdrop.lib

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coverdrop.lib.crypto.MessageCipher
import com.coverdrop.lib.crypto.hexToByteArray
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PerfTests {

    val ITERATIONS = 64

    fun measure(name: String, actionToBeMeasured: () -> Unit) {
        val measurements_ns = mutableListOf<Long>()

        for (i in 0..ITERATIONS - 1) {
            val startTime = System.nanoTime()
            actionToBeMeasured()
            val endTime = System.nanoTime()
            measurements_ns.add(endTime - startTime)
        }

        // from nano to ms
        val measurements_ms = measurements_ns.map { it.toDouble() / 1000000.0 }

        Log.i("PERF", "Testing: ${name}")
        Log.i("PERF", "Average: ${measurements_ms.average()}ms")
        Log.i("PERF", "Stdev: ${measurements_ms.stdev()}ms")
    }

    @Test
    fun test_perf_dummyMessageCreation() {
        val pubKeySgx = MessageCipher().generatePrivateKeyPair().public
        measure("test_perf_dummyMessageCreation") {
            MessageCipher().createDummyMessage(pubKeySgx)
        }
    }

    @Test
    fun test_perf_decryptMessageReal() {
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
        measure("test_perf_decryptMessageReal") {
            MessageCipher().decrypt(sgxSignPub, reporterPub, userPriv, packetSgxToUser)
        }
    }

    @Test
    fun test_perf_decryptMessage_wrongReporter() {
        val sgxSignPub = hexToByteArray("26ff3007f5c0f421c8483f3dbb46a42b8826b51502e3199e2dcee13b0700f473")
        val reporterPub = hexToByteArray("c5f52b0ac235273efc8d84cedf1dfc0c38b5b51413c2aa8448a12c2095b20676")
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
        measure("test_perf_decryptMessage_wrongReporter") {
            try {
                MessageCipher().decrypt(sgxSignPub, reporterPub, userPriv, packetSgxToUser)
            } catch (e: AssertionError) {
                //ignore
            }
        }
    }

    @Test
    fun test_perf_decryptMessage_wrongUser() {
        val sgxSignPub = hexToByteArray("26ff3007f5c0f421c8483f3dbb46a42b8826b51502e3199e2dcee13b0700f473")
        val reporterPub = hexToByteArray("d67599ef7b6119dc33058701a8c484dd47a53394323c783652898b7e853cfc69")
        val userPriv = hexToByteArray("0043350c837d94d8c8606f7e4593d7b242610a872b7c8d7293a90f0fd2534cc7")
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
        measure("test_perf_decryptMessage_wrongUser") {
            try {
                MessageCipher().decrypt(sgxSignPub, reporterPub, userPriv, packetSgxToUser)
            } catch (e: AssertionError) {
                //ignore
            }
        }
    }
}

private fun List<Double>.stdev(): Double {
    val mean = this.average()
    val squares = this.map { (it - mean) * (it - mean) }
    return Math.sqrt(squares.sum() / squares.size)
}
