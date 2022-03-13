package my.noveldokusha.ui.reader

import android.graphics.Typeface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import my.noveldokusha.R

class FontsLoader {
    companion object {
        val availableFonts = listOf(
            "casual",
            "cursive",
            "monospace",
            "sans-serif",
            "sans-serif-black",
            "sans-serif-condensed",
            "sans-serif-condensed-light",
            "sans-serif-light",
            "sans-serif-medium",
            "sans-serif-smallcaps",
            "sans-serif-thin",
            "serif",
            "serif-monospace"
        )

    }

    private val fontFamilyNORMALCache = mutableMapOf<String, FontFamily>()

    fun getFontFamilyNORMAL(name: String) = fontFamilyNORMALCache.getOrPut(name) {
        FontFamily(Typeface.create(name, Typeface.NORMAL))
    }
}