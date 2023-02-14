package my.noveldokusha

import my.noveldokusha.tools.BookTextMapper
import my.noveldokusha.ui.screens.reader.ReaderItem
import my.noveldokusha.ui.screens.reader.tools.indexOfReaderItemBinarySearch
import my.noveldokusha.ui.screens.reader.tools.indexOfReaderItemLinearSearch
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReaderItemBinarySearchTest {

    private val list = listOf<ReaderItem>(
        ReaderItem.BookEnd(chapterUrl = "", chapterPosition = -1),

        ReaderItem.Divider(chapterUrl = "", chapterPosition = 0),
        ReaderItem.Title(chapterUrl = "", chapterPosition = 0, chapterItemPosition = 0, text = ""),
        ReaderItem.GoogleTranslateAttribution(chapterUrl = "", chapterPosition = 0),
        ReaderItem.Error(chapterUrl = "", chapterPosition = 0, text = ""),
        ReaderItem.Progressbar(chapterUrl = "", chapterPosition = 0),
        ReaderItem.Padding(chapterUrl = "", chapterPosition = 0),

        ReaderItem.Divider(chapterUrl = "", chapterPosition = 1),
        ReaderItem.Title(chapterUrl = "", chapterPosition = 1, chapterItemPosition = 0, text = ""),
        ReaderItem.GoogleTranslateAttribution(chapterUrl = "", chapterPosition = 1),
        ReaderItem.Progressbar(chapterUrl = "", chapterPosition = 1),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 1, chapterItemPosition = 1, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 1, chapterItemPosition = 2, text = "", location = ReaderItem.Location.FIRST),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 1, chapterItemPosition = 3, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Image(chapterUrl = "", chapterPosition = 1, chapterItemPosition = 4, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 1, chapterItemPosition = 5, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 1, chapterItemPosition = 6, text = "", location = ReaderItem.Location.LAST),
        ReaderItem.Image(chapterUrl = "", chapterPosition = 1, chapterItemPosition = 7, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Error(chapterUrl = "", chapterPosition = 1, text = ""),
        ReaderItem.Padding(chapterUrl = "", chapterPosition = 1),

        ReaderItem.Divider(chapterUrl = "", chapterPosition = 2),
        ReaderItem.Title(chapterUrl = "", chapterPosition = 2, chapterItemPosition = 0, text = ""),
        ReaderItem.GoogleTranslateAttribution(chapterUrl = "", chapterPosition = 2),
        ReaderItem.Progressbar(chapterUrl = "", chapterPosition = 2),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 2, chapterItemPosition = 1, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 2, chapterItemPosition = 2, text = "", location = ReaderItem.Location.FIRST),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 2, chapterItemPosition = 3, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Image(chapterUrl = "", chapterPosition = 2, chapterItemPosition = 4, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 2, chapterItemPosition = 5, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 2, chapterItemPosition = 6, text = "", location = ReaderItem.Location.LAST),
        ReaderItem.Image(chapterUrl = "", chapterPosition = 2, chapterItemPosition = 7, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Error(chapterUrl = "", chapterPosition = 2, text = ""),
        ReaderItem.Padding(chapterUrl = "", chapterPosition = 2),

        ReaderItem.Divider(chapterUrl = "", chapterPosition = 3),
        ReaderItem.Title(chapterUrl = "", chapterPosition = 3, chapterItemPosition = 0, text = ""),
        ReaderItem.GoogleTranslateAttribution(chapterUrl = "", chapterPosition = 3),
        ReaderItem.Progressbar(chapterUrl = "", chapterPosition = 3),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 3, chapterItemPosition = 1, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 3, chapterItemPosition = 2, text = "", location = ReaderItem.Location.FIRST),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 3, chapterItemPosition = 3, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Image(chapterUrl = "", chapterPosition = 3, chapterItemPosition = 4, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 3, chapterItemPosition = 5, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterPosition = 3, chapterItemPosition = 6, text = "", location = ReaderItem.Location.LAST),
        ReaderItem.Image(chapterUrl = "", chapterPosition = 3, chapterItemPosition = 7, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Error(chapterUrl = "", chapterPosition = 3, text = ""),
        ReaderItem.Padding(chapterUrl = "", chapterPosition = 3),

        ReaderItem.BookEnd(chapterUrl = "", chapterPosition = 4),
    )

    // Compare against the naive linear search as the gold standard, indices should return the same
    private fun assertEqualsIndices(chapterIndex: Int, chapterItemIndex: Int) {
        val indexExpected = indexOfReaderItemLinearSearch(list, chapterPosition = chapterIndex, chapterItemPosition = chapterItemIndex)
        val indexGot = indexOfReaderItemBinarySearch(list, chapterPosition = chapterIndex, chapterItemPosition = chapterItemIndex)
        assertEquals(
            "Indices not matching for chapterPos:$chapterIndex pos:$chapterItemIndex",
            indexExpected,
            indexGot
        )
    }

    @Test fun findItem0() {     assertEqualsIndices(chapterIndex = -1, chapterItemIndex = 0)   }
    @Test fun findItem1() {     assertEqualsIndices(chapterIndex = 0, chapterItemIndex = 1)    }
    @Test fun findItem2() {     assertEqualsIndices(chapterIndex = 0, chapterItemIndex = 1)    }
    @Test fun findItem3() {     assertEqualsIndices(chapterIndex = 1, chapterItemIndex = 0)    }
    @Test fun findItem4() {     assertEqualsIndices(chapterIndex = 1, chapterItemIndex = 4)    }
    @Test fun findItem5() {     assertEqualsIndices(chapterIndex = 1, chapterItemIndex = 7)    }
    @Test fun findItem6() {     assertEqualsIndices(chapterIndex = 1, chapterItemIndex = 8)    }
    @Test fun findItem7() {     assertEqualsIndices(chapterIndex = 2, chapterItemIndex = 0)    }
    @Test fun findItem8() {     assertEqualsIndices(chapterIndex = 2, chapterItemIndex = 3)    }
    @Test fun findItem9() {     assertEqualsIndices(chapterIndex = 3, chapterItemIndex = 2)    }
    @Test fun findItem10() {    assertEqualsIndices(chapterIndex = 4, chapterItemIndex = 2)    }
    @Test fun findItem11() {    assertEqualsIndices(chapterIndex = 7, chapterItemIndex = 2)    }
    @Test fun findItem12() {    assertEqualsIndices(chapterIndex = 3, chapterItemIndex = -5)   }
}