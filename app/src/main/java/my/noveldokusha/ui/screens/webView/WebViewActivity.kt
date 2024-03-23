package my.noveldokusha.ui.screens.webView

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.R
import my.noveldokusha.databinding.ActivityWebviewBinding
import my.noveldokusha.di.AppCoroutineScope
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.toUrl
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : BaseActivity() {
    @Inject
    lateinit var appScope: AppCoroutineScope

    class IntentData : Intent {
        var url by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, url: String) : super(ctx, WebViewActivity::class.java) {
            this.url = url
        }
    }

    private val extras by lazy { IntentData(intent) }
    private val viewBind by lazy { ActivityWebviewBinding.inflate(layoutInflater) }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBind.root)
        setSupportActionBar(viewBind.toolbar)

        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW)) {
            toasty.show(R.string.webview_not_available)
            finish()
            return
        }

        extras.url.toUrl()?.authority ?: run {
            toasty.show(R.string.invalid_URL)
            finish()
            return
        }

        viewBind.webview.settings.javaScriptEnabled = true

        viewBind.webview.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (view != null && url != null) {
                    toasty.show(R.string.cookies_saved)
                }
            }
        }

        viewBind.webview.loadUrl(extras.url)

        supportActionBar!!.subtitle = extras.url
    }
}