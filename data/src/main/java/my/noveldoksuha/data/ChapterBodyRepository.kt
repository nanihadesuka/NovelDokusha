package my.noveldoksuha.data

import my.noveldokusha.core.Response
import my.noveldokusha.core.isLocalUri
import my.noveldokusha.core.map
import my.noveldokusha.feature.local_database.AppDatabase
import my.noveldokusha.feature.local_database.DAOs.ChapterBodyDao
import my.noveldokusha.feature.local_database.tables.ChapterBody
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

    private suspend fun insertReplace(chapterBody: ChapterBody) =
        chapterBodyDao.insertReplace(chapterBody)

    suspend fun removeRows(chaptersUrl: List<String>) =
        chaptersUrl.chunked(500).forEach { chapterBodyDao.removeChapterRows(it) }

    private suspend fun insertWithTitle(chapterBody: ChapterBody, title: String?) = appDatabase.transaction {
        insertReplace(chapterBody)
        if (title != null)
            bookChaptersRepository.updateTitle(chapterBody.url, title)
    }

    suspend fun fetchBody(urlChapter: String, tryCache: Boolean = true): Response<String> {
        if (tryCache) chapterBodyDao.get(urlChapter)?.let {
            // Stale-cache guard: translation proxy URLs should have English bodies.
            // Before the source-routing fix, the wrong source was selected for
            // translate.goog / gemini.goog URLs, so Chinese text was cached.
            // Detect that and force a re-fetch so the translation source can
            // re-translate.
            if ((urlChapter.contains("translate.goog") || urlChapter.contains("gemini.goog"))
                && isPrimarilyCJK(it.body)) {
                chapterBodyDao.removeChapterRows(listOf(urlChapter))
            } else {
                return@fetchBody Response.Success(it.body)
            }
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

    companion object {
        /** Minimum CJK ratio to consider text "primarily Chinese" */
        private const val CJK_THRESHOLD = 0.12f
    }

    /**
     * Quick check whether text is primarily CJK (Chinese/Japanese/Korean).
     */
    private fun isPrimarilyCJK(text: String): Boolean {
        if (text.isBlank()) return false
        val cjk = text.count { ch ->
            ch.code in 0x4E00..0x9FFF ||   // CJK Unified Ideographs
            ch.code in 0x3400..0x4DBF ||   // CJK Extension A
            ch.code in 0x20000..0x2A6DF || // CJK Extension B
            ch.code in 0xF900..0xFAFF ||   // CJK Compatibility Ideographs
            ch.code in 0x3040..0x309F ||   // Hiragana
            ch.code in 0x30A0..0x30FF      // Katakana
        }
        val total = text.count { !it.isWhitespace() }
        return total > 0 && cjk.toFloat() / total > CJK_THRESHOLD
    }
}