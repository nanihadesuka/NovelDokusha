package my.noveldokusha.network

import my.noveldokusha.core.Response
import my.noveldokusha.core.flatMapError
import my.noveldokusha.core.flatten
import my.noveldokusha.core.tryAsResponse
import java.net.SocketTimeoutException

suspend fun <T> tryFlatConnect(
    extraErrorInfo: String = "",
    call: suspend () -> Response<T>
): Response<T> = tryAsResponse { call() }.flatten().specifyNetworkErrors(extraErrorInfo)

suspend fun <T> tryConnect(
    extraErrorInfo: String = "",
    call: suspend () -> T
): Response<T> = tryAsResponse { call() }.specifyNetworkErrors(extraErrorInfo)


private suspend fun <T> Response<T>.specifyNetworkErrors(extraErrorInfo: String = "") =
    flatMapError {
        when (it.exception) {
            is SocketTimeoutException -> {
                val error = listOf(
                    "Timeout error.",
                    "",
                    "Info:",
                    extraErrorInfo.ifBlank { "No info" },
                    "",
                    "Message:",
                    it.exception.message
                ).joinToString("\n")

                Response.Error(error, it.exception)
            }
            else -> {
                val error = listOf(
                    "Unknown error.",
                    "",
                    "Info:",
                    extraErrorInfo.ifBlank { "No Info" },
                    "",
                    "Message:",
                    it.exception.message,
                    "",
                    "Stacktrace:",
                    it.exception.stackTraceToString()
                ).joinToString("\n")

                Response.Error(error, it.exception)
            }
        }
    }
