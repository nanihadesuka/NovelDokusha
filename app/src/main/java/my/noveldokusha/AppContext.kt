package my.noveldokusha

val String.isLocalUri: Boolean get() = startsWith("local://")
val String.isContentUri: Boolean get() = startsWith("content://")
