package my.noveldokusha.scraper

import android.util.Log
import my.noveldokusha.App
import my.noveldokusha.BuildConfig
import okhttp3.*
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

val cookieJar = ScraperCookieJar()
private val cacheDir = File(App.cacheDir, "network_cache")
private const val cacheSize = 5L * 1024 * 1024 // 5 MiB

private val okhttpLoggingInterceptor = HttpLoggingInterceptor { Log.v("OkHttp",it) }.apply {
    level = HttpLoggingInterceptor.Level.BODY
}

val client = OkHttpClient.Builder()
    .let {
        if (BuildConfig.DEBUG) {
            it.addInterceptor(okhttpLoggingInterceptor)
        } else it
    }
    .addInterceptor(UserAgentInterceptor())
    .cookieJar(cookieJar)
    .cache(Cache(cacheDir, cacheSize))
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

val clientRedirects = client
    .newBuilder()
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

suspend fun Call.await(): Response {
    return suspendCoroutine { continuation ->
        enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                continuation.resume(response)
            }
        })
    }
}

private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build()
private val DEFAULT_HEADERS = Headers.Builder().build()
private val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

fun Request.Builder.postScope(scope: FormBody.Builder.() -> Unit): Request.Builder {
    val builder = FormBody.Builder()
    scope(builder)
    return post(builder.build())
}

fun getRequest(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    cache: CacheControl = DEFAULT_CACHE_CONTROL
) = Request.Builder()
    .url(url)
    .headers(headers)
    .cacheControl(cache)


fun postRequest(
    url: String,
    headers: Headers = DEFAULT_HEADERS,
    body: RequestBody = DEFAULT_BODY,
    cache: CacheControl = DEFAULT_CACHE_CONTROL
) = Request.Builder()
    .url(url)
    .post(body)
    .headers(headers)
    .cacheControl(cache)

suspend fun OkHttpClient.call(builder: Request.Builder) = newCall(builder.build()).await()


