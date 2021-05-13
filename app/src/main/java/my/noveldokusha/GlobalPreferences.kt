package my.noveldokusha

import android.content.Context
import android.content.SharedPreferences
import kotlin.reflect.KProperty

val globalThemeList = mapOf(
	"light" to R.style.AppTheme_Light,
	"dark" to R.style.AppTheme_BaseDark_Dark,
	"grey" to R.style.AppTheme_BaseDark_Grey,
	"black" to R.style.AppTheme_BaseDark_Black
)

var SharedPreferences.THEME_ID by PreferenceDelegate_Int(R.style.AppTheme_Light)
var SharedPreferences.READER_FONT_SIZE by PreferenceDelegate_Float(14f)
var SharedPreferences.READER_FONT_FAMILY by PreferenceDelegate_String("sans-serif")


fun Context.appSharedPreferences(): SharedPreferences = getSharedPreferences("${this.packageName}_preferences", Context.MODE_PRIVATE)

class PreferenceDelegate_Int(val defaultValue: Int)
{
	operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getInt(property.name, defaultValue)
	operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Int) =
		thisRef.edit().putInt(property.name, value).apply()
}

class PreferenceDelegate_Float(val defaultValue: Float)
{
	operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getFloat(property.name, defaultValue)
	operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Float) =
		thisRef.edit().putFloat(property.name, value).apply()
}

class PreferenceDelegate_String(val defaultValue: String)
{
	operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getString(property.name, null) ?: defaultValue
	operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: String?) =
		thisRef.edit().putString(property.name, value).apply()
}

