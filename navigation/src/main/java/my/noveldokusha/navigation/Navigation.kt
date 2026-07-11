package my.noveldokusha.navigation

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import my.noveldokusha.feature.local_database.BookMetadata
import javax.inject.Inject

interface NavigationRoutes {

    fun main(context: Context): Intent

    fun chapters(context: Context, bookMetadata: BookMetadata): Intent

    fun webView(context: Context, url: String): Intent

    fun reader(
        context: Context,
        bookUrl: String,
        chapterUrl: String,
        scrollToSpeakingItem: Boolean = false
    ): Intent

    fun databaseSearch(
        context: Context,
        input: String,
        databaseUrlBase: String = "https://www.novelupdates.com/"
    ): Intent

    fun databaseSearch(
        context: Context,
        databaseBaseUrl: String
    ): Intent

    fun globalSearch(context: Context, text: String): Intent
    fun sourceCatalog(context: Context, sourceBaseUrl: String): Intent
}

@HiltViewModel
class NavigationRouteViewModel @Inject constructor(
    private val appNavigationRoutes: NavigationRoutes
) : NavigationRoutes by appNavigationRoutes, ViewModel()