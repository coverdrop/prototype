package com.coverdrop

import android.net.TrafficStats
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.coverdrop.remote.RemoteService
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class NetworkTrafficTest {

    val CDN_PROTOCOL = "https"
    val CDN_DOMAIN = "TODO_CHANGE_ME"
    val ITERATIONS = 16

    val random = Random(0)

    fun measure(name: String, actionToBeMeasured: (remote: RemoteService) -> Unit) {
        val measurements_ns = mutableListOf<Long>()
        val measurements_rx_bytes = mutableListOf<Long>()
        val measurements_tx_bytes = mutableListOf<Long>()


        for (i in 0 until ITERATIONS) {
            // it's important that the OkHttpClient is initialised within the loop to make sure we are not re-using connections
            Thread.sleep(500)
            val remote = RemoteService(protocol = CDN_PROTOCOL, domain = CDN_DOMAIN)

            val startRx = TrafficStats.getTotalRxBytes()
            val startTx = TrafficStats.getTotalTxBytes()
            val startTime = System.nanoTime()

            actionToBeMeasured(remote)

            val endRx = TrafficStats.getTotalRxBytes()
            val endTx = TrafficStats.getTotalTxBytes()
            val endTime = System.nanoTime()

            measurements_ns.add(endTime - startTime)
            measurements_rx_bytes.add(endRx - startRx)
            measurements_tx_bytes.add(endTx - startTx)

        }

        // from nano to ms
        val measurements_ms = measurements_ns.map { it.toDouble() / 1000000.0 }
        val measurements_rx_kb = measurements_rx_bytes.map { it.toDouble() / 1000.0 }
        val measurements_tx_kb = measurements_tx_bytes.map { it.toDouble() / 1000.0 }

        Log.i("TRAFFIC", "Testing: ${name}")
        Log.i(
            "TRAFFIC",
            String.format("  TIME avg: %6.1f ms, stdev: %6.1f ms", measurements_ms.average(), measurements_ms.stdev())
        )
        Log.i(
            "TRAFFIC",
            String.format(
                "  RX   avg: %6.1f KB, stdev: %6.1f KB",
                measurements_rx_kb.average(),
                measurements_rx_kb.stdev()
            )
        )
        Log.i(
            "TRAFFIC",
            String.format(
                "  TX   avg: %6.1f KB, stdev: %6.1f KB",
                measurements_tx_kb.average(),
                measurements_tx_kb.stdev()
            )
        )
    }

    @Test
    fun test_postUserMessage() {
        measure("POST_USER_MESSAGE") { remote ->
            remote.sendUserMessage(randomByteArray(385))
        }
    }

    @Test
    fun test_getPubKeys() {
        measure("GET_PUBKEYS") { remote ->
            remote.downloadPubKeys()
        }
    }

    @Test
    fun test_getDeadDrop() {
        measure("GET_DEADDROP") { remote ->
            remote.downloadDeaddropMessages()
        }
    }

    private fun context() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun randomByteArray(size: Int) = random.nextBytes(size)
}

private fun List<Double>.stdev(): Double {
    val mean = this.average()
    val squares = this.map { (it - mean) * (it - mean) }
    return Math.sqrt(squares.sum() / squares.size)
}
