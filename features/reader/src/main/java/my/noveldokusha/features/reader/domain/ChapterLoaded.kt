package my.noveldokusha.features.reader.domain

internal data class ChapterLoaded(val chapterIndex: Int, val type: Type) {
    enum class Type { Previous, Next, Initial }
}
