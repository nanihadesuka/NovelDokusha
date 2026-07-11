package my.noveldokusha.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response


internal class UserAgentInterceptor : Interceptor {

    companion object {
        /**
         * Modern Android Chrome User-Agent.
         *
         * Many novel hosting sites (Cloudflare-fronted ones especially) block
         * the legacy desktop Windows UA `Mozilla/5.0 (Windows NT 6.3; WOW64)`
         * because it is trivially recognisable as a non-browser client. Using
         * a current Android Pixel / Chrome UA matches what the in-app WebView
         * sends and significantly reduces false-positive blocks, retries, and
         * Cloudflare challenges — improving effective fetch speed and reducing
         * wasted bandwidth.
         */
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val hasNoUserAgent = originalRequest.header("User-Agent").isNullOrBlank()
        val modifiedRequest = if (hasNoUserAgent) {
            originalRequest
                .newBuilder()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", DEFAULT_USER_AGENT)
                .build()
        } else originalRequest
        return chain.proceed(modifiedRequest)
    }
}