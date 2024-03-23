package my.noveldokusha.network

import android.webkit.CookieManager
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class ScraperCookieJar : CookieJar {
    private val manager = CookieManager.getInstance().also {
        it.setAcceptCookie(true)
    }

    private fun String.toCookiesMap(): Map<String, String> = this
        .split(";")
        .map { it.trim().split("=") }
        .filter { it.size == 2 }
        .associate { it[0] to it[1] }

    private fun get(url: String?): Map<String, String> {
        url ?: return mapOf()
        return manager.getCookie(url)?.toCookiesMap() ?: mapOf()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val new = get(url.toString())
            .mapNotNull { Cookie.parse(url, "${it.key}=${it.value}") }
        return new
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookieEntry in cookies) {
            manager.setCookie(url.toString(), "${cookieEntry.name}=${cookieEntry.value}")
        }
        manager.flush()
    }
}
