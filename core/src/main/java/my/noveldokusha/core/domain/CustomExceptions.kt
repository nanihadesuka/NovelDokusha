package my.noveldokusha.core.domain

import java.io.IOException


class WebViewCookieManagerInitializationFailedException(
    message: String = "Webview cookies not found for website"
) : IOException(message)

class CloudfareVerificationBypassFailedException(
    message: String = "Cloudfare verification failed"
) : IOException(message)
