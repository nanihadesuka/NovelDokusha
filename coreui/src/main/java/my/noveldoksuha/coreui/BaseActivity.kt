package my.noveldoksuha.coreui

import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import my.noveldoksuha.coreui.theme.ThemeProvider
import javax.inject.Inject

@AndroidEntryPoint
open class BaseActivity : AppCompatActivity() {

    @Inject
    lateinit var themeProvider: ThemeProvider
}