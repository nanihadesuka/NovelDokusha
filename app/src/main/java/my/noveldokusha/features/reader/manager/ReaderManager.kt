package my.noveldokusha.features.reader.manager

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.features.reader.tools.InitialPositionChapter
import javax.inject.Inject
import javax.inject.Singleton

interface ReaderManagerViewCallReferences {
    var forceUpdateListViewState: (suspend () -> Unit)?
    var maintainLastVisiblePosition: (suspend (suspend () -> Unit) -> Unit)?
    var maintainStartPosition: (suspend (suspend () -> Unit) -> Unit)?
    var setInitialPosition: (suspend (InitialPositionChapter) -> Unit)?
    var showInvalidChapterDialog: (suspend () -> Unit)?
    var introScrollToCurrentChapter: Boolean
}

@Singleton
class ReaderManager @Inject constructor(
    private val readerSessionProvider: ReaderSessionProvider,
) : ReaderManagerViewCallReferences {


    var session: ReaderSession? = null
        private set

    @Volatile
    override var forceUpdateListViewState: (suspend () -> Unit)? = null

    @Volatile
    override var maintainLastVisiblePosition: (suspend (suspend () -> Unit) -> Unit)? = null

    @Volatile
    override var maintainStartPosition: (suspend (suspend () -> Unit) -> Unit)? = null

    @Volatile
    override var setInitialPosition: (suspend (InitialPositionChapter) -> Unit)? = null

    @Volatile
    override var showInvalidChapterDialog: (suspend () -> Unit)? = null

    @Volatile
    override var introScrollToCurrentChapter: Boolean = false

    fun initiateOrGetSession(
        bookUrl: String,
        chapterUrl: String,
    ): ReaderSession {
        val currentSession = session
        if (currentSession != null && bookUrl == currentSession.bookUrl && chapterUrl == currentSession.currentChapter.chapterUrl) {
            introScrollToCurrentChapter = true
            return currentSession
        }

        currentSession?.close()
        introScrollToCurrentChapter = false

        val newSession = readerSessionProvider.create(
            bookUrl = bookUrl,
            initialChapterUrl = chapterUrl,
            forceUpdateListViewState = { withMainNow { forceUpdateListViewState?.invoke() } },
            maintainLastVisiblePosition = {
                withMainNow { maintainLastVisiblePosition?.invoke(it) ?: it() }
            },
            maintainStartPosition = {
                withMainNow { maintainStartPosition?.invoke(it) ?: it() }
            },
            setInitialPosition = { withMainNow { setInitialPosition?.invoke(it) } },
            showInvalidChapterDialog = { withMainNow { showInvalidChapterDialog?.invoke() } },

            )
        session = newSession
        newSession.init()

        return newSession
    }

    fun invalidateViewsHandlers() {
        forceUpdateListViewState = null
        maintainLastVisiblePosition = null
        maintainStartPosition = null
        setInitialPosition = null
        showInvalidChapterDialog = null
    }

    fun close() {
        session?.close()
        session = null
    }
}

private suspend fun <T> withMainNow(fn: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Main.immediate, fn)