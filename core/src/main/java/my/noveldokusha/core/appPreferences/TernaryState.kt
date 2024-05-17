package my.noveldokusha.core.appPreferences

enum class TernaryState {
    active,
    inverse,
    inactive;

    fun next() = when (this) {
        active -> inverse
        inverse -> inactive
        inactive -> active
    }
}