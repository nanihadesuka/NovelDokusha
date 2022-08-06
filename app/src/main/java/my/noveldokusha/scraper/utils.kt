package my.noveldokusha.scraper

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException
import java.net.URLEncoder

suspend fun Connection.getIO(): Document = withContext(Dispatchers.IO) { get() }
suspend fun Connection.postIO(): Document = withContext(Dispatchers.IO) { post() }
suspend fun Connection.executeIO(): Connection.Response = withContext(Dispatchers.IO) { execute() }

fun String.urlEncode(): String = URLEncoder.encode(this, "utf-8")
fun String.toUrl(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
fun String.toUrlBuilder(): Uri.Builder? = toUrl()?.buildUpon()
fun String.toUrlBuilderSafe(): Uri.Builder = toUrl()?.buildUpon()!!
fun Uri.Builder.ifCase(case: Boolean, action: Uri.Builder.() -> Uri.Builder) = when {
    case -> action(this)
    else -> this
}

fun Uri.Builder.addPath(vararg path: String) = path.fold(this) { builder, s ->
    builder.appendPath(s)
}

fun Uri.Builder.add(vararg query: Pair<String, Any>) = query.fold(this) { builder, s ->
    builder.appendQueryParameter(s.first, s.second.toString())
}

fun Uri.Builder.add(key: String, value: Any): Uri.Builder =
    appendQueryParameter(key, value.toString())

fun Connection.addHeaderRequest(): Connection = this.header("X-Requested-With", "XMLHttpRequest")

fun okhttp3.Response.toDocument(): Document {
    return Jsoup.parse(body!!.string())
}

suspend fun fetchDoc(url: String, timeoutMilliseconds: Int = 20 * 1000): Document {
    val response = client.call(getRequest(url = url))
    return response.toDocument()
}

suspend fun fetchDoc(url: Uri.Builder, timeoutMilliseconds: Int = 20 * 1000) =
    fetchDoc(url.toString(), timeoutMilliseconds)

suspend fun <T> tryConnect(
    extraErrorInfo: String = "",
    call: suspend () -> Response<T>
): Response<T> = try {
    call()
} catch (e: SocketTimeoutException) {
    val error = listOf(
        "Timeout error.",
        "",
        "Info:",
        extraErrorInfo.ifBlank { "No info" },
        "",
        "Message:",
        e.message
    ).joinToString("\n")

    Response.Error(error)
} catch (e: Exception) {
    val error = listOf(
        "Unknown error.",
        "",
        "Info:",
        extraErrorInfo.ifBlank { "No Info" },
        "",
        "Message:",
        e.message,
        "",
        "Stacktrace:",
        e.stackTraceToString()
    ).joinToString("\n")

    Response.Error(error)
}