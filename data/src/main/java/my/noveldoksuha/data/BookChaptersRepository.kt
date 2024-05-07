package my.noveldoksuha.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.tooling.local_database.DAOs.ChapterDao
import my.noveldokusha.tooling.local_database.tables.Chapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookChaptersRepository @Inject constructor(
    private val chapterDao: ChapterDao,
) {
    suspend fun update(chapter: Chapter) = chapterDao.update(chapter)
    suspend fun updatePosition(chapterUrl: String, lastReadPosition: Int, lastReadOffset: Int) =
        chapterDao.updatePosition(
            chapterUrl = chapterUrl,
            lastReadPosition = lastReadPosition,
            lastReadOffset = lastReadOffset
        )

    suspend fun setAsRead(chapterUrl: String, read: Boolean) =
        chapterDao.setAsRead(chapterUrl, read)

    suspend fun get(url: String) = chapterDao.get(url)
    suspend fun hasChapters(bookUrl: String) = chapterDao.hasChapters(bookUrl)
    suspend fun getAll() = chapterDao.getAll()
    suspend fun updateTitle(url: String, title: String) =
        chapterDao.updateTitle(url, title)

    suspend fun setAsRead(chaptersUrl: List<String>) =
        chaptersUrl.chunked(500).forEach { chapterDao.setAsRead(it) }

    suspend fun setAsUnread(chaptersUrl: List<String>) =
        chaptersUrl.chunked(500).forEach { chapterDao.setAsUnread(it) }

    suspend fun insert(chapters: List<Chapter>) =
        chapterDao.insert(chapters.filter(::isValid))

    suspend fun insertReplace(chapters: List<Chapter>) =
        chapterDao.insertReplace(chapters.filter(::isValid))

    suspend fun removeAllFromBook(bookUrl: String) = chapterDao.removeAllFromBook(bookUrl)
    suspend fun chapters(bookUrl: String) = chapterDao.chapters(bookUrl)
    suspend fun getFirstChapter(bookUrl: String) = chapterDao.getFirstChapter(bookUrl)
    fun getChaptersWithContextFlow(bookUrl: String) =
        chapterDao.getChaptersWithContextFlow(bookUrl)

    suspend fun merge(newChapters: List<Chapter>, bookUrl: String) = withContext(Dispatchers.IO){
        val current = chapters(bookUrl).associateBy { it.url }.toMutableMap()
        for (chapter in newChapters)
            current.merge(
                chapter.url,
                chapter
            ) { old, new -> old.copy(position = new.position) }
        insertReplace(current.values.toList())
    }
}