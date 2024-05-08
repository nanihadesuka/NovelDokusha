package my.noveldokusha.tooling.text_translator

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import my.noveldokusha.core.AppCoroutineScope
import my.noveldokusha.text_translator.domain.TranslationManager
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FullModule {

    @Provides
    @Singleton
    fun provideTranslationManager(coroutineScope: AppCoroutineScope): TranslationManager {
        return TranslationManagerMLKit(coroutineScope)
    }
}