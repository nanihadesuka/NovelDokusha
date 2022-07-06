package my.noveldokusha

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.utils.*
import javax.inject.Inject

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

    val THEME_ID = object: Preference<Int>("THEME_ID") {
        override var value by SharedPreference_Int(name, preferences, R.style.AppTheme_Light)
    }
    val THEME_FOLLOW_SYSTEM = object : Preference<Boolean>("THEME_FOLLOW_SYSTEM"){
        override var value by SharedPreference_Boolean(name,preferences, true)
    }
    val READER_FONT_SIZE = object : Preference<Float>("READER_FONT_SIZE"){
        override var value by SharedPreference_Float(name,preferences, 14f)
    }
    val READER_FONT_FAMILY = object : Preference<String>("READER_FONT_FAMILY"){
        override var value by SharedPreference_String(name,preferences, "serif")
    }
    val CHAPTERS_SORT_ASCENDING = object : Preference<TERNARY_STATE>("CHAPTERS_SORT_ASCENDING"){
        override var value by SharedPreference_Enum(name,preferences, TERNARY_STATE.active) { enumValueOf(it) }
    }
    val SOURCES_LANGUAGES = object : Preference<Set<String>>("SOURCES_LANGUAGES"){
        override var value by SharedPreference_StringSet(name,preferences, setOf("English"))
    }
    val LIBRARY_FILTER_READ = object : Preference<TERNARY_STATE>("LIBRARY_FILTER_READ"){
        override var value by SharedPreference_Enum(name,preferences, TERNARY_STATE.inactive) { enumValueOf(it) }
    }
    val LIBRARY_SORT_READ = object : Preference<TERNARY_STATE>("LIBRARY_SORT_READ"){
        override var value by SharedPreference_Enum(name,preferences, TERNARY_STATE.inverse) { enumValueOf(it) }
    }
    val BOOKS_LIST_LAYOUT_MODE = object : Preference<LIST_LAYOUT_MODE>("BOOKS_LIST_LAYOUT_MODE"){
        override var value by SharedPreference_Enum(name,preferences, LIST_LAYOUT_MODE.verticalGrid) { enumValueOf(it) }
    }

    enum class TERNARY_STATE
    {
        active, inverse, inactive;
        fun next() = when (this)
        {
            active -> inverse
            inverse -> inactive
            inactive -> active
        }
    }

    enum class LIST_LAYOUT_MODE
    { verticalList, verticalGrid }

    abstract inner class Preference<T>(val name: String)
    {
        abstract var value: T
        fun flow() = toFlow(name) { value }
        fun state(scope: CoroutineScope) = toState(scope, name) { value }
    }

    /**
     * Given a key, returns a flow of values of the mapper if that key preference
     * had any change.
     * Notice: will always return an initial value.
     */
    fun <T> toFlow(key: String, mapper: (String) -> T): Flow<T>
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

    fun <T> toState(scope: CoroutineScope, key:String, mapper: (String) -> T): State<T>
    {
        val state = mutableStateOf(mapper(key))
        scope.launch(Dispatchers.IO) {
            toFlow(key, mapper).collect {
                withContext(Dispatchers.Main){
                    state.value = it
                }
            }
        }
        return state
    }
}
