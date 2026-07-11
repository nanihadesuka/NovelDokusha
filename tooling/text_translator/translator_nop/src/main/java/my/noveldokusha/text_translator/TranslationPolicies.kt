package my.noveldokusha.text_translator

import kotlinx.coroutines.delay

internal data class RetryPolicy(
    val maxAttempts: Int,
    val baseDelayMs: Long = 0L,
    val maxDelayMs: Long = Long.MAX_VALUE,
) {
    suspend fun backoff(attempt: Int) {
        if (attempt >= maxAttempts - 1 || baseDelayMs <= 0L) return
        val delayMs = (baseDelayMs * (attempt + 1)).coerceAtMost(maxDelayMs)
        delay(delayMs)
    }
}
