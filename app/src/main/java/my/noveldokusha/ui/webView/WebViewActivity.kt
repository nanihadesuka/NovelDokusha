package my.noveldokusha.ui.webView

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.webkit.*
import androidx.activity.viewModels
import my.noveldokusha.cookiesData
import my.noveldokusha.databinding.ActivityWebviewBinding
import my.noveldokusha.headersData
import my.noveldokusha.ui.BaseActivity
import my.noveldokusha.ui.sourceCatalog.WebViewModel
import my.noveldokusha.uiUtils.Extra_String
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
	private val viewModel by viewModels<WebViewModel>()
	private val viewHolder by lazy { ActivityWebviewBinding.inflate(layoutInflater) }
	private val viewAdapter = object
	{}
	private val viewLayoutManager = object
	{}
	
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(viewHolder.root)
		setSupportActionBar(viewHolder.toolbar)
		viewModel.initialization()
		
		if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WEBVIEW))
		{
			toast("Web view not available")
			finish()
		}
		
		viewHolder.webview.settings.javaScriptEnabled = true
		viewHolder.webview.webViewClient = object : WebViewClient()
		{
			
			override fun onPageFinished(view: WebView?, url: String?)
			{
				if (url != null) CookieManager.getInstance().also { manager ->
					val cookies = manager.getCookie(url)
						.split(";")
						.map { it.split("=") }
						.associate { it[0].trim() to it[1].trim() }
					
					cookiesData.add(url, cookies)
				}
				
				super.onPageFinished(view, url)
			}
			
			override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse?
			{
				request?.also { rq ->
					headersData.add(rq.url.toString(), rq.requestHeaders)
				}
				return super.shouldInterceptRequest(view, request)
			}
		}
		viewHolder.webview.loadUrl(extras.url)
		
		
		
		supportActionBar!!.let {
			it.subtitle = extras.url
		}
	}
}