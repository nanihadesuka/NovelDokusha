package my.noveldoksuha.coreui.theme

import android.content.Context
import android.graphics.Color
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

@ColorInt
fun @receiver:AttrRes Int.colorAttrRes(ctx: Context): Int =
    ctx.theme.obtainStyledAttributes(intArrayOf(this)).use {
        it.getColor(0, Color.MAGENTA)
    }
