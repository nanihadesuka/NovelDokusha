package my.noveldokusha.ui.main

import my.noveldokusha.scrubber
import my.noveldokusha.ui.BaseViewModel

class FinderModel : BaseViewModel()
{
	var sourcesList = arrayListOf<FinderFragment.Item>(
		FinderFragment.Item.Header("Databases"),
		*scrubber.databasesList.map { FinderFragment.Item.Database(it.name, it.baseUrl) }.toTypedArray(),
		FinderFragment.Item.Header("Sources"),
		*scrubber.sourcesListCatalog.map { FinderFragment.Item.Source(it.name, it.baseUrl) }.toTypedArray()
	)
}