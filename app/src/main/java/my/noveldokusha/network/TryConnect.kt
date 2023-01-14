package my.noveldokusha.network

import my.noveldokusha.data.Response
import my.noveldokusha.data.flatten
import my.noveldokusha.data.mapError
import my.noveldokusha.utils.tryAsResult
import java.net.SocketTimeoutException

suspend fun <T> tryConnect(
    extraErrorInfo: String = "",
    call: suspend () -> Response<T>
): Response<T> =
    tryAsResult { call() }.flatten()
        .mapError {
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