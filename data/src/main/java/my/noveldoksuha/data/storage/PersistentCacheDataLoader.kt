package my.noveldoksuha.data.storage

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.Response
import my.noveldokusha.core.asNotNull
import my.noveldokusha.core.flatMapError
import my.noveldokusha.core.tryAsResponse
import timber.log.Timber
import java.io.File

class PersistentCacheDataLoader<T>(
    private val cacheFile: File,
    private val adapterProvider: (moshi: Moshi) -> JsonAdapter<T>
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val jsonAdapter: JsonAdapter<T> = adapterProvider(moshi)


    private suspend fun hasFile(): Boolean = withContext(Dispatchers.IO) { cacheFile.exists() }
    private suspend fun getFileContent(): Response<T> = tryAsResponse {
        withContext(Dispatchers.IO) {
            jsonAdapter.fromJson(cacheFile.readText())
        }
    }.asNotNull()

    private suspend fun set(value: T) = tryAsResponse {
        withContext(Dispatchers.IO) {
            cacheFile.writeText(jsonAdapter.toJson(value))
        }
    }

    private suspend fun cache(fn: suspend () -> Response<T>): Response<T> {
        return when {
            hasFile() -> getFileContent()
                .onError { Timber.e(it.exception, it.message) }
                .flatMapError { fn() }
            else -> fn()
        }.onSuccess { set(it) }
    }

    suspend fun fetch(
        tryCache: Boolean = true,
        getRemote: suspend PersistentCacheDataLoader<T>.() -> Response<T>
    ): Response<T> = if (tryCache) cache { getRemote() } else getRemote()
}

