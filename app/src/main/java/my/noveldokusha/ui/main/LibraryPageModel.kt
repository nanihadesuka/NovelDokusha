package my.noveldokusha.ui.main

import LiveEvent
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import my.noveldokusha.Book
import my.noveldokusha.bookstore
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChaptersList
import my.noveldokusha.ui.BaseViewModel
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import kotlin.properties.Delegates

class LibraryPageModel : BaseViewModel()
{
	fun initialization(showCompleted: Boolean) = callOneTime {
		this.showCompleted = showCompleted
	}
	
	val booksWithContextFlow = bookstore.bookLibrary
		.getBooksInLibraryWithContextFlow
		.map { it.filter { book -> book.book.completed == showCompleted } }
	
	private var showCompleted by Delegates.notNull<Boolean>()
	val refreshing = MutableLiveData(false)
	
	data class UpdateNotice(val hasUpdates: List<String>, val hasFailed: List<String>)
	
	val updateNotice = LiveEvent<UpdateNotice>()
	
	fun update()
	{
		refreshing.postValue(true)
		val completed = showCompleted
		CoroutineScope(Dispatchers.IO).launch {
			bookstore.bookLibrary.getAllInLibrary().asSequence()
				.filter { it.completed == completed }
				.filter { !it.url.startsWith("local://") }
				.groupBy { it.url.toHttpUrlOrNull()?.host }
				.map { (_, books) -> async { updateBooks(books) } }
				.awaitAll()
				.unzip()
				.let { (hasUpdates, hasFailed) -> updateNotice.postValue(LibraryPageModel.UpdateNotice(hasUpdates.flatten(), hasFailed.flatten())) }
			
			refreshing.postValue(false)
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
	}
	Pair(hasUpdates, hasFailed)
}