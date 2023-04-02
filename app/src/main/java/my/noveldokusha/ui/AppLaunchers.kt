package my.noveldokusha.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import my.noveldokusha.data.BookMetadata
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.screens.chaptersList.ChaptersActivity
import my.noveldokusha.ui.screens.databaseBookInfo.DatabaseBookInfoActivity
import my.noveldokusha.ui.screens.databaseSearch.DatabaseSearchActivity
import my.noveldokusha.ui.screens.globalSourceSearch.GlobalSourceSearchActivity
import my.noveldokusha.ui.screens.reader.ReaderActivity
import my.noveldokusha.ui.screens.sourceCatalog.SourceCatalogActivity

fun Context.goToSourceCatalog(source: SourceInterface.Catalog) {
    SourceCatalogActivity
        .IntentData(this, sourceBaseUrl = source.baseUrl)
        .let(::startActivity)
}

fun Context.goToDatabaseSearch(database: DatabaseInterface) {
    DatabaseSearchActivity
        .IntentData(this, databaseBaseUrl = database.baseUrl)
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

fun Context.goToDatabaseSearchResults(
    inputText: String,
    databaseUrlBase: String = "https://www.novelupdates.com/"
) {
    DatabaseSearchActivity
        .IntentData(this, databaseBaseUrl = databaseUrlBase)
        .let(::startActivity)
}

fun Context.goToDatabaseBookInfo(book: BookMetadata, databaseUrlBase: String) {
    DatabaseBookInfoActivity
        .IntentData(this, databaseUrlBase = databaseUrlBase, bookMetadata = book)
        .let(::startActivity)
}

fun Context.goToBookChapters(book: BookMetadata) {
    ChaptersActivity
        .IntentData(this, bookMetadata = BookMetadata(title = book.title, url = book.url))
        .let(::startActivity)
}