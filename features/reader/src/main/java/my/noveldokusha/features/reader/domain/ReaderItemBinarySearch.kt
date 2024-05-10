package my.noveldokusha.features.reader.domain

internal fun indexOfReaderItem(
    list: List<ReaderItem>,
    chapterIndex: Int,
    chapterItemPosition: Int
): Int = when {
    list.size < 128 -> indexOfReaderItemLinearSearch(
        list = list,
        chapterIndex = chapterIndex,
        chapterItemPosition = chapterItemPosition
    )
    else -> indexOfReaderItemBinarySearch(
        list = list,
        chapterIndex = chapterIndex,
        chapterItemPosition = chapterItemPosition
    )
}

/**
 * O(n) search
 */
internal fun indexOfReaderItemLinearSearch(
    list: List<ReaderItem>,
    chapterIndex: Int,
    chapterItemPosition: Int
) = list.indexOfFirst {
    it.chapterIndex == chapterIndex &&
            it is ReaderItem.Position &&
            it.chapterItemPosition == chapterItemPosition
}

/**
 * O(log2(n)) search
 */
internal fun indexOfReaderItemBinarySearch(
    list: List<ReaderItem>,
    chapterIndex: Int,
    chapterItemPosition: Int
): Int {
    var low = 0
    var high = list.lastIndex

    while (low < high) {
        var mid = low + (high - low) / 2
        val it = list[mid]
        val compare = when (val compareChapter = it.chapterIndex.compareTo(chapterIndex)) {
            0 -> when (it) {
                is ReaderItem.Position -> it.chapterItemPosition.compareTo(chapterItemPosition)
                else -> {
                    // We don't have a position, try to find one and make that the mid point
                    val index = list.subList(mid, high)
                        .asSequence()
                        .takeWhile { it.chapterIndex == chapterIndex }
                        .indexOfFirst { it is ReaderItem.Position }

                    if (index == -1) {
                        +1
                    } else {
                        mid += index
                        val item = list[mid] as ReaderItem.Position
                        item.chapterItemPosition.compareTo(chapterItemPosition)
                    }
                }
            }
            else -> compareChapter
        }
        if (compare < 0) {
            low = mid + 1
        } else if (compare > 0) {
            high = mid
        } else {
            return mid
        }
    }
//    return -(low + 1)
    return -1
}