package my.noveldokusha.network

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
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
import kotlin.coroutines.suspendCoroutine

private suspend fun Call.await(): Response = withContext(Dispatchers.IO) {
    suspendCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
    }
}

suspend fun OkHttpClient.call(builder: Request.Builder) = newCall(builder.build()).await()

fun Response.toDocument(): Document {
    return Jsoup.parse(body.string())
}

fun Response.toJson(): JsonElement {
    return JsonParser.parseString(body.string())
}