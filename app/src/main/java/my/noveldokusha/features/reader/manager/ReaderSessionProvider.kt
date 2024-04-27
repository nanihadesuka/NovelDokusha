package my.noveldokusha.features.reader.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import my.noveldokusha.AppPreferences
import my.noveldokusha.features.reader.features.ReaderViewHandlersActions
import my.noveldokusha.repository.AppRepository
import my.noveldokusha.repository.ReaderRepository
import my.noveldokusha.tools.TranslationManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReaderSessionProvider @Inject constructor(
    private val appRepository: AppRepository,
    private val appPreferences: AppPreferences,
    @ApplicationContext private val context: Context,
    private val translationManager: TranslationManager,
    private val readerRepository: ReaderRepository,
    private val readerViewHandlersActions: ReaderViewHandlersActions,
) {
    fun create(
        bookUrl: String,
        initialChapterUrl: String,
    ): ReaderSession = ReaderSession(
        bookUrl = bookUrl,
        initialChapterUrl = initialChapterUrl,
        appRepository = appRepository,
        translationManager = translationManager,
        appPreferences = appPreferences,
        context = context,
        readerRepository = readerRepository,
        readerViewHandlersActions = readerViewHandlersActions,
    )
}
