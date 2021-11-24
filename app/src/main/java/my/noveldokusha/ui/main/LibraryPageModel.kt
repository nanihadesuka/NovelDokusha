package my.noveldokusha.ui.main

import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.map
import my.noveldokusha.*
import my.noveldokusha.ui.BaseViewModel

class LibraryPageModel(val showCompleted: Boolean) : BaseViewModel()
{
	val booksWithContext = bookstore.bookLibrary
		.getBooksInLibraryWithContextFlow
		.map { it.filter { book -> book.book.completed == showCompleted } }
		.asLiveData()
}
