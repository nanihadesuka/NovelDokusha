package my.noveldokusha.ui

import android.content.Context
import my.noveldoksuha.databaseexplorer.databaseSearch.DatabaseSearchActivity
import my.noveldoksuha.databaseexplorer.databaseSearch.DatabaseSearchExtras
import my.noveldokusha.globalsourcesearch.GlobalSourceSearchActivity
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.sourceexplorer.SourceCatalogActivity

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
