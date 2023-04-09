package my.noveldokusha.ui.screens.reader.tools

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import my.noveldokusha.repository.Repository
import my.noveldokusha.ui.screens.reader.ChapterUrl

class ChaptersIsReadRoutine(
    val repository: Repository,
    private val scope: CoroutineScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("ChapterIsReadRoutine")
    )
) {
    fun setReadStart(chapterUrl: String) = checkLoadStatus(chapterUrl) { it.copy(startSeen = true) }
    fun setReadEnd(chapterUrl: String) = checkLoadStatus(chapterUrl) { it.copy(endSeen = true) }

    private data class ChapterReadStatus(val startSeen: Boolean, val endSeen: Boolean)

    private val chapterRead = mutableMapOf<ChapterUrl, ChapterReadStatus>()

    private fun checkLoadStatus(chapterUrl: String, fn: (ChapterReadStatus) -> ChapterReadStatus) =
        scope.launch {

            val chapter = repository.bookChapters.get(chapterUrl) ?: return@launch
            val oldStatus = chapterRead.getOrPut(chapterUrl) {
                when (chapter.read) {
                    true -> ChapterReadStatus(startSeen = true, endSeen = true)
                    false -> ChapterReadStatus(startSeen = false, endSeen = false)
                }
            }

            if (oldStatus.startSeen && oldStatus.endSeen) return@launch

            val newStatus = fn(oldStatus)
            if (newStatus.startSeen && newStatus.endSeen) {
                repository.bookChapters.setAsRead(chapterUrl = chapterUrl, read = true)
            }

            chapterRead[chapterUrl] = newStatus
        }
}