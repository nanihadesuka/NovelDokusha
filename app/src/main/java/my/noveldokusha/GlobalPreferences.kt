package my.noveldokusha

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlin.reflect.KProperty

object globalThemeList
{
	val light = mapOf(
		R.style.AppTheme_Light to "Light",
	)
	val dark = mapOf(
		R.style.AppTheme_BaseDark_Dark to "Dark",
		R.style.AppTheme_BaseDark_Grey to "Grey",
		R.style.AppTheme_BaseDark_Black to "Black"
	)
}

var SharedPreferences.THEME_ID by PreferenceDelegate_Int(R.style.AppTheme_Light)
var SharedPreferences.THEME_FOLLOW_SYSTEM by PreferenceDelegate_Boolean(true)
var SharedPreferences.READER_FONT_SIZE by PreferenceDelegate_Float(14f)
var SharedPreferences.READER_FONT_FAMILY by PreferenceDelegate_String("sans-serif")
var SharedPreferences.CHAPTERS_SORT_ASCENDING by PreferenceDelegate_Enum(TERNARY_STATE.active) { enumValueOf(it) }

fun SharedPreferences.CHAPTERS_SORT_ASCENDING_flow() = toFlow(::CHAPTERS_SORT_ASCENDING.name) { CHAPTERS_SORT_ASCENDING }

enum class TERNARY_STATE
{ active, inverse, inactive }

fun Context.appSharedPreferences(): SharedPreferences =
	applicationContext.getSharedPreferences("${this.packageName}_preferences", Context.MODE_PRIVATE)

fun <T> SharedPreferences.toFlow(key: String, mapper: (String) -> T): Flow<T>
{
	val flow = MutableStateFlow(mapper(key))
	val scope = CoroutineScope(Dispatchers.Default)
	val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, vkey ->
		if (key == vkey)
			scope.launch { flow.value = mapper(vkey) }
	}
	App.instance.preferencesChangeListeners.add(listener)
	registerOnSharedPreferenceChangeListener(listener)
	return flow.onCompletion {
		App.instance.preferencesChangeListeners.remove(listener)
		unregisterOnSharedPreferenceChangeListener(listener)
	}.flowOn(Dispatchers.Default)
}

class PreferenceDelegate_Enum<T : Enum<T>>(val defaultValue: T, val deserializer: (String) -> T)
{
	operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>): T =
		thisRef.getString(property.name, null)?.let { kotlin.runCatching { deserializer(it) }.getOrNull() } ?: defaultValue
	
	operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: T) =
		thisRef.edit().putString(property.name, value.name).apply()
}

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

class PreferenceDelegate_Boolean(val defaultValue: Boolean)
{
	operator fun getValue(thisRef: SharedPreferences, property: KProperty<*>) = thisRef.getBoolean(property.name, defaultValue)
	operator fun setValue(thisRef: SharedPreferences, property: KProperty<*>, value: Boolean) =
		thisRef.edit().putBoolean(property.name, value).apply()
}
