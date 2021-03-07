package com.coverdrop.newsreader.ui.coverdrop

import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import com.coverdrop.lib.CoverdropLib
import com.coverdrop.lib.RemoteContact
import com.coverdrop.newsreader.R
import com.coverdrop.newsreader.ui.byteArrayToHex
import kotlinx.android.synthetic.main.fragment_coverdrop_menu.*
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

/**
 * Fragment showing the CoverDrop menu where active and available chat sessions are listed.
 * In this prototype it also shows a debug button to force a synchronisation operation.
 */
class CoverdropMenuFragment : Fragment() {

    val coverdropLib = CoverdropLib.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_coverdrop_menu, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updateNextSendTime()
        button_debug_force_sync.setOnClickListener { DebugForceSync().execute() }

        LoadMenuEntries().execute()
    }

    private fun showActiveChats(activeContacts: List<RemoteContact>) {
        if (activeContacts.isEmpty()) {
            text_empty_active_chats.visibility = View.VISIBLE
            layout_active_chats_list.visibility = View.GONE
            return
        }

        text_empty_active_chats.visibility = View.GONE
        layout_active_chats_list.visibility = View.VISIBLE
        layout_active_chats_list.removeAllViewsInLayout()

        for (remoteContact in activeContacts) {
            inflateRemoteContact(layout_active_chats_list, remoteContact)
        }
    }

    private fun showAvailableContacts(allContacts: List<RemoteContact>) {
        if (allContacts.isEmpty()) {
            text_empty_contacts.visibility = View.VISIBLE
            layout_available_contacts_list.visibility = View.GONE
            return
        }

        text_empty_contacts.visibility = View.GONE
        layout_available_contacts_list.visibility = View.VISIBLE
        layout_available_contacts_list.removeAllViewsInLayout()

        for (remoteContact in allContacts) {
            inflateRemoteContact(layout_available_contacts_list, remoteContact)
        }
    }

    private fun inflateRemoteContact(parent: ViewGroup, remoteContact: RemoteContact) {
        val view = layoutInflater.inflate(R.layout.chat_contact_item, parent, false)
        view.findViewById<TextView>(R.id.text_contact_name).text = remoteContact.name
        view.findViewById<TextView>(R.id.text_contact_pubkey).text =
            byteArrayToHex(remoteContact.pubkey, useDelimiter = true)
        view.setOnClickListener {
            val navController = Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
            val args = Bundle().also { it.putLong("contact_id", remoteContact.id) }
            navController.navigate(R.id.action_navigation_coverdrop_menu_to_navigation_coverdrop_chat, args)
        }

        parent.addView(view)
    }


    data class MenuEntries(
        val activeSessions: List<RemoteContact>,
        val allContacts: List<RemoteContact>
    )

    inner class LoadMenuEntries : AsyncTask<Void, Void, MenuEntries>() {

        override fun doInBackground(vararg params: Void?): MenuEntries {
            val remoteContacts = coverdropLib.getCoverdropPublicData().getAllRemoteContacts()
            val activeChats = coverdropLib.getUnlockedCoverdropUserSession().getChatLogs()
            return MenuEntries(activeChats.map { it.remoteContact }, remoteContacts)
        }

        override fun onPostExecute(result: MenuEntries?) {
            result!!
            showAvailableContacts(result.allContacts)
            showActiveChats(result.activeSessions)
        }
    }

    inner class DebugForceSync : AsyncTask<Void, Void, Void?>() {

        override fun onPreExecute() {
            button_debug_force_sync.isEnabled = false
        }

        override fun doInBackground(vararg params: Void?): Void? {
            coverdropLib.doRegularBackgroundOperation()
            coverdropLib.getUnlockedCoverdropUserSession().debugForceSync()
            return null
        }

        override fun onPostExecute(void: Void?) {
            LoadMenuEntries().execute()
            button_debug_force_sync.isEnabled = true
        }
    }

    private fun updateNextSendTime() {
        val nextSyncTime = coverdropLib.getNextSyncTime()

        val timeString = SimpleDateFormat("HH:mm", Locale.US).format(nextSyncTime)
        text_next_send_time.text = getString(R.string.text_template_coverdrop_next_send_at, timeString)

        val secondsUntilNextSyncTime = nextSyncTime.toInstant().epochSecond - Instant.now().epochSecond
        val secondsForEachIntervall = coverdropLib.getSyncIntervalInSeconds()
        val progress = 10_000f - 10_000f * secondsUntilNextSyncTime.toFloat() / secondsForEachIntervall.toFloat()
        progress_bar_next_send_time.max = 10_000
        progress_bar_next_send_time.progress = progress.toInt()

        Handler(Looper.getMainLooper()).postDelayed({
            try {
                updateNextSendTime()
            } catch (ignore: Exception) {
                // ignore
            }
        }, 200)
    }

}
