package com.coverdrop.newsreader

import android.app.Application
import android.os.AsyncTask
import com.coverdrop.lib.CoverdropApplication
import com.coverdrop.lib.CoverdropLib
import com.coverdrop.lib.RemoteContact
import com.coverdrop.remote.RemoteService

/**
 * Main entry point of the app. Here we initialise the CoverDrop library (can also be done later to not impact start-up
 * time) and register our callback methods that call into [RemoteService].
 */
class NewsreaderApplication : Application(), CoverdropApplication {

    override fun onCreate() {
        super.onCreate()
        CoverdropLibInitTask().execute()
    }

    override fun downloadRemoteContacts(): List<RemoteContact> {
        val reporters = RemoteService().downloadReporterList()
        return reporters.reporters.map {
            RemoteContact(it.id, it.name, it.pubkey)
        }
    }

    override fun downloadSgxKeys(): Pair<ByteArray, ByteArray> {
        val keys = RemoteService().downloadPubKeys()
        return Pair(keys.getValue("sgx_key"), keys.getValue("sgx_sign_key"))
    }

    override fun downloadDeaddrop(): List<ByteArray> {
        return RemoteService().downloadDeaddropMessages()
    }

    override fun sendOutgoingMessage(message: ByteArray?) {
        message?.apply { RemoteService().sendUserMessage(this) }
    }

    inner class CoverdropLibInitTask() : AsyncTask<Unit, Unit, Unit>() {
        override fun doInBackground(vararg params: Unit?) {
            CoverdropLib.onAppInit(
                applicationContext = this@NewsreaderApplication,
                coverdropApplication = this@NewsreaderApplication
            )
        }

    }

}
