package my.noveldokusha.tooling.epub_parser

data class EpubFile(
    val absPath: String,
    val data: ByteArray,
)

data class EpubBook(
    val fileName: String,
    val title: String,
    val author: String?,
    val description: String?,
    val coverImage: Image?,
    val chapters: List<Chapter>,
    val images: List<Image>,
    val toc: List<ToCEntry> = emptyList()
) {
    data class Chapter(
        val absPath: String,
        val title: String,
        val body: String
    )

    data class Image(
        val absPath: String,
        val image: ByteArray
    )

    data class ToCEntry(
        val chapterTitle: String,
        val chapterLink: String
    )
}
data class ManifestItem(
    val id: String,
    val absPath: String,
    val mediaType: String,
    val properties: String
)


