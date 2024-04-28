package my.noveldokusha.core

data class PagedList<T>(
    val list: List<T>,
    val index: Int,
    val isLastPage: Boolean
) {
    val hasNoNextPage = list.isEmpty() || isLastPage

    companion object {
        fun <T> createEmpty(index: Int) =
            PagedList<T>(list = listOf(), index = index, isLastPage = true)
    }
}