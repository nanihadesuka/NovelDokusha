package my.noveldokusha.network

import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException

suspend fun <T> tryConnect(
    extraErrorInfo: String = "",
    call: suspend () -> Response<T>
): Response<T> = try {
    call()
} catch (e: SocketTimeoutException) {
    val error = listOf(
        "Timeout error.",
        "",
        "Info:",
        extraErrorInfo.ifBlank { "No info" },
        "",
        "Message:",
        e.message
    ).joinToString("\n")

    Response.Error(error, e)
} catch (e: Exception) {
    val error = listOf(
        "Unknown error.",
        "",
        "Info:",
        extraErrorInfo.ifBlank { "No Info" },
        "",
        "Message:",
        e.message,
        "",
        "Stacktrace:",
        e.stackTraceToString()
    ).joinToString("\n")

    Response.Error(error, e)
}