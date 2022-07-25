package my.noveldokusha.tools

import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.Job
import java.util.*

data class TranslationModelState(
    val language: String,
    val available: Boolean,
    val downloading: Boolean,
    val downloadingFailed: Boolean,
) {
    val locale = Locale(language)
}

data class TranslatorState(
    val source: String,
    var target: String,
    val translate: suspend (input: String) -> String,
) {
    val sourceLocale = Locale(source)
    val targetLocale = Locale(target)
}

interface TranslationManager {

    val available: Boolean

    val models: SnapshotStateList<TranslationModelState>

    // TODO Should not use blocking code
    fun hasModelDownloadedSync(language: String): TranslationModelState?

    /**
     * Doesn't check if the model has been downloaded. Must be externally guaranteed.
     * @param source language locale
     * @param target language locale
     */
    fun getTranslator(source: String, target: String): TranslatorState

    fun downloadModel(language: String): Job

    fun removeModel(language: String): Job
}