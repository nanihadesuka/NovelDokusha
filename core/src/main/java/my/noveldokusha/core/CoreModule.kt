package my.noveldokusha.core

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import my.noveldoksuha.coreui.theme.ThemeProvider
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
abstract class CoreModule {

    @Binds
    @Singleton
    abstract fun themeProviderBinder(appThemeProvider: AppThemeProvider): ThemeProvider


    companion object {

        @Provides
        @Singleton
        fun provideAppCoroutineScope(): AppCoroutineScope {
            return object : AppCoroutineScope {
                override val coroutineContext =
                    SupervisorJob() + Dispatchers.Main.immediate + CoroutineName("App")
            }
        }
    }
}