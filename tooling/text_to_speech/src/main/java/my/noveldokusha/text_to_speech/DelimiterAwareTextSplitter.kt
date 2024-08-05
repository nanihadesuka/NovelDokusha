package my.noveldokusha.text_to_speech

import java.nio.CharBuffer

/**
 * Returns the text chunked in max sizes of maxSliceLength and tries to
 * preserve contiguity of text by slicing to the nearest charDelimiter
 */
internal fun delimiterAwareTextSplitter(
    fullText: String,
    maxSliceLength: Int,
    charDelimiter: Char = '.',
): List<String> = buildList {
    if (fullText.length < maxSliceLength) {
        add(fullText)
        return@buildList
    }

    /**
     * Avoid string slice/substring as they are very expensive.
     * Use views of the underlying string with CharBuffer (avoids copies)
     */
    var remainingText = CharBuffer.wrap(fullText)

    while (true) {
        if (remainingText.length < maxSliceLength) {
            add(remainingText.toString())
            break
        }

        val index = remainingText.lastIndexOf(charDelimiter, maxSliceLength)
        if (index != -1) {
            val slice = remainingText.softEndSlice(index)
            add(slice.toString())
            remainingText = remainingText.softStartSlice(index)
        } else {
            val slice = remainingText.softEndSlice(maxSliceLength - 1)
            add(slice.toString())
            remainingText = remainingText.softStartSlice(maxSliceLength - 1)
        }
    }
}

private fun CharBuffer.softEndSlice(endIndex: Int): CharBuffer {
    val end = (endIndex + 1).coerceAtMost(length)
    return subSequence(0, end)
}

private fun CharBuffer.softStartSlice(startIndex: Int): CharBuffer {
    val start = (startIndex + 1).coerceAtMost(length)
    return subSequence(start, length)
}
