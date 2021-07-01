package my.noveldokusha.scraper

import android.net.Uri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import my.noveldokusha.*
import my.noveldokusha.scraper.databases.BakaUpdates
import my.noveldokusha.scraper.databases.NovelUpdates
import my.noveldokusha.scraper.sources.*
import net.dankito.readability4j.Readability4J
import org.jsoup.Connection
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.PrintWriter
import java.io.StringWriter
import java.net.SocketTimeoutException
import java.net.URLEncoder
import java.util.*
import kotlin.collections.ArrayList

fun String.urlEncode(): String = URLEncoder.encode(this, "utf-8")

suspend fun Connection.getIO(): Document = withContext(Dispatchers.IO) { get() }
suspend fun Connection.postIO(): Document = withContext(Dispatchers.IO) { post() }
suspend fun Connection.executeIO(): Connection.Response = withContext(Dispatchers.IO) { execute() }
suspend fun String.urlEncodeAsync(): String = withContext(Dispatchers.IO) { this@urlEncodeAsync.urlEncode() }

fun connect(url: String): Connection = Jsoup.connect(url).apply {
	referrer("http://www.google.com")
	userAgent("Mozilla/5.0 (X11; U; Linux i586; en-US; rv:1.7.3) Gecko/20040924 Epiphany/1.4.4 (Ubuntu)")
	header("Content-Language", "en-US")
	header("Accept", "text/html")
	header("Accept-Encoding", "gzip,deflate")
	headers(headersData.get(url))
	cookies(cookiesData.get(url))
}

fun String.toUrl(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
fun String.toUrlBuilder(): Uri.Builder? = toUrl()?.buildUpon()
fun Uri.Builder.add(key: String, value: Any): Uri.Builder = appendQueryParameter(key, value.toString())

fun Connection.addHeaderRequest() = this.header("X-Requested-With", "XMLHttpRequest")!!

sealed class Response<T>
{
	class Success<T>(val data: T) : Response<T>()
	class Error<T>(val message: String) : Response<T>()
}

object scrubber
{
	fun getCompatibleSource(url: String): source_interface? = sourcesList.find { url.startsWith(it.baseUrl) }
	fun getCompatibleSourceCatalog(url: String): source_interface.catalog? = sourcesListCatalog.find { url.startsWith(it.baseUrl) }
	fun getCompatibleDatabase(url: String): database_interface? = databasesList.find { url.startsWith(it.baseUrl) }
	
	interface source_interface
	{
		val name: String
		val baseUrl: String
		
		// Transform current url to preferred url
		suspend fun transformChapterUrl(url: String): String = url
		
		suspend fun getChapterTitle(doc: Document): String?
		suspend fun getChapterText(doc: Document): String
		
		interface base : source_interface
		interface catalog : source_interface
		{
			val catalogUrl: String
			
			// The chapter order is ascendent: from 1 ... etc
			suspend fun getChapterList(doc: Document): List<ChapterMetadata>
			suspend fun getCatalogList(index: Int): Response<List<BookMetadata>>
			suspend fun getCatalogSearch(index: Int, input: String): Response<List<BookMetadata>>
		}
	}
	
	interface database_interface
	{
		val id: String
		val name: String
		val baseUrl: String
		
		val searchGenresCache get() = DataCache_DatabaseSearchGenres(id)
		
		suspend fun getSearchAuthorSeries(index: Int, urlAuthorPage: String): Response<List<BookMetadata>>
		suspend fun getSearchGenres(): Response<Map<String, String>>
		suspend fun getSearch(index: Int, input: String): Response<List<BookMetadata>>
		suspend fun getSearchAdvanced(
			index: Int,
			genresIncludedId: List<String>,
			genresExcludedId: List<String>
		): Response<List<BookMetadata>>
		
		data class BookAuthor(val name: String, val url: String?)
		data class BookData(
			val title: String,
			val description: String,
			val alternativeTitles: List<String>,
			val authors: List<BookAuthor>,
			val tags: List<String>,
			val genres: List<String>,
			val bookType: String,
			val relatedBooks: List<BookMetadata>,
			val similarRecommended: List<BookMetadata>,
			val coverImageUrl: String?
		)
		
		fun getBookData(doc: Document): BookData
	}
	
	private fun getPTraverse(node: Node): String
	{
		fun innerTraverse(node: Node): String = node.childNodes().joinToString("") { child ->
			when
			{
				child.nodeName() == "br" -> "\n"
				child is TextNode -> child.text()
				else -> innerTraverse(child)
			}
		}
		
		val paragraph = innerTraverse(node).trim()
		return if (paragraph.isEmpty()) "" else innerTraverse(node).trim() + "\n\n"
	}
	
	private fun getNodeTextTraverse(node: Node): String
	{
		val children = node.childNodes()
		if (children.isEmpty())
			return ""
		
		return children.joinToString("") { child ->
			when
			{
				child.nodeName() == "p" -> getPTraverse(child)
				child.nodeName() == "br" -> "\n"
				child.nodeName() == "hr" -> "\n\n"
				child is TextNode ->
				{
					val text = child.text().trim()
					if (text.isEmpty()) "" else text + "\n\n"
				}
				else -> getNodeTextTraverse(child)
			}
		}
	}
	
	fun getNodeStructuredText(node: Node): String
	{
		val children = node.childNodes()
		if (children.isEmpty())
			return ""
		
		return children.joinToString("") { child ->
			when
			{
				child.nodeName() == "p" -> getPTraverse(child)
				child.nodeName() == "br" -> "\n"
				child.nodeName() == "hr" -> "\n\n"
				child is TextNode -> child.text().trim()
				else -> getNodeTextTraverse(child)
			}
		}
	}
	
	val sourcesList = setOf(
		LightNovelsTranslations(),
		ReadLightNovel(),
		ReadNovelFull(),
		my.noveldokusha.scraper.sources.NovelUpdates(),
		Reddit(),
		AT(),
		Wuxia(),
		BestLightNovel(),
	)
	
	val sourcesListCatalog = sourcesList.filterIsInstance<source_interface.catalog>().toSet()
	
	val databasesList = setOf(
		NovelUpdates(),
		BakaUpdates()
	)
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
				body = source.getChapterText(doc),
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

suspend fun <T> tryConnect(extraErrorInfo: String = "", call: suspend () -> Response<T>): Response<T> = try
{
	call()
}
catch (e: SocketTimeoutException)
{
	val error = """
		Timeout error.

		Info:
		${extraErrorInfo.ifBlank { "No info" }}
		
		Message:
		${e.message}
	""".trimIndent()
	Response.Error(error)
}
catch (e: Exception)
{
	val stacktrace = StringWriter().apply { e.printStackTrace(PrintWriter(this)) }
	val error = """
		Unknown error.
		
		Info:
		${extraErrorInfo.ifBlank { "No Info" }}
		
		Message:
		${e.message}

		Stacktrace:
		$stacktrace
	""".trimIndent()
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
	
	fun removeAllObservers(owner: LifecycleOwner)
	{
		listOf(onSuccess, onCompleted, onCompletedEmpty, onError, onFetching).forEach {
			it.removeObservers(owner)
		}
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
					}
					else
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
