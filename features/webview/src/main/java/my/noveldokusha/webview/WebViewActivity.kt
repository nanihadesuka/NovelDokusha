package my.noveldokusha.webview

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import my.noveldoksuha.coreui.theme.Theme
import my.noveldoksuha.coreui.theme.ThemeProvider
import my.noveldokusha.core.Toasty
import my.noveldokusha.core.utils.Extra_String
import my.noveldokusha.network.toUrl
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : ComponentActivity() {

    @Inject
    lateinit var toasty: Toasty

    @Inject
    lateinit var themeProvider: ThemeProvider

    class IntentData : Intent {
        var url by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, url: String) : super(ctx, WebViewActivity::class.java) {
            this.url = url
        }
    }

    private val extras by lazy { IntentData(intent) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this).also {
            it.loadUrl(extras.url)
        }
        setContent {
            Theme(themeProvider = themeProvider) {
                WebViewScreen(
                    toolbarTitle = extras.url,
                    webViewFactory = { webView },
                    onBackClicked = { this@WebViewActivity.onBackPressed() },
                    onReloadClicked = { webView.reload() }
                )
            }
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
            toasty.show(R.string.web_view_not_available)
            finish()
            return
        }

        extras.url.toUrl()?.authority ?: run {
            toasty.show(R.string.invalid_URL)
            finish()
            return
        }

        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null && url != null) {
                    toasty.show(R.string.cookies_saved)
                }
            }
        }
    }
}