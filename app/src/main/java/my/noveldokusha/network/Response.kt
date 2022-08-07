package my.noveldokusha.network

sealed class Response<T> {
    class Success<T>(val data: T) : Response<T>()
    class Error<T>(val message: String) : Response<T>()

    fun toSuccessOrNull(): Success<T>? = when (this) {
        is Error -> null
        is Success -> this
    }
}

