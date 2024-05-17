package my.noveldokusha.features.reader

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldoksuha.coreui.BaseViewModel
import my.noveldoksuha.coreui.mappers.toTheme
import my.noveldokusha.core.utils.StateExtra_Boolean
import my.noveldokusha.core.utils.StateExtra_String
import my.noveldokusha.features.reader.manager.ReaderManager
import my.noveldokusha.features.reader.ui.ReaderScreenState
import my.noveldokusha.features.reader.ui.ReaderViewHandlersActions
import javax.inject.Inject
import kotlin.properties.Delegates

interface ReaderStateBundle {
    var bookUrl: String
    var chapterUrl: String
    var introScrollToSpeaker: Boolean
}

@HiltViewModel
internal class ReaderViewModel @Inject constructor(
    stateHandler: SavedStateHandle,
    private val appPreferences: AppPreferences,
    private val readerManager: ReaderManager,
    private val readerViewHandlersActions: ReaderViewHandlersActions,
) : BaseViewModel(), ReaderStateBundle {

    override var bookUrl by StateExtra_String(stateHandler)
    override var chapterUrl by StateExtra_String(stateHandler)
    override var introScrollToSpeaker by StateExtra_Boolean(stateHandler)

    private val readerSession = readerManager.initiateOrGetSession(
        bookUrl = bookUrl,
        chapterUrl = chapterUrl
    )

    private val readingPosStats = readerSession.readingStats
    private val themeId = appPreferences.THEME_ID.state(viewModelScope)

    val state = ReaderScreenState(
        showReaderInfo = mutableStateOf(false),
        readerInfo = ReaderScreenState.CurrentInfo(
            chapterTitle = derivedStateOf {
                readingPosStats.value?.chapterTitle ?: ""
            },
            chapterCurrentNumber = derivedStateOf {
                readingPosStats.value?.run { chapterIndex + 1 } ?: 0
            },
            chapterPercentageProgress = readerSession.readingChapterProgressPercentage,
            chaptersCount = derivedStateOf { readingPosStats.value?.chapterCount ?: 0 },
            chapterUrl = derivedStateOf { readingPosStats.value?.chapterUrl ?: "" }
        ),
        settings = ReaderScreenState.Settings(
            selectedSetting = mutableStateOf(ReaderScreenState.Settings.Type.None),
            isTextSelectable = appPreferences.READER_SELECTABLE_TEXT.state(viewModelScope),
            keepScreenOn = appPreferences.READER_KEEP_SCREEN_ON.state(viewModelScope),
            textToSpeech = readerSession.readerTextToSpeech.state,
            liveTranslation = readerSession.readerLiveTranslation.state,
            style = ReaderScreenState.Settings.StyleSettingsData(
                followSystem = appPreferences.THEME_FOLLOW_SYSTEM.state(viewModelScope),
                currentTheme = derivedStateOf { themeId.value.toTheme },
                textFont = appPreferences.READER_FONT_FAMILY.state(viewModelScope),
                textSize = appPreferences.READER_FONT_SIZE.state(viewModelScope),
            )
        ),
        showInvalidChapterDialog = mutableStateOf(false)
    )

    init {
        readerViewHandlersActions.showInvalidChapterDialog = {
            withContext(Dispatchers.Main) {
                state.showInvalidChapterDialog.value = true
            }
        }
    }

    val items = readerSession.items
    val chaptersLoader = readerSession.readerChaptersLoader
    val readerSpeaker = readerSession.readerTextToSpeech
    var readingCurrentChapter by Delegates.observable(readerSession.currentChapter) { _, _, new ->
        readerSession.currentChapter = new
    }
    val onTranslatorChanged = readerSession.readerLiveTranslation.onTranslatorChanged
    val ttsScrolledToTheTop = readerSession.readerTextToSpeech.scrolledToTheTop
    val ttsScrolledToTheBottom = readerSession.readerTextToSpeech.scrolledToTheBottom

    fun onCloseManually() {
        readerManager.close()
    }


    fun startSpeaker(itemIndex: Int) =
        readerSession.startSpeaker(itemIndex = itemIndex)

    fun reloadReader() {
        val currentChapter = readingCurrentChapter.copy()
        readerSession.reloadReader()
        chaptersLoader.tryLoadRestartedInitial(currentChapter)
    }

    fun updateInfoViewTo(itemIndex: Int) =
        readerSession.updateInfoViewTo(itemIndex = itemIndex)

    fun markChapterStartAsSeen(chapterUrl: String) =
        readerSession.markChapterStartAsSeen(chapterUrl = chapterUrl)

    fun markChapterEndAsSeen(chapterUrl: String) =
        readerSession.markChapterEndAsSeen(chapterUrl = chapterUrl)
}
