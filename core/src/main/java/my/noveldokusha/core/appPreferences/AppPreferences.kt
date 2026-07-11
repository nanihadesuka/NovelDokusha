@file:Suppress("PropertyName")

package my.noveldokusha.core.appPreferences

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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import my.noveldokusha.core.LanguageCode
import my.noveldokusha.core.SharedPreference_Boolean
import my.noveldokusha.core.SharedPreference_Enum
import my.noveldokusha.core.SharedPreference_Float
import my.noveldokusha.core.SharedPreference_Int
import my.noveldokusha.core.SharedPreference_Serializable
import my.noveldokusha.core.SharedPreference_String
import my.noveldokusha.core.SharedPreference_StringSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext val context: Context,
) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val preferencesChangeListeners =
        java.util.Collections.synchronizedSet(mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>())

    val THEME_ID = object : Preference<PreferenceThemes>("THEME_ID") {
        override var value by SharedPreference_Enum(name, preferences, PreferenceThemes.Light) {
            enumValueOf(it)
        }
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

    val READER_FULL_SCREEN = object : Preference<Boolean>("READER_FULL_SCREEN") {
        override var value by SharedPreference_Boolean(name, preferences, true)
    }

    val CHAPTERS_SORT_ASCENDING = object : Preference<TernaryState>("CHAPTERS_SORT_ASCENDING") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            TernaryState.Active
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
    val LIBRARY_FILTER_READ = object : Preference<TernaryState>("LIBRARY_FILTER_READ") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            TernaryState.Inactive
        ) { enumValueOf(it) }
    }
    val LIBRARY_SORT_LAST_READ = object : Preference<TernaryState>("LIBRARY_SORT_LAST_READ") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            TernaryState.Inverse
        ) { enumValueOf(it) }
    }
    val BOOKS_LIST_LAYOUT_MODE = object : Preference<ListLayoutMode>("BOOKS_LIST_LAYOUT_MODE") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            ListLayoutMode.VerticalGrid
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

    val GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_ENABLED =
        object : Preference<Boolean>("GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_ENABLED") {
            override var value by SharedPreference_Boolean(name, preferences, true)
        }

    val GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_INTERVAL_HOURS =
        object : Preference<Int>("GLOBAL_APP_AUTOMATIC_LIBRARY_UPDATES_INTERVAL_HOURS") {
            override var value by SharedPreference_Int(name, preferences, 24)
        }

    // ── Gemini AI Translation settings ──────────────────────────────────

    val GEMINI_API_KEY = object : Preference<String>("GEMINI_API_KEY") {
        override var value by SharedPreference_String(name, preferences, "")
    }

    val GEMINI_MODEL = object : Preference<String>("GEMINI_MODEL") {
        override var value by SharedPreference_String(name, preferences, "gemini-2.5-flash")
    }

    val GEMINI_TEMPERATURE = object : Preference<Float>("GEMINI_TEMPERATURE") {
        override var value by SharedPreference_Float(name, preferences, 0.55f)
    }

    // ── Cloud Translation Provider settings (ported from NoveLA) ────────

    /**
     * Active translation provider:
     * "GOOGLE_PA"   — Google Translate Enhanced (default, auto-fetches API key)
     * "GOOGLE_FREE" — Google Translate Free (no key needed)
     * "GEMINI"      — Google Gemini API (requires GEMINI_API_KEY)
     * "OPENAI"      — OpenAI-compatible API (requires TRANSLATION_OPENAI_API_KEYS)
     */
    val TRANSLATION_PROVIDER = object : Preference<String>("TRANSLATION_PROVIDER") {
        override var value by SharedPreference_String(name, preferences, "GOOGLE_PA")
    }

    val TRANSLATION_PROMPT_PRESET = object : Preference<String>("TRANSLATION_PROMPT_PRESET") {
        override var value by SharedPreference_String(name, preferences, "Balanced (Default)")
    }

    val TRANSLATION_BATCH_SIZE = object : Preference<Int>("TRANSLATION_BATCH_SIZE") {
        override var value by SharedPreference_Int(name, preferences, 15)
    }

    val TRANSLATION_MAX_OUTPUT_TOKENS = object : Preference<Int>("TRANSLATION_MAX_OUTPUT_TOKENS") {
        override var value by SharedPreference_Int(name, preferences, 8192)
    }



    // ── Additional translation provider prefs (NoveLA-compatible names) ──

    val TRANSLATION_ACTIVE_SYSTEM_PROMPT = object : Preference<String>("TRANSLATION_ACTIVE_SYSTEM_PROMPT") {
        override var value by SharedPreference_String(name, preferences, "")
    }

    val TRANSLATION_GEMINI_API_KEY = object : Preference<String>("TRANSLATION_GEMINI_API_KEY") {
        override var value by SharedPreference_String(name, preferences, "")
    }

    val TRANSLATION_GEMINI_MODEL = object : Preference<String>("TRANSLATION_GEMINI_MODEL") {
        override var value by SharedPreference_String(name, preferences, "gemini-2.5-flash")
    }

    val TRANSLATION_GOOGLE_PA_API_KEYS = object : Preference<String>("TRANSLATION_GOOGLE_PA_API_KEYS") {
        override var value by SharedPreference_String(name, preferences, "")
    }

    val TRANSLATION_GOOGLE_PA_CACHED_KEY = object : Preference<String>("TRANSLATION_GOOGLE_PA_CACHED_KEY") {
        override var value by SharedPreference_String(name, preferences, "")
    }

    val TRANSLATION_GOOGLE_PA_KEY_LAST_CHECKED = object : Preference<String>("TRANSLATION_GOOGLE_PA_KEY_LAST_CHECKED") {
        override var value by SharedPreference_String(name, preferences, "0")
    }

    val TRANSLATION_OPENAI_API_KEYS = object : Preference<String>("TRANSLATION_OPENAI_API_KEYS") {
        override var value by SharedPreference_String(name, preferences, "")
    }

    val TRANSLATION_OPENAI_BASE_URL = object : Preference<String>("TRANSLATION_OPENAI_BASE_URL") {
        override var value by SharedPreference_String(name, preferences, "https://api.openai.com/v1")
    }

    val TRANSLATION_OPENAI_MODEL = object : Preference<String>("TRANSLATION_OPENAI_MODEL") {
        override var value by SharedPreference_String(name, preferences, "gpt-4o-mini")
    }

    val TRANSLATION_PROMPT_USE_ENGLISH_LOCALE = object : Preference<Boolean>("TRANSLATION_PROMPT_USE_ENGLISH_LOCALE") {
        override var value by SharedPreference_Boolean(name, preferences, false)
    }

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    val LOCAL_SOURCES_URI_DIRECTORIES =
        object : Preference<Set<String>>("LOCAL_SOURCES_URI_DIRECTORIES") {
            override var value by SharedPreference_StringSet(name, preferences, setOf())
        }

    @Deprecated("Removed", level = DeprecationLevel.HIDDEN)
    val LIBRARY_SORT_READ = object : Preference<TernaryState>("LIBRARY_SORT_READ") {
        override var value by SharedPreference_Enum(
            name,
            preferences,
            TernaryState.Active
        ) { enumValueOf(it) }
    }

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
    private fun <T> toFlow(key: String, mapper: (String) -> T): Flow<T> {
        val flow = MutableStateFlow(mapper(key))
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, vkey ->
            if (key == vkey)
                flow.value = mapper(vkey)
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
    private fun <T> toState(
        scope: CoroutineScope,
        key: String,
        mapper: (String) -> T,
        setter: (T) -> Unit
    ): MutableState<T> = object : MutableState<T> {

        private val internalValue = mutableStateOf(mapper(key))
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