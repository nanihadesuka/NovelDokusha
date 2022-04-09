package my.noveldokusha.ui.main

import android.content.Context
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import my.noveldokusha.AppPreferences
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class FinderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appPreferences: AppPreferences
) : BaseViewModel()
{
    val list = appPreferences.SOURCES_LANGUAGES.flow().map { activeLangs ->
        val catalogSources = scraper.sourcesListCatalog.filter { it.language in activeLangs }.map { Item.Source(it.name, it.baseUrl) }

        return@map listOf<Item>(
            Item.Header("Databases"),
            *scraper.databasesList.map { Item.Database(it.name, it.baseUrl) }.toTypedArray(),
            Item.Header("Sources"),
            *catalogSources.toTypedArray()
        )
    }.asLiveData()
}