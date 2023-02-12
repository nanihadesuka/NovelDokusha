package my.noveldokusha.ui.screens.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import my.noveldokusha.AppPreferences
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.utils.StateExtra_String
import javax.inject.Inject
import kotlin.properties.Delegates

interface ReaderStateBundle {
    var bookUrl: String
    var chapterUrl: String
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    state: SavedStateHandle,
    private val appPreferences: AppPreferences,
    private val readerManager: ReaderManager,
) : BaseViewModel(),
    ReaderStateBundle,
    ReaderManagerViewCallReferences by readerManager {

    override var bookUrl by StateExtra_String(state)
    override var chapterUrl by StateExtra_String(state)

    private val readerSession = readerManager.initiateOrGetSession(
        bookUrl = bookUrl,
        chapterUrl = chapterUrl
    )

    val textFont by appPreferences.READER_FONT_FAMILY.state(viewModelScope)
    val textSize by appPreferences.READER_FONT_SIZE.state(viewModelScope)
    val isTextSelectable by appPreferences.READER_SELECTABLE_TEXT.state(viewModelScope)

    var showReaderInfoView by mutableStateOf(false)

    val items = readerSession.items
    val chaptersLoader = readerSession.chaptersLoader
    val textToSpeechSettingData = readerSession.readerSpeaker.settings
    val readerSpeaker = readerSession.readerSpeaker
    val chapterPercentageProgress = readerSession.screenScrollChapterPercentageProgress
    val orderedChapters = readerSession.orderedChapters.toList()
    var currentChapter by Delegates.observable(readerSession.currentChapter) { _, _, new ->
        readerSession.currentChapter = new
    }
    val readingPosStats = readerSession.screenScrollReadingPosStats

    val liveTranslationSettingState = readerSession.liveTranslation.settingsState
    val onTranslatorChanged = readerSession.liveTranslation.onTranslatorChanged

    val ttsScrolledToTheTop = readerSession.readerSpeaker.scrolledToTheTop
    val ttsScrolledToTheBottom = readerSession.readerSpeaker.scrolledToTheBottom

    fun onBackPressed() {
        readerManager.close()
    }

    fun onViewDestroyed() {
        readerManager.invalidateViewsHandlers()
    }

    fun startSpeaker(itemIndex: Int) =
        readerSession.startSpeaker(itemIndex = itemIndex)

    fun reloadReader() =
        readerSession.reloadReader()

    fun updateInfoViewTo(itemIndex: Int) =
        readerSession.updateInfoViewTo(itemIndex = itemIndex)

    fun markChapterStartAsSeen(chapterUrl: String) =
        readerSession.markChapterStartAsSeen(chapterUrl = chapterUrl)

    fun markChapterEndAsSeen(chapterUrl: String) =
        readerSession.markChapterEndAsSeen(chapterUrl = chapterUrl)
}
