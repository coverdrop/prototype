package com.coverdrop.newsreader.ui.news

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import com.coverdrop.newsreader.R
import com.coverdrop.newsreader.model.NewsList
import com.coverdrop.remote.RemoteService
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_news.*

/**
 * Showing all news stories.
 */
class NewsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        swipe_refresh_layout.setOnRefreshListener { startForegroundUpdate() }
        swipe_refresh_layout.setColorSchemeResources(R.color.colorPrimary)
        startForegroundUpdate()
    }

    private fun startForegroundUpdate() {
        NewsListUpdate().execute()
    }

    inner class NewsListUpdate : AsyncTask<Void, Void, NewsList>() {

        override fun onPreExecute() {
            swipe_refresh_layout.isRefreshing = true
        }

        override fun doInBackground(vararg params: Void?): NewsList {
            return RemoteService().downloadNewsOverview()
        }

        override fun onPostExecute(result: NewsList) {
            updateUi(result)
            swipe_refresh_layout.isRefreshing = false
        }
    }

    private fun updateUi(result: NewsList) {
        layout_article_list.removeAllViews()
        for (newsItem in result.news) {
            val entry = layoutInflater.inflate(R.layout.news_story_card, null)

            val textViewTitle: TextView = entry.findViewById(R.id.text_title)
            val textViewDetails: TextView = entry.findViewById(R.id.text_details)
            val imageViewLeft: ImageView = entry.findViewById(R.id.image_view_left)

            textViewTitle.text = newsItem.headline
            textViewDetails.text = newsItem.content

            val imageUri = newsItem.getImageUri(w = 256, h = 256)
            Picasso.get().load(imageUri).into(imageViewLeft)

            layout_article_list.addView(entry)
            (entry.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin =
                (resources.displayMetrics.density * 8).toInt()

            entry.setOnClickListener {
                val navController = findNavController(activity!!, R.id.nav_host_fragment)
                val args = Bundle().also { it.putInt("story_id", newsItem.id) }
                navController.navigate(R.id.action_navigation_news_to_navigation_news_story, args)
            }
        }
    }
}
