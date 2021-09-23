package my.noveldokusha.scraper

import android.net.Uri
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import my.noveldokusha.Chapter
import my.noveldokusha.cookiesData
import my.noveldokusha.headersData
import net.dankito.readability4j.Readability4J
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.SocketTimeoutException
import java.net.URLEncoder

suspend fun Connection.getIO(): Document = withContext(Dispatchers.IO) { get() }
suspend fun Connection.postIO(): Document = withContext(Dispatchers.IO) { post() }
suspend fun Connection.executeIO(): Connection.Response = withContext(Dispatchers.IO) { execute() }
suspend fun String.urlEncodeAsync(): String = withContext(Dispatchers.IO) { this@urlEncodeAsync.urlEncode() }

fun String.urlEncode(): String = URLEncoder.encode(this, "utf-8")
fun String.toUrl(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
fun String.toUrlBuilder(): Uri.Builder? = toUrl()?.buildUpon()
fun Uri.Builder.add(key: String, value: Any): Uri.Builder = appendQueryParameter(key, value.toString())
fun Connection.addHeaderRequest(): Connection = this.header("X-Requested-With", "XMLHttpRequest")

suspend fun connect(url: String): Connection = Jsoup.connect(url).apply {
    referrer("http://www.google.com")
    userAgent("Mozilla/5.0 (X11; U; Linux i586; en-US; rv:1.7.3) Gecko/20040924 Epiphany/1.4.4 (Ubuntu)")
    header("Content-Language", "en-US")
    header("Accept", "text/html")
    header("Accept-Encoding", "gzip,deflate")
    headers(headersData.get(url))
    cookies(cookiesData.get(url))
}

sealed class Response<T>
{
    class Success<T>(val data: T) : Response<T>()
    class Error<T>(val message: String) : Response<T>()
}


suspend fun downloadChaptersList(bookUrl: String): Response<List<Chapter>>
{
    val error by lazy {
        """
			Incompatible source.
			
			Can't find compatible source for:
			$bookUrl
		""".trimIndent()
    }

    // Return if can't find compatible source for url
    val scrap = scrubber.getCompatibleSourceCatalog(bookUrl) ?: return Response.Error(error)

    return tryConnect {
        val doc = fetchDoc(bookUrl)
        scrap.getChapterList(doc)
            .mapIndexed { index, it -> Chapter(title = it.title, url = it.url, bookUrl = bookUrl, position = index) }
            .let { Response.Success(it) }
    }
}


data class ChapterDownload(val body: String, val title: String?)

suspend fun downloadChapter(chapterUrl: String): Response<ChapterDownload>
{
    return tryConnect {
        val con = connect(chapterUrl)
            .timeout(30 * 1000)
            .followRedirects(true)
            .executeIO()

        val realUrl = con.url().toString()

        val error by lazy {
            """
				Unable to load chapter from url:
				$chapterUrl
				
				Redirect url:
				$realUrl
				
				Source not supported
			""".trimIndent()
        }

        scrubber.getCompatibleSource(realUrl)?.also { source ->
            val doc = fetchDoc(source.transformChapterUrl(realUrl))
            val data = ChapterDownload(
                body = source.getChapterText(doc) ?: return@also,
                title = source.getChapterTitle(doc)
            )
            return@tryConnect Response.Success(data)
        }

        // If no predefined source is found try extracting text with Readability4J library
        Readability4J(realUrl, fetchDoc(realUrl)).parse().also { article ->
            val content = article.articleContent ?: return@also
            val data = ChapterDownload(
                body = scrubber.getNodeStructuredText(content),
                title = article.title
            )
            return@tryConnect Response.Success(data)
        }

        Response.Error(error)
    }
}

suspend fun <T> tryConnect(extraErrorInfo: String = "", call: suspend () -> Response<T>): Response<T> = try
{
    call()
} catch (e: SocketTimeoutException)
{
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
} catch (e: Exception)
{
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

suspend fun fetchDoc(url: String, timeoutMilliseconds: Int = 20 * 1000): Document
{
    return connect(url)
        .timeout(timeoutMilliseconds)
        .getIO()
}

suspend fun fetchDoc(url: Uri.Builder, timeoutMilliseconds: Int = 20 * 1000) = fetchDoc(url.toString(), timeoutMilliseconds)


class FetchIterator<T>(
    private val coroutineScope: CoroutineScope,
    private val list: ArrayList<T> = ArrayList(listOf()),
    private var fn: (suspend (index: Int) -> Response<List<T>>)
)
{
    enum class STATE
    { IDLE, LOADING, CONSUMED }

    private var state = STATE.IDLE
    private var index = 0
    private var job: Job? = null

    val onSuccess = MutableLiveData<List<T>>()
    val onCompleted = MutableLiveData<Unit>()
    val onCompletedEmpty = MutableLiveData<Unit>()
    val onError = MutableLiveData<Response.Error<List<T>>>()
    val onFetching = MutableLiveData<Boolean>()
    val onReset = MutableLiveData<Unit>()

    fun setFunction(fn: (suspend (index: Int) -> Response<List<T>>))
    {
        this.fn = fn
    }

    fun reset()
    {
        job?.cancel()
        state = STATE.IDLE
        index = 0
        list.clear()
        onSuccess.value = list.toList()
        onReset.value = Unit
    }

    fun fetchTrigger(trigger: () -> Boolean)
    {
        if (state == STATE.IDLE && trigger())
            fetchNext()
    }

    fun fetchNext()
    {
        if (state != STATE.IDLE) return
        state = STATE.LOADING

        job = coroutineScope.launch(Dispatchers.Main) {
            onFetching.value = true
            val res = withContext(Dispatchers.IO) { fn(index) }
            onFetching.value = false
            if (!isActive) return@launch
            when (res)
            {
                is Response.Success ->
                {
                    if (res.data.isEmpty())
                    {
                        state = STATE.CONSUMED
                        if (list.isEmpty())
                            onCompletedEmpty.value = Unit
                        else
                            onCompleted.value = Unit
                    } else
                    {
                        state = STATE.IDLE
                        list.addAll(res.data)
                        onSuccess.value = list.toList()
                    }
                }
                is Response.Error ->
                {
                    state = STATE.CONSUMED
                    onError.value = res
                    if (list.isEmpty())
                        onCompletedEmpty.value = Unit
                    else
                        onCompleted.value = Unit
                }
            }
            index += 1
        }
    }
}
