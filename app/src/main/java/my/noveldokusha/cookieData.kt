package my.noveldokusha

import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.scraper.toUrl
import java.io.File

private typealias mmm = MutableMap<String, MutableMap<String, String>>

class Data(val name: String)
{
	private val file = File(App.cacheDir, name)
	
	private fun fileGet(): mmm = GsonBuilder().create().fromJson(file.readText())
	private fun fileSet(value: mmm): Unit = file.writeText(GsonBuilder().create().toJson(value))
	
	private val list = mutableMapOf<String, MutableMap<String, String>>()
	private val scope = CoroutineScope(Dispatchers.IO)
	
	fun load() = scope.launch {
		// Here we play with the fact the application will have enough
		// time to load the data before is actually used, so we don't
		// worry if loads in another thread.
		if (!file.exists()) fileSet(list)
		else list.putAll(fileGet())
	}
	
	fun add(url: String, values: Map<String, String>)
	{
		val domainName = url.toUrl().authority!!
		list.getOrPut(domainName) { mutableMapOf() }.putAll(values)
		val copy = mutableMapOf<String, MutableMap<String, String>>()
		copy.putAll(list)
		scope.launch { fileSet(copy) }
	}
	
	fun get(url: String): Map<String, String>
	{
		val domainName = url.toUrl().authority!!
		return list.get(domainName)?.toMap() ?: mapOf()
	}
}

val cookiesData = Data("webview_cookies")
val headersData = Data("webview_headers")