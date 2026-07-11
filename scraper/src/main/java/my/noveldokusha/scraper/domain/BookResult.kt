package my.noveldokusha.scraper.domain

data class BookResult(
    val title: String,
    val url: String,
    val coverImageUrl: String = "",
    val description: String = "",
)


