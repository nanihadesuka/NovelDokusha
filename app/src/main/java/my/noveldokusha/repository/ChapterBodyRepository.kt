package my.noveldokusha.repository

import my.noveldokusha.data.database.AppDatabaseOperations
import my.noveldokusha.data.database.DAOs.ChapterBodyDao
import my.noveldokusha.data.database.tables.ChapterBody
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.network.Response
import my.noveldokusha.network.map
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.downloadChapter

class ChapterBodyRepository(
    private val chapterBodyDao: ChapterBodyDao,
    private val operations: AppDatabaseOperations,
    private val bookChaptersRepository: BookChaptersRepository,
    private val scraper: Scraper,
    private val networkClient: NetworkClient
) {
    suspend fun getAll() = chapterBodyDao.getAll()
    suspend fun insertReplace(chapterBodies: List<ChapterBody>) =
        chapterBodyDao.insertReplace(chapterBodies)

    suspend fun insertReplace(chapterBody: ChapterBody) =
        chapterBodyDao.insertReplace(chapterBody)

    suspend fun removeRows(chaptersUrl: List<String>) =
        chaptersUrl.chunked(500).forEach { chapterBodyDao.removeChapterRows(it) }

    suspend fun insertWithTitle(chapterBody: ChapterBody, title: String?) = operations.transaction {
        insertReplace(chapterBody)
        if (title != null)
            bookChaptersRepository.updateTitle(chapterBody.url, title)
    }

    suspend fun fetchBody(urlChapter: String, tryCache: Boolean = true): Response<String> {
        if (tryCache) chapterBodyDao.get(urlChapter)?.let {
            return@fetchBody Response.Success(it.body)
        }

        if (urlChapter.startsWith("local://")) {
            return Response.Error(
                """
                Unable to load chapter from url:
                $urlChapter
                
                Source is local but chapter content missing.
            """.trimIndent(), Exception()
            )
        }

        return downloadChapter(scraper, networkClient, urlChapter)
            .map {
                insertWithTitle(
                    chapterBody = ChapterBody(url = urlChapter, body = it.body),
                    title = it.title
                )
                it.body
            }
    }
}