package com.coverdrop.newsreader.ui.reporters

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.coverdrop.newsreader.R
import com.coverdrop.newsreader.model.ReporterList
import com.coverdrop.newsreader.ui.RoundedTransformation
import com.coverdrop.remote.RemoteService
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_reporters.*
import java.util.*

/**
 * Showing all reporters
 */
class ReporterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reporters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipe_refresh_layout.setOnRefreshListener { startForegroundUpdate() }
        swipe_refresh_layout.setColorSchemeResources(R.color.colorPrimary)
        startForegroundUpdate()
    }

    private fun startForegroundUpdate() {
        ReporterListUpdate().execute()
    }

    inner class ReporterListUpdate : AsyncTask<Void, Void, ReporterList>() {

        override fun onPreExecute() {
            swipe_refresh_layout.isRefreshing = true
        }

        override fun doInBackground(vararg params: Void?): ReporterList {
            return RemoteService().downloadReporterList()
        }

        override fun onPostExecute(result: ReporterList) {
            updateUi(result)
            swipe_refresh_layout.isRefreshing = false
        }
    }

    private fun updateUi(result: ReporterList) {
        layout_reporter_list.removeAllViews()
        for (reporter in result.reporters) {
            val entry = layoutInflater.inflate(R.layout.reporter_card, null)

            val textName: TextView = entry.findViewById(R.id.text_journalist_name)
            val textDescription: TextView = entry.findViewById(R.id.text_journalist_description)
            val imageViewLeft: ImageView = entry.findViewById(R.id.image_view_left)
            val buttonEmail: Button = entry.findViewById(R.id.button_send_email)
            val buttonSendTip: Button = entry.findViewById(R.id.button_send_tip)

            textName.text = reporter.name.toUpperCase(Locale.UK)
            textDescription.text =
                getString(R.string.journalist_description_template, reporter.name)

            val imageUri = reporter.getImageUri(200, 200)
            Picasso.get().load(imageUri).transform(RoundedTransformation(128)).into(imageViewLeft)

            buttonEmail.setOnClickListener {
                Toast.makeText(context, R.string.toast_email_not_implemented, Toast.LENGTH_SHORT)
                    .show()
            }

            buttonSendTip.setOnClickListener {
                findNavController().navigate(R.id.navigation_coverdrop)
            }

            layout_reporter_list.addView(entry)
            (entry.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                (resources.displayMetrics.density * 8).toInt()
        }
    }
}
