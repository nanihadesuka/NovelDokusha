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
        ReaderItem.BookEnd(chapterIndex = -1),

        ReaderItem.Divider(chapterIndex = 0),
        ReaderItem.Title(chapterUrl = "", chapterIndex = 0, chapterItemPosition = 0, text = ""),
        ReaderItem.GoogleTranslateAttribution(chapterIndex = 0),
        ReaderItem.Error(chapterIndex = 0, text = ""),
        ReaderItem.Progressbar(chapterIndex = 0),
        ReaderItem.Padding(chapterIndex = 0),

        ReaderItem.Divider(chapterIndex = 1),
        ReaderItem.Title(chapterUrl = "", chapterIndex = 1, chapterItemPosition = 0, text = ""),
        ReaderItem.GoogleTranslateAttribution(chapterIndex = 1),
        ReaderItem.Progressbar(chapterIndex = 1),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 1, chapterItemPosition = 1, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 1, chapterItemPosition = 2, text = "", location = ReaderItem.Location.FIRST),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 1, chapterItemPosition = 3, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Image(chapterUrl = "", chapterIndex = 1, chapterItemPosition = 4, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 1, chapterItemPosition = 5, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 1, chapterItemPosition = 6, text = "", location = ReaderItem.Location.LAST),
        ReaderItem.Image(chapterUrl = "", chapterIndex = 1, chapterItemPosition = 7, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Error(chapterIndex = 1, text = ""),
        ReaderItem.Padding(chapterIndex = 1),

        ReaderItem.Divider(chapterIndex = 2),
        ReaderItem.Title(chapterUrl = "", chapterIndex = 2, chapterItemPosition = 0, text = ""),
        ReaderItem.GoogleTranslateAttribution(chapterIndex = 2),
        ReaderItem.Progressbar(chapterIndex = 2),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 2, chapterItemPosition = 1, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 2, chapterItemPosition = 2, text = "", location = ReaderItem.Location.FIRST),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 2, chapterItemPosition = 3, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Image(chapterUrl = "", chapterIndex = 2, chapterItemPosition = 4, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 2, chapterItemPosition = 5, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 2, chapterItemPosition = 6, text = "", location = ReaderItem.Location.LAST),
        ReaderItem.Image(chapterUrl = "", chapterIndex = 2, chapterItemPosition = 7, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Error(chapterIndex = 2, text = ""),
        ReaderItem.Padding(chapterIndex = 2),

        ReaderItem.Divider(chapterIndex = 3),
        ReaderItem.Title(chapterUrl = "", chapterIndex = 3, chapterItemPosition = 0, text = ""),
        ReaderItem.GoogleTranslateAttribution(chapterIndex = 3),
        ReaderItem.Progressbar(chapterIndex = 3),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 3, chapterItemPosition = 1, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 3, chapterItemPosition = 2, text = "", location = ReaderItem.Location.FIRST),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 3, chapterItemPosition = 3, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Image(chapterUrl = "", chapterIndex = 3, chapterItemPosition = 4, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 3, chapterItemPosition = 5, text = "", location = ReaderItem.Location.MIDDLE),
        ReaderItem.Body(chapterUrl = "", chapterIndex = 3, chapterItemPosition = 6, text = "", location = ReaderItem.Location.LAST),
        ReaderItem.Image(chapterUrl = "", chapterIndex = 3, chapterItemPosition = 7, text = "", location = ReaderItem.Location.MIDDLE, image = BookTextMapper.ImgEntry("",1f)),
        ReaderItem.Error( chapterIndex = 3 , text = ""),
        ReaderItem.Padding( chapterIndex = 3),

        ReaderItem.BookEnd(chapterIndex = 4),
    )

    // Compare against the naive linear search as the gold standard, indices should return the same
    private fun assertEqualsIndices(chapterIndex: Int, chapterItemPosition: Int) {
        val indexExpected = indexOfReaderItemLinearSearch(list, chapterIndex = chapterIndex, chapterItemPosition = chapterItemPosition)
        val indexGot = indexOfReaderItemBinarySearch(list, chapterIndex = chapterIndex, chapterItemPosition = chapterItemPosition)
        assertEquals(
            "Indices not matching for chapterPos:$chapterIndex pos:$chapterItemPosition",
            indexExpected,
            indexGot
        )
    }

    @Test fun findItem0() {     assertEqualsIndices(chapterIndex = -1, chapterItemPosition = 0)   }
    @Test fun findItem1() {     assertEqualsIndices(chapterIndex = 0, chapterItemPosition = 1)    }
    @Test fun findItem2() {     assertEqualsIndices(chapterIndex = 0, chapterItemPosition = 1)    }
    @Test fun findItem3() {     assertEqualsIndices(chapterIndex = 1, chapterItemPosition = 0)    }
    @Test fun findItem4() {     assertEqualsIndices(chapterIndex = 1, chapterItemPosition = 4)    }
    @Test fun findItem5() {     assertEqualsIndices(chapterIndex = 1, chapterItemPosition = 7)    }
    @Test fun findItem6() {     assertEqualsIndices(chapterIndex = 1, chapterItemPosition = 8)    }
    @Test fun findItem7() {     assertEqualsIndices(chapterIndex = 2, chapterItemPosition = 0)    }
    @Test fun findItem8() {     assertEqualsIndices(chapterIndex = 2, chapterItemPosition = 3)    }
    @Test fun findItem9() {     assertEqualsIndices(chapterIndex = 3, chapterItemPosition = 2)    }
    @Test fun findItem10() {    assertEqualsIndices(chapterIndex = 4, chapterItemPosition = 2)    }
    @Test fun findItem11() {    assertEqualsIndices(chapterIndex = 7, chapterItemPosition = 2)    }
    @Test fun findItem12() {    assertEqualsIndices(chapterIndex = 3, chapterItemPosition = -5)   }
}