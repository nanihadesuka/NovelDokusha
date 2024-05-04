package my.noveldokusha.epub_parser

data class EpubFile(
    val absPath: String,
    val data: ByteArray,
)

data class EpubBook(
    val coverImage: Image?,
    val chapters: List<Chapter>,
    val images: List<Image>
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
}
