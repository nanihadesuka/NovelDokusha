package my.noveldokusha.foss

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import my.noveldokusha.di.AppCoroutineScope
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.tools.TranslationManager
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FossModule {

    @Provides
    @Singleton
    fun provideTranslationManager(coroutineScope: AppCoroutineScope): TranslationManager {
        return TranslationManagerEmpty(coroutineScope)
    }
}