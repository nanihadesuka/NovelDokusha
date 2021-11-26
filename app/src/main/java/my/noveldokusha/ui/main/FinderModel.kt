package my.noveldokusha.ui.main

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.asLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import my.noveldokusha.SOURCES_LANGUAGES_flow
import my.noveldokusha.appSharedPreferences
import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class FinderModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences : SharedPreferences
) : BaseViewModel()
{
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