package my.noveldokusha.services.libraryUpdate

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.Parcelable
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import my.noveldokusha.data.LibraryCategory
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.isLocalUri
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.repository.Repository
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.utils.Extra_Parcelable
import my.noveldokusha.utils.isServiceRunning
import my.noveldokusha.utils.tryAsResponse
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class LibraryUpdateService : Service() {
    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var networkClient: NetworkClient

    @Inject
    lateinit var scraper: Scraper

    @Inject
    lateinit var libraryUpdateNotifications: LibraryUpdateNotifications

    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("LibraryUpdateService")
    )

    private class IntentData : Intent {

        @Parcelize
        data class Data(
            val updateCategory: LibraryCategory
        ) : Parcelable

        var data by Extra_Parcelable<Data>()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, category: LibraryCategory) : super(
            ctx,
            LibraryUpdateService::class.java
        ) {
            data = Data(updateCategory = category)
        }
    }

    companion object {
        fun start(ctx: Context, updateCategory: LibraryCategory) {
            if (!isRunning(ctx))
                ContextCompat.startForegroundService(ctx, IntentData(ctx, updateCategory))
        }

        private fun isRunning(context: Context): Boolean =
            context.isServiceRunning(LibraryUpdateService::class.java)

    }


    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val notification = libraryUpdateNotifications.createEmptyUpdatingNotification()
        startForeground(libraryUpdateNotifications.notificationId, notification)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val intentData = IntentData(intent)

        serviceScope.launch {
            tryAsResponse {
                updateLibrary(intentData.data.updateCategory)
            }.onError {
                Timber.e(it.exception)
            }

            stopSelf(startId)
        }
        return START_STICKY
    }


    private data class NewUpdate(
        val newChapters: List<Chapter>,
        val book: Book
    )

    private data class CountingUpdating(
        val updated: Int,
        val total: Int
    )


    /**
     * Library update function. Will update non completed books or completed books.
     * This function will also show a status notificaton of the update progress.
     */
    private suspend fun updateLibrary(
        updateCategory: LibraryCategory
    ) = withContext(Dispatchers.Main) {

        val isCompleted = updateCategory == LibraryCategory.COMPLETED

        val countingUpdating = MutableStateFlow<CountingUpdating?>(null)
        val currentUpdating = MutableStateFlow<Set<Book>>(setOf())
        val newUpdates = MutableStateFlow<Set<NewUpdate>>(setOf())
        val failedUpdates = MutableStateFlow<Set<Book>>(setOf())

        val currentUpdatingNotifyJob = launch(Dispatchers.Main) {
            combine(
                flow = countingUpdating,
                flow2 = currentUpdating,
                transform = { count, current -> count to current }
            ).collectLatest { (counting, current) ->
                libraryUpdateNotifications.updateUpdatingNotification(
                    countingUpdated = counting?.updated ?: 0,
                    countingTotal = counting?.total ?: 0,
                    books = current
                )
            }
        }

        repository.libraryBooks.getAllInLibrary()
            .filter { it.completed == isCompleted }
            .filter { !it.url.isLocalUri }
            .also { list ->
                countingUpdating.update { CountingUpdating(updated = 0, total = list.size) }
            }
            .groupBy { it.url.toHttpUrlOrNull()?.host }
            .map { (_, books) ->
                async {
                    updateBooks(
                        books = books,
                        currentUpdating = currentUpdating,
                        newUpdates = newUpdates,
                        failedUpdates = failedUpdates,
                        countingUpdating = countingUpdating
                    )
                }
            }
            .awaitAll()

        newUpdates.value.forEachIndexed { index, newUpdate ->
            libraryUpdateNotifications.showNewChaptersNotification(
                book = newUpdate.book,
                newChapters = newUpdate.newChapters,
                silent = index == 0
            )
        }

        if (failedUpdates.value.isNotEmpty()) {
            libraryUpdateNotifications.showFailedNotification(
                books = failedUpdates.value
            )
        }

        currentUpdatingNotifyJob.cancel()
    }

    private suspend fun updateBooks(
        books: List<Book>,
        countingUpdating: MutableStateFlow<CountingUpdating?>,
        currentUpdating: MutableStateFlow<Set<Book>>,
        newUpdates: MutableStateFlow<Set<NewUpdate>>,
        failedUpdates: MutableStateFlow<Set<Book>>,
    ): Unit = withContext(Dispatchers.Default) {
        for (book in books) {
            currentUpdating.update { it + book }
            val oldChaptersList = async(Dispatchers.IO) {
                repository.bookChapters.chapters(book.url).map { it.url }.toSet()
            }

            downloadChaptersList(scraper, book.url).onSuccess { chapters ->
                oldChaptersList.join()
                repository.bookChapters.merge(chapters, book.url)
                val newChapters = chapters.filter { it.url !in oldChaptersList.await() }
                if (newChapters.isEmpty()) return@onSuccess
                newUpdates.update { it + NewUpdate(book = book, newChapters = newChapters) }

            }.onError {
                failedUpdates.update { it + book }
            }
            currentUpdating.update { it - book }
            countingUpdating.update { it?.copy(updated = it.updated + 1) }
        }
    }
}