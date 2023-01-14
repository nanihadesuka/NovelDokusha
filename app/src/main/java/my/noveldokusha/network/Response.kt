package my.noveldokusha.network

sealed class Response<out T> {
    data class Success<out T>(val data: T) : Response<T>()
    data class Error(val message: String, val exception: Exception) : Response<Nothing>()

    fun toSuccessOrNull(): Success<T>? = when (this) {
        is Error -> null
        is Success -> this
    }
}

suspend inline fun <T, R> Response<T>.map(crossinline call: suspend (T) -> R): Response<R> = when (this) {
    is Response.Error -> this
    is Response.Success -> Response.Success(call(data))
}

