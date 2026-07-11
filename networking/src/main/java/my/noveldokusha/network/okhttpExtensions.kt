package my.noveldokusha.network

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Suspend-await an OkHttp [Call], propagating coroutine cancellation to the
 * underlying call so that when the calling coroutine is cancelled (e.g. the
 * user navigates away from the reader), the in-flight HTTP request is also
 * cancelled — releasing the socket and the thread instead of letting it
 * run to completion and discard the result.
 */
private suspend fun Call.await(): Response = withContext(Dispatchers.IO) {
    suspendCancellableCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
        continuation.invokeOnCancellation { runCatching { cancel() } }
    }
}

suspend fun OkHttpClient.call(builder: Request.Builder) = newCall(builder.build()).await()

/**
 * Parse the response body as a Jsoup [Document].
 *
 * The body is read fully (and closed) via [okhttp3.ResponseBody.string] before
 * Jsoup parses it, so the underlying connection is returned to the pool even
 * if parsing throws.
 */
fun Response.toDocument(): Document {
    val html = body.string()
    return Jsoup.parse(html, request.url.toString())
}

/**
 * Parse the response body as a Gson [JsonElement].
 *
 * Same lifecycle guarantee as [toDocument] — the body is closed before the
 * (potentially throwing) parse runs.
 */
fun Response.toJson(): JsonElement {
    val json = body.string()
    return JsonParser.parseString(json)
}
