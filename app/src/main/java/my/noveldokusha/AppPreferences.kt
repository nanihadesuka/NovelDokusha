@file:Suppress("PropertyName")

package my.noveldokusha

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import my.noveldokusha.scraper.LanguageCode
import my.noveldokusha.utils.SharedPreference_Boolean
import my.noveldokusha.utils.SharedPreference_Enum
import my.noveldokusha.utils.SharedPreference_Float
import my.noveldokusha.utils.SharedPreference_Int
import my.noveldokusha.utils.SharedPreference_Serializable
import my.noveldokusha.utils.SharedPreference_String
import my.noveldokusha.utils.SharedPreference_StringSet
import javax.inject.Inject

@Serializable
data class VoicePredefineState(
    val savedName: String,
    val voiceId: String,
    val pitch: Float,
    val speed: Float
)

class AppPreferences @Inject constructor(
    @ApplicationContext val context: Context
) {
    companion object {
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
    private val preferencesChangeListeners =
        mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    val THEME_ID = object : Preference<Int>("THEME_ID") {
        override var value by SharedPreference_Int(name, preferences, R.style.AppTheme_Light)
    }
    val THEME_FOLLOW_SYSTEM = object : Preference<Boolean>("THEME_FOLLOW_SYSTEM") {
        override var value by SharedPreference_Boolean(name, preferences, true)
    }
    val READER_FONT_SIZE = object : Preference<Float>("READER_FONT_SIZE") {
        override var value by SharedPreference_Float(name, preferences, 14f)
    }
    val READER_FONT_FAMILY = object : Preference<String>("READER_FONT_FAMILY") {
        override var value by SharedPreference_String(name, preferences, "serif")
    }
    val READER_TEXT_TO_SPEECH_VOICE_ID =
        object : Preference<String>("READER_TEXT_TO_SPEECH_VOICE_ID") {
            override var value by SharedPreference_String(name, preferences, "")
        }
    val READER_TEXT_TO_SPEECH_VOICE_SPEED =
        object : Preference<Float>("READER_TEXT_TO_SPEECH_VOICE_SPEED") {
            override var value by SharedPreference_Float(name, preferences, 1f)
        }
    val READER_TEXT_TO_SPEECH_VOICE_PITCH =
        object : Preference<Float>("READER_TEXT_TO_SPEECH_VOICE_PITCH") {
            override var value by SharedPreference_Float(name, preferences, 1f)
        }

    val READER_TEXT_TO_SPEECH_SAVED_PREDEFINED_LIST =
        object : Preference<List<VoicePredefineState>>(
            "READER_TEXT_TO_SPEECH_SAVED_PREDEFINED_LIST"
        ) {
            override var value by SharedPreference_Serializable<List<VoicePredefineState>>(
                name = name,
                sharedPreferences = preferences,
                defaultValue = listOf(),
                encode = { Json.encodeToString(it) },
                decode = { Json.decodeFromString(it) }
            )
        }

    val READER_SELECTABLE_TEXT = object : Preference<Boolean>("READER_SELECTABLE_TEXT") {
        override var value by SharedPreference_Boolean(name, preferences, false)
    }

    val READER_KEEP_SCREEN_ON = object : Preference<Boolean>("READER_KEEP_SCREEN_ON") {
        override var value by SharedPreference_Boolean(name, preferences, false)
    }

    val CHAPTERS_SORT_ASCENDING = object : Preference<TERNARY_STATE>("CHAPTERS_SORT_ASCENDING") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            TERNARY_STATE.active
        ) { enumValueOf(it) }
    }
    val SOURCES_LANGUAGES_ISO639_1 = object : Preference<Set<String>>("SOURCES_LANGUAGES") {
        override var value by SharedPreference_StringSet(
            name,
            preferences,
            setOf(LanguageCode.ENGLISH.iso639_1)
        )
    }
    val FINDER_SOURCES_PINNED = object : Preference<Set<String>>("FINDER_SOURCES_PINNED") {
        override var value by SharedPreference_StringSet(name, preferences, setOf())
    }
    val LIBRARY_FILTER_READ = object : Preference<TERNARY_STATE>("LIBRARY_FILTER_READ") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            TERNARY_STATE.inactive
        ) { enumValueOf(it) }
    }
    val LIBRARY_SORT_LAST_READ = object : Preference<TERNARY_STATE>("LIBRARY_SORT_LAST_READ") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            TERNARY_STATE.inverse
        ) { enumValueOf(it) }
    }
    val BOOKS_LIST_LAYOUT_MODE = object : Preference<LIST_LAYOUT_MODE>("BOOKS_LIST_LAYOUT_MODE") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            LIST_LAYOUT_MODE.verticalGrid
        ) { enumValueOf(it) }
    }
    val GLOBAL_TRANSLATION_ENABLED = object : Preference<Boolean>("GLOBAL_TRANSLATION_ENABLED") {
        override var value by SharedPreference_Boolean(name, preferences, false)
    }
    val GLOBAL_TRANSLATION_PREFERRED_SOURCE =
        object : Preference<String>("GLOBAL_TRANSLATIOR_PREFERRED_SOURCE") {
            override var value by SharedPreference_String(name, preferences, "en")
        }
    val GLOBAL_TRANSLATION_PREFERRED_TARGET =
        object : Preference<String>("GLOBAL_TRANSLATION_PREFERRED_TARGET") {
            override var value by SharedPreference_String(name, preferences, "")
        }

    val GLOBAL_APP_UPDATER_CHECKER_ENABLED =
        object : Preference<Boolean>("GLOBAL_APP_UPDATER_CHECKER_ENABLED") {
            override var value by SharedPreference_Boolean(name, preferences, true)
        }

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    val LOCAL_SOURCES_URI_DIRECTORIES =
        object : Preference<Set<String>>("LOCAL_SOURCES_URI_DIRECTORIES") {
            override var value by SharedPreference_StringSet(name, preferences, setOf())
        }

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    val LIBRARY_SORT_READ = object : Preference<TERNARY_STATE>("LIBRARY_SORT_READ") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            TERNARY_STATE.active
        ) { enumValueOf(it) }
    }

    enum class TERNARY_STATE {
        active,
        inverse,
        inactive;

        fun next() = when (this) {
            active -> inverse
            inverse -> inactive
            inactive -> active
        }
    }

    enum class LIST_LAYOUT_MODE { verticalList, verticalGrid }

    abstract inner class Preference<T>(val name: String) {
        abstract var value: T
        fun flow() = toFlow(name) { value }.flowOn(Dispatchers.IO)
        fun state(scope: CoroutineScope) = toState(
            scope = scope, key = name, mapper = { value }, setter = { value = it }
        )
    }

    /**
     * Given a key, returns a flow of values of the mapper if that key preference
     * had any change.
     * Notice: will always return an initial value.
     */
    fun <T> toFlow(key: String, mapper: (String) -> T): Flow<T> {
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

    /**
     * This custom implementation has probably some details wrong
     * Use only OUTSIDE of composable scope (e.g. viewModel)
     */
    fun <T> toState(
        scope: CoroutineScope,
        key: String,
        mapper: (String) -> T,
        setter: (T) -> Unit
    ): MutableState<T> = object : MutableState<T> {

        private val internalValue = mutableStateOf<T>(mapper(key))
        override var value: T
            get() = internalValue.value
            set(newValue) {
                if (internalValue.value != newValue) {
                    setter(newValue)
                }
            }

        init {
            scope.launch(Dispatchers.IO) {
                toFlow(key, mapper).collect {
                    withContext(Dispatchers.Main) {
                        internalValue.value = it
                    }
                }
            }
        }

        override fun component1(): T = value
        override fun component2() = ::value::set
    }
}
