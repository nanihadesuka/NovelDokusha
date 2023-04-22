package my.noveldokusha.services

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import my.noveldokusha.R
import my.noveldokusha.data.Response
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.repository.Repository
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.utils.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
@OptIn(ObsoleteCoroutinesApi::class)
class LibraryUpdateService : Service() {
    @Inject
    lateinit var repository: Repository

    @Inject
    lateinit var networkClient: NetworkClient

    @Inject
    lateinit var scraper: Scraper

    @Inject
    lateinit var notificationsCenter: NotificationsCenter


    private class IntentData : Intent {
        var completedCategory by Extra_Boolean()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, completedCategory: Boolean) : super(
            ctx,
            LibraryUpdateService::class.java
        ) {
            this.completedCategory = completedCategory
        }
    }

    companion object {
        fun start(ctx: Context, completedCategory: Boolean) {
            if (!isRunning(ctx))
                ContextCompat.startForegroundService(ctx, IntentData(ctx, completedCategory))
        }

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(LibraryUpdateService::class.java)

        const val channel_id = "Library update"
    }

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private var job: Job? = null


    /**
     * Updates in the notification which books are currently in this instant being updated.
     */
    val updateActor = CoroutineScope(Dispatchers.IO).actor<Pair<Book, Boolean>> {
        val books = mutableSetOf<Book>()
        for ((book, add) in channel) {
            if (add) books.add(book) else books.remove(book)
            if (books.isNotEmpty()) {
                notificationsCenter.modifyNotification(notificationBuilder, channel_id) {
                    text = books.joinToString("\n") { it.title }
                }
            }
        }
    }

    /**
     * Updates in the notification the update progress count.
     */
    val updateActorCounter =
        CoroutineScope(Dispatchers.IO).actor<Pair<Int, Boolean>>(Dispatchers.IO) {
            var count = 0
            var totalCount = 0
            for ((value, isInit) in channel) {
                if (isInit) {
                    count = 0
                    totalCount = value
                } else count += 1

                notificationsCenter.modifyNotification(notificationBuilder, channel_id) {
                    title = "Updating library ($count/$totalCount)"
                    setProgress(totalCount, count, false)
                }
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        notificationBuilder = notificationsCenter.showNotification(
            channel_id = channel_id,
            importance = NotificationManager.IMPORTANCE_LOW
        )
        startForeground(channel_id.hashCode(), notificationBuilder.build())
    }

    override fun onDestroy() {
        job?.cancel()
        updateActor.close()
        updateActorCounter.close()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY
        val intentData = IntentData(intent)

        job = CoroutineScope(Dispatchers.IO).launch {
            tryAsResponse {
                updateLibrary(intentData.completedCategory)
            }.onError {
                Timber.e(it.exception)
            }

            stopSelf(startId)
        }
        return START_STICKY
    }

    /**
     * Library update function. Will update non completed books or completed books.
     * This function will also show a status notificaton of the update progress.
     */
    private suspend fun updateLibrary(completedCategory: Boolean) = withContext(Dispatchers.IO) {

        notificationsCenter.showNotification(channel_id) {
            setStyle(NotificationCompat.BigTextStyle())
            title = getString(R.string.updaing_library)
        }

        repository.libraryBooks.getAllInLibrary()
            .filter { it.completed == completedCategory }
            .filter { !it.url.startsWith("local://") }
            .also { launch(Dispatchers.IO) { updateActorCounter.send(Pair(it.size, true)) } }
            .groupBy { it.url.toHttpUrlOrNull()?.host }
            .map { (_, books) -> async { updateBooks(books) } }
            .awaitAll()
            .unzip()
            .let { (hasUpdates, hasFailed) ->
                val updates = hasUpdates.flatten()
                val failed = hasFailed.flatten()


                if (updates.isNotEmpty()) notificationsCenter.showNotification(
                    channel_id = "New chapters found for",
                    importance = NotificationManager.IMPORTANCE_DEFAULT
                ) {
                    title = getString(R.string.new_chapters_found_for)
                    text = updates.joinToString("\n")
                    setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    removeProgressBar()
                }

                if (failed.isNotEmpty()) notificationsCenter.showNotification("Update error") {
                    title = getString(R.string.update_error)
                    text = failed.joinToString("\n")
                    setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    removeProgressBar()
                }
            }
        notificationsCenter.close(channel_id)
    }

    private suspend fun updateBooks(books: List<Book>): Pair<List<String>, List<String>> =
        withContext(Dispatchers.Default)
        {
            val hasUpdates = mutableListOf<String>()
            val hasFailed = mutableListOf<String>()
            for (book in books) {
                val oldChaptersList = async(Dispatchers.IO) {
                    repository.bookChapters.chapters(book.url).map { it.url }.toSet()
                }

                launch(Dispatchers.IO) {
                    updateActor.send(Pair(book, true))
                }

                when (val res = downloadChaptersList(scraper, book.url)) {
                    is Response.Success -> {
                        oldChaptersList.join()
                        repository.bookChapters.merge(res.data, book.url)
                        val hasNewChapters = res.data.any { it.url !in oldChaptersList.await() }
                        if (hasNewChapters)
                            hasUpdates.add(book.title)
                    }

                    is Response.Error -> hasFailed.add(book.title)
                }

                launch(Dispatchers.IO) {
                    updateActor.send(Pair(book, false))
                    updateActorCounter.send(Pair(0, false))
                }
            }
            Pair(hasUpdates, hasFailed)
        }
}