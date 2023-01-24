package my.noveldokusha.ui.screens.reader

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.asSharedFlow
import my.noveldokusha.AppPreferences
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.screens.reader.tools.LiveTranslation
import my.noveldokusha.utils.StateExtra_String
import javax.inject.Inject

interface ReaderStateBundle {
    var bookUrl: String
    var chapterUrl: String
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    state: SavedStateHandle,
    private val appPreferences: AppPreferences,
    private val readerManager: ReaderManager,
    private val liveTranslation: LiveTranslation,
) :
    BaseViewModel(),
    ReaderStateBundle,
    ReaderManagerViewCallReferences by readerManager
{

    override var bookUrl by StateExtra_String(state)
    override var chapterUrl by StateExtra_String(state)

    init {
        readerManager.initiateOrGetSession(
            bookUrl = bookUrl,
            chapterUrl = chapterUrl
        )
    }

    val textFont by appPreferences.READER_FONT_FAMILY.state(viewModelScope)
    val textSize by appPreferences.READER_FONT_SIZE.state(viewModelScope)
    val isTextSelectable by appPreferences.READER_SELECTABLE_TEXT.state(viewModelScope)

    var showReaderInfoView by mutableStateOf(false)

    private val session get() = readerManager.session

    val items = session.items
    val chaptersLoader = session.chaptersLoader
    val textToSpeechSettingData = session.readerSpeaker.settings
    val readerSpeaker = session.readerSpeaker
    val chapterPercentageProgress = session.chapterPercentageProgress
    val orderedChapters = session.orderedChapters
    var currentChapter
        get() = session.currentChapter
        set(value) = run { session.currentChapter = value }
    var readingPosStats
        get() = session.readingPosStats
        set(value) = run { session.readingPosStats = value }

    val liveTranslationSettingState = liveTranslation.settingsState
    val onTranslatorChanged = liveTranslation.onTranslatorChanged.asSharedFlow()

    val ttsScrolledToTheTop = session.ttsScrolledToTheTop
    val ttsScrolledToTheBottom = session.ttsScrolledToTheBottom

    fun onBackPressed() {
        session.close()
    }

    fun onViewDestroyed() {
        readerManager.invalidateViewsHandlers()
    }

    fun startSpeaker(itemIndex: Int) =
        readerManager.session.startSpeaker(itemIndex = itemIndex)

    fun reloadReader() =
        readerManager.session.reloadReader()

    fun updateInfoViewTo(itemIndex: Int) =
        readerManager.session.updateInfoViewTo(itemIndex = itemIndex)

    fun markChapterStartAsSeen(chapterUrl: String) =
        readerManager.session.markChapterStartAsSeen(chapterUrl = chapterUrl)

    fun markChapterEndAsSeen(chapterUrl: String) =
        readerManager.session.markChapterEndAsSeen(chapterUrl = chapterUrl)
}
