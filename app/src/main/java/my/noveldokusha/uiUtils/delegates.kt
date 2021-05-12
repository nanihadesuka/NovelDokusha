package my.noveldokusha.uiUtils

import android.content.Intent
import kotlin.reflect.KProperty

class Extra_StringArrayList(val intent: Intent)
{
	operator fun getValue(thisRef: Any?, property: KProperty<*>) = intent.extras!!.getStringArrayList(property.name)!!
	operator fun setValue(thisRef: Any?, property: KProperty<*>, value: ArrayList<String>) = intent.putExtra(property.name, value)
}

class Extra_String(val intent: Intent)
{
	operator fun getValue(thisRef: Any?, property: KProperty<*>) = intent.extras!!.getString(property.name)!!
	operator fun setValue(thisRef: Any?, property: KProperty<*>, value: String) = intent.putExtra(property.name, value)
}

class Extra_Int(val intent: Intent)
{
	operator fun getValue(thisRef: Any?, property: KProperty<*>) = intent.extras!!.getInt(property.name)
	operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = intent.putExtra(property.name, value)
}

class Extra_Float(val intent: Intent)
{
	operator fun getValue(thisRef: Any?, property: KProperty<*>) = intent.extras!!.getFloat(property.name)
	operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) = intent.putExtra(property.name, value)
}

class Extra_Boolean(val intent: Intent)
{
	operator fun getValue(thisRef: Any?, property: KProperty<*>) = intent.extras!!.getBoolean(property.name)
	operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = intent.putExtra(property.name, value)
}
