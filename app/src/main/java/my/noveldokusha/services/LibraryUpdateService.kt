package my.noveldokusha.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import my.noveldokusha.*
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.uiUtils.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject

@AndroidEntryPoint
class LibraryUpdateService : Service()
{
    @Inject
    lateinit var repository: Repository

    private class IntentData : Intent
    {
        var completedCategory by Extra_Boolean()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, completedCategory: Boolean) : super(ctx, LibraryUpdateService::class.java)
        {
            this.completedCategory = completedCategory
        }
    }

    companion object
    {
        fun start(ctx: Context, completedCategory: Boolean)
        {
            if (!isRunning(ctx))
                ContextCompat.startForegroundService(ctx, IntentData(ctx, completedCategory))
        }

        fun isRunning(context: Context): Boolean =
            context.isServiceRunning(LibraryUpdateService::class.java)

        const val channel_id = "Library update"
    }

    private lateinit var notification: NotificationCompat.Builder
    private var job: Job? = null


    /**
     * Updates in the notification what books are currently in this instant being updated.
     */
    val updateActor = CoroutineScope(Dispatchers.IO).actor<Pair<Book, Boolean>> {
        val books = mutableSetOf<Book>()
        for ((book, add) in channel)
        {
            if (add) books.add(book) else books.remove(book)
            if (books.isNotEmpty()) notification.showNotification(channel_id) {
                text = books.joinToString("\n") { it.title }
            }
        }
    }

    /**
     * Updates in the notification the update progress count.
     */
    val updateActorCounter = CoroutineScope(Dispatchers.IO).actor<Pair<Int, Boolean>>(Dispatchers.IO) {
        var count = 0
        var totalCount = 0
        for ((value, isInit) in channel)
        {
            if (isInit)
            {
                count = 0
                totalCount = value
            } else count += 1

            notification.showNotification(channel_id) {
                title = "Updating library ($count/$totalCount)"
                setProgress(totalCount, count, false)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate()
    {
        super.onCreate()
        notification = showNotification(this, channel_id) {}
        startForeground(channel_id.hashCode(), notification.build())
    }

    override fun onDestroy()
    {
        job?.cancel()
        updateActor.close()
        updateActorCounter.close()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
    {
        if (intent == null) return START_NOT_STICKY
        val intentData = IntentData(intent)

        job = CoroutineScope(Dispatchers.IO).launch {
            try
            {
                updateLibrary(intentData.completedCategory)
            } catch (e: Exception)
            {
                Log.e(this::class.simpleName, "Failed to start command")
            }

            stopSelf(startId)
        }
        return START_NOT_STICKY
    }

    /**
     * Library update function. Will update non completed books or completed books.
     * This function will also show a status notificaton of the update progress.
     */
    suspend fun updateLibrary(completedCategory: Boolean) = withContext(Dispatchers.IO) {

        notification.showNotification(channel_id) {
            setStyle(NotificationCompat.BigTextStyle())
            title = getString(R.string.updaing_library)
        }

        repository.bookLibrary.getAllInLibrary()
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

                if (updates.isNotEmpty()) notification.showNotification("New chapters found for") {
                    title = "New chapters found for"
                    text = updates.joinToString("\n")
                    setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    removeProgressBar()
                }

                if (failed.isNotEmpty()) notification.showNotification("Update error") {
                    title = "Update error"
                    text = failed.joinToString("\n")
                    setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    removeProgressBar()
                }
            }
        notification.close(this@LibraryUpdateService, channel_id)
    }

    private suspend fun updateBooks(books: List<Book>): Pair<List<String>, List<String>> = withContext(Dispatchers.Default)
    {
        val hasUpdates = mutableListOf<String>()
        val hasFailed = mutableListOf<String>()
        for (book in books)
        {
            val oldChaptersList = async(Dispatchers.IO) {
                repository.bookChapter.chapters(book.url).map { it.url }.toSet()
            }

            launch(Dispatchers.IO) {
                updateActor.send(Pair(book, true))
            }

            when (val res = downloadChaptersList(book.url))
            {
                is Response.Success ->
                {
                    oldChaptersList.join()
                    launch(Dispatchers.IO) {
                        repository.bookChapter.merge(res.data, book.url)
                    }
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