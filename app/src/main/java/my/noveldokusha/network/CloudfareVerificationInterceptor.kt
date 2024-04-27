package my.noveldokusha.network

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.seconds

private val ERROR_CODES = listOf(
    HttpsURLConnection.HTTP_FORBIDDEN /*403*/,
    HttpsURLConnection.HTTP_UNAVAILABLE /*503*/
)
private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")

/**
 * If a CloudFare security verification redirection is detected, execute a
 * webView and retrieve the necessary headers.
 */
class CloudFareVerificationInterceptor(
    @ApplicationContext private val appContext: Context
) : Interceptor {

    private val lock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (isNotCloudFare(response)) {
            return response
        }

        return lock.withLock {
            try {
                response.close()
                // Remove old cf_clearance from the cookie
                val cookie = CookieManager
                    .getInstance()
                    .getCookie(request.url.toString())
                    .splitToSequence(";")
                    .map { it.split("=").map(String::trim) }
                    .filter { it[0] != "cf_clearance" }
                    .joinToString(";") { it.joinToString("=") }

                CookieManager
                    .getInstance()
                    .setCookie(request.url.toString(), cookie)

                runBlocking(Dispatchers.IO) {
                    resolveWithWebView(request)
                }

                chain.proceed(request)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                when (e) {
                    is CancellationException -> throw e
                    else -> throw IOException(e)
                }
            }
        }
    }

    private fun isNotCloudFare(response: Response): Boolean {
        return response.code !in ERROR_CODES ||
                response.header("Server") !in SERVER_CHECK
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun resolveWithWebView(request: Request) = withContext(Dispatchers.Default) {
        val headers = request
            .headers
            .toMultimap()
            .mapValues { it.value.firstOrNull() ?: "" }

        val cookieManager = CookieManager.getInstance()
        WebSettings.getDefaultUserAgent(appContext)

        withContext(Dispatchers.Main) {
            val webView = WebView(appContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.userAgentString = request.header("user-agent")
                    ?: UserAgentInterceptor.DEFAULT_USERAGENT

                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)
                webViewClient = object : WebViewClient() {}
            }
            webView.loadUrl(request.url.toString(), headers)
            // This will won't be often executed so no need for eager delay exit
            delay(20.seconds)
            webView.stopLoading()
            webView.destroy()
        }
    }
}