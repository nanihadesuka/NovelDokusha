package my.noveldokusha

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.scraper.Response
import java.io.File

class DataCacheDatabaseSearchGenres(val name: String)
{
	private val serializer: Gson = GsonBuilder().setPrettyPrinting().create()
	private val file get() = File(App.cacheDir, "database_search_genres__${name}")
	private suspend fun has(): Boolean = withContext(Dispatchers.IO) { file.exists() }
	private suspend fun get(): Map<String, String> = withContext(Dispatchers.IO) {
		serializer.fromJson(file.readText(), object : TypeToken<Map<String, String>>()
		{}.type)
	}
	
	private suspend fun set(value: Map<String, String>): Unit = withContext(Dispatchers.IO) {
		file.writeText(serializer.toJson(value))
	}
	
	suspend fun fetch(
			tryCache: Boolean = true,
			getRemote: suspend () -> Response<Map<String, String>>)
			: Response<Map<String, String>> = withContext(Dispatchers.IO)
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

fun DataCache_DatabaseSearchGenres(id: String) = DataCacheDatabaseSearchGenres(id)
