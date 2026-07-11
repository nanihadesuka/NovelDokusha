package my.noveldokusha.features.reader.features

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import my.noveldokusha.core.appPreferences.AppPreferences
import my.noveldokusha.text_translator.domain.TranslationManager
import my.noveldokusha.text_translator.domain.TranslationModelState
import my.noveldokusha.text_translator.domain.TranslatorState

internal data class LiveTranslationSettingData(
    val isAvailable: Boolean,
    val enable: MutableState<Boolean>,
    val listOfAvailableModels: SnapshotStateList<TranslationModelState>,
    val source: MutableState<TranslationModelState?>,
    val target: MutableState<TranslationModelState?>,
    val activeProviderName: MutableState<String>,
    val onEnable: (Boolean) -> Unit,
    val onSourceChange: (TranslationModelState?) -> Unit,
    val onTargetChange: (TranslationModelState?) -> Unit,
    val onProviderChange: () -> Unit,
)

internal class ReaderLiveTranslation(
    private val translationManager: TranslationManager,
    private val appPreferences: AppPreferences,
    private val scope: CoroutineScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineName("LiveTranslator")
    )
) {
    val state = LiveTranslationSettingData(
        isAvailable = translationManager.available,
        listOfAvailableModels = translationManager.models,
        enable = mutableStateOf(appPreferences.GLOBAL_TRANSLATION_ENABLED.value),
        source = mutableStateOf(null),
        target = mutableStateOf(null),
        activeProviderName = mutableStateOf(getActiveProviderName()),
        onEnable = ::onEnable,
        onSourceChange = ::onSourceChange,
        onTargetChange = ::onTargetChange,
        onProviderChange = ::cycleProvider
    )

    var translatorState: TranslatorState? = null
        private set

    private val _onTranslatorChanged = MutableSharedFlow<Unit>()
    val onTranslatorChanged = _onTranslatorChanged.asSharedFlow()

    suspend fun init() {
        val source = appPreferences.GLOBAL_TRANSLATION_PREFERRED_SOURCE.value
        val target = appPreferences.GLOBAL_TRANSLATION_PREFERRED_TARGET.value
        state.source.value = getValidTranslatorOrNull(source)
        state.target.value = getValidTranslatorOrNull(target)
        updateTranslatorState()
    }

    private suspend fun getValidTranslatorOrNull(language: String): TranslationModelState? {
        if (language.isBlank()) return null
        return translationManager.hasModelDownloaded(language)
    }

    /**
     * @return true if reader session needs to be updated
     */
    private fun updateTranslatorState(): Boolean {
        val isEnabled = state.enable.value
        val source = state.source.value
        val target = state.target.value

        val old = translatorState
        val new = when {
            !isEnabled || source == null || target == null || source.language == target.language -> null
            else -> translationManager.getTranslator(
                source = source.language,
                target = target.language
            )
        }.also { this.translatorState = it }


        return when {
            old == null && new == null -> false
            old != null && new != null -> when {
                old.source != new.source && old.target != new.target -> true
                else -> false
            }
            old == null && new != null -> new.source != new.target
            old != null && new == null -> old.source != old.target
            else -> true
        }
    }

    private fun onEnable(it: Boolean) {
        state.enable.value = it
        appPreferences.GLOBAL_TRANSLATION_ENABLED.value = it
        val update = updateTranslatorState()
        if (update) scope.launch {
            _onTranslatorChanged.emit(Unit)
        }
    }

    private fun onSourceChange(it: TranslationModelState?) {
        state.source.value = it
        appPreferences.GLOBAL_TRANSLATION_PREFERRED_SOURCE.value = it?.language ?: ""
        val update = updateTranslatorState()
        if (update) scope.launch {
            _onTranslatorChanged.emit(Unit)
        }
    }

    private fun onTargetChange(it: TranslationModelState?) {
        state.target.value = it
        appPreferences.GLOBAL_TRANSLATION_PREFERRED_TARGET.value = it?.language ?: ""
        val update = updateTranslatorState()
        if (update) scope.launch {
            _onTranslatorChanged.emit(Unit)
        }
    }

    private fun getActiveProviderName(): String = when (appPreferences.TRANSLATION_PROVIDER.value) {
        "GEMINI"      -> "Gemini"
        "GOOGLE_FREE" -> "Google Free"
        "OPENAI"      -> "OpenAI"
        else          -> "Google Enhanced"
    }

    /**
     * Cycles through the 4 providers when the user taps the provider button.
     * GOOGLE_PA → GOOGLE_FREE → GEMINI → OPENAI → GOOGLE_PA
     */
    private fun cycleProvider() {
        val current = appPreferences.TRANSLATION_PROVIDER.value
        val next = when (current) {
            "GOOGLE_PA"   -> "GOOGLE_FREE"
            "GOOGLE_FREE" -> "GEMINI"
            "GEMINI"      -> "OPENAI"
            else          -> "GOOGLE_PA"
        }
        appPreferences.TRANSLATION_PROVIDER.value = next
        state.activeProviderName.value = getActiveProviderName()
        val update = updateTranslatorState()
        if (update) scope.launch {
            _onTranslatorChanged.emit(Unit)
        }
    }
}
