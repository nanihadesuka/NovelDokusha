package my.noveldokusha.data

import my.noveldokusha.data.Response.Error
import my.noveldokusha.data.Response.Success

sealed class Response<out T> {
    data class Success<out T>(val data: T) : Response<T>()
    data class Error(val message: String, val exception: Exception) : Response<Nothing>()

    fun toSuccessOrNull(): Success<T>? = when (this) {
        is Error -> null
        is Success -> this
    }

    inline fun onSuccess(call: (Success<T>) -> Unit) = apply {
        if (this is Success) call(this)
    }

    inline fun onError(call: (Error) -> Unit) = apply {
        if (this is Error) call(this)
    }
}

suspend inline fun <T, R> Response<T>.map(crossinline call: suspend (T) -> R): Response<R> =
    when (this) {
        is Error -> this
        is Success -> Success(call(data))
    }

suspend inline fun <T> Response<T>.mapError(crossinline call: suspend (Error) -> Error): Response<T> =
    when (this) {
        is Error -> call(this)
        is Success -> this
    }

fun <T> Response<Response<T>>.flatten(): Response<T> =
    when (this) {
        is Error -> this
        is Success -> {
            when (this.data) {
                is Error -> this.data
                is Success -> {
                    this.data
                }
            }
        }
    }