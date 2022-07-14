package my.noveldokusha.tools

import androidx.compose.runtime.mutableStateListOf
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

data class TranslationModelState(
    val language: String,
    val model: TranslateRemoteModel?,
    val downloading: Boolean,
    val downloadingFailed: Boolean
) {
    val locale = Locale(language)
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

    init {
        coroutineScope.launch {
            val downloaded = loadDownloadedModelsList().associateBy { it.language }
            models.replaceAll {
                it.copy(model = downloaded[it.language])
            }
        }
    }

    private suspend fun loadDownloadedModelsList() = withContext(Dispatchers.IO) {
        RemoteModelManager
            .getInstance()
            .getDownloadedModels(TranslateRemoteModel::class.java)
            .await()
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
            }
            .addOnFailureListener {
                models[index] = models[index].copy(
                    downloadingFailed = true,
                    downloading = false
                )
            }
    }

    fun removeModel(language: String) = coroutineScope.launch {
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
            }
    }
}