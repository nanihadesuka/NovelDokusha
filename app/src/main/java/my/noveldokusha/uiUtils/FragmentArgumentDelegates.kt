package my.noveldokusha.uiUtils

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import kotlin.properties.Delegates
import kotlin.reflect.KProperty



private val Fragment.defArgs: Bundle
	get() = run {
		arguments = arguments ?: Bundle()
		arguments!!
	}

class Argument_Boolean
{
	operator fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.defArgs.getBoolean(property.name)
	operator fun setValue(thisRef: Fragment, property: KProperty<*>, value: Boolean) = thisRef.defArgs.putBoolean(property.name, value)
}

class Argument_StringArrayList
{
	operator fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.defArgs.getStringArrayList(property.name)!!
	operator fun setValue(thisRef: Fragment, property: KProperty<*>, value: ArrayList<String>) =
		thisRef.defArgs.putStringArrayList(property.name, value)
}

class Argument_String
{
	operator fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.defArgs.getString(property.name)!!
	operator fun setValue(thisRef: Fragment, property: KProperty<*>, value: String) = thisRef.defArgs.putString(property.name, value)
}

class Argument_Int
{
	operator fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.defArgs.getInt(property.name)
	operator fun setValue(thisRef: Fragment, property: KProperty<*>, value: Int) = thisRef.defArgs.putInt(property.name, value)
}

class Argument_Float
{
	operator fun getValue(thisRef: Fragment, property: KProperty<*>) = thisRef.defArgs.get(property.name)
	operator fun setValue(thisRef: Fragment, property: KProperty<*>, value: Float) = thisRef.defArgs.putFloat(property.name, value)
}
