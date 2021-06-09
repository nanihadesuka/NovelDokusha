package my.noveldokusha.uiUtils

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import kotlin.properties.Delegates
import kotlin.reflect.KProperty

class Extra_StringArrayList
{
	operator fun getValue(thisRef: Intent, property: KProperty<*>) = thisRef.extras!!.getStringArrayList(property.name)!!
	operator fun setValue(thisRef: Intent, property: KProperty<*>, value: ArrayList<String>) = thisRef.putExtra(property.name, value)
}

class Extra_String
{
	operator fun getValue(thisRef: Intent, property: KProperty<*>) = thisRef.extras!!.getString(property.name)!!
	operator fun setValue(thisRef: Intent, property: KProperty<*>, value: String) = thisRef.putExtra(property.name, value)
}

class Extra_Int
{
	operator fun getValue(thisRef: Intent, property: KProperty<*>) = thisRef.extras!!.getInt(property.name)
	operator fun setValue(thisRef: Intent, property: KProperty<*>, value: Int) = thisRef.putExtra(property.name, value)
}

class Extra_Float
{
	operator fun getValue(thisRef: Intent, property: KProperty<*>) = thisRef.extras!!.getFloat(property.name)
	operator fun setValue(thisRef: Intent, property: KProperty<*>, value: Float) = thisRef.putExtra(property.name, value)
}

class Extra_Boolean
{
	operator fun getValue(thisRef: Intent, property: KProperty<*>) = thisRef.extras!!.getBoolean(property.name)
	operator fun setValue(thisRef: Intent, property: KProperty<*>, value: Boolean) = thisRef.putExtra(property.name, value)
}

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

class ObservableNoInitValue<T>(private val fn: (KProperty<*>, T, T) -> Unit)
{
	private var value by Delegates.observable<T?>(null) { prep, old, new ->
		if (old != null) fn(prep, old, new!!)
	}
	
	operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value!!
	operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
	{
		this.value = value
	}
}
