package my.noveldokusha.core.utils

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import kotlin.reflect.KProperty

class Extra_StringArrayList {
    operator fun getValue(thisRef: Intent, property: KProperty<*>) =
        thisRef.extras!!.getStringArrayList(property.name)!!

    operator fun setValue(thisRef: Intent, property: KProperty<*>, value: ArrayList<String>) =
        thisRef.putExtra(property.name, value)
}

class Extra_String {
    operator fun getValue(thisRef: Intent, property: KProperty<*>) =
        thisRef.extras!!.getString(property.name)!!

    operator fun setValue(thisRef: Intent, property: KProperty<*>, value: String) =
        thisRef.putExtra(property.name, value)
}

class Extra_StringNullable {
    operator fun getValue(thisRef: Intent, property: KProperty<*>) =
        thisRef.extras!!.getString(property.name)

    operator fun setValue(thisRef: Intent, property: KProperty<*>, value: String?) =
        thisRef.putExtra(property.name, value)
}

class Extra_Parcelable<T : Parcelable> {
    operator fun getValue(thisRef: Intent, property: KProperty<*>) =
        thisRef.extras!!.getParcelable<T>(property.name)!!

    operator fun setValue(thisRef: Intent, property: KProperty<*>, value: T) =
        thisRef.putExtra(property.name, value)
}

class Extra_Uri {
    operator fun getValue(thisRef: Intent, property: KProperty<*>) =
        thisRef.extras!!.get(property.name) as Uri

    operator fun setValue(thisRef: Intent, property: KProperty<*>, value: Uri) =
        thisRef.putExtra(property.name, value)
}

class Extra_Int {
    operator fun getValue(thisRef: Intent, property: KProperty<*>) =
        thisRef.extras!!.getInt(property.name)

    operator fun setValue(thisRef: Intent, property: KProperty<*>, value: Int) =
        thisRef.putExtra(property.name, value)
}

class Extra_Float {
    operator fun getValue(thisRef: Intent, property: KProperty<*>) =
        thisRef.extras!!.getFloat(property.name)

    operator fun setValue(thisRef: Intent, property: KProperty<*>, value: Float) =
        thisRef.putExtra(property.name, value)
}

class Extra_Boolean {
    operator fun getValue(thisRef: Intent, property: KProperty<*>) =
        thisRef.extras!!.getBoolean(property.name)

    operator fun setValue(thisRef: Intent, property: KProperty<*>, value: Boolean) =
        thisRef.putExtra(property.name, value)
}