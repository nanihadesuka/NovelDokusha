package my.noveldokusha.ui.main

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
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
	
	data class UpdateNotice(val newChapters: List<String>, val failed: List<String>)
	
	val updateNotice = MutableLiveData<UpdateNotice>()
	
	fun update()
	{
		refreshing.postValue(true)
		val completed = showCompleted
		CoroutineScope(Dispatchers.IO).launch {
			bookstore.bookLibrary.getAllInLibrary()
				.filter { it.completed == completed }
				.groupBy { it.url.toHttpUrlOrNull()?.host }
				.map { (host, books) ->
					val newChapters = mutableListOf<String>()
					val failed = mutableListOf<String>()
					async(Dispatchers.IO) {
						for (book in books)
						{
							val oldChaptersList = async { bookstore.bookChapter.chapters(book.url).map { it.url }.toSet() }
							when (val res = downloadChaptersList(book.url))
							{
								is Response.Success ->
								{
									oldChaptersList.join()
									launch { bookstore.bookChapter.merge(res.data, book.url) }
									val existingChapters = oldChaptersList.await()
									val hasNewChapters = res.data.any { it.url !in existingChapters }
									if (hasNewChapters)
										newChapters.add(book.title)
								}
								is Response.Error -> failed.add(book.title)
							}
						}
						return@async Pair(newChapters, failed)
					}
				}.awaitAll()
				.unzip()
				.let { (newChapters, failed) -> newChapters.flatten() to failed.flatten() }
				.let { (newChapters, failed) -> updateNotice.postValue(UpdateNotice(newChapters, failed)) }
			
			refreshing.postValue(false)
		}
	}
}