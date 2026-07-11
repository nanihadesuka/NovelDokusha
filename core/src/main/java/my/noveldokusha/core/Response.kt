package my.noveldokusha.core

import my.noveldokusha.core.Response.Error
import my.noveldokusha.core.Response.Success

sealed class Response<out T> {
    data class Success<out T>(val data: T) : Response<T>()
    data class Error(val message: String, val exception: Exception) : Response<Nothing>()

    fun toSuccessOrNull(): Success<T>? = when (this) {
        is Error -> null
        is Success -> this
    }

    inline fun onSuccess(call: (T) -> Unit) = apply {
        if (this is Success) call(data)
    }

    inline fun onError(call: (Error) -> Unit) = apply {
        if (this is Error) call(this)
    }
}

fun <T> Response<T>.getOrNull() = toSuccessOrNull()?.data

suspend inline fun <T, R> Response<T>.map(crossinline call: suspend (T) -> R): Response<R> =
    when (this) {
        is Error -> this
        is Success -> Success(call(data))
    }

inline fun <T, R> Response<T>.syncMap(crossinline call: (T) -> R): Response<R> =
    when (this) {
        is Error -> this
        is Success -> Success(call(data))
    }

@Suppress("unused")
suspend inline fun <T> Response<T>.mapError(crossinline call: suspend (Error) -> T): Response<T> =
    when (this) {
        is Error -> Success(call(this))
        is Success -> Success(data)
    }

@Suppress("unused")
suspend inline fun <T, R> Response<T>.flatMap(crossinline call: suspend (T) -> Response<R>): Response<R> =
    when (this) {
        is Error -> this
        is Success -> call(data)
    }

suspend inline fun <T> Response<T>.flatMapError(crossinline call: suspend (Error) -> Response<T>): Response<T> =
    when (this) {
        is Error -> call(this)
        is Success -> this
    }

fun <T> Response<T?>.asNotNull(): Response<T> = when (this) {
    is Error -> this
    is Success -> if (data == null) Error("null", NullPointerException()) else Success(data)
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

@Suppress("unused")
fun <T> Response<T>.toResult() = when (this) {
    is Error -> Result.failure<T>(exception)
    is Success -> Result.success(this)
}