package com.coverdrop.newsreader.ui.coverdrop

import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.coverdrop.lib.crypto.MemorablePassphrase
import com.coverdrop.lib.crypto.MemorablePassphraseGenerator
import com.coverdrop.newsreader.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.fragment_coverdrop.*

/**
 * Fragment showing the CoverDrop entry screen where passwords can be entered
 */
class CoverdropFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_coverdrop, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        GeneratePassphraseTask().execute()

        button_check_for_messages.setOnClickListener { maybeOpenCoverdrop(edit_text_user_passphrase.text.toString()) }
        button_send_tip.setOnClickListener { maybeOpenCoverdrop(edit_text_generated_passphrase.text.toString()) }
    }

    fun maybeOpenCoverdrop(passphrase: String) {
        CheckPassphraseTask(passphrase).execute()
    }

    fun openCoverDrop(passphrase: String) {
        val bundle = CoverdropSplashFragment.createBundle(passphrase)
        findNavController().navigate(
            R.id.action_navigation_coverdrop_to_navigation_coverdrop_splash,
            bundle
        )
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.coverdrop_public_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.coverdrop_public_menu_prefs -> {
                findNavController().navigate(R.id.navigation_coverdrop_public_prefs)
                true
            }
            else -> false
        }
    }

    fun confirmUnusualPassphrase(passphrase: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.alert_unusual_passphrase_title)
            .setMessage(R.string.alert_unusual_passphrase_message)
            .setPositiveButton(R.string.alert_unusual_passphrase_button_continue) { _, _ ->
                openCoverDrop(passphrase)
            }
            .setNegativeButton(R.string.alert_unusual_passphrase_button_cancel) { _, _ -> }
            .create()
            .show()
    }

    inner class GeneratePassphraseTask : AsyncTask<Unit, Unit, MemorablePassphrase>() {
        override fun doInBackground(vararg params: Unit): MemorablePassphrase {
            return MemorablePassphraseGenerator(requireContext())
                .generatePassphrase()
        }

        override fun onPostExecute(result: MemorablePassphrase) {
            edit_text_generated_passphrase.setText(result.value)
        }
    }

    inner class CheckPassphraseTask(private val passphrase: String) :
        AsyncTask<Unit, Unit, Boolean>() {
        override fun doInBackground(vararg params: Unit): Boolean {
            return try {
                MemorablePassphraseGenerator(requireContext()).isValidPassphrase(passphrase)
            } catch (e: IllegalArgumentException) {
                false
            } catch (e: IllegalArgumentException) {
                false
            }
        }

        override fun onPostExecute(valid: Boolean) = when {
            valid -> openCoverDrop(passphrase)
            else -> confirmUnusualPassphrase(passphrase)
        }
    }
}
