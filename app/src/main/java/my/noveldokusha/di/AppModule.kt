package my.noveldokusha.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import my.noveldokusha.App
import my.noveldokusha.AppNavigationRoutes
import my.noveldokusha.BuildConfig
import my.noveldokusha.core.AppInternalState
import my.noveldokusha.core.Toasty
import my.noveldokusha.core.ToastyToast
import my.noveldokusha.navigation.NavigationRoutes
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindAppNavigationRoutes(nav: AppNavigationRoutes): NavigationRoutes

//    @Binds
//    @Singleton
//    abstract fun bindAppNavigationRoutesViewModel(nav: AppNavigationRoutesViewModel): NavigationRouteViewModel

    @Binds
    @Singleton
    abstract fun bindToasty(toast: ToastyToast): Toasty

    companion object {

        @Provides
        @Singleton
        fun providesApp(@ApplicationContext context: Context): App {
            return context as App
        }

        @Provides
        @Singleton
        fun providesAppInternalState(): AppInternalState = object : AppInternalState {
            override val isDebugMode = BuildConfig.DEBUG
            override val versionCode = BuildConfig.VERSION_CODE
            override val versionName = BuildConfig.VERSION_NAME
        }
    }
}