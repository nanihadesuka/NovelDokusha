package my.noveldokusha.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import timber.log.Timber

class ScraperCookieJar : CookieJar {
    private val manager = CookieManager.getInstance().apply {
        setAcceptCookie(true)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())
        return if (cookies != null && cookies.isNotBlank()) {
            val authority = url.toUrl().authority
            Timber.v("url:$url\n\nauthority:$authority\n\ncookies:$cookies")

            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val authority = url.toUrl().authority
        Timber.v("url:$url\n\nauthority:$authority\n\ncookies:$cookies")

        cookies.forEach {
            manager.setCookie(url.toString(), it.toString())
        }
    }
}