package my.noveldokusha.ui.webView

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.*
import androidx.activity.viewModels
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import my.noveldokusha.App
import my.noveldokusha.R
import my.noveldokusha.cookiesData
import my.noveldokusha.databinding.ActivityWebviewBinding
import my.noveldokusha.headersData
import my.noveldokusha.scraper.toUrl
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.uiUtils.Extra_String
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast

class WebViewActivity : BaseActivity()
{
	class IntentData : Intent
	{
		var url by Extra_String()
		
		constructor(intent: Intent) : super(intent)
		constructor(ctx: Context, url: String) : super(ctx, WebViewActivity::class.java)
		{
			this.url = url
		}
	}
	
	private val extras by lazy { IntentData(intent) }
	private val viewBind by lazy { ActivityWebviewBinding.inflate(layoutInflater) }
	
	@SuppressLint("SetJavaScriptEnabled")
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewBind.root)
		setSupportActionBar(viewBind.toolbar)
		
		if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW))
		{
			toast(R.string.webview_not_available.stringRes())
			finish()
			return
		}
		
		val authority = extras.url.toUrl()?.authority ?: run {
			toast(R.string.invalid_URL.stringRes())
			finish()
			return
		}
		
		viewBind.webview.settings.javaScriptEnabled = true
		
		viewBind.webview.webViewClient = object : WebViewClient()
		{
			
			override fun onPageFinished(view: WebView?, url: String?)
			{
				if (url?.toUrl()?.authority == authority) CookieManager.getInstance().also { manager ->
					val cookies = manager.getCookie(url)
						.split(";")
						.map { it.split("=") }
						.associate { it[0].trim() to it[1].trim() }
					App.scope.launch(Dispatchers.Default) { cookiesData.add(url, cookies) }
					toast(R.string.cookies_saved.stringRes())
				}
				
				super.onPageFinished(view, url)
			}
			
			override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse?
			{
				if (request?.url?.authority == authority)
					App.scope.launch(Dispatchers.Default) { headersData.add(request.url.toString(), request.requestHeaders) }
				return super.shouldInterceptRequest(view, request)
			}
		}
		
		viewBind.webview.loadUrl(extras.url)
		
		supportActionBar!!.let {
			it.subtitle = extras.url
		}
	}
}