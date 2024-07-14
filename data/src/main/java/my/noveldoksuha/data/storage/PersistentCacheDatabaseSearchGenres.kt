package my.noveldoksuha.data.storage

import android.content.Context
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SearchGenre
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersistentCacheDatabaseSearchGenresProvider @Inject constructor(
    @ApplicationContext private val appContext: Context,
) {

    fun provide(database: DatabaseInterface): PersistentCacheDataLoader<List<SearchGenre>> {
        return PersistentCacheDataLoader(
            cacheFile = File(appContext.cacheDir, database.searchGenresCacheFileName),
            adapterProvider = {
                val listMyData =
                    Types.newParameterizedType(List::class.java, SearchGenre::class.java)
                it.adapter(listMyData)
            }
        )
    }
}
