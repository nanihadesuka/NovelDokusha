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
        val authority = url.toUrl().authority
        val cookies = manager.getCookie(authority)
        return if (cookies != null && cookies.isNotBlank()) {
            if(BuildConfig.DEBUG){
                Log.v("CookieJar:loadForRequest", "url:$url\n\nauthority:$authority\n\ncookies:$cookies")
            }
            cookies.split(";").mapNotNull { Cookie.parse(url, it) }
        } else emptyList()
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val authority = url.toUrl().authority
        if(BuildConfig.DEBUG){
            Log.v("CookieJar:saveFromResponse", "url:$url\n\nauthority:$authority\n\ncookies:$cookies")
        }
        cookies.forEach {
            manager.setCookie(authority, it.toString())
        }
    }
}