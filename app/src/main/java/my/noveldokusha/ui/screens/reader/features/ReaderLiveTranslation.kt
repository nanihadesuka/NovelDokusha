package my.noveldokusha.ui.screens.reader.tools

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import my.noveldokusha.AppPreferences
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.tools.TranslatorState

data class LiveTranslationSettingData(
    val isAvailable: Boolean,
    val enable: MutableState<Boolean>,
    val listOfAvailableModels: SnapshotStateList<TranslationModelState>,
    val source: MutableState<TranslationModelState?>,
    val target: MutableState<TranslationModelState?>,
    val onEnable: (Boolean) -> Unit,
    val onSourceChange: (TranslationModelState?) -> Unit,
    val onTargetChange: (TranslationModelState?) -> Unit,
    val onDownloadTranslationModel: (language: String) -> Unit,
)

class ReaderLiveTranslation(
    private val translationManager: TranslationManager,
    private val appPreferences: AppPreferences,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("LiveTranslator")
    )
) {
    val settingsState = LiveTranslationSettingData(
        isAvailable = translationManager.available,
        listOfAvailableModels = translationManager.models,
        enable = mutableStateOf(appPreferences.GLOBAL_TRANSLATION_ENABLED.value),
        source = mutableStateOf(null),
        target = mutableStateOf(null),
        onEnable = ::onEnable,
        onSourceChange = ::onSourceChange,
        onTargetChange = ::onTargetChange,
        onDownloadTranslationModel = translationManager::downloadModel
    )

    var translatorState: TranslatorState? = null
        private set

    private val _onTranslatorChanged = MutableSharedFlow<Unit>()
    val onTranslatorChanged = _onTranslatorChanged.asSharedFlow()

    suspend fun init() {
        val source = appPreferences.GLOBAL_TRANSLATION_PREFERRED_SOURCE.value
        val target = appPreferences.GLOBAL_TRANSLATION_PREFERRED_TARGET.value
        settingsState.source.value = getValidTranslatorOrNull(source)
        settingsState.target.value = getValidTranslatorOrNull(target)
        updateTranslatorState()
    }

    private suspend fun getValidTranslatorOrNull(language: String): TranslationModelState? {
        if (language.isBlank()) return null
        return translationManager.hasModelDownloaded(language)
    }

    private fun updateTranslatorState() {
        val isEnabled = settingsState.enable.value
        val source = settingsState.source.value
        val target = settingsState.target.value
        translatorState = if (
            !isEnabled ||
            source == null ||
            target == null ||
            source.language == target.language
        ) {
            if (translatorState == null) return
            null
        } else {
            val old = translatorState
            if (old != null && old.source == source.language && old.target == target.language)
                return
            translationManager.getTranslator(
                source = source.language,
                target = target.language
            )
        }
    }

    private fun onEnable(it: Boolean) {
        settingsState.enable.value = it
        appPreferences.GLOBAL_TRANSLATION_ENABLED.value = it
        updateTranslatorState()
        scope.launch {
            _onTranslatorChanged.emit(Unit)
        }
    }

    private fun onSourceChange(it: TranslationModelState?) {
        settingsState.source.value = it
        appPreferences.GLOBAL_TRANSLATION_PREFERRED_SOURCE.value = it?.language ?: ""
        updateTranslatorState()
        scope.launch {
            _onTranslatorChanged.emit(Unit)
        }
    }

    private fun onTargetChange(it: TranslationModelState?) {
        settingsState.target.value = it
        appPreferences.GLOBAL_TRANSLATION_PREFERRED_TARGET.value = it?.language ?: ""
        updateTranslatorState()
        scope.launch {
            _onTranslatorChanged.emit(Unit)
        }
    }
}
