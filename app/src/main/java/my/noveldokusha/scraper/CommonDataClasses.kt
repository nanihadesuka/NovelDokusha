package my.noveldokusha.scraper

data class ChapterDownload(val body: String, val title: String?)

sealed class Response<T>
{
    class Success<T>(val data: T) : Response<T>()
    class Error<T>(val message: String) : Response<T>()
}