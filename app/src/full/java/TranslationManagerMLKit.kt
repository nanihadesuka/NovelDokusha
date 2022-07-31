package my.noveldokusha.full

import androidx.compose.runtime.mutableStateListOf
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import my.noveldokusha.tools.TranslationManager
import my.noveldokusha.tools.TranslationModelState
import my.noveldokusha.tools.TranslatorState

class TranslationManagerMLKit(
    private val coroutineScope: CoroutineScope
) : TranslationManager {

    override val available = true

    override val models = mutableStateListOf<TranslationModelState>().apply {
        val list = TranslateLanguage.getAllLanguages().map {
            TranslationModelState(
                language = it,
                available = false,
                downloading = false,
                downloadingFailed = false
            )
        }
        addAll(list)
    }

    init {
        coroutineScope.launch {
            val downloaded = loadDownloadedModelsList().associateBy { it.language }
            models.replaceAll {
                it.copy(available = downloaded.containsKey(it.language))
            }
        }
    }

    private suspend fun loadDownloadedModelsList() = withContext(Dispatchers.IO) {
        RemoteModelManager
            .getInstance()
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .await()
    }


    override suspend fun hasModelDownloaded(language: String) = withContext(Dispatchers.IO) {
        val model = TranslateRemoteModel.Builder(language).build()
        val isDownloaded = RemoteModelManager
            .getInstance()
            .isModelDownloaded(model)
            .await()
        if (isDownloaded) TranslationModelState(
            available = isDownloaded,
            downloading = false,
            language = language,
            downloadingFailed = false
        ) else null
    }

    override fun getTranslator(
        source: String,
        target: String
    ): TranslatorState {
        val option = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()

        val translator = Translation.getClient(option)

        return TranslatorState(
            source = source,
            target = target,
            translate = { input -> translator.translate(input).await() },
        )
    }

    override fun downloadModel(language: String) = coroutineScope.launch {
        val index = models.indexOfFirst { it.language == language }
        if (index == -1 || models[index].available) return@launch

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
                    available = true
                )
            }
            .addOnFailureListener {
                models[index] = models[index].copy(
                    downloadingFailed = true,
                    downloading = false
                )
            }
    }

    override fun removeModel(language: String) = coroutineScope.launch {

        // English can't be removed.
        if (language == "en") return@launch

        val index = models.indexOfFirst { it.language == language }
        if (index == -1 || !models[index].available)
            return@launch

        RemoteModelManager
            .getInstance()
            .deleteDownloadedModel(TranslateRemoteModel.Builder(language).build())
            .addOnSuccessListener {
                models[index] = models[index].copy(
                    downloadingFailed = false,
                    downloading = false,
                    available = false
                )
            }
    }
}