package my.noveldokusha.foss

import androidx.compose.runtime.mutableStateListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.tools.TranslatorState

class TranslationManagerEmpty(
    private val coroutineScope: CoroutineScope
) : TranslationManager {

    override val available = false
    override val models = mutableStateListOf<TranslationModelState>()
    override suspend fun hasModelDownloaded(language: String) = null
    override fun getTranslator(
        source: String,
        target: String
    ): TranslatorState = TranslatorState(
        source = source,
        target = target,
        translate = { _ -> "" },
    )

    override fun downloadModel(language: String) = coroutineScope.launch { }
    override fun removeModel(language: String) = coroutineScope.launch { }
}