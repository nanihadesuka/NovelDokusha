package my.noveldokusha.ui.main

import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.map
import my.noveldokusha.*
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.uiUtils.LiveEvent
import my.noveldokusha.uiUtils.stringRes
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class LibraryPageModel(val showCompleted: Boolean) : BaseViewModel()
{
	val booksWithContextFlow = bookstore.bookLibrary
		.getBooksInLibraryWithContextFlow
		.map { it.filter { book -> book.book.completed == showCompleted } }
	
	val refreshing = MutableLiveData(false)
	
	data class UpdateNotice(val hasUpdates: List<String>, val hasFailed: List<String>)
	
	val updateNotice = LiveEvent<UpdateNotice>()
	
	val update_channel_id = "Updating library"
	
	private val builder = App.buildNotification(update_channel_id) {
		setStyle(NotificationCompat.BigTextStyle())
		title = R.string.updaing_library.stringRes()
	}
	
	fun update()
	{
		refreshing.postValue(true)
		
		App.scope.launch(Dispatchers.IO) {
			bookstore.bookLibrary.getAllInLibrary()
				.filter { it.completed == showCompleted }
				.filter { !it.url.startsWith("local://") }
				.also { launch(Dispatchers.IO) { updateActorCounter.send(Pair(it.size, true)) } }
				.groupBy { it.url.toHttpUrlOrNull()?.host }
				.map { (_, books) -> async { updateBooks(books) } }
				.awaitAll()
				.unzip()
				.let { (hasUpdates, hasFailed) -> updateNotice.postValue(UpdateNotice(hasUpdates.flatten(), hasFailed.flatten())) }
			builder.close(update_channel_id)
			refreshing.postValue(false)
		}
	}
	
	private val updateActor = App.scope.actor<Pair<Book, Boolean>>(Dispatchers.IO) {
		val books = mutableSetOf<Book>()
		for ((book, add) in channel)
		{
			if (add) books.add(book) else books.remove(book)
			if (books.isNotEmpty()) builder.showNotification(update_channel_id) {
				text = books.joinToString("\n") { it.title }
			}
		}
	}
	
	private val updateActorCounter = App.scope.actor<Pair<Int, Boolean>>(Dispatchers.IO) {
		var count = 0
		var totalCount = 0
		for ((value, isInit) in channel)
		{
			if (isInit)
			{
				count = 0
				totalCount = value
			}
			else count += 1
			
			builder.showNotification(update_channel_id) {
				title = "Updating library ($count/$totalCount)"
				setProgress(totalCount, count, false)
			}
		}
	}
	
	private suspend fun updateBooks(books: List<Book>): Pair<List<String>, List<String>> = withContext(Dispatchers.Default)
	{
		val hasUpdates = mutableListOf<String>()
		val hasFailed = mutableListOf<String>()
		for (book in books)
		{
			val oldChaptersList = async(Dispatchers.IO) { bookstore.bookChapter.chapters(book.url).map { it.url }.toSet() }
			launch(Dispatchers.IO) { updateActor.send(Pair(book, true)) }
			when (val res = downloadChaptersList(book.url))
			{
				is Response.Success ->
				{
					oldChaptersList.join()
					launch(Dispatchers.IO) { bookstore.bookChapter.merge(res.data, book.url) }
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
