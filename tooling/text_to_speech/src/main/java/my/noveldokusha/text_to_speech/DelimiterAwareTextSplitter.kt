package my.noveldokusha.text_to_speech

/**
 * Returns the text chunked in max sizes of maxSliceLength and tries to
 * preserve contiguity of text by slicing to the nearest charDelimiter
 */
internal fun delimiterWareTextSplitter(
    fullText: String,
    maxSliceLength: Int,
    charDelimiter: Char = '.',
): List<String> = buildList {
    var remainingText: String = fullText
    while (true) {
        if (remainingText.length < maxSliceLength) {
            add(remainingText)
            break
        }

        val index = remainingText.lastIndexOf(charDelimiter, maxSliceLength)
        if (index != -1) {
            val slice = remainingText.softEndSlice(index)
            add(slice)
            remainingText = remainingText.softStartSlice(index)
        } else {
            val slice = remainingText.softEndSlice(maxSliceLength - 1)
            add(slice)
            remainingText = remainingText.softStartSlice(maxSliceLength - 1)
        }
    }
}

private fun String.softEndSlice(endIndex: Int): String {
    return this.slice((0 until endIndex + 1) intersect indices)
}

private fun String.softStartSlice(startIndex: Int): String {
    return this.slice((startIndex + 1 until length) intersect indices)
}