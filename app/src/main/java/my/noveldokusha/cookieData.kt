package my.noveldokusha

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.scraper.toUrl
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private typealias CHMap = ConcurrentHashMap<String, ConcurrentHashMap<String, String>>
private typealias MM = MutableMap<String, MutableMap<String, String>>

class Data(val name: String)
{
	private val serializer: Gson = GsonBuilder().setPrettyPrinting().create()
	private val file = File(App.cacheDir, name)
	
	private fun fileGet(): CHMap = serializer.fromJson<MM>(file.readText())
		.mapValues { ConcurrentHashMap(it.value) }
		.let { ConcurrentHashMap(it) }
	
	private fun fileSet(value: CHMap): Unit = value.mapValues { it.value.toMap() }
		.toMap()
		.let { file.writeText(serializer.toJson(it)) }
	
	private val list = CHMap()
	private val scope = CoroutineScope(Dispatchers.IO)
	
	fun load() = scope.launch {
		// Here we play with the fact the application will have enough
		// time to load the data before is actually used, so we don't
		// worry if loads in another thread.
		if (file.exists())
			list.putAll(fileGet())
	}
	
	fun add(url: String, values: Map<String, String>)
	{
		val domainName = url.toUrl()?.authority ?: return
		list.getOrPut(domainName) { ConcurrentHashMap() }.putAll(values)
		scope.launch { fileSet(list) }
	}
	
	fun get(url: String): Map<String, String>
	{
		val domainName = url.toUrl()?.authority ?: return mapOf()
		return list.get(domainName)?.toMap() ?: mapOf()
	}
}

val cookiesData = Data("webview_cookies")
val headersData = Data("webview_headers")