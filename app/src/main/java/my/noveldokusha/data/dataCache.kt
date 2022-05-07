package my.noveldokusha.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.App
import my.noveldokusha.scraper.Response
import java.io.File

class CacheDataLoader<T>(
    val name: String
)
{
    private val serializer: Gson = GsonBuilder().setPrettyPrinting().create()
    private val file get() = File(App.cacheDir, name)
    private suspend fun hasFile(): Boolean = withContext(Dispatchers.IO) { file.exists() }
    private suspend fun getFileContent(): T = withContext(Dispatchers.IO) {
        serializer.fromJson(file.readText(), object : TypeToken<T>()
        {}.type)
    }

    private suspend fun set(value: T): Unit = withContext(Dispatchers.IO) {
        file.writeText(serializer.toJson(value))
    }

    suspend fun cache(fn: suspend () -> Response<T>): Response<T>
    {
        if (hasFile())
            return Response.Success(getFileContent())
        return when (val res = fn())
        {
            is Response.Error -> res
            is Response.Success ->
            {
                set(res.data)
                res
            }
        }
    }

    suspend fun fetch(
        tryCache: Boolean = true,
        getRemote: suspend CacheDataLoader<T>.() -> Response<T>
    ): Response<T> = if (tryCache) cache { getRemote() } else getRemote()
}

fun DataCache_DatabaseSearchGenres(id: String) = CacheDataLoader<Map<String, String>>(
    name = "database_search_genres__$id"
)