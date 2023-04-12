package my.noveldokusha.utils

import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.saveable
import kotlin.reflect.KProperty

fun <T> SavedStateHandle.asMutableStateOf(key: String, default: () -> T): MutableState<T> =
    object : MutableState<T> by saveable(key = key, init = { mutableStateOf(default()) }) {}

class StateExtra_StringArrayList(private val state: SavedStateHandle) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        state.get<ArrayList<String>>(property.name)!!

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: ArrayList<String>) =
        state.set(property.name, value)
}

class StateExtra_String(private val state: SavedStateHandle) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        state.get<String>(property.name)!!

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) =
        state.set(property.name, value)
}

class StateExtra_Parcelable<T : Parcelable>(private val state: SavedStateHandle) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        state.get<T>(property.name)!!

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        state.set(property.name, value)
}

class StateExtra_Uri(private val state: SavedStateHandle) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = state.get<Uri>(property.name)!!
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Uri) =
        state.set(property.name, value)
}

class StateExtra_Int(private val state: SavedStateHandle) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = state.get<Int>(property.name)!!
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) =
        state.set(property.name, value)
}

class StateExtra_Float(private val state: SavedStateHandle) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) = state.get<Float>(property.name)!!
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) =
        state.set(property.name, value)
}

class StateExtra_Boolean(private val state: SavedStateHandle) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>) =
        state.get<Boolean>(property.name)!!

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) =
        state.set(property.name, value)
}