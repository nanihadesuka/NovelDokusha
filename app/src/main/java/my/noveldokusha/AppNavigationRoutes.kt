package my.noveldokusha

import android.content.Context
import android.content.Intent
import my.noveldokusha.features.chaptersList.ChaptersActivity
import my.noveldokusha.features.main.MainActivity
import my.noveldokusha.features.webView.WebViewActivity
import my.noveldokusha.navigation.NavigationRoutes
import my.noveldokusha.tooling.local_database.BookMetadata
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class AppNavigationRoutes @Inject constructor() : NavigationRoutes {

    override fun main(context: Context): Intent {
        return Intent(context, MainActivity::class.java)
    }

    override fun chapters(context: Context, bookMetadata: BookMetadata): Intent {
        return ChaptersActivity.IntentData(context, bookMetadata)
    }

    override fun webView(context: Context, url: String): Intent {
        return WebViewActivity.IntentData(context, url = url)
    }

}