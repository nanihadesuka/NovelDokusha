package my.noveldokusha.text_to_speech

import org.junit.Assert.assertEquals
import org.junit.Test

class DelimiterAwareTextSplitterTest {

    @Test
    fun `no delimiter in text`() {
        val text = "hello how are you, fine you ?"
        val result = delimiterAwareTextSplitter(
            fullText = text,
            maxSliceLength = 40,
            charDelimiter = '.'
        )
        assertEquals(
            listOf(
                "hello how are you, fine you ?",
            ), result
        )
        assertEquals(text, result.joinToString(""))
    }

    @Test
    fun `with delimiter in text`() {
        val text = "hello how are you. fine you ?"
        val result = delimiterAwareTextSplitter(
            fullText = text,
            maxSliceLength = 40,
            charDelimiter = '.'
        )
        assertEquals(
            listOf(
                "hello how are you. fine you ?",
            ), result
        )
        assertEquals(text, result.joinToString(""))
    }

    @Test
    fun `with delimiter and sliced`() {
        val text = "hello how are you. fine you.. Mind taking a moment?."
        val result = delimiterAwareTextSplitter(
            fullText = text,
            maxSliceLength = 5,
            charDelimiter = '.'
        )
        assertEquals(
            listOf(
                "hello",
                " how ",
                "are y",
                "ou.",
                " fine",
                " you..",
                " Mind",
                " taki",
                "ng a ",
                "momen",
                "t?.",
            ), result
        )
        assertEquals(text, result.joinToString(""))
    }

}