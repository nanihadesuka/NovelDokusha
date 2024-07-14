package my.noveldokusha.network

import android.net.Uri
import java.net.URLEncoder

fun String.toUrlBuilderSafe(): Uri.Builder = toUrl()?.buildUpon()!!
fun String.toUrl(): Uri? = runCatching { Uri.parse(this) }.getOrNull()
fun String.toUrlBuilder(): Uri.Builder? = toUrl()?.buildUpon()


fun Uri.Builder.ifCase(case: Boolean, action: Uri.Builder.() -> Uri.Builder) = when {
    case -> action(this)
    else -> this
}

fun Uri.Builder.addPath(vararg path: String) = path.fold(this) { builder, s ->
    builder.appendPath(s)
}

fun Uri.Builder.add(vararg query: Pair<String, Any>) = query.fold(this) { builder, s ->
    builder.appendQueryParameter(s.first, s.second.toString())
}

fun Uri.Builder.add(key: String, value: Any): Uri.Builder =
    appendQueryParameter(key, value.toString())