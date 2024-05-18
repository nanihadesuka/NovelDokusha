package my.noveldokusha.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import my.noveldoksuha.databaseexplorer.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldoksuha.databaseexplorer.databaseSearch.DatabaseSearchActivity
import my.noveldoksuha.databaseexplorer.databaseSearch.DatabaseSearchExtras
import my.noveldokusha.features.chapterslist.ChaptersActivity
import my.noveldokusha.features.reader.ReaderActivity
import my.noveldokusha.features.webView.WebViewActivity
import my.noveldokusha.globalsourcesearch.GlobalSourceSearchActivity
import my.noveldokusha.mappers.mapToBookMetadata
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.domain.BookResult
import my.noveldokusha.sourceexplorer.SourceCatalogActivity
import my.noveldokusha.tooling.local_database.BookMetadata

fun Context.goToSourceCatalog(source: SourceInterface.Catalog) {
    SourceCatalogActivity
        .IntentData(this, sourceBaseUrl = source.baseUrl)
        .let(::startActivity)
}

fun Context.goToDatabaseSearch(database: my.noveldokusha.scraper.DatabaseInterface) {
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


fun Context.goToBookChapters(bookResult: BookResult) =
    goToBookChapters(bookMetadata = bookResult.mapToBookMetadata())

fun Context.goToBookChapters(bookMetadata: BookMetadata) {
    ChaptersActivity
        .IntentData(this, bookMetadata = bookMetadata)
        .let(::startActivity)
}

fun Context.goToWebViewWithUrl(url: String) {
    WebViewActivity.IntentData(this, url = url).let(::startActivity)
}

