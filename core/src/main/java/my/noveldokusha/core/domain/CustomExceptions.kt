package my.noveldokusha.core.domain

import java.io.IOException


class WebViewCookieManagerInitializationFailedException :
    IOException("Webview cookies not found for websited")

class CloudfareVerificationBypassFailedException :
    IOException("Cloudfare verification failed")
