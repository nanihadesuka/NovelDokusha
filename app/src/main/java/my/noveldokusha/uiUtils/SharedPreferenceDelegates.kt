package my.noveldokusha.uiUtils

import android.content.SharedPreferences
import kotlin.reflect.KProperty

class SharedPreference_Enum<T : Enum<T>>(val name: String, val sharedPreferences: SharedPreferences, val defaultValue: T, val deserializer: (String) -> T)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T =
        sharedPreferences.getString(name, null)?.let { kotlin.runCatching { deserializer(it) }.getOrNull() } ?: defaultValue

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        sharedPreferences.edit().putString(name, value.name).apply()
}

class SharedPreference_Int(val name: String, val sharedPreferences: SharedPreferences, val defaultValue: Int)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getInt(name, defaultValue)
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) =
        sharedPreferences.edit().putInt(name, value).apply()
}

class SharedPreference_Float(val name: String, val sharedPreferences: SharedPreferences, val defaultValue: Float)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getFloat(name, defaultValue)
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) =
        sharedPreferences.edit().putFloat(name, value).apply()
}

class SharedPreference_String(val name: String, val sharedPreferences: SharedPreferences, val defaultValue: String)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getString(name, null) ?: defaultValue
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) =
        sharedPreferences.edit().putString(name, value).apply()
}

class SharedPreference_StringSet(val name: String, val sharedPreferences: SharedPreferences, val defaultValue: Set<String>)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getStringSet(name, null)?.toSet() ?: defaultValue
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) =
        sharedPreferences.edit().putStringSet(name, value).apply()
}

class SharedPreference_Boolean(val name: String, val sharedPreferences: SharedPreferences, val defaultValue: Boolean)
{
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = sharedPreferences.getBoolean(name, defaultValue)
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        sharedPreferences.edit().putBoolean(name, value).apply()
}