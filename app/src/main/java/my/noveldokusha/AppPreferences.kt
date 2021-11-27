package my.noveldokusha

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import my.noveldokusha.uiUtils.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KProperty

class AppPreferences @Inject constructor(
    @ApplicationContext val context: Context
)
{
    companion object
    {
        val globalThemeListLight = mapOf(
            R.style.AppTheme_Light to "Light"
        )
        val globalThemeListDark = mapOf(
            R.style.AppTheme_BaseDark_Dark to "Dark",
            R.style.AppTheme_BaseDark_Grey to "Grey",
            R.style.AppTheme_BaseDark_Black to "Black"
        )
    }
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val preferencesChangeListeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    var THEME_ID by SharedPreference_Int(preferences, R.style.AppTheme_Light)
    var THEME_FOLLOW_SYSTEM by SharedPreference_Boolean(preferences, true)
    var READER_FONT_SIZE by SharedPreference_Float(preferences, 14f)
    var READER_FONT_FAMILY by SharedPreference_String(preferences, "serif")
    var CHAPTERS_SORT_ASCENDING by SharedPreference_Enum(preferences, TERNARY_STATE.active) { enumValueOf(it) }
    var SOURCES_LANGUAGES by SharedPreference_StringSet(preferences, setOf("English"))

    fun THEME_ID_flow() = toFlow(::THEME_ID.name) { THEME_ID }
    fun THEME_FOLLOW_SYSTEM_flow() = toFlow(::THEME_FOLLOW_SYSTEM.name) { THEME_FOLLOW_SYSTEM }
    fun READER_FONT_SIZE_flow() = toFlow(::READER_FONT_SIZE.name) { READER_FONT_SIZE }
    fun READER_FONT_FAMILY_flow() = toFlow(::READER_FONT_FAMILY.name) { READER_FONT_FAMILY }
    fun CHAPTERS_SORT_ASCENDING_flow() = toFlow(::CHAPTERS_SORT_ASCENDING.name) { CHAPTERS_SORT_ASCENDING }
    fun SOURCES_LANGUAGES_flow() = toFlow(::SOURCES_LANGUAGES.name) { SOURCES_LANGUAGES }

    enum class TERNARY_STATE
    { active, inverse, inactive }

    fun addListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        preferences.registerOnSharedPreferenceChangeListener(listener)

    /**
     * Given a key, returns a flow of values of the mapper if that key preference
     * had any change.
     * Notice: will always return an initial value.
     */
    private fun <T> toFlow(key: String, mapper: (String) -> T): Flow<T>
    {
        val flow = MutableStateFlow(mapper(key))
        val scope = CoroutineScope(Dispatchers.Default)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, vkey ->
            if (key == vkey)
                scope.launch { flow.value = mapper(vkey) }
        }

        return flow
            .onSubscription {
                preferencesChangeListeners.add(listener)
                preferences.registerOnSharedPreferenceChangeListener(listener)
            }.onCompletion {
                preferencesChangeListeners.remove(listener)
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }.flowOn(Dispatchers.Default)
    }
}
