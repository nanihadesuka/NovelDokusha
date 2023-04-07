package my.noveldokusha.ui.screens.reader.manager

import android.content.Context
import kotlinx.coroutines.*
import my.noveldokusha.AppPreferences
import my.noveldokusha.di.AppCoroutineScope
import my.noveldokusha.repository.Repository
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.ui.screens.reader.tools.InitialPositionChapter

interface ReaderManagerViewCallReferences {
    var forceUpdateListViewState: (suspend () -> Unit)?
    var maintainLastVisiblePosition: (suspend (suspend () -> Unit) -> Unit)?
    var maintainStartPosition: (suspend (suspend () -> Unit) -> Unit)?
    var setInitialPosition: (suspend (InitialPositionChapter) -> Unit)?
    var showInvalidChapterDialog: (suspend () -> Unit)?
}

class ReaderManager(
    private val repository: Repository,
    private val translationManager: TranslationManager,
    private val appPreferences: AppPreferences,
    private val context: Context,
    private val appScope: AppCoroutineScope,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("ReaderManager")
    )
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

    fun initiateOrGetSession(
        bookUrl: String,
        chapterUrl: String,
    ): ReaderSession {
        session?.let {
            if (bookUrl == it.bookUrl) {
                return it
            } else {
                it.close()
            }
        }

        return ReaderSession(
            bookUrl = bookUrl,
            initialChapterUrl = chapterUrl,
            scope = scope,
            appScope = appScope,
            repository = repository,
            translationManager = translationManager,
            appPreferences = appPreferences,
            forceUpdateListViewState = { withMainNow { forceUpdateListViewState?.invoke() } },
            maintainLastVisiblePosition = {
                withMainNow { maintainLastVisiblePosition?.invoke(it) ?: it() }
            },
            maintainStartPosition = {
                withMainNow { maintainStartPosition?.invoke(it) ?: it() }
            },
            setInitialPosition = { withMainNow { setInitialPosition?.invoke(it) } },
            showInvalidChapterDialog = { withMainNow { showInvalidChapterDialog?.invoke() } },
            context = context
        ).also {
            session = it
            it.init()
        }
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