package my.noveldokusha.feature.local_database.tables

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity
data class Book(
    val title: String,
    @PrimaryKey val url: String,
    val completed: Boolean = false,
    val lastReadChapter: String? = null,
    val inLibrary: Boolean = false,
    val coverImageUrl: String = "",
    val description: String = "",
    val lastReadEpochTimeMilli: Long = 0,
) : Parcelable