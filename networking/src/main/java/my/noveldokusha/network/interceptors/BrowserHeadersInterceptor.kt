package my.noveldokusha.network.interceptors

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Makes OkHttp requests look like a real browser by setting the standard
 * headers that Chrome/Firefox send on every navigation request.
 *
 * This is the **Tier 1** defense against Cloudflare and similar bot-protection
 * services: many sites never escalate to a JS challenge at all if the initial
 * request looks browser-like. Conversely, a request that omits these headers
 * is trivially identifiable as a non-browser client and is much more likely
 * to be served a Cloudflare interstitial, a 403, or a managed challenge.
 *
 * Inspired by the trawl project's Tier 1 fetcher, which sends the same set
 * of headers and reports a significantly lower challenge rate than a bare
 * OkHttp client.
 *
 * Headers set (only if not already present on the request — callers can
 * override any of them by setting them explicitly):
 *
 *   - Accept              — standard browser content negotiation
 *   - Accept-Language     — en-US,en;q=0.9 (matches the WebView default)
 *   - Accept-Encoding     — gzip, deflate, br (the DecodeResponseInterceptor
 *                           handles decompression transparently)
 *   - Cache-Control       — no-cache (avoids serving stale CF challenge pages
 *                           from the OkHttp cache)
 *   - Pragma              — no-cache (HTTP/1.0 equivalent, belt-and-suspenders)
 *   - Upgrade-Insecure-Requests — 1 (Chrome always sends this on navigations)
 *   - Sec-Fetch-Dest      — document
 *   - Sec-Fetch-Mode      — navigate
 *   - Sec-Fetch-Site      — none (top-level navigation, no referrer)
 *   - Sec-Fetch-User      — ?1 (user-initiated navigation)
 *   - Sec-CH-UA           — matching the User-Agent set by UserAgentInterceptor
 *   - Sec-CH-UA-Mobile    — ?1 (mobile UA)
 *   - Sec-CH-UA-Platform  — "Android" (matches the Pixel UA)
 *
 * The User-Agent itself is left to [UserAgentInterceptor] — order matters,
 * see [ScraperNetworkClient] for the interceptor chain ordering.
 *
 * Note: We deliberately do NOT set `Referer` here. Setting a Referer that
 * doesn't match the user's actual browsing flow is itself a fingerprinting
 * signal. Sources that need a specific Referer (e.g. for search endpoints)
 * set it explicitly on the request, and we respect that.
 */
internal class BrowserHeadersInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()

        // Only set each header if the caller hasn't already provided one.
        // This lets sources override individual headers when needed.
        if (original.header("Accept").isNullOrBlank()) {
            builder.header(
                "Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9," +
                    "image/avif,image/webp,*/*;q=0.8"
            )
        }
        if (original.header("Accept-Language").isNullOrBlank()) {
            builder.header("Accept-Language", "en-US,en;q=0.9")
        }
        if (original.header("Accept-Encoding").isNullOrBlank()) {
            // DecodeResponseInterceptor transparently handles gzip + br.
            builder.header("Accept-Encoding", "gzip, deflate, br")
        }
        if (original.header("Cache-Control").isNullOrBlank()) {
            builder.header("Cache-Control", "no-cache")
        }
        if (original.header("Pragma").isNullOrBlank()) {
            builder.header("Pragma", "no-cache")
        }
        if (original.header("Upgrade-Insecure-Requests").isNullOrBlank()) {
            builder.header("Upgrade-Insecure-Requests", "1")
        }

        // Sec-Fetch-* — Chrome sends these on every navigation. Their absence
        // is one of the strongest non-JS signals that a client is not a real
        // browser.
        if (original.header("Sec-Fetch-Dest").isNullOrBlank()) {
            builder.header("Sec-Fetch-Dest", "document")
        }
        if (original.header("Sec-Fetch-Mode").isNullOrBlank()) {
            builder.header("Sec-Fetch-Mode", "navigate")
        }
        if (original.header("Sec-Fetch-Site").isNullOrBlank()) {
            builder.header("Sec-Fetch-Site", "none")
        }
        if (original.header("Sec-Fetch-User").isNullOrBlank()) {
            builder.header("Sec-Fetch-User", "?1")
        }

        // Sec-CH-UA client hints — must match the User-Agent set by
        // UserAgentInterceptor. Chrome refuses to send these on the very
        // first request of a session, but most bot protection only checks
        // that they're *present and consistent*, not that they follow
        // Chrome's exact critical-CH negotiation dance. Setting them here
        // is strictly better than omitting them.
        if (original.header("Sec-CH-UA").isNullOrBlank()) {
            // Brand + version must match the Chrome 120 in DEFAULT_USER_AGENT.
            builder.header(
                "Sec-CH-UA",
                "\"Chromium\";v=\"120\", \"Not(A:Brand\";v=\"24\", " +
                    "\"Google Chrome\";v=\"120\""
            )
        }
        if (original.header("Sec-CH-UA-Mobile").isNullOrBlank()) {
            builder.header("Sec-CH-UA-Mobile", "?1")
        }
        if (original.header("Sec-CH-UA-Platform").isNullOrBlank()) {
            builder.header("Sec-CH-UA-Platform", "\"Android\"")
        }

        return chain.proceed(builder.build())
    }
}
