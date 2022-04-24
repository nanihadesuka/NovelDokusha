package my.noveldokusha.ui.main.library

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.*
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.data.BookWithContext
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.services.EpubImportService
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.ui.chaptersList.ChaptersActivity
import my.noveldokusha.ui.theme.Theme

@AndroidEntryPoint
class LibraryFragment : BaseFragment()
{
    private val viewModel by viewModels<LibraryViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        val view = ComposeView(requireContext())
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            Theme(appPreferences = appPreferences) {
                LibraryBody(
                    tabs = listOf("Default", "Completed"),
                    onBookClick = ::goToBookChaptersPage,
                    onBookLongClick = ::openCompletedDialog
                )
            }
        }
        return view
    }

    private fun goToBookChaptersPage(book: BookWithContext)
    {
        ChaptersActivity.IntentData(
            requireContext(),
            bookMetadata = BookMetadata(title = book.book.title, url = book.book.url)
        ).let(::startActivity)
    }

    private fun openCompletedDialog(book: BookWithContext)
    {
        completedDialog(requireContext(), book.book)
    }

    private fun completedDialog(ctx: Context, book: Book) = MaterialDialog(ctx).show {
        title(text = book.title)
        checkBoxPrompt(text = "Completed", isCheckedDefault = book.completed) {}
        negativeButton(text = "Cancel")
        positiveButton(text = "Ok") {
            val completed = isCheckPromptChecked()
            viewModel.setBookAsCompleted(book, completed)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater)
    {
        inflater.inflate(R.menu.library_menu__appbar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId)
    {
        R.id.import_epub -> openEpubImporter().let { true }
        R.id.library_filter_actions -> openLibraryFilterActions().let { true }
        else -> super.onOptionsItemSelected(item)
    }

    fun openEpubImporter()
    {
        val read = Manifest.permission.READ_EXTERNAL_STORAGE
        permissionRequest(read) {
            val intent = Intent(Intent.ACTION_GET_CONTENT).also {
                it.addCategory(Intent.CATEGORY_OPENABLE)
                it.type = "application/epub+zip"
            }

            activityRequest(intent) { resultCode, data ->
                if (resultCode != Activity.RESULT_OK) return@activityRequest
                val uri = data?.data ?: return@activityRequest
                EpubImportService.start(requireContext(), uri)
            }
        }
    }

    fun openLibraryFilterActions()
    {
        if (null != parentFragmentManager.findFragmentByTag(LibraryFragmentBottomSheetDialog.tag))
            return

        LibraryFragmentBottomSheetDialog()
            .show(parentFragmentManager, LibraryFragmentBottomSheetDialog.tag)
    }
}