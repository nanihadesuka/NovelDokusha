package my.noveldokusha.scraper

data class ChapterDownload(val body: String, val title: String?)

sealed class Response<T> {
    class Success<T>(val data: T) : Response<T>()
    class Error<T>(val message: String) : Response<T>()

    fun toSuccessOrNull(): Success<T>? = when (this) {
        is Error -> null
        is Success -> this
    }
}

data class PagedList<T>(
    val list: List<T>,
    val index: Int,
    private val isLastPage: Boolean
) {
    val hasNoNextPage = list.isEmpty() || isLastPage

    companion object {
        fun <T> createEmpty(index: Int) =
            PagedList<T>(list = listOf(), index = index, isLastPage = true)
    }
}

enum class IteratorState { IDLE, LOADING, CONSUMED }