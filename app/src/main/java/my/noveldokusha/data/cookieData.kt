package my.noveldokusha.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import my.noveldokusha.App
import my.noveldokusha.scraper.toUrl
import java.io.File
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

private typealias CHMap = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>
private typealias MM = MutableMap<String, MutableMap<String, String>>

class ScraperCookiesStorage(
    val name: String,
    private val coroutineScope: CoroutineScope
) : CookieStore {
    private val serializer: Gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File(App.cacheDir, name)
    private val data = CHMap()
    private val loadJob: Job
    private val persistentSaveFlow = MutableSharedFlow<MM>(
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        loadJob = coroutineScope.launch(Dispatchers.IO) {
            if (file.exists()) return@launch
            data.putAll(mapTextToData(getFileText()))
        }
        coroutineScope.launch(Dispatchers.IO) {
            persistentSaveFlow
                .debounce(timeoutMillis = 1_000)
                .collectLatest {
                    loadJob.join()
                    setFileText(mapDataToText(data))
                }
        }
    }

    private suspend fun getFileText() = withContext(Dispatchers.IO) { file.readText() }
    private suspend fun setFileText(text: String) = withContext(Dispatchers.IO) {
        file.writeText(text)
    }

    private suspend fun mapTextToData(text: String) = withContext(Dispatchers.Default) {
        serializer.fromJson<MM>(text, object : TypeToken<MM>() {}.type)
            .mapValues { ConcurrentHashMap(it.value) }
            .let { ConcurrentHashMap(it) }
    }

    private suspend fun mapDataToText(data: CHMap) = withContext(Dispatchers.Default) {
        data.mapValues { it.value.toMap() }
            .toMap()
            .let { serializer.toJson(it) }
    }

    suspend fun add(url: String, values: Map<String, String>) = withContext(Dispatchers.Default)
    {
        val domainName = url.toUrl()?.authority ?: return@withContext
        loadJob.join()
        data.getOrPut(domainName) { ConcurrentHashMap() }.putAll(values)

    }

    suspend fun get(url: String): Map<String, String> = withContext(Dispatchers.Default)
    {
        val domainName = url.toUrl()?.authority ?: return@withContext mapOf()
        loadJob.join()
        return@withContext data[domainName]?.toMap() ?: mapOf()
    }

    fun addImmediate(url: String, values: Map<String, String>) {
        coroutineScope.launch { add(url, values) }
    }

    fun getImmediate(url: String): Map<String, String> {
        val domainName = url.toUrl()?.authority ?: return mapOf()
        return data[domainName]?.toMap() ?: mapOf()
    }

    override fun add(uri: URI, cookie: HttpCookie) {
        TODO("Not yet implemented")
    }

    override fun get(uri: URI?): MutableList<HttpCookie> {
        TODO("Not yet implemented")
    }

    override fun getCookies(): MutableList<HttpCookie> {
        TODO("Not yet implemented")
    }

    override fun getURIs(): MutableList<URI> {
        TODO("Not yet implemented")
    }

    override fun remove(uri: URI?, cookie: HttpCookie?): Boolean {
        TODO("Not yet implemented")
    }

    override fun removeAll(): Boolean {
        TODO("Not yet implemented")
    }
}

//val cookiesData = ScraperCookiesStorage("webview_cookies")
//val headersData = ScraperCookiesStorage("webview_headers")