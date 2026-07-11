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

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Validate the URL up front — if it's missing or malformed, bail out
        // BEFORE creating a WebView (creating a WebView allocates a non-trivial
        // chunk of native memory and starts a WebKit renderer thread; better to
        // not bother if we're going to finish() immediately).
        extras.url.toUrl()?.authority ?: run {
            toasty.show(R.string.invalid_URL)
            finish()
            return
        }

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
            toasty.show(R.string.web_view_not_available)
            finish()
            return
        }

        val webView = WebView(this).also {
            it.settings.javaScriptEnabled = true
            it.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (view != null && url != null) {
                        toasty.show(R.string.cookies_saved)
                    }
                }
            }
            it.loadUrl(extras.url)
        }
        this.webView = webView

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
    }

    override fun onDestroy() {
        // Drop the WebView reference and tear down its renderer thread.
        // Without this the WebKit renderer thread outlives the Activity,
        // holding the Activity's context via the WebViewClient and leaking
        // the entire Activity view hierarchy on every "open in webview".
        webView?.apply {
            stopLoading()
            removeAllViews()
            destroy()
        }
        webView = null
        super.onDestroy()
    }
}