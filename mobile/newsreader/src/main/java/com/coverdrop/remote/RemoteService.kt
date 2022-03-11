package com.coverdrop.remote

import android.util.Log
import com.coverdrop.newsreader.model.NewsItem
import com.coverdrop.newsreader.model.NewsList
import com.coverdrop.newsreader.model.Reporter
import com.coverdrop.newsreader.model.ReporterList
import com.coverdrop.newsreader.ui.byteArrayToHex
import com.coverdrop.newsreader.ui.hexToByteArray
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.headersContentLength
import org.json.JSONArray
import org.json.JSONObject


private const val LOGTAG = "RemoteService"
private const val DEFAULT_PROTOCOL = "https"
private const val DEFAULT_DOMAIN = "TODO_CHANGE_ME"

private const val AUTH_TOKEN_NEWS = "Token news_app_token"

/**
 * Provides all methods for accessing the CDN/WebApi. We decided to use a callback design to allow the news
 * organisation to adapt this to their networking stack.
 */
class RemoteService(
    val protocol: String = DEFAULT_PROTOCOL,
    val domain: String = DEFAULT_DOMAIN,
    val client: OkHttpClient = OkHttpClient()
) {

    fun downloadNewsOverview(): NewsList {
        val response = internalDownloadFromRemote("/")
        Log.d(LOGTAG, "headersContentLength(): ${response.headersContentLength()}")

        val parsedResult = parseNewsList(response)
        Log.d(LOGTAG, "parsed got ${parsedResult.news.size} news articles")
        return parsedResult
    }

    fun downloadNewsStory(id: Int): NewsItem {
        val response = internalDownloadFromRemote("/story/$id")
        Log.d(LOGTAG, "headersContentLength(): ${response.headersContentLength()}")

        return parseNewsItem(response)
    }

    fun downloadReporterList(): ReporterList {
        val response = internalDownloadFromRemote("/reporters")
        Log.d(LOGTAG, "headersContentLength(): ${response.headersContentLength()}")

        return parseReporterList(response)
    }

    fun downloadPubKeys(): Map<String, ByteArray> {
        val response = internalDownloadFromRemote("/pubkeys")
        Log.d(LOGTAG, "headersContentLength(): ${response.headersContentLength()}")

        return parsePublicKeys(response)
    }

    fun downloadDeaddropMessages(): List<ByteArray> {
        val response = internalDownloadFromRemote("/deaddrop")
        return parseBinaryMessageArray(JSONArray(response.body!!.string()))
    }

    fun sendUserMessage(message: ByteArray) {
        val response = internalPostMessage("/user_message", message)
        Log.i(LOGTAG, "reponse=${response.code}")
    }


    private fun parseNewsList(response: Response): NewsList {
        val jsonArray = JSONArray(response.body!!.string())

        return NewsList(news = List(jsonArray.length()) {
            parseNewsItem(jsonArray.getJSONObject(it))
        })
    }

    private fun parseReporterList(response: Response): ReporterList {
        val jsonArray = JSONArray(response.body!!.string())

        return ReporterList(reporters = List(jsonArray.length()) {
            parseReporter(jsonArray.getJSONObject(it))
        })
    }

    private fun parseNewsItem(response: Response): NewsItem {
        val jsonObject = JSONObject(response.body!!.string())
        return parseNewsItem(jsonObject)
    }

    private fun parseNewsItem(jsonObject: JSONObject) = NewsItem(
        id = jsonObject.getInt("id"),
        headline = jsonObject.getString("headline"),
        content = jsonObject.getString("content"),
        image = jsonObject.getString("image"),
        reporter = parseReporter(jsonObject.getJSONObject("reporter"))
    )

    private fun parseReporter(jsonObject: JSONObject) = Reporter(
        id = jsonObject.getLong("id"),
        name = jsonObject.getString("name"),
        image = jsonObject.getString("image"),
        pubkey = hexToByteArray(jsonObject.getString("pub_key"))
    )

    private fun parsePublicKeys(response: Response): Map<String, ByteArray> {
        val jsonObject = JSONObject(response.body!!.string())

        val result = HashMap<String, ByteArray>()
        result["sgx_key"] = hexToByteArray(jsonObject.getString("sgx_key"))
        result["sgx_sign_key"] = hexToByteArray(jsonObject.getString("sgx_sign_key"))
        return result
    }

    private fun parseBinaryMessageArray(jsonArray: JSONArray) =
        List(jsonArray.length()) { idx ->
            hexToByteArray(jsonArray.getString(idx))
        }

    private fun internalDownloadFromRemote(
        path: String,
        token: String = AUTH_TOKEN_NEWS
    ): Response {
        val request = Request.Builder()
            .url("$protocol://$domain$path")
            .addHeader("Authorization", token)
            .get()
            .build()

        Log.d(LOGTAG, request.toString())

        return client.newCall(request).execute()
    }

    private fun internalPostMessage(
        path: String,
        message: ByteArray,
        token: String = AUTH_TOKEN_NEWS
    ): Response {
        val messageAsHex = byteArrayToHex(message)
        val jsonString = "{\"message\": \"$messageAsHex\"}"

        val body: RequestBody = jsonString.toRequestBody(jsonString.toMediaTypeOrNull())

        val request = Request.Builder()
            .url("$protocol://$domain$path")
            .addHeader("Authorization", token)
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        Log.d(LOGTAG, "$request $jsonString")

        return client.newCall(request).execute()
    }
}
