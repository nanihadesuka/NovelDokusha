package my.noveldokusha.network

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.asResponseBody
import okhttp3.internal.http.promisesBody
import okio.GzipSource
import okio.buffer
import okio.source
import org.brotli.dec.BrotliInputStream

/**
 * Interceptor to decode/uncompress the a response body
 * with header content-encoding:br
 */
class BrotliDecodeResponseInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        val response = chain.proceed(chain.request())
        if (response.header("Content-Encoding") == "br") {
            return uncompress(response)
        }
        return response
    }

    /**
     * Taken from okhttp-brotli [BrotliInterceptor.kt]
     */
    private fun uncompress(response: Response): Response {
        if (!response.promisesBody()) {
            return response
        }
        val body = response.body ?: return response
        val encoding = response.header("Content-Encoding") ?: return response

        val decompressedSource = when {
            encoding.equals("br", ignoreCase = true) ->
                BrotliInputStream(body.source().inputStream()).source().buffer()
            encoding.equals("gzip", ignoreCase = true) ->
                GzipSource(body.source()).buffer()
            else -> return response
        }

        return response.newBuilder()
            .removeHeader("Content-Encoding")
            .removeHeader("Content-Length")
            .body(decompressedSource.asResponseBody(body.contentType(), -1))
            .build()
    }
}