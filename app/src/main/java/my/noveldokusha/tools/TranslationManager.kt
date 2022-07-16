package my.noveldokusha.tools

import androidx.compose.runtime.mutableStateListOf
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModel
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.*
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Locale

data class TranslationModelState(
    val language: String,
    val model: TranslateRemoteModel?,
    val downloading: Boolean,
    val downloadingFailed: Boolean
) {
    val locale = Locale(language)
}

data class TranslatorState(
    val translator: Translator,
    val source: String,
    var target: String
) {
    val sourceLocale = Locale(source)
    val targetLocale = Locale(target)
    fun translate(input: String) = translator.translate(input)
}

class TranslationManager(
    private val coroutineScope: CoroutineScope
) {
    val models = mutableStateListOf<TranslationModelState>().apply {
        val list = TranslateLanguage.getAllLanguages().map {
            TranslationModelState(
                language = it,
                model = null,
                downloading = false,
                downloadingFailed = false
            )
        }
        addAll(list)
    }

    val modelsDownloaded = mutableStateListOf<TranslationModelState>()

    init {
        coroutineScope.launch {
            val downloaded = loadDownloadedModelsList().associateBy { it.language }
            models.replaceAll {
                it.copy(model = downloaded[it.language])
            }
            updateModelsDownloaded()
        }
    }

    private fun updateModelsDownloaded() {
        modelsDownloaded.clear()
        modelsDownloaded.addAll(models.filter { it.model != null })
    }

    private suspend fun loadDownloadedModelsList() = withContext(Dispatchers.IO) {
        RemoteModelManager
            .getInstance()
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .await()
    }

    // TODO Should not use blocking code
    fun hasModelDownloadedSync(language: String) = runBlocking {
        val model = TranslateRemoteModel.Builder(language).build()
        val isDownloaded = RemoteModelManager
            .getInstance()
            .isModelDownloaded(model)
            .await()
        if (isDownloaded) TranslationModelState(
            model = model,
            downloading = false,
            language = language,
            downloadingFailed = false
        ) else null
    }

    /**
     * Doesn't check if the model has been downloaded. Must be externally guaranteed.
     * @param source [TranslateLanguage]
     * @param target [TranslateLanguage]
     */
    fun getTranslator(
        source: String,
        target: String
    ): TranslatorState {
        val option = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()

        return TranslatorState(
            translator = Translation.getClient(option),
            source = source,
            target = target,
        )
    }

    fun downloadModel(language: String) = coroutineScope.launch {
        val index = models.indexOfFirst { it.language == language }
        if (index == -1 || models[index].model != null) return@launch

        models[index] = models[index].copy(
            downloadingFailed = false,
            downloading = true,
        )

        RemoteModelManager
            .getInstance()
            .download(
                TranslateRemoteModel.Builder(language).build(),
                DownloadConditions.Builder().build()
            )
            .addOnSuccessListener {
                models[index] = models[index].copy(
                    downloadingFailed = false,
                    downloading = false,
                    model = TranslateRemoteModel.Builder(language).build()
                )
                updateModelsDownloaded()
            }
            .addOnFailureListener {
                models[index] = models[index].copy(
                    downloadingFailed = true,
                    downloading = false
                )
            }
    }

    fun removeModel(language: String) = coroutineScope.launch {

        // English can't be removed.
        if (language == "en") return@launch

        val index = models.indexOfFirst { it.language == language }
        if (index == -1) return@launch
        val model = models[index].model ?: return@launch

        RemoteModelManager
            .getInstance()
            .deleteDownloadedModel(model)
            .addOnSuccessListener {
                models[index] = models[index].copy(
                    downloadingFailed = false,
                    downloading = false,
                    model = null
                )
                updateModelsDownloaded()
            }
    }
}