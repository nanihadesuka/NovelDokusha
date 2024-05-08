package my.noveldokusha.features.reader.manager

import my.noveldokusha.features.reader.ui.ReaderViewHandlersActions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderManager @Inject constructor(
    private val readerSessionProvider: ReaderSessionProvider,
    private val readerViewHandlersActions: ReaderViewHandlersActions
) {
    var session: ReaderSession? = null
        private set


    fun initiateOrGetSession(
        bookUrl: String,
        chapterUrl: String,
    ): ReaderSession {
        val currentSession = session
        if (currentSession != null && bookUrl == currentSession.bookUrl && chapterUrl == currentSession.currentChapter.chapterUrl) {
            readerViewHandlersActions.introScrollToCurrentChapter = true
            return currentSession
        }

        currentSession?.close()
        readerViewHandlersActions.introScrollToCurrentChapter = false

        val newSession = readerSessionProvider.create(
            bookUrl = bookUrl,
            initialChapterUrl = chapterUrl,
        )
        session = newSession
        newSession.init()

        return newSession
    }

    fun close() {
        session?.close()
        session = null
    }
}
