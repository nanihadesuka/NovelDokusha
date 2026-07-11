package my.noveldokusha.settings

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.snapshots.SnapshotStateList
import my.noveldokusha.core.domain.RemoteAppVersion
import my.noveldokusha.text_translator.domain.TranslationModelState
import my.noveldoksuha.coreui.theme.Themes

data class SettingsScreenState(
    val databaseSize: MutableState<String>,
    val imageFolderSize: MutableState<String>,
    val followsSystemTheme: State<Boolean>,
    val currentTheme: State<Themes>,
    val isTranslationSettingsVisible: State<Boolean>,
    val translationModelsStates: SnapshotStateList<TranslationModelState>,
    val updateAppSetting: UpdateApp,
    val libraryAutoUpdate: LibraryAutoUpdate,
    val translationSettings: TranslationSettings,
) {
    data class UpdateApp(
        val currentAppVersion: String,
        val appUpdateCheckerEnabled: MutableState<Boolean>,
        val showNewVersionDialog: MutableState<RemoteAppVersion?>,
        val checkingForNewVersion: MutableState<Boolean>,
    )

    data class LibraryAutoUpdate(
        val autoUpdateEnabled: MutableState<Boolean>,
        val autoUpdateIntervalHours: MutableState<Int>,
    )

    /**
     * Comprehensive translation settings covering all 4 providers.
     *
     * Provider selection ("GOOGLE_PA", "GOOGLE_FREE", "GEMINI", "OPENAI")
     * determines which provider-specific fields are relevant.
     */
    data class TranslationSettings(
        val provider: MutableState<String>,
        // Google PA
        val googlePaApiKeys: MutableState<String>,
        // Gemini
        val geminiApiKey: MutableState<String>,
        val geminiModel: MutableState<String>,
        val geminiTemperature: MutableState<Float>,
        // OpenAI
        val openAiApiKeys: MutableState<String>,
        val openAiBaseUrl: MutableState<String>,
        val openAiModel: MutableState<String>,
        // Shared LLM settings (Gemini + OpenAI)
        val promptPreset: MutableState<String>,
        val activeSystemPrompt: MutableState<String>,
        val batchSize: MutableState<Int>,
        val maxOutputTokens: MutableState<Int>,
        val useEnglishLocale: MutableState<Boolean>,
    )
}
