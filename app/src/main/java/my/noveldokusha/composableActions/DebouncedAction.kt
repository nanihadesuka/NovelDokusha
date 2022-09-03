package my.noveldokusha.composableActions

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import kotlinx.coroutines.delay

@Composable
fun debouncedAction(waitMillis: Long = 250, action: () -> Unit): () -> Unit {
    var ready by rememberSaveable { mutableStateOf(true) }
    LaunchedEffect(ready, waitMillis, action) {
        delay(waitMillis)
        ready = true
    }

    return {
        if (ready) {
            action()
        }
        ready = false
    }
}