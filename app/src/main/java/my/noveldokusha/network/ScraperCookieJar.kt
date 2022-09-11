package my.noveldokusha.network

import android.util.Log
import android.webkit.CookieManager
import my.noveldokusha.BuildConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class ScraperCookieJar : CookieJar {
    private val manager = CookieManager.getInstance().apply {
        setAcceptCookie(true)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookies = manager.getCookie(url.toString())
        return if (cookies != null && cookies.isNotBlank()) {
            if(BuildConfig.DEBUG){
                val authority = url.toUrl().authority
                Log.v("CookieJar:loadForRequest", "url:$url\n\nauthority:$authority\n\ncookies:$cookies")
            }
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if(BuildConfig.DEBUG){
            val authority = url.toUrl().authority
            Log.v("CookieJar:saveFromResponse", "url:$url\n\nauthority:$authority\n\ncookies:$cookies")
        }
        cookies.forEach {
            manager.setCookie(url.toString(), it.toString())
        }
    }
}