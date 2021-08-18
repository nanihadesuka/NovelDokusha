package my.noveldokusha.ui.main

import my.noveldokusha.scraper.scrubber
import my.noveldokusha.ui.BaseViewModel

class FinderModel : BaseViewModel()
{
	val sourcesList = arrayListOf<Item>(
		Item.Header("Databases"),
		*scrubber.databasesList.map { Item.Database(it.name, it.baseUrl) }.toTypedArray(),
		Item.Header("Sources"),
		*scrubber.sourcesListCatalog.map { Item.Source(it.name, it.baseUrl) }.toTypedArray()
	)
}