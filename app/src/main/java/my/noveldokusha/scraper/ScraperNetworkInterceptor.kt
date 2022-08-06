package my.noveldokusha.scraper

import my.noveldokusha.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber

private const val DEFAULT_USERAGENT = "Mozilla/5.0 (Windows NT 6.3; WOW64)"

class UserAgentInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        return if (originalRequest.header("User-Agent").isNullOrEmpty()) {
            val newRequest = originalRequest
                .newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", DEFAULT_USERAGENT)
                .build()
            chain.proceed(newRequest)
        } else {
            chain.proceed(originalRequest)
        }
    }
}