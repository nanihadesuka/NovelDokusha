package my.noveldokusha

import android.util.Log


class LogClass(private val head: String) {
    infix fun see(body: Any?) = Log.e(head, body.toString())
}

fun log(head: String, bodies: LogClass.() -> Unit) {
    bodies(LogClass(head))
}
