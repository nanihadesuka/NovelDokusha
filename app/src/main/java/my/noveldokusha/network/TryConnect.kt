package my.noveldokusha.network

import my.noveldokusha.data.Response
import my.noveldokusha.data.flatMapError
import my.noveldokusha.data.flatten
import my.noveldokusha.utils.tryAsResponse
import java.net.SocketTimeoutException

suspend fun <T> tryConnect(
    extraErrorInfo: String = "",
    call: suspend () -> Response<T>
): Response<T> =
    tryAsResponse { call() }
        .flatten()
        .flatMapError {
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