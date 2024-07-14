package my.noveldokusha.core.utils

import android.os.Binder
import android.os.Bundle
import android.os.Parcelable
import android.util.Size
import android.util.SizeF
import android.util.SparseArray
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.compose.saveable
import java.io.Serializable

fun <T> SavedStateHandle.asMutableStateOf(key: String, default: () -> T): MutableState<T> =
    object : MutableState<T> by saveable(key = key, init = { mutableStateOf(default()) }) {}


fun <T : Any, A : SnapshotStateList<T>> SavedStateHandle.asMutableListStateOf(
    key: String,
    default: () -> A
): A {
    val saver = saverList<T>()

    @Suppress("UNCHECKED_CAST")
    saver as Saver<A, Any>
    // value is restored using the SavedStateHandle or created via [init] lambda
    @Suppress("DEPRECATION") // Bundle.get has been deprecated in API 31
    val value = get<Bundle?>(key)?.get("value")?.let(saver::restore) ?: default()

    // Hook up saving the state to the SavedStateHandle
    setSavedStateProvider(key) {
        bundleOf("value" to with(saver) {
            SaverScope(Validators::validateValue).save(value)
        })
    }
    return value
}


private fun <T : Any> saverList() = listSaver<SnapshotStateList<T>, T>(
    save = { stateList ->
        if (stateList.isNotEmpty()) {
            val first = stateList.first()
            if (!canBeSaved(first)) {
                throw IllegalStateException("${first::class} cannot be saved. By default only types which can be stored in the Bundle class can be saved.")
            }
        }
        stateList.toList()
    },
    restore = { it.toMutableStateList() }
)

// Copied from Android's source code as it has inter-libraries access restrictions
private object Validators {

    fun validateValue(value: Any?): Boolean {
        if (value == null) {
            return true
        }
        for (cl in ACCEPTABLE_CLASSES) {
            if (cl!!.isInstance(value)) {
                return true
            }
        }
        return false
    }

    // doesn't have Integer, Long etc box types because they are "Serializable"
    private val ACCEPTABLE_CLASSES = arrayOf( // baseBundle
        Boolean::class.javaPrimitiveType,
        BooleanArray::class.java,
        Double::class.javaPrimitiveType,
        DoubleArray::class.java,
        Int::class.javaPrimitiveType,
        IntArray::class.java,
        Long::class.javaPrimitiveType,
        LongArray::class.java,
        String::class.java,
        Array<String>::class.java, // bundle
        Binder::class.java,
        Bundle::class.java,
        Byte::class.javaPrimitiveType,
        ByteArray::class.java,
        Char::class.javaPrimitiveType,
        CharArray::class.java,
        CharSequence::class.java,
        Array<CharSequence>::class.java,
        // type erasure ¯\_(ツ)_/¯, we won't eagerly check elements contents
        ArrayList::class.java,
        Float::class.javaPrimitiveType,
        FloatArray::class.java,
        Parcelable::class.java,
        Array<Parcelable>::class.java,
        Serializable::class.java,
        Short::class.javaPrimitiveType,
        ShortArray::class.java,
        SparseArray::class.java,
        Size::class.java,
        SizeF::class.java
    )
}
