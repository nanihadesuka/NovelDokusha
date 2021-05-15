package my.noveldokusha

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

internal fun <T> Gson.fromJson(json: String): T = fromJson<T>(json, object : TypeToken<T>()
{}.type)

class DataCache<T>(val prefix: String, val name: String)
{
	private val file get() = File(App.cacheDir, "${prefix}__${name}")
	private fun has(): Boolean = file.exists()
	private fun get(): T = GsonBuilder().create().fromJson(file.readText())
	private fun set(value: T): Unit = file.writeText(GsonBuilder().create().toJson(value))
	suspend fun fetch(tryCache: Boolean = true, getRemote: suspend () -> Response<T>): Response<T>
	{
		if (tryCache && has()) return Response.Success(get())
		return when (val res = getRemote())
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