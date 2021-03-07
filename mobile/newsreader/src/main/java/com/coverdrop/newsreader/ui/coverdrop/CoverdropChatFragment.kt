package com.coverdrop.newsreader.ui.coverdrop

import android.os.AsyncTask
import android.os.Bundle
import android.text.Html
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.coverdrop.lib.ChatLog
import com.coverdrop.lib.ChatMessage
import com.coverdrop.lib.CoverdropLib
import com.coverdrop.newsreader.R
import kotlinx.android.synthetic.main.fragment_coverdrop_chat.*
import java.time.Duration
import java.time.Instant

/**
 * The [CoverdropChatFragment] displays the current chat session between the user and the provided `contact_id` given
 * that CoverDrop is currently unlocked.
 */
class CoverdropChatFragment : Fragment() {

    val coverdropLib = CoverdropLib.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_coverdrop_chat, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val contactId = requireArguments().getLong("contact_id", -1)
        if (contactId == -1L) {
            throw IllegalArgumentException("Missing contact_id in bundle")
        }

        CreateOrLoadChatLog().execute()
    }

    inner class CreateOrLoadChatLog : AsyncTask<Void, Void, ChatLog>() {
        override fun doInBackground(vararg params: Void?): ChatLog {
            val session = coverdropLib.getUnlockedCoverdropUserSession()
            val remoteContactId = requireArguments().getLong("contact_id", -1)
            val chatLog = session.createOrGetChatLog(remoteContactId)

            return chatLog
        }

        override fun onPostExecute(result: ChatLog?) {
            updateChatLog(result!!)
        }
    }

    inner class SendMessage : AsyncTask<ChatMessage, Void, Void>() {
        override fun doInBackground(vararg params: ChatMessage?): Void? {
            val session = coverdropLib.getUnlockedCoverdropUserSession()
            val remoteContactId = requireArguments().getLong("contact_id", -1)
            session.sendMessage(remoteContactId, params[0]!!)
            return null
        }

        override fun onPostExecute(result: Void?) {
            CreateOrLoadChatLog().execute()
        }
    }

    private fun updateChatLog(chatLog: ChatLog) {
        layout_chat_messages.removeAllViews()

        for (message in chatLog.messages) {
            val view = layoutInflater.inflate(R.layout.chat_message, layout_chat_messages, false)

            val sentByUser = message.remoteContact == null
            val sender = if (sentByUser) {
                "You"
            } else {
                "Remote"
            }
            val messageString = Html.fromHtml("<html><strong>${sender}: </strong>${Html.escapeHtml(message.text)}")
            view.findViewById<TextView>(R.id.text_chat_message).text = messageString

            val relativeBeforeString = DateUtils.getRelativeTimeSpanString(message.time.toEpochMilli())
            val timeString = "${if (sentByUser) "Created" else "Received"}: $relativeBeforeString"
            view.findViewById<TextView>(R.id.text_chat_time).text = timeString


            val recentMessage = (Duration.between(message.time, Instant.now()) < Duration.ofMinutes(60))
            view.findViewById<TextView>(R.id.text_delay_warning).visibility = if (sentByUser && recentMessage) {
                View.VISIBLE
            } else {
                View.GONE
            }
            layout_chat_messages.addView(view)
        }

        button_send_message.setOnClickListener {
            val text = edit_message.text.toString()
            SendMessage().execute(
                ChatMessage(
                    remoteContact = null,
                    text = text,
                    time = Instant.now()
                )
            )
        }
    }

}
