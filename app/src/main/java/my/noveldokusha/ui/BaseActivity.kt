package my.noveldokusha.ui

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import my.noveldokusha.*
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast

open class BaseActivity : AppCompatActivity()
{
	private val sharedPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
		if (key == sharedPreferences::THEME_ID.name || key == sharedPreferences::THEME_FOLLOW_SYSTEM.name)
			recreate()
	}
	
	private fun getAppTheme(): Int
	{
		if (!sharedPreferences.THEME_FOLLOW_SYSTEM)
			return sharedPreferences.THEME_ID
		
		val isThemeLight = globalThemeList.light.contains(sharedPreferences.THEME_ID)
		val isSystemThemeLight = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES
		
		if (isSystemThemeLight && !isThemeLight) return R.style.AppTheme_Light
		if (!isSystemThemeLight && isThemeLight) return R.style.AppTheme_BaseDark_Dark
		return sharedPreferences.THEME_ID
	}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		setTheme(getAppTheme())
		sharedPreferences.registerOnSharedPreferenceChangeListener(sharedPreferencesListener)
		super.onCreate(savedInstanceState)
	}
	
	val sharedPreferences: SharedPreferences by lazy { appSharedPreferences() }
	
	private var activitiesCallbacksCounter: Int = 0
	private val activitiesCallbacks = mutableMapOf<Int, (resultCode: Int, data: Intent?) -> Unit>()
	
	fun activityRequest(intent: Intent, reply: (resultCode: Int, data: Intent?) -> Unit)
	{
		val requestCode = activitiesCallbacksCounter++
		activitiesCallbacks[requestCode] = reply
		startActivityForResult(intent, requestCode)
	}
	
	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
	{
		super.onActivityResult(requestCode, resultCode, data)
		activitiesCallbacks.remove(requestCode)?.let { it(resultCode, data) }
	}
	
	private var permissionsCallbacksCounter: Int = 0
	private val permissionsCallbacks = mutableMapOf<Int, Pair<() -> Unit, (List<String>) -> Unit>>()
	
	fun permissionRequest(
		vararg permissions: String,
		denied: (deniedPermissions: List<String>) -> Unit = { toast(R.string.permissions_denied.stringRes()) },
		granted: () -> Unit
	)
	{
		val hasPermissions = permissions.all { permission ->
			ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
		}
		
		if (hasPermissions) granted()
		else
		{
			val requestCode = permissionsCallbacksCounter++
			permissionsCallbacks[requestCode] = Pair(granted, denied)
			requestPermissions(permissions, requestCode)
		}
	}
	
	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults)
		permissionsCallbacks.remove(requestCode)?.let {
			when
			{
				grantResults.isEmpty() -> it.second(listOf())
				grantResults.all { result -> result == PackageManager.PERMISSION_GRANTED } -> it.first()
				else -> it.second(permissions.filterIndexed { index, _ -> grantResults[index] == PackageManager.PERMISSION_DENIED })
			}
		}
	}
}