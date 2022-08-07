package my.noveldokusha.utils

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import kotlin.reflect.KProperty

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