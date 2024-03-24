package my.noveldokusha.features.webView

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.R
import my.noveldokusha.di.AppCoroutineScope
import my.noveldokusha.ui.Toasty
import my.noveldokusha.utils.Extra_String
import my.noveldokusha.utils.toUrl
import javax.inject.Inject

@AndroidEntryPoint
class WebViewActivity : ComponentActivity() {
    @Inject
    lateinit var appScope: AppCoroutineScope

    @Inject
    lateinit var toasty: Toasty

    class IntentData : Intent {
        var url by Extra_String()

        constructor(intent: Intent) : super(intent)
        constructor(ctx: Context, url: String) : super(ctx, WebViewActivity::class.java) {
            this.url = url
        }
    }

    private val extras by lazy { IntentData(intent) }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this).also {
            it.loadUrl(extras.url)
        }
        setContent {
            Scaffold(
                topBar = {
                    Surface {
                        TopAppBar(
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Unspecified,
                                scrolledContainerColor = Color.Unspecified,
                            ),
                            title = {
                                Text(
                                    text = extras.url,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { this@WebViewActivity.onBackPressed() }
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
                                }
                            },
                        )
                    }
                },
                content = {
                    AndroidView(
                        modifier = Modifier.padding(it),
                        factory = { webView }
                    )
                }
            )
        }

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