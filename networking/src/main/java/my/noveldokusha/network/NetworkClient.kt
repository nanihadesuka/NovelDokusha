package my.noveldokusha.network

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import my.noveldokusha.core.AppInternalState
import my.noveldokusha.network.interceptors.BrowserHeadersInterceptor
import my.noveldokusha.network.interceptors.CloudFareVerificationInterceptor
import my.noveldokusha.network.interceptors.DecodeResponseInterceptor
import my.noveldokusha.network.interceptors.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

interface NetworkClient {
    suspend fun call(request: Request.Builder, followRedirects: Boolean = false): Response
    suspend fun get(url: String): Response
    suspend fun get(url: Uri.Builder): Response
}

@Singleton
class ScraperNetworkClient @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appInternalState: AppInternalState,
) : NetworkClient {

    private val cacheDir = File(appContext.cacheDir, "network_cache")
    private val cacheSize = 5L * 1024 * 1024

    private val cookieJar = ScraperCookieJar()

    private val okhttpLoggingInterceptor = HttpLoggingInterceptor {
        Timber.v(it)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .let {
            if (appInternalState.isDebugMode) {
                it.addInterceptor(okhttpLoggingInterceptor)
            } else it
        }
        // Order matters:
        //   1. UserAgentInterceptor   — sets the User-Agent header (must run
        //      first so BrowserHeadersInterceptor can set Sec-CH-UA to match).
        //   2. BrowserHeadersInterceptor — sets Accept, Accept-Language,
        //      Sec-Fetch-*, Sec-CH-UA-*, etc. so the request looks like a
        //      real Chrome navigation. This is the Tier 1 Cloudflare-evasion
        //      layer: many sites never escalate to a JS challenge at all if
        //      the initial request looks browser-like.
        //   3. DecodeResponseInterceptor — decompresses gzip/br response
        //      bodies so downstream code sees plain text.
        //   4. CloudFareVerificationInterceptor — Tier 2/3: if the response
        //      is a CF challenge, fire up a WebView to solve it and retry.
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(BrowserHeadersInterceptor())
        .addInterceptor(DecodeResponseInterceptor())
        .addInterceptor(CloudFareVerificationInterceptor(appContext))
        .cookieJar(cookieJar)
        .cache(Cache(cacheDir, cacheSize))
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val clientWithRedirects = client
        .newBuilder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    override suspend fun call(request: Request.Builder, followRedirects: Boolean): Response {
        return if (followRedirects) clientWithRedirects.call(request) else client.call(request)
    }

    override suspend fun get(url: String) = call(getRequest(url))
    override suspend fun get(url: Uri.Builder) = call(getRequest(url.toString()))
}
