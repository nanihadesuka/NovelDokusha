package my.noveldokusha.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class ScraperCookieJar : CookieJar {
    private val manager = CookieManager.getInstance().also {
        it.setAcceptCookie(true)
    }

    private fun getCookieList(url: String?): List<String> {
        url ?: return emptyList()
        return manager.getCookie(url)?.split(";") ?: emptyList()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return getCookieList(url.toString()).mapNotNull { Cookie.parse(url, it) }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookieEntry in cookies) {
            manager.setCookie(url.toString(), "${cookieEntry.name}=${cookieEntry.value}")
        }
        manager.flush()
    }
}
