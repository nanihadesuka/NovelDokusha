package my.noveldokusha.full

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import my.noveldokusha.tools.TranslationManager
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object FullModule {

    @Provides
    @Singleton
    fun provideTranslationManager(coroutineScope: CoroutineScope): TranslationManager {
        return TranslationManagerMLKit(coroutineScope)
    }
}