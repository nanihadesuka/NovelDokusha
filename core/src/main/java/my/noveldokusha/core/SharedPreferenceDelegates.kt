package my.noveldokusha.core

import android.content.SharedPreferences
import kotlin.reflect.KProperty

internal class SharedPreference_Serializable<T>(
    val name: String,
    val sharedPreferences: SharedPreferences,
    val defaultValue: T,
    val encode: (T) -> String,
    val decode: (String) -> T,
) {

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        sharedPreferences.edit().putString(name, encode(value)).apply()
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return sharedPreferences
            .getString(name, null)
            ?.let { kotlin.runCatching { decode(it) }.getOrNull() }
            ?: defaultValue
    }
}

internal class SharedPreference_Enum<T : Enum<T>>(
    val name: String,
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: T,
    val deserializer: (String) -> T
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = kotlin.runCatching {
        sharedPreferences.getString(name, null)?.let { deserializer(it) }
    }.getOrNull() ?: defaultValue

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        sharedPreferences.edit().putString(name, value.name).apply()
}

internal class SharedPreference_Int(
    val name: String,
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: Int
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        sharedPreferences.getInt(name, defaultValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) =
        sharedPreferences.edit().putInt(name, value).apply()
}

internal class SharedPreference_Float(
    val name: String,
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: Float
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        sharedPreferences.getFloat(name, defaultValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) =
        sharedPreferences.edit().putFloat(name, value).apply()
}

internal class SharedPreference_String(
    val name: String,
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: String
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        sharedPreferences.getString(name, null) ?: defaultValue

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) =
        sharedPreferences.edit().putString(name, value).apply()
}

internal class SharedPreference_StringSet(
    val name: String,
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: Set<String>
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        sharedPreferences.getStringSet(name, null)?.toSet() ?: defaultValue

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Set<String>) =
        sharedPreferences.edit().putStringSet(name, value).apply()
}

internal class SharedPreference_Boolean(
    val name: String,
    private val sharedPreferences: SharedPreferences,
    private val defaultValue: Boolean
) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        sharedPreferences.getBoolean(name, defaultValue)

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        sharedPreferences.edit().putBoolean(name, value).apply()
}