package my.noveldokusha.features.reader.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.features.reader.domain.InitialPositionChapter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReaderViewHandlersActions @Inject constructor() {

    suspend fun doForceUpdateListViewState() {
        withContext(Dispatchers.Main.immediate) {
            forceUpdateListViewState?.invoke()
        }
    }

    suspend fun doMaintainLastVisiblePosition(fn: suspend () -> Unit) {
        withContext(Dispatchers.Main.immediate) {
            maintainLastVisiblePosition?.invoke(fn) ?: fn()
        }
    }

    suspend fun doMaintainStartPosition(fn: suspend () -> Unit) {
        withContext(Dispatchers.Main.immediate) {
            maintainStartPosition?.invoke(fn)
        }
    }

    suspend fun doSetInitialPosition(position: InitialPositionChapter) {
        withContext(Dispatchers.Main.immediate) {
            setInitialPosition?.invoke(position)
        }
    }

    suspend fun doShowInvalidChapterDialog() {
        withContext(Dispatchers.Main.immediate) {
            showInvalidChapterDialog?.invoke()
        }
    }

    @Volatile
    var forceUpdateListViewState: (suspend () -> Unit)? = null

    @Volatile
    var maintainLastVisiblePosition: (suspend (suspend () -> Unit) -> Unit)? = null

    @Volatile
    var maintainStartPosition: (suspend (suspend () -> Unit) -> Unit)? = null

    @Volatile
    var setInitialPosition: (suspend (InitialPositionChapter) -> Unit)? = null

    @Volatile
    var showInvalidChapterDialog: (suspend () -> Unit)? = null

    @Volatile
    var introScrollToCurrentChapter: Boolean = false


    fun invalidate() {
        forceUpdateListViewState = null
        maintainLastVisiblePosition = null
        maintainStartPosition = null
        setInitialPosition = null
        showInvalidChapterDialog = null
    }
}