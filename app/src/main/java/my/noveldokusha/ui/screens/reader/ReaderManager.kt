package my.noveldokusha.ui.screens.reader

import android.content.Context
import kotlinx.coroutines.*
import my.noveldokusha.AppPreferences
import my.noveldokusha.repository.Repository
import my.noveldokusha.ui.screens.reader.tools.ItemPosition
import my.noveldokusha.ui.screens.reader.tools.LiveTranslation

interface ReaderManagerViewCallReferences {
    var forceUpdateListViewState: (suspend () -> Unit)?
    var maintainLastVisiblePosition: (suspend (suspend () -> Unit) -> Unit)?
    var maintainStartPosition: (suspend (suspend () -> Unit) -> Unit)?
    var setInitialPosition: (suspend (ItemPosition) -> Unit)?
    var showInvalidChapterDialog: (suspend () -> Unit)?
}

class ReaderManager(
    private val repository: Repository,
    private val liveTranslation: LiveTranslation,
    private val appPreferences: AppPreferences,
    private val context: Context
) : ReaderManagerViewCallReferences {
    lateinit var session: ReaderSession
        private set

    private val sessionScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("Reader")
    )

    @Volatile
    override var forceUpdateListViewState: (suspend () -> Unit)? = null

    @Volatile
    override var maintainLastVisiblePosition: (suspend (suspend () -> Unit) -> Unit)? = null

    @Volatile
    override var maintainStartPosition: (suspend (suspend () -> Unit) -> Unit)? = null

    @Volatile
    override var setInitialPosition: (suspend (ItemPosition) -> Unit)? = null

    @Volatile
    override var showInvalidChapterDialog: (suspend () -> Unit)? = null

    fun initiateOrGetSession(
        bookUrl: String,
        chapterUrl: String,
    ) {
        if (::session.isInitialized) {
            if (session.bookUrl == bookUrl) {
                return
            } else {
                session.close()
            }
        }

        session = ReaderSession(
            bookUrl = bookUrl,
            initialChapterUrl = chapterUrl,
            scope = sessionScope,
            repository = repository,
            liveTranslation = liveTranslation,
            appPreferences = appPreferences,
            forceUpdateListViewState = { withMainNow { forceUpdateListViewState?.invoke() } },
            maintainLastVisiblePosition = { withMainNow { maintainLastVisiblePosition?.invoke(it) } },
            maintainStartPosition = { withMainNow { maintainStartPosition?.invoke(it) } },
            setInitialPosition = { withMainNow { setInitialPosition?.invoke(it) } },
            showInvalidChapterDialog = { withMainNow { showInvalidChapterDialog?.invoke() } },
            context = context
        )

        session.init()
    }

    fun invalidateViewsHandlers() {
        forceUpdateListViewState = null
        maintainLastVisiblePosition = null
        maintainStartPosition = null
        setInitialPosition = null
        showInvalidChapterDialog = null
    }
}

private suspend fun <T> withMainNow(fn: suspend CoroutineScope.() -> T) =
    withContext(Dispatchers.Main.immediate, fn)