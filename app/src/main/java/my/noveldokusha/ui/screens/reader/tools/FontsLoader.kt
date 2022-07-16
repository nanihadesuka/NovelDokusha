package my.noveldokusha.ui.screens.reader.tools

import android.graphics.Typeface
import androidx.compose.ui.text.font.FontFamily

class FontsLoader
{
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
    private val typeFaceNORMALCache = mutableMapOf<String, Typeface>()
    private val typeFaceBOLDCache = mutableMapOf<String, Typeface>()
    private val fontFamilyCache = mutableMapOf<String, FontFamily>()

    fun getTypeFaceNORMAL(name: String) = typeFaceNORMALCache.getOrPut(name) {
        Typeface.create(name, Typeface.NORMAL)
    }

    fun getTypeFaceBOLD(name: String) = typeFaceBOLDCache.getOrPut(name) {
        Typeface.create(name, Typeface.BOLD)
    }

    fun getFontFamily(name: String) = fontFamilyCache.getOrPut(name) {
        FontFamily(getTypeFaceNORMAL(name))
    }
}