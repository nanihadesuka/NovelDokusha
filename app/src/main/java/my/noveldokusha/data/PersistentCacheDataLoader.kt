package my.noveldokusha.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.network.Response
import my.noveldokusha.scraper.DatabaseInterface
import java.io.File

class PersistentCacheDataLoader<T>(
    private val cacheFile: File
) {
    private val serializer: Gson = GsonBuilder().setPrettyPrinting().create()
    private suspend fun hasFile(): Boolean = withContext(Dispatchers.IO) { cacheFile.exists() }
    private suspend fun getFileContent(): T = withContext(Dispatchers.IO) {
        serializer.fromJson(cacheFile.readText(), object : TypeToken<T>() {}.type)
    }

    private suspend fun set(value: T): Unit = withContext(Dispatchers.IO) {
        cacheFile.writeText(serializer.toJson(value))
    }

    suspend fun cache(fn: suspend () -> Response<T>): Response<T> {
        if (hasFile())
            return Response.Success(getFileContent())
        return when (val res = fn()) {
            is Response.Error -> res
            is Response.Success -> {
                set(res.data)
                res
            }
        }
    }

    suspend fun fetch(
        tryCache: Boolean = true,
        getRemote: suspend PersistentCacheDataLoader<T>.() -> Response<T>
    ): Response<T> = if (tryCache) cache { getRemote() } else getRemote()
}

fun persistentCacheDatabaseSearchGenres(
    database: DatabaseInterface,
    appCacheDir: File,
): PersistentCacheDataLoader<Map<String, String>> {
    return PersistentCacheDataLoader(
        cacheFile = File(appCacheDir, database.searchGenresCacheFileName)
    )
}