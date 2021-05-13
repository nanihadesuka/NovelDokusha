package my.noveldokusha

import android.content.Context
import android.content.SharedPreferences

enum class AppPreferences
{
	THEME_ID, // Int
	READER_FONT_SIZE, // Float
	READER_FONT_FAMILY, // String
}

val globalThemeList = mapOf(
	"light" to R.style.AppTheme_Light,
	"dark" to R.style.AppTheme_BaseDark_Dark,
	"grey" to R.style.AppTheme_BaseDark_Grey,
	"black" to R.style.AppTheme_BaseDark_Black
)

fun SharedPreferences.getAppThemeId(): Int = this.get(AppPreferences.THEME_ID, R.style.AppTheme_Light)
fun SharedPreferences.Editor.setAppThemeId(value: Int) = this.set(AppPreferences.THEME_ID, value)

fun SharedPreferences.getReaderFontSize(): Float = this.get(AppPreferences.READER_FONT_SIZE, 14f)
fun SharedPreferences.Editor.setReaderFontSize(value: Float) = this.set(AppPreferences.READER_FONT_SIZE, value)

fun SharedPreferences.getReaderFontFamily(): String = this.get(AppPreferences.READER_FONT_FAMILY, "sans-serif")
fun SharedPreferences.Editor.setReaderFontFamily(value: String) = this.set(AppPreferences.READER_FONT_FAMILY, value)


fun SharedPreferences.get(enum: AppPreferences, default: Int): Int = getInt(enum.name, default)
fun SharedPreferences.get(enum: AppPreferences, default: Float): Float = getFloat(enum.name, default)
fun SharedPreferences.get(enum: AppPreferences, default: String): String = getString(enum.name, default) ?: default

fun SharedPreferences.Editor.set(enum: AppPreferences, value: Int): SharedPreferences.Editor = putInt(enum.name, value)
fun SharedPreferences.Editor.set(enum: AppPreferences, value: Float): SharedPreferences.Editor = putFloat(enum.name, value)
fun SharedPreferences.Editor.set(enum: AppPreferences, value: String): SharedPreferences.Editor = putString(enum.name, value)


fun Context.appSharedPreferences(): SharedPreferences = getSharedPreferences("${this.packageName}_preferences", Context.MODE_PRIVATE)