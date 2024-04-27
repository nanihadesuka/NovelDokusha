package my.noveldokusha.repository

import my.noveldokusha.data.Response
import my.noveldokusha.data.map
import my.noveldokusha.feature.local_database.AppDatabase
import my.noveldokusha.feature.local_database.DAOs.ChapterBodyDao
import my.noveldokusha.feature.local_database.tables.ChapterBody
import my.noveldokusha.isLocalUri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterBodyRepository @Inject constructor(
    private val chapterBodyDao: ChapterBodyDao,
    private val appDatabase: AppDatabase,
    private val bookChaptersRepository: BookChaptersRepository,
    private val downloaderRepository: DownloaderRepository,
) {
    suspend fun getAll() = chapterBodyDao.getAll()
    suspend fun insertReplace(chapterBodies: List<ChapterBody>) =
        chapterBodyDao.insertReplace(chapterBodies)

    suspend fun insertReplace(chapterBody: ChapterBody) =
        chapterBodyDao.insertReplace(chapterBody)

    suspend fun removeRows(chaptersUrl: List<String>) =
        chaptersUrl.chunked(500).forEach { chapterBodyDao.removeChapterRows(it) }

    suspend fun insertWithTitle(chapterBody: ChapterBody, title: String?) = appDatabase.transaction {
        insertReplace(chapterBody)
        if (title != null)
            bookChaptersRepository.updateTitle(chapterBody.url, title)
    }

    suspend fun fetchBody(urlChapter: String, tryCache: Boolean = true): Response<String> {
        if (tryCache) chapterBodyDao.get(urlChapter)?.let {
            return@fetchBody Response.Success(it.body)
        }

        if (urlChapter.isLocalUri) {
            return Response.Error(
                """
                Unable to load chapter from url:
                $urlChapter
                
                Source is local but chapter content missing.
            """.trimIndent(), Exception()
            )
        }

        return downloaderRepository.bookChapter(urlChapter)
            .map {
                insertWithTitle(
                    chapterBody = ChapterBody(url = urlChapter, body = it.body),
                    title = it.title
                )
                it.body
            }
    }
}