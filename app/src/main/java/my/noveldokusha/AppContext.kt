package my.noveldokusha

val String.isLocalUri: Boolean get() = startsWith("local://")
val String.isHttpsUrl: Boolean get() = startsWith("http://", true) || startsWith("https://", true)
val String.removeLocalUriPrefix: String get() = removePrefix("local://")
val String.addLocalUriPrefix: String get() = "local://${this}"

val String.isContentUri: Boolean get() = startsWith("content://")
