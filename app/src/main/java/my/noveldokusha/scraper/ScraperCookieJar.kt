package my.noveldokusha.scraper

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class ScraperCookieJar : CookieJar {
    private val manager = CookieManager.getInstance().apply {
        setAcceptCookie(true)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val authority = url.toUrl().authority
        val cookies = manager.getCookie(authority)
        return if (cookies != null && cookies.isNotBlank()) {
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val authority = url.toUrl().authority
        cookies.forEach {
            manager.setCookie(authority, it.toString())
        }
    }
}