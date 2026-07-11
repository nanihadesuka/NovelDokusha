package my.noveldokusha.feature.local_database.tables

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stores all translated paragraphs for a chapter + language pair as a single row.
 * translatedParagraphs is a JSON array of translated body paragraphs.
 * titleTranslation is the translated chapter title (empty = not translated).
 */
@Entity(
    indices = [
        Index(value = ["chapterUrl", "sourceLang", "targetLang"], unique = true)
    ]
)
data class ChapterTranslation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterUrl: String,
    val sourceLang: String,
    val targetLang: String,
    val translatedParagraphs: String,
    val titleTranslation: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
