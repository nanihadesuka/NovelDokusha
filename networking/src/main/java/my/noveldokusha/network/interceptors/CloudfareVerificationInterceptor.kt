package my.noveldokusha.network.interceptors

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import my.noveldokusha.core.domain.CloudfareVerificationBypassFailedException
import my.noveldokusha.core.domain.WebViewCookieManagerInitializationFailedException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.withLock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * HTTP status codes that Cloudflare (and similar services like DDoS-Guard)
 * use to signal "I think you're a bot". Sourced from the trawl project's
 * `isBlocked()` detector plus real-world CF edge behaviour:
 *
 *  - 202 — some CDNs use this as a bot-gate (returns a challenge page that
 *          a real browser would auto-solve via JS).
 *  - 403 — CF "blocked" / WAF rule match / managed challenge failure.
 *  - 429 — CF rate-limit / "Too many requests" challenge.
 *  - 502 — transient CF edge error; often resolves on retry.
 *  - 503 — CF "Just a moment…" interstitial (the classic).
 */
private val ERROR_CODES = listOf(
    202 /*bot-gate*/,
    HttpsURLConnection.HTTP_FORBIDDEN /*403*/,
    429 /*Too Many Requests*/,
    HttpsURLConnection.HTTP_BAD_GATEWAY /*502 — transient CF edge errors*/,
    HttpsURLConnection.HTTP_UNAVAILABLE /*503*/,
)

/**
 * Header values for the `Server` response header that indicate the response
 * came from a Cloudflare edge node (not the origin server).
 */
private val SERVER_HEADER_VALUES = arrayOf(
    "cloudflare-nginx",
    "cloudflare",
    "cloudflare-iad",
    "ddos-guard",
    "ddos-guard.net",
)

/**
 * Response-header names whose mere presence indicates a Cloudflare
 * challenge / block. The `cf-mitigated` header in particular is set by
 * Cloudflare when a request is challenged or blocked, even on responses
 * that don't include the typical challenge HTML body.
 */
private val CHALLENGE_HEADER_NAMES = arrayOf(
    "cf-mitigated",
)

/**
 * Substrings that identify a Cloudflare / DDoS-Guard / similar challenge or
 * interstitial page in the response body. Cloudflare's "Just a moment…"
 * page always contains at least one of these markers. We fall back to body
 * inspection when the server header is missing or stripped, and to catch
 * "managed challenge" pages that return HTTP 200 (which the old code missed
 * entirely).
 *
 * The markers are a superset of trawl's `isCloudflarePage()` detector,
 * expanded with the legacy markers the previous implementation already
 * handled so we don't regress on older CF challenge templates.
 */
private val CHALLENGE_BODY_MARKERS = arrayOf(
    // Legacy CF markers (kept for backwards compat)
    "cf-browser-verification",
    "cf-challenge-running",
    "/cdn-cgi/challenge-platform/",
    "cf-please-wait",
    "Just a moment",
    "Checking your browser",
    "cf_chl_opt",
    "cf-mitigated",
    "Attention Required! | Cloudflare",
    "challenge-platform",
    "ray id",
    // Trawl-derived markers — catch Turnstile, DDoS-Guard, and the newer
    // "managed challenge" templates that return HTTP 200.
    "verify you are human",
    "enable javascript and cookies to continue",
    "one more step",
    "id=\"challenge-running\"",
    "id=\"cf-challenge-running\"",
    "id=\"turnstile-wrapper\"",
    "cf-turnstile",
    "challenges.cloudflare.com/turnstile",
    "ddos-guard.net",
    ".ddos-guard.net",
)

/**
 * Page-title substrings that indicate an active Cloudflare challenge.
 * Mirrors trawl's `CF_CHALLENGE_TITLE` regex. We check the WebView's title
 * against these (case-insensitive) during the polling loop — when the title
 * changes to something else, the challenge is done.
 */
private val CHALLENGE_TITLE_MARKERS = arrayOf(
    "just a moment",
    "verify you are human",
    "please wait",
    "one more step",
    "attention required",
    "checking your browser",
)

/**
 * URL substrings that indicate the IP itself has been blocked by Cloudflare
 * (error 1020) or rate-limited (error 1015). In these cases no amount of
 * cookie-baking will help — the challenge can't be solved from this IP.
 * We detect this and bail out fast instead of spinning through all retries.
 */
private val IP_BLOCKED_URL_MARKERS = arrayOf(
    "/cdn-cgi/error/",
    "error=1020",
    "error=1015",
)

/**
 * Maximum number of WebView-based bypass attempts per intercepted request.
 * Each attempt waits up to [MAX_CHALLENGE_WAIT] for the cf_clearance cookie
 * to appear before giving up and retrying.
 */
private const val MAX_BYPASS_ATTEMPTS = 3

/**
 * Hard cap on how long a single WebView challenge attempt may run.
 * Real challenges usually resolve in 2-6 seconds; 20 s is a generous ceiling
 * that also covers Turnstile interactive widgets on slow connections and
 * the "force re-navigation" fallback (which itself can take up to 8 s).
 */
private val MAX_CHALLENGE_WAIT = 20.seconds

/**
 * Polling interval used while waiting for the cf_clearance cookie to appear
 * inside the WebView. Tight enough to feel snappy on a fast site, loose
 * enough not to burn CPU on a slow one.
 */
private val CHALLENGE_POLL_INTERVAL = 300.milliseconds

/**
 * Once `cf_clearance` has been obtained, Cloudflare normally auto-redirects
 * to the original URL within 2-3 seconds. If it hasn't redirected after this
 * many milliseconds, we manually navigate the WebView to the original URL —
 * this is the same trick trawl's `challengeWait.ts` uses to break out of
 * "cookie set but page stuck on challenge" loops, which are common with
 * Turnstile's non-interactive mode.
 */
private val FORCE_NAVIGATION_DELAY = 5.seconds

/**
 * JavaScript snippet injected into the WebView to attempt clicking the
 * Cloudflare Turnstile checkbox. Turnstile is the successor to the classic
 * "I'm not a robot" reCAPTCHA and is what most modern CF-protected sites
 * use. The checkbox is inside a cross-origin iframe from
 * `challenges.cloudflare.com`, so direct DOM access is blocked — but we
 * can still dispatch a click event at the iframe's on-page coordinates,
 * which Chrome will forward to the iframe as a user gesture.
 *
 * This is a best-effort approach: if the challenge is in "managed" mode
 * (non-interactive), it will resolve on its own and this JS is a no-op.
 * If it's in "interactive" mode, this click is what makes it pass.
 *
 * The snippet is idempotent and safe to evaluate repeatedly.
 */
private val TURNSTILE_CLICK_JS = """
    (function() {
        try {
            // Find any iframe whose src points at Cloudflare's challenge platform.
            var frames = document.querySelectorAll('iframe[src*="challenges.cloudflare.com"], iframe[src*="cdn-cgi/challenge-platform"]');
            for (var i = 0; i < frames.length; i++) {
                var f = frames[i];
                var rect = f.getBoundingClientRect();
                if (rect.width < 20 || rect.height < 20) continue;
                // Dispatch a real click at the centre of the iframe.
                var cx = rect.left + rect.width / 2;
                var cy = rect.top + rect.height / 2;
                var ev = new MouseEvent('click', {
                    bubbles: true, cancelable: true, view: window,
                    clientX: cx, clientY: cy
                });
                f.dispatchEvent(ev);
                // Also try a synthetic pointer event for newer Turnstile.
                var pe = new PointerEvent('pointerdown', {
                    bubbles: true, cancelable: true, view: window,
                    clientX: cx, clientY: cy, pointerId: 1
                });
                f.dispatchEvent(pe);
                return true;
            }
            // Fallback: look for an in-page Turnstile widget (no iframe).
            var widget = document.querySelector('.cf-turnstile, [data-sitekey]');
            if (widget) {
                var r = widget.getBoundingClientRect();
                widget.dispatchEvent(new MouseEvent('click', {
                    bubbles: true, cancelable: true, view: window,
                    clientX: r.left + r.width / 2, clientY: r.top + r.height / 2
                }));
                return true;
            }
        } catch (e) { /* swallow — best-effort */ }
        return false;
    })();
""".trimIndent()

/**
 * If a CloudFare security verification redirection is detected, execute a
 * WebView, wait for the challenge to resolve, harvest the cf_clearance cookie
 * (plus any other cookies Cloudflare set), then retry the original request.
 *
 * Improvements over the original implementation (and additional techniques
 * ported from the **trawl** Cloudflare-bypass engine):
 *
 *  1. **Tier 1 — body scan detection** — not just 403/503 + Server header.
 *     Cloudflare sometimes returns 200 with a challenge HTML body and no
 *     `Server` header, which the old code missed entirely.
 *  2. **Tier 1 — `cf-mitigated` header** — Cloudflare sets this header on
 *     challenged/blocked responses even when the body is empty or
 *     non-HTML. Checking it catches a class of API-level blocks that the
 *     body scan misses.
 *  3. **Tier 1 — HTTP 202 bot-gate** — some CDNs (and CF itself, in some
 *     configs) return 202 with a challenge body instead of 403/503.
 *  4. **Tier 1 — DDoS-Guard detection** — DDoS-Guard is a CF competitor
 *     used by several novel sites; its challenge page is similar enough
 *     that the same WebView bypass works, but the detection markers differ.
 *  5. **Polling-based completion** — instead of a fixed 20 s sleep, we poll
 *     the WebView's CookieManager every 300 ms and exit as soon as
 *     `cf_clearance` appears OR the page URL changes away from the
 *     challenge URL. Fast challenges finish in ~2 s instead of always
 *     waiting 20 s.
 *  6. **Retry mechanism** — up to [MAX_BYPASS_ATTEMPTS] attempts. A single
 *     transient Cloudflare hiccup no longer kills the entire fetch.
 *  7. **Full header forwarding** — Referer, Accept, Accept-Language, and the
 *     modern `Sec-CH-UA-*` client hints are forwarded to the WebView so its
 *     fingerprint matches the OkHttp request that triggered the challenge.
 *  8. **UA consistency** — the WebView's userAgentString is forced to match
 *     [UserAgentInterceptor.DEFAULT_USER_AGENT] so Cloudflare sees the same
 *     browser on the initial fetch and on the challenge fetch. This is the
 *     #1 cause of "challenge loops" — CF remembers the UA that triggered
 *     it.
 *  9. **Title-based completion detection** (from trawl) — in addition to
 *     checking for the `cf_clearance` cookie, we also check the WebView's
 *     page title. When the title no longer matches `just a moment` /
 *     `verify you are human` / etc., the challenge is done. This catches
 *     challenges that set the cookie late or via a redirect that doesn't
 *     change the URL.
 * 10. **Force re-navigation when stuck** (from trawl) — if `cf_clearance`
 *     has been set but the page is still on the challenge URL after 5 s,
 *     we manually load the original URL. CF sometimes "forgets" to fire
 *     the auto-redirect; doing it ourselves breaks the loop.
 * 11. **Turnstile checkbox click** (from trawl) — we inject JS to dispatch
 *     a click event at the Turnstile iframe. For interactive Turnstile
 *     widgets, this is what actually solves the challenge. For non-
 *     interactive ("managed") widgets, it's a harmless no-op.
 * 12. **IP-block fast-fail** (from trawl) — if the WebView navigates to a
 *     URL containing `/cdn-cgi/error/` or `error=1020`/`error=1015`, we
 *     abort immediately. No amount of cookie-baking will fix a hard IP
 *     block; the user needs a different network.
 * 13. **Leak-safe WebView lifecycle** — WebView is created on the main
 *     thread, always stopLoading() + destroy() in a finally block, AND we
 *     reset the WebViewClient reference before destroy() to avoid holding
 *     the activity context via the WebViewClient's outer class reference.
 * 14. **Cookie preservation** — we keep ALL existing cookies (not just
 *     strip cf_clearance) and let the WebView add new ones on top. This
 *     preserves session/language cookies that some CF-fronted sites rely
 *     on.
 * 15. **Body-less 403 handling** — we close the original response body
 *     before retrying so we never leak a connection.
 */
internal class CloudFareVerificationInterceptor(
    @ApplicationContext private val appContext: Context
) : Interceptor {

    private val lock = ReentrantLock()

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val challengeInfo = classifyResponse(response)
        if (!challengeInfo.isCloudflare) {
            return response
        }

        return lock.withLock {
            try {
                // Always close the challenged response so its connection can
                // be returned to the pool before we re-issue the request.
                response.close()

                @Suppress("SENSELESS_COMPARISON")
                val cookieManager = CookieManager.getInstance()
                    ?: throw WebViewCookieManagerInitializationFailedException()

                // Retry the request up to MAX_BYPASS_ATTEMPTS times. Each
                // attempt: (a) primes the WebView with cookies, (b) waits for
                // the challenge to resolve, (c) re-issues the original OkHttp
                // request which now carries the freshly-baked cf_clearance
                // cookie via the shared CookieManager / ScraperCookieJar.
                var lastFailure: Exception? = null
                repeat(MAX_BYPASS_ATTEMPTS) { attempt ->
                    try {
                        runBlocking(Dispatchers.IO) {
                            resolveWithWebView(request, cookieManager)
                        }

                        val retried = chain.proceed(request)
                        val reclassified = classifyResponse(retried)
                        if (!reclassified.isCloudflare) {
                            return@withLock retried
                        }
                        // Still challenged — close and retry
                        retried.close()
                        lastFailure = CloudfareVerificationBypassFailedException(
                            "Attempt ${attempt + 1}/$MAX_BYPASS_ATTEMPTS: " +
                                "still challenged after WebView bypass"
                        )
                    } catch (ce: CancellationException) {
                        throw ce
                    } catch (e: Exception) {
                        lastFailure = e
                    }
                }

                throw lastFailure
                    ?: CloudfareVerificationBypassFailedException(
                        "Cloudflare bypass failed after $MAX_BYPASS_ATTEMPTS attempts"
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                throw e
            } catch (e: Exception) {
                throw IOException(e.message, e.cause)
            }
        }
    }

    /**
     * A response is "Cloudflare" (or a similar bot-protection interstitial)
     * if ANY of the following is true:
     *
     *   1. The response carries a header named in [CHALLENGE_HEADER_NAMES]
     *      (e.g. `cf-mitigated`). This is the strongest signal — CF sets
     *      it even on non-HTML responses.
     *   2. The HTTP status code is in [ERROR_CODES] AND the `Server` header
     *      looks like Cloudflare (or DDoS-Guard).
     *   3. The HTTP status code is in [ERROR_CODES] OR 200, AND the body
     *      contains one of the [CHALLENGE_BODY_MARKERS] substrings. This
     *      catches the 200-status "managed challenge" pages that newer
     *      Cloudflare configs serve, as well as 403/503 pages where the
     *      Server header was stripped.
     */
    private fun classifyResponse(response: Response): ChallengeInfo {
        val code = response.code

        // (1) Header presence check — strongest signal, works on any body.
        for (headerName in CHALLENGE_HEADER_NAMES) {
            if (response.header(headerName) != null) {
                return ChallengeInfo(isCloudflare = true)
            }
        }

        val serverHeader = response.header("Server")
        val serverLooksLikeCloudflare = serverHeader != null &&
            SERVER_HEADER_VALUES.any { serverHeader.equals(it, ignoreCase = true) }

        // (2) Status code + Server header check.
        if (code in ERROR_CODES && serverLooksLikeCloudflare) {
            return ChallengeInfo(isCloudflare = true)
        }

        // (3) Body-based fallback: peek a small amount of the body and scan
        // for challenge markers. This catches 200-status managed challenge
        // pages and 403/503 pages where the Server header was stripped.
        return try {
            val peeked = response.peekBody(32 * 1024).string()
            ChallengeInfo(
                isCloudflare = bodyLooksLikeChallenge(peeked),
                bodyText = peeked,
            )
        } catch (e: Exception) {
            // If we can't read the body, fall back to the status-code check
            // (without the Server header, since we already failed that above).
            ChallengeInfo(
                isCloudflare = code in ERROR_CODES && serverLooksLikeCloudflare
            )
        }
    }

    private fun bodyLooksLikeChallenge(body: String): Boolean {
        if (body.isBlank()) return false
        return CHALLENGE_BODY_MARKERS.any { marker ->
            body.contains(marker, ignoreCase = true)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun resolveWithWebView(
        request: Request,
        cookieManager: CookieManager,
    ) = withContext(Dispatchers.Default) {
        // Forward all relevant headers from the OkHttp request to the WebView.
        // Cloudflare fingerprints the request: mismatched Accept-Language or
        // Sec-CH-UA between the triggering request and the challenge-solving
        // request is a common cause of "infinite challenge loops".
        val headersMap = mutableMapOf<String, String>()
        request.header("Accept")?.let { headersMap["Accept"] = it }
        request.header("Accept-Language")?.let { headersMap["Accept-Language"] = it }
        request.header("Accept-Encoding")?.let { headersMap["Accept-Encoding"] = it }
        request.header("Referer")?.let { headersMap["Referer"] = it }
        request.header("Sec-CH-UA")?.let { headersMap["Sec-CH-UA"] = it }
        request.header("Sec-CH-UA-Mobile")?.let { headersMap["Sec-CH-UA-Mobile"] = it }
        request.header("Sec-CH-UA-Platform")?.let { headersMap["Sec-CH-UA-Platform"] = it }
        request.header("Sec-Fetch-Dest")?.let { headersMap["Sec-Fetch-Dest"] = it }
        request.header("Sec-Fetch-Mode")?.let { headersMap["Sec-Fetch-Mode"] = it }
        request.header("Sec-Fetch-Site")?.let { headersMap["Sec-Fetch-Site"] = it }

        val requestUrl = request.url.toString()
        val userAgent = request.header("user-agent")
            ?: UserAgentInterceptor.DEFAULT_USER_AGENT

        // The ScraperCookieJar already shares the WebView CookieManager, so
        // any cookies Cloudflare previously set (e.g. __cf_bm) are already
        // present. We deliberately do NOT clear them: preserving
        // session/language cookies is important for some CF-fronted sites.
        cookieManager.setAcceptCookie(true)

        withContext(Dispatchers.Main) {
            // Use application context to avoid leaking Activity references.
            // WebView works fine with ApplicationContext for headless
            // challenge solving — it doesn't need to render anything
            // visible to the user.
            val webView = WebView(appContext).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.cacheMode = WebSettings.LOAD_DEFAULT
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.mediaPlaybackRequiresUserGesture = true
                settings.blockNetworkImage = true   // we don't need images for a challenge
                settings.setSupportZoom(false)

                // CRITICAL: force the WebView's UA to match the OkHttp UA.
                // Cloudflare rejects challenges where the UA that triggered
                // the challenge differs from the UA that solves it — this
                // is the #1 cause of "challenge loops" where the cookie is
                // set but immediately rejected on the next request.
                settings.userAgentString = userAgent

                cookieManager.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                    // We intentionally override nothing — the default
                    // WebViewClient already loads the challenge page and
                    // runs its JS. We only need to wait for the cookie to
                    // appear.
                }
            }

            try {
                webView.loadUrl(requestUrl, headersMap)

                // Poll for challenge completion. The challenge is "done"
                // when ANY of:
                //   (a) the `cf_clearance` cookie appears, OR
                //   (b) the page title changes away from a challenge title
                //       (e.g. no longer "Just a moment…"), OR
                //   (c) the page URL changes away from the challenge URL
                //       AND doesn't look like an IP-block error page.
                //
                // If `cf_clearance` has been set but the page is still on
                // the challenge URL after FORCE_NAVIGATION_DELAY, we
                // manually navigate to the original URL — this breaks the
                // "cookie set but page stuck" loop that Turnstile sometimes
                // triggers.
                //
                // If the page navigates to a URL matching
                // IP_BLOCKED_URL_MARKERS, we abort immediately — no amount
                // of retrying will help from this IP.
                val deadline = System.currentTimeMillis() +
                    MAX_CHALLENGE_WAIT.inWholeMilliseconds
                var cfClearanceAt: Long? = null
                var lastClickAttempt = 0L
                var lastUrl: String? = requestUrl

                while (System.currentTimeMillis() < deadline) {
                    delay(CHALLENGE_POLL_INTERVAL)

                    // (c) IP-block fast-fail.
                    val currentUrl = runCatching { webView.url }.getOrNull()
                    if (currentUrl != null && IP_BLOCKED_URL_MARKERS.any {
                            currentUrl.contains(it, ignoreCase = true)
                        }) {
                        throw CloudfareVerificationBypassFailedException(
                            "IP blocked by Cloudflare (error 1020/1015). " +
                                "Try a different network."
                        )
                    }

                    // (a) Check if cf_clearance cookie is now present.
                    val currentCookies = cookieManager.getCookie(requestUrl) ?: ""
                    if (currentCookies.contains("cf_clearance")) {
                        if (cfClearanceAt == null) {
                            cfClearanceAt = System.currentTimeMillis()
                        }
                        // Give CF 500ms to fire its auto-redirect after the
                        // cookie lands. If it doesn't, fall through to the
                        // force-navigation check below.
                        delay(500)
                        val urlAfterCookie = runCatching { webView.url }.getOrNull()
                        if (urlAfterCookie == null ||
                            !urlAfterCookie.contains("challenge", ignoreCase = true) &&
                            !urlAfterCookie.contains("__cf_chl", ignoreCase = true) &&
                            !urlAfterCookie.contains("cdn-cgi/challenge-platform", ignoreCase = true)
                        ) {
                            break
                        }
                    }

                    // Force re-navigation: cf_clearance has been set but
                    // the page is still on the challenge URL. Manually
                    // load the original URL — this is the trawl trick that
                    // breaks "cookie set but page stuck" loops.
                    if (cfClearanceAt != null &&
                        System.currentTimeMillis() - cfClearanceAt!! >=
                        FORCE_NAVIGATION_DELAY.inWholeMilliseconds
                    ) {
                        runCatching {
                            webView.loadUrl(requestUrl, headersMap)
                        }
                        delay(1_000)  // give the navigation a moment to land
                        break
                    }

                    // (b) Title-based completion: if the title is no
                    // longer a challenge title, we're done. This catches
                    // challenges that set the cookie via a redirect that
                    // doesn't change the visible URL.
                    val title = runCatching { webView.title }.getOrNull() ?: ""
                    if (title.isNotBlank() &&
                        CHALLENGE_TITLE_MARKERS.none {
                            title.contains(it, ignoreCase = true)
                        } &&
                        !currentUrl.isNullOrBlank() &&
                        currentUrl != requestUrl &&
                        !currentUrl.contains("challenge", ignoreCase = true) &&
                        !currentUrl.contains("__cf_chl", ignoreCase = true) &&
                        !currentUrl.contains("cdn-cgi/challenge-platform", ignoreCase = true)
                    ) {
                        // Title looks legit and URL has changed — give the
                        // cookie a moment to settle, then break.
                        delay(300)
                        break
                    }

                    // (c) URL changed away from challenge URL (legacy
                    // signal — kept for compatibility with CF configs that
                    // don't change the title).
                    if (currentUrl != null && currentUrl != lastUrl &&
                        !currentUrl.contains("challenge", ignoreCase = true) &&
                        !currentUrl.contains("__cf_chl", ignoreCase = true) &&
                        !currentUrl.contains("cdn-cgi/challenge-platform", ignoreCase = true) &&
                        IP_BLOCKED_URL_MARKERS.none {
                            currentUrl.contains(it, ignoreCase = true)
                        }
                    ) {
                        lastUrl = currentUrl
                        // Give the cookie a brief moment to be set after
                        // the redirect lands.
                        delay(300)
                        break
                    }

                    // Attempt to click the Turnstile checkbox every 3 s.
                    // This is a no-op for non-interactive ("managed")
                    // challenges but is what solves interactive Turnstile
                    // widgets. Same cadence as trawl's challengeWait.ts.
                    val now = System.currentTimeMillis()
                    if (now - lastClickAttempt >= 3_000) {
                        lastClickAttempt = now
                        runCatching {
                            webView.evaluateJavascript(TURNSTILE_CLICK_JS, null)
                        }
                    }
                }
            } finally {
                runCatching { webView.stopLoading() }
                // Reset the WebViewClient to a fresh instance before
                // destroy() so the WebView doesn't hold a strong ref to
                // the (potentially activity-leaking) anonymous WebViewClient
                // instance via the outer class reference.
                runCatching { webView.webViewClient = WebViewClient() }
                runCatching { webView.removeAllViews() }
                runCatching { webView.destroy() }
            }
        }
    }

    private data class ChallengeInfo(
        val isCloudflare: Boolean,
        val bodyText: String? = null,
    )
}
