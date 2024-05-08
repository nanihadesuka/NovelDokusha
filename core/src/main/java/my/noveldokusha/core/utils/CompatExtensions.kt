package my.noveldokusha.core.utils

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.os.Build

fun TaskStackBuilder.getPendingIntentCompat(
    requestCode: Int,
    flags: Int,
    isMutable: Boolean
): PendingIntent = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val mutable = if (isMutable) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_IMMUTABLE
        getPendingIntent(requestCode, flags or mutable)
    }
    else -> {
        getPendingIntent(requestCode, flags)
    }
}
