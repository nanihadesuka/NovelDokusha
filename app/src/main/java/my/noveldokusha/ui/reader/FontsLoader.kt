package my.noveldokusha.ui.reader

import android.graphics.Typeface

class FontsLoader
{
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

    private val fontFamilyNORMALCache = mutableMapOf<String, Typeface>()
    private val fontFamilyBOLDCache = mutableMapOf<String, Typeface>()

    fun getFontFamilyNORMAL(name: String) = fontFamilyNORMALCache.getOrPut(name) {
        Typeface.create(name, Typeface.NORMAL)
    }

    fun getFontFamilyBOLD(name: String) = fontFamilyBOLDCache.getOrPut(name) {
        Typeface.create(name, Typeface.BOLD)
    }
}