package my.noveldokusha.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import my.noveldokusha.App
import my.noveldokusha.scraper.toUrl
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private typealias CHMap = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>
private typealias MM = MutableMap<String, MutableMap<String, String>>

class Data(val name: String)
{
	private val serializer: Gson = GsonBuilder().setPrettyPrinting().create()
	private val file = File(App.cacheDir, name)
	private val data = CHMap()
	private var loaded = false
	private var loadMutex = Mutex()
	
	private suspend fun fileGet(): CHMap = withContext(Dispatchers.Default) {
		val text = withContext(Dispatchers.IO) { file.readText() }
		serializer.fromJson<MM>(text, object : TypeToken<MM>()
		{}.type)
			.mapValues { ConcurrentHashMap(it.value) }
			.let { ConcurrentHashMap(it) }
	}
	
	private suspend fun fileSet(value: CHMap): Unit = withContext(Dispatchers.Default) {
		val text = value.mapValues { it.value.toMap() }
			.toMap()
			.let { serializer.toJson(it) }
		withContext(Dispatchers.IO) { file.writeText(text) }
	}
	
	private suspend fun load() = withContext(Dispatchers.Default) {
		loadMutex.withLock {
			if (loaded) return@withLock
			
			if (file.exists())
				data.putAll(fileGet())
			loaded = true
		}
	}
	
	suspend fun add(url: String, values: Map<String, String>) = withContext(Dispatchers.Default)
	{
		val domainName = url.toUrl()?.authority ?: return@withContext
		load()
		data.getOrPut(domainName) { ConcurrentHashMap() }.putAll(values)
		CoroutineScope(Dispatchers.IO).launch { fileSet(data) }
	}
	
	suspend fun get(url: String): Map<String, String> = withContext(Dispatchers.Default)
	{
		val domainName = url.toUrl()?.authority ?: return@withContext mapOf()
		load()
		return@withContext data.get(domainName)?.toMap() ?: mapOf()
	}
}

val cookiesData = Data("webview_cookies")
val headersData = Data("webview_headers")