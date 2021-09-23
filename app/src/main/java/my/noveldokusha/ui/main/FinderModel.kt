package my.noveldokusha.ui.main

import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import my.noveldokusha.App
import my.noveldokusha.SOURCES_LANGUAGES_flow
import my.noveldokusha.appSharedPreferences
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class FinderModel : BaseViewModel()
{

    private val preferences = App.instance.appSharedPreferences()

    val list = preferences.SOURCES_LANGUAGES_flow().map { activeLangs ->
        val catalogSources = scrubber.sourcesListCatalog.filter { it.language in activeLangs }.map { Item.Source(it.name, it.baseUrl) }

        return@map listOf<Item>(
            Item.Header("Databases"),
            *scrubber.databasesList.map { Item.Database(it.name, it.baseUrl) }.toTypedArray(),
            Item.Header("Sources"),
            *catalogSources.toTypedArray()
        )
    }.asLiveData()
}