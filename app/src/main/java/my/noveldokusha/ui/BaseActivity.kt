package my.noveldokusha.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.asLiveData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.drop
import my.noveldokusha.*
import my.noveldokusha.utils.toast

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity()
{
    val appPreferences: AppPreferences by lazy { AppModule.provideAppPreferencies(applicationContext) }

    private fun getAppTheme(): Int
    {
        if (!appPreferences.THEME_FOLLOW_SYSTEM.value)
            return appPreferences.THEME_ID.value

        val isSystemThemeLight = !isSystemInDarkTheme()
        val isThemeLight = AppPreferences.globalThemeListLight.contains(appPreferences.THEME_ID.value)

        if (isSystemThemeLight && !isThemeLight) return R.style.AppTheme_Light
        if (!isSystemThemeLight && isThemeLight) return R.style.AppTheme_BaseDark_Dark
        return appPreferences.THEME_ID.value
    }

    private fun isSystemInDarkTheme() : Boolean {
        val uiMode = resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        setTheme(getAppTheme())
        appPreferences.THEME_ID.flow().drop(1).asLiveData().observe(this) { recreate() }
        appPreferences.THEME_FOLLOW_SYSTEM.flow().drop(1).asLiveData().observe(this) { recreate() }
        super.onCreate(savedInstanceState)
    }

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
        denied: (deniedPermissions: List<String>) -> Unit = { toast(getString(R.string.permissions_denied)) },
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