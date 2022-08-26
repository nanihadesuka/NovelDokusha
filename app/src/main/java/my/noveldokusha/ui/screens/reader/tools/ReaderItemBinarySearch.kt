package my.noveldokusha.ui.screens.reader.tools

import my.noveldokusha.ui.screens.reader.ReaderItem

fun indexOfReaderItem(
    list: List<ReaderItem>,
    chapterIndex: Int,
    chapterItemIndex: Int
): Int = when {
    list.size < 128 -> indexOfReaderItemLinearSearch(
        list = list,
        chapterIndex = chapterIndex,
        chapterItemIndex = chapterItemIndex
    )
    else -> indexOfReaderItemBinarySearch(
        list = list,
        chapterIndex = chapterIndex,
        chapterItemIndex = chapterItemIndex
    )
}

/**
 * O(n) search
 */
fun indexOfReaderItemLinearSearch(
    list: List<ReaderItem>,
    chapterIndex: Int,
    chapterItemIndex: Int
) = list.indexOfFirst {
    it.chapterIndex == chapterIndex &&
            it is ReaderItem.Position &&
            it.chapterItemIndex == chapterItemIndex
}

/**
 * O(log2(n)) search
 */
fun indexOfReaderItemBinarySearch(
    list: List<ReaderItem>,
    chapterIndex: Int,
    chapterItemIndex: Int
): Int {
    var low = 0
    var high = list.lastIndex

    while (low < high) {
        var mid = low + (high - low) / 2
        val it = list[mid]
        val compare = when (val compareChapter = it.chapterIndex.compareTo(chapterIndex)) {
            0 -> when (it) {
                is ReaderItem.Position -> it.chapterItemIndex.compareTo(chapterItemIndex)
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
                        item.chapterItemIndex.compareTo(chapterItemIndex)
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