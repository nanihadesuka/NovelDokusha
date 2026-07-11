package my.noveldokusha.core

interface AppInternalState {
    val isDebugMode: Boolean
    val versionCode: Int
    val versionName: String
}