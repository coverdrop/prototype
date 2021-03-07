package com.coverdrop.newsreader.model

import android.net.Uri

data class NewsItem(
    val id: Int,
    val headline: String,
    val content: String,
    val image: String,
    val reporter: Reporter
) {
    fun getImageUri(w: Int, h: Int) = imageUriWithResolution(image, w, h)
}

data class NewsList(val news: List<NewsItem>)

data class Reporter(val id: Long, val name: String, val image: String, val pubkey: ByteArray) {
    fun getImageUri(w: Int, h: Int) = imageUriWithResolution(image, w, h)
}

data class ReporterList(val reporters: List<Reporter>)

private fun imageUriWithResolution(imageUri: String, w: Int, h: Int): Uri =
    Uri.parse(imageUri.replace("XXX/YYY", "${w}/${h}"))
