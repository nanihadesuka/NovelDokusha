package my.noveldokusha.ui.screens.reader.tools

import androidx.compose.runtime.mutableStateOf
import my.noveldokusha.AppPreferences
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.tools.TranslatorState
import my.noveldokusha.ui.screens.reader.LiveTranslationSettingData

class LiveTranslation(
    private val translationManager: TranslationManager,
    private val appPreferences: AppPreferences,
) {
    val settingsState = LiveTranslationSettingData(
        isAvailable = translationManager.available,
        listOfAvailableModels = translationManager.models,
        enable = mutableStateOf(appPreferences.GLOBAL_TRANSLATIOR_ENABLED.value),
        source = mutableStateOf(null),
        target = mutableStateOf(null),
        onEnable = ::onEnable,
        onSourceChange = ::oSourceChange,
        onTargetChange = ::onTargetChange,
        onDownloadTranslationModel = translationManager::downloadModel
    )

    var translator: TranslatorState? = null
        private set

    var onTranslatorChanged: (() -> Unit)? = null

    suspend fun init() {
        val source = appPreferences.GLOBAL_TRANSLATIOR_PREFERRED_SOURCE.value
        val target = appPreferences.GLOBAL_TRANSLATIOR_PREFERRED_TARGET.value
        settingsState.source.value = getValidTranslatorOrNull(source)
        settingsState.target.value = getValidTranslatorOrNull(target)

        updateLiveTranslation()
    }

    private suspend fun getValidTranslatorOrNull(language: String): TranslationModelState? {
        if (language.isBlank()) return null
        return translationManager.hasModelDownloaded(language)
    }

    private fun updateLiveTranslation() {
        val isEnabled = settingsState.enable.value
        val source = settingsState.source.value
        val target = settingsState.target.value
        translator = if (
            !isEnabled ||
            source == null ||
            target == null ||
            source.language == target.language
        ) {
            if (translator == null) return
            null
        } else {
            val old = translator
            if (old != null && old.source == source.language && old.target == target.language)
                return
            translationManager.getTranslator(
                source = source.language,
                target = target.language
            )
        }
        onTranslatorChanged?.invoke()
    }

    private fun onEnable(it: Boolean) {
        settingsState.enable.value = it
        appPreferences.GLOBAL_TRANSLATIOR_ENABLED.value = it
        updateLiveTranslation()
    }

    private fun oSourceChange(it: TranslationModelState?) {
        settingsState.source.value = it
        appPreferences.GLOBAL_TRANSLATIOR_PREFERRED_SOURCE.value = it?.language ?: ""
        updateLiveTranslation()
    }

    private fun onTargetChange(it: TranslationModelState?) {
        settingsState.target.value = it
        appPreferences.GLOBAL_TRANSLATIOR_PREFERRED_TARGET.value = it?.language ?: ""
        updateLiveTranslation()
    }
}