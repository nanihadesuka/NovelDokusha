package my.noveldokusha.navigation

import android.content.Context
import android.content.Intent
import my.noveldokusha.tooling.local_database.BookMetadata

interface NavigationRoutes {

    fun main(context: Context): Intent

    fun chapters(context: Context, bookMetadata: BookMetadata): Intent

    fun webView(context: Context, url: String): Intent
}