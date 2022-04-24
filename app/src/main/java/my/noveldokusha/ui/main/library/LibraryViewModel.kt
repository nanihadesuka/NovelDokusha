package my.noveldokusha.ui.main.library

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.ui.BaseViewModel
import java.io.InputStream
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val repository: Repository,
) : BaseViewModel()
{

    fun setBookAsCompleted(book: Book, completed: Boolean) = CoroutineScope(Dispatchers.IO).launch {
        repository.bookLibrary.update(book.copy(completed = completed))
    }
}