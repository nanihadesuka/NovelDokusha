package my.noveldokusha.utils

import my.noveldokusha.data.Response
import kotlin.coroutines.cancellation.CancellationException

suspend inline fun <T> tryAsResult(crossinline call: suspend () -> T): Response<T> = try {
    Response.Success(call())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Response.Error(e.message ?: "", e)
}