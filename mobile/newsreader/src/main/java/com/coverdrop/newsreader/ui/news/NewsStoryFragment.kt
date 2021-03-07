package com.coverdrop.newsreader.ui.news

import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.coverdrop.newsreader.R
import com.coverdrop.newsreader.model.NewsItem
import com.coverdrop.remote.RemoteService
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_news_story.*
import java.util.*

/**
 * Showing one particular news story
 */
class NewsStoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_story, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val storyId = requireArguments().getInt("story_id", -1)
        if (storyId == -1) {
            throw IllegalArgumentException("Missing story_id in bundle")
        }
        NewsStoryUpdate().execute(storyId)
    }

    inner class NewsStoryUpdate : AsyncTask<Int, Void, NewsItem>() {

        override fun doInBackground(vararg params: Int?): NewsItem {
            return RemoteService().downloadNewsStory(params[0]!!)
        }

        override fun onPostExecute(result: NewsItem) {
            updateUi(result)
        }
    }

    private fun updateUi(newsItem: NewsItem) {
        text_content.text = newsItem.content
        text_header.text = newsItem.headline
        text_reporter.text =
            getString(R.string.written_by_template, newsItem.reporter.name.toUpperCase(Locale.UK))

        val imageUri = newsItem.getImageUri(w = 800, h = 400)
        Picasso.get().load(imageUri).into(image_view_hero)

        text_reporter.setOnClickListener {
            findNavController().navigate(R.id.navigation_reporter)
        }
    }
}
