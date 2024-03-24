package my.noveldokusha.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.features.chaptersList.ChaptersActivity
import my.noveldokusha.features.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.features.databaseSearch.DatabaseSearchActivity
import my.noveldokusha.features.databaseSearch.DatabaseSearchExtras
import my.noveldokusha.features.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.features.reader.ReaderActivity
import my.noveldokusha.features.sourceCatalog.SourceCatalogActivity
import my.noveldokusha.features.webView.WebViewActivity
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SourceInterface

fun Context.goToSourceCatalog(source: SourceInterface.Catalog) {
    SourceCatalogActivity
        .IntentData(this, sourceBaseUrl = source.baseUrl)
        .let(::startActivity)
}

fun Context.goToDatabaseSearch(database: DatabaseInterface) {
    DatabaseSearchActivity
        .IntentData(this, DatabaseSearchExtras.Catalog(databaseBaseUrl = database.baseUrl))
        .let(::startActivity)
}

fun Context.goToGlobalSearch(text: String) {
    GlobalSourceSearchActivity
        .IntentData(this, text)
        .let(::startActivity)
}

fun Context.goToReader(bookUrl: String, chapterUrl: String) {
    ReaderActivity
        .IntentData(this, bookUrl = bookUrl, chapterUrl = chapterUrl)
        .let(::startActivity)
}

fun Context.goToWebBrowser(url: String) {
    Intent(Intent.ACTION_VIEW)
        .also { it.data = Uri.parse(url) }
        .let(::startActivity)
}

fun Context.goToDatabaseSearch(
    input: String,
    databaseUrlBase: String = "https://www.novelupdates.com/"
) {
    DatabaseSearchActivity
        .IntentData(
            this,
            DatabaseSearchExtras.Title(databaseBaseUrl = databaseUrlBase, title = input)
        )
        .let(::startActivity)
}

fun Context.goToDatabaseSearchGenres(
    includedGenresIds: List<String>,
    databaseUrlBase: String
) {
    DatabaseSearchActivity
        .IntentData(
            this,
            DatabaseSearchExtras.Genres(
                databaseBaseUrl = databaseUrlBase,
                includedGenresIds = includedGenresIds,
                excludedGenresIds = listOf()
            )
        )
        .let(::startActivity)
}

fun Context.goToDatabaseBookInfo(bookMetadata: BookMetadata, databaseUrlBase: String) {
    DatabaseBookInfoActivity
        .IntentData(this, databaseUrlBase = databaseUrlBase, bookMetadata = bookMetadata)
        .let(::startActivity)
}

fun Context.goToBookChapters(bookMetadata: BookMetadata) {
    ChaptersActivity
        .IntentData(this, bookMetadata = bookMetadata)
        .let(::startActivity)
}

fun Context.goToWebViewWithUrl(url: String) {
    WebViewActivity.IntentData(this, url = url).let(::startActivity)
}

