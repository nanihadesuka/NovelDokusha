package my.noveldokusha.ui.screens.reader

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.*
import com.google.mlkit.nl.translate.TranslateLanguage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import my.noveldokusha.AppPreferences
import my.noveldokusha.data.Repository
import my.noveldokusha.data.database.tables.Chapter
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.tools.TranslatorState
import my.noveldokusha.ui.BaseViewModel
import my.noveldokusha.ui.screens.reader.tools.ChaptersIsReadRoutine
import my.noveldokusha.ui.screens.reader.tools.saveLastReadPositionState
import my.noveldokusha.utils.StateExtra_String
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.properties.Delegates

interface ReaderStateBundle {
    var bookUrl: String
    var chapterUrl: String
}

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: Repository,
    private val state: SavedStateHandle,
    val appPreferences: AppPreferences,
    private val translationManager: TranslationManager,
) : BaseViewModel(), ReaderStateBundle {
    enum class ReaderState { IDLE, LOADING, INITIAL_LOAD }

    data class ChapterState(val url: String, val position: Int, val offset: Int)
    data class ChapterStats(val itemCount: Int, val chapter: Chapter, val index: Int)

    override var bookUrl by StateExtra_String(state)
    override var chapterUrl by StateExtra_String(state)

    var onTranslatorStateChanged: (() -> Unit)? = null

    val textFont by appPreferences.READER_FONT_FAMILY.state(viewModelScope)
    val textSize by appPreferences.READER_FONT_SIZE.state(viewModelScope)

    var translator: TranslatorState? = null

    val liveTranslationSettingData = LiveTranslationSettingData(
        listOfAvailableModels = translationManager.models,
        enable = mutableStateOf(appPreferences.GLOBAL_TRANSLATIOR_ENABLED.value),
        source = mutableStateOf(getValidTranslatorOrNull(appPreferences.GLOBAL_TRANSLATIOR_PREFERRED_SOURCE.value)),
        target = mutableStateOf(getValidTranslatorOrNull(appPreferences.GLOBAL_TRANSLATIOR_PREFERRED_TARGET.value)),
        onEnable = ::translatorOnEnable,
        onSourceChange = ::translatorOnSourceChange,
        onTargetChange = ::translatorOnTargetChange,
        onDownloadTranslationModel = translationManager::downloadModel
    )

    private fun getValidTranslatorOrNull(language: String): TranslationModelState? {
        if (language.isBlank()) return null
        return translationManager.hasModelDownloadedSync(language)
    }

    private fun updateTranslatorState() {
        val isEnabled = liveTranslationSettingData.enable.value
        val source = liveTranslationSettingData.source.value
        val target = liveTranslationSettingData.target.value
        if (!isEnabled || source == null || target == null) {
            if (translator == null) return
            translator = null
        } else {
            val old = translator
            if (old != null && old.source == source.language && old.target == target.language)
                return
            if (source.language == target.language)
                return
            translator = translationManager.getTranslator(
                source = source.language,
                target = target.language
            )
            onTranslatorStateChanged?.invoke()
        }
    }

    private fun translatorOnEnable(it: Boolean) {
        liveTranslationSettingData.enable.value = it
        appPreferences.GLOBAL_TRANSLATIOR_ENABLED.value = it
        updateTranslatorState()
    }

    private fun translatorOnSourceChange(it: TranslationModelState?) {
        liveTranslationSettingData.source.value = it
        appPreferences.GLOBAL_TRANSLATIOR_PREFERRED_SOURCE.value = it?.language ?: ""
        updateTranslatorState()
    }

    private fun translatorOnTargetChange(it: TranslationModelState?) {
        liveTranslationSettingData.target.value = it
        appPreferences.GLOBAL_TRANSLATIOR_PREFERRED_TARGET.value = it?.language ?: ""
        updateTranslatorState()
    }

    val localBookBaseFolder =
        File(repository.settings.folderBooks, bookUrl.removePrefix("local://"))

    var currentChapter: ChapterState by Delegates.observable(
        ChapterState(
            chapterUrl,
            0,
            0
        )
    ) { _, old, new ->
        chapterUrl = new.url
        if (old.url != new.url) saveLastReadPositionState(repository, bookUrl, new, old)
    }

    var showReaderInfoView by mutableStateOf(false)
    val orderedChapters: List<Chapter>

    var readingPosStats by mutableStateOf<Pair<ChapterStats, Int>?>(null)

    init {
        updateTranslatorState()

        val chapter =
            viewModelScope.async(Dispatchers.IO) { repository.bookChapter.get(chapterUrl) }
        val bookChapters =
            viewModelScope.async(Dispatchers.IO) { repository.bookChapter.chapters(bookUrl) }

        // Need to fix this somehow
        runBlocking {
            currentChapter = ChapterState(
                url = chapterUrl,
                position = chapter.await()?.lastReadPosition ?: 0,
                offset = chapter.await()?.lastReadOffset ?: 0
            )
            this@ReaderViewModel.orderedChapters = bookChapters.await()
        }
    }

    val items = ArrayList<ReaderItem>()
    val readRoutine = ChaptersIsReadRoutine(repository)
    var readerState = ReaderState.INITIAL_LOAD

    private val chaptersStats = mutableMapOf<String, ChapterStats>()
    private val initialLoadDone = AtomicBoolean(false)

    override fun onCleared() {
        saveLastReadPositionState(repository, bookUrl, currentChapter)
        super.onCleared()
    }

    fun reloadReader() {
        readerState = ReaderState.INITIAL_LOAD
        items.clear()
    }

    fun initialLoad(fn: () -> Unit) {
        if (initialLoadDone.compareAndSet(false, true)) fn()
    }

    fun getNextChapterIndex(currentChapterUrl: String) =
        chaptersStats[currentChapterUrl]!!.index + 1

    fun getPreviousChapterIndex(currentChapterUrl: String) =
        chaptersStats[currentChapterUrl]!!.index - 1

    fun updateInfoViewTo(chapterUrl: String, itemPos: Int) {
        val chapter = chaptersStats[chapterUrl] ?: return
        readingPosStats = Pair(chapter, itemPos)
    }

    fun addChapterStats(chapter: Chapter, itemCount: Int, index: Int) {
        chaptersStats[chapter.url] = ChapterStats(itemCount, chapter, index)
    }

    suspend fun fetchChapterBody(chapterUrl: String) =
        repository.bookChapterBody.fetchBody(chapterUrl)

    suspend fun getChapterInitialPosition(): Pair<Int, Int>? {
        val stats = chaptersStats[currentChapter.url] ?: return null
        return my.noveldokusha.ui.screens.reader.tools.getChapterInitialPosition(
            repository = repository,
            bookUrl = bookUrl,
            chapter = stats.chapter,
            items = items
        )
    }
}
