package my.noveldokusha

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.scraper.Response
import java.io.File

fun <T> Gson.fromJson(json: String): T = fromJson<T>(json, object : TypeToken<T>()
{}.type)

class DataCache<T>(val prefix: String, val name: String)
{
	private val serializer: Gson = GsonBuilder().setPrettyPrinting().create()
	private val file get() = File(App.cacheDir, "${prefix}__${name}")
	private suspend fun has(): Boolean = withContext(Dispatchers.IO) { file.exists() }
	private suspend fun get(): T = withContext(Dispatchers.IO) { serializer.fromJson(file.readText()) }
	private suspend fun set(value: T): Unit = withContext(Dispatchers.IO) { file.writeText(serializer.toJson(value)) }
	suspend fun fetch(tryCache: Boolean = true, getRemote: suspend () -> Response<T>): Response<T> = withContext(Dispatchers.IO)
	{
		if (tryCache && has()) return@withContext Response.Success(get())
		return@withContext when (val res = getRemote())
		{
			is Response.Error -> res
			is Response.Success ->
			{
				set(res.data)
				res
			}
		}
	}
}

fun DataCache_DatabaseSearchGenres(id: String) = DataCache<Map<String, String>>("database_search_genres", id)
