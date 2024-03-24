package my.noveldokusha.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.asLiveData
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.drop
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.di.AppModule
import javax.inject.Inject

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {

    val appPreferences: AppPreferences by lazy { AppModule.provideAppPreferences(applicationContext) }

    @Inject
    lateinit var toasty: Toasty

    private fun getAppTheme(): Int {
        if (!appPreferences.THEME_FOLLOW_SYSTEM.value)
            return appPreferences.THEME_ID.value

        val isSystemThemeLight = !isSystemInDarkTheme()
        val isThemeLight =
            AppPreferences.globalThemeListLight.contains(appPreferences.THEME_ID.value)

        if (isSystemThemeLight && !isThemeLight) return R.style.AppTheme_Light
        if (!isSystemThemeLight && isThemeLight) return R.style.AppTheme_BaseDark_Dark
        return appPreferences.THEME_ID.value
    }

    private fun isSystemInDarkTheme(): Boolean {
        val uiMode = resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    }

    // This will remain until Reader Screen has no View XML usages
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(getAppTheme())
        appPreferences.THEME_ID.flow().drop(1).asLiveData().observe(this) { recreate() }
        appPreferences.THEME_FOLLOW_SYSTEM.flow().drop(1).asLiveData().observe(this) { recreate() }
        super.onCreate(savedInstanceState)
    }
}