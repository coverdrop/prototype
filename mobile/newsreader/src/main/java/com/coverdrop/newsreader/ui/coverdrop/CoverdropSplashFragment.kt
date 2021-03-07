package com.coverdrop.newsreader.ui.coverdrop

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.coverdrop.lib.CoverdropLib
import com.coverdrop.lib.crypto.MemorablePassphrase
import com.coverdrop.newsreader.R

/**
 * Splash screen while the CoverDrop session is unlocked (i.e. persistence file is decrypted) and the buffered
 * messages from the server are decrypted. Should take not more than 10-20 seconds.
 */
class CoverdropSplashFragment : Fragment() {

    companion object {

        const val BUNDLE_KEY_PASSPHRASE = "passphrase"

        fun createBundle(passphrase: String): Bundle {
            return Bundle().also { it.putString(BUNDLE_KEY_PASSPHRASE, passphrase) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_coverdrop_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val passphrase = arguments?.getString(BUNDLE_KEY_PASSPHRASE)
            ?: throw IllegalStateException("passphrase is null")
        LoadCoverdropSessionTask().execute(passphrase)
    }

    inner class LoadCoverdropSessionTask : AsyncTask<String, Unit, Unit>() {

        override fun doInBackground(vararg params: String) {
            val coverdropLib = CoverdropLib.getInstance()
            val passphrase = MemorablePassphrase(params[0])
            coverdropLib.createOrUnlockCoverdropUserSession(passphrase)
        }

        override fun onPostExecute(result: Unit) {
            findNavController().navigate(R.id.action_navigation_coverdrop_splash_to_navigation_coverdrop_menu)
        }
    }

}
