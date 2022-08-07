package my.noveldokusha.network

import okhttp3.*
import java.util.concurrent.TimeUnit

private val DEFAULT_CACHE_CONTROL = CacheControl.Builder().maxAge(10, TimeUnit.MINUTES).build()
private val DEFAULT_HEADERS = Headers.Builder().build()
private val DEFAULT_BODY: RequestBody = FormBody.Builder().build()

fun Request.Builder.postPayload(scope: FormBody.Builder.() -> Unit): Request.Builder {
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
