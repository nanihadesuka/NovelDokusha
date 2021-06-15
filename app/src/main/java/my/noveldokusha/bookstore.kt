package my.noveldokusha

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import my.noveldokusha.scraper.Response
import my.noveldokusha.scraper.downloadChapter
import java.io.InputStream

data class BookMetadata(val title: String, val url: String)
{
	override fun equals(other: Any?): Boolean = if (other is BookMetadata) (url == other.url) else false
	override fun hashCode(): Int = url.hashCode()
}

data class ChapterMetadata(val title: String, val url: String)
{
	override fun equals(other: Any?): Boolean = if (other is ChapterMetadata) (url == other.url) else false
	override fun hashCode(): Int = url.hashCode()
}

object bookstore
{
	
	@Entity
	data class Book(
		val title: String,
		@PrimaryKey val url: String,
		val completed: Boolean = false,
		val lastReadChapter: String? = null,
		val inLibrary: Boolean = false
	)
	
	@Entity
	data class Chapter(
		val title: String,
		@PrimaryKey val url: String,
		val bookUrl: String,
		val position: Int,
		val read: Boolean = false,
		val lastReadPosition: Int = 0,
		val lastReadOffset: Int = 0
	)
	
	@Entity
	data class ChapterBody(@PrimaryKey val url: String, val body: String)
	
	@Dao
	interface LibraryDao
	{
		@Query("SELECT * FROM Book")
		suspend fun getAll(): List<Book>
		
		@Query("SELECT * FROM Book WHERE inLibrary == 1")
		fun booksInLibraryFlow(): Flow<List<Book>>
		
		@Insert(onConflict = OnConflictStrategy.IGNORE)
		suspend fun insert(book: Book)
		
		@Insert(onConflict = OnConflictStrategy.IGNORE)
		suspend fun insert(book: List<Book>)
		
		@Delete
		suspend fun remove(book: Book)
		
		@Update
		suspend fun update(book: Book)
		
		@Query("SELECT * FROM Book WHERE url = :url")
		suspend fun get(url: String): Book?
		
		@Query("SELECT EXISTS(SELECT * FROM Book WHERE url == :url AND inLibrary == 1)")
		suspend fun existInLibrary(url: String): Boolean
		
		@Query("SELECT EXISTS(SELECT * FROM Book WHERE url == :url AND inLibrary == 1)")
		fun existInLibraryFlow(url: String): Flow<Boolean>
		
		data class BookWithContext(@Embedded val book: Book, val chaptersCount: Int, val chaptersReadCount: Int)
		
		@Query(
			"""
			SELECT Book.*, COUNT(Chapter.read) AS chaptersCount, SUM(Chapter.read) AS chaptersReadCount
			FROM Book
			LEFT JOIN Chapter ON Chapter.bookUrl = Book.url
			WHERE Book.inLibrary == 1
			GROUP BY Book.url
		"""
		)
		fun getBooksInLibraryWithContextFlow(): Flow<List<BookWithContext>>
		
		@Query("DELETE FROM Book WHERE inLibrary == 0")
		suspend fun removeAllNonLibraryRows()
	}
	
	@Dao
	interface ChapterDao
	{
		@Query("SELECT * FROM Chapter")
		suspend fun getAll(): List<Chapter>
		
		@Query("SELECT * FROM Chapter WHERE bookUrl = :bookUrl")
		suspend fun chapters(bookUrl: String): List<Chapter>
		
		@Update
		suspend fun update(chapter: Chapter)
		
		@Query("SELECT EXISTS(SELECT * FROM Chapter WHERE Chapter.bookUrl = :bookUrl LIMIT 1)")
		suspend fun hasChapters(bookUrl: String): Boolean
		
		@Query("UPDATE Chapter SET read = 1 WHERE url in (:chaptersUrl)")
		suspend fun setAsRead(chaptersUrl: List<String>)
		
		@Query("UPDATE Chapter SET title = :title WHERE url == :url")
		suspend fun updateTitle(url: String, title: String)
		
		@Query("UPDATE Chapter SET read = 0 WHERE url in (:chaptersUrl)")
		suspend fun setAsUnread(chaptersUrl: List<String>)
		
		@Insert(onConflict = OnConflictStrategy.IGNORE)
		suspend fun insert(chapters: List<Chapter>)
		
		@Insert(onConflict = OnConflictStrategy.REPLACE)
		suspend fun replace(chapters: List<Chapter>)
		
		@Query("SELECT * FROM Chapter WHERE url = :url")
		suspend fun get(url: String): Chapter?
		
		@Query("SELECT COUNT(*) FROM Chapter WHERE bookUrl = :bookUrl AND read = 0")
		fun numberOfUnreadChaptersFlow(bookUrl: String): Flow<Int>
		
		@Query("DELETE FROM Chapter WHERE Chapter.bookUrl NOT IN (SELECT Book.url FROM Book)")
		suspend fun removeAllNonLibraryRows()
		
		data class ChapterWithContext(@Embedded val chapter: Chapter, val downloaded: Boolean, val lastReadChapter: Boolean)
		
		@Query(
			"""
			SELECT Chapter.*, ChapterBody.url IS NOT NULL AS downloaded , Book.lastReadChapter IS NOT NULL AS lastReadChapter
			FROM Chapter
			LEFT JOIN ChapterBody ON ChapterBody.url = Chapter.url
			LEFT JOIN Book ON Book.url = :bookUrl AND Book.lastReadChapter == Chapter.url
			WHERE Chapter.bookUrl == :bookUrl
			ORDER BY position ASC
		"""
		)
		fun getChaptersWithContextFlow(bookUrl: String): Flow<List<ChapterWithContext>>
	}
	
	@Dao
	interface ChapterBodyDao
	{
		@Query("SELECT * FROM ChapterBody")
		suspend fun getAll(): List<ChapterBody>
		
		@Insert(onConflict = OnConflictStrategy.REPLACE)
		suspend fun insert(chapterBody: ChapterBody)
		
		@Insert(onConflict = OnConflictStrategy.REPLACE)
		suspend fun insert(chapterBody: List<ChapterBody>)
		
		@Query("SELECT * FROM ChapterBody WHERE url = :url")
		suspend fun get(url: String): ChapterBody?
		
		@Query("DELETE FROM ChapterBody WHERE ChapterBody.url NOT IN (SELECT Chapter.url FROM Chapter)")
		suspend fun removeAllNonChapterRows()
		
		@Query("DELETE FROM ChapterBody WHERE ChapterBody.url IN (:chaptersUrl)")
		suspend fun removeChapterRows(chaptersUrl: List<String>)
	}
	
	@Database(entities = [Book::class, Chapter::class, ChapterBody::class], version = 3, exportSchema = false)
	abstract class LibraryDatabase : RoomDatabase()
	{
		abstract fun libraryDao(): LibraryDao
		abstract fun chapterDao(): ChapterDao
		abstract fun chapterBodyDao(): ChapterBodyDao
	}
	
	fun migration(vi: Int, vf: Int, migrate: (SupportSQLiteDatabase) -> Unit) = object : Migration(vi, vf)
	{
		override fun migrate(database: SupportSQLiteDatabase) = migrate(database)
	}
	
	fun migrations() = arrayOf(
		migration(1, 2) { it.execSQL("ALTER TABLE Chapter ADD COLUMN position INTEGER NOT NULL DEFAULT 0") },
		migration(2, 3) {
			it.execSQL("ALTER TABLE Book ADD COLUMN inLibrary INTEGER NOT NULL DEFAULT 0")
			it.execSQL("UPDATE Book SET inLibrary = 1")
		}
	)
	
	val db_context: Context by lazy { App.instance.applicationContext }
	val appDB by lazy { DBase(db_context, "bookEntry") }
	
	val settings by lazy { appDB.settings }
	val bookLibrary by lazy { appDB.bookLibrary }
	val bookChapter by lazy { appDB.bookChapter }
	val bookChapterBody by lazy { appDB.bookChapterBody }
	
	private fun createRoom(ctx: Context, name: String) = Room
		.databaseBuilder(ctx, LibraryDatabase::class.java, name)
		.addMigrations(*migrations())
	
	class DBase
	{
		private val db: LibraryDatabase
		val context: Context
		val name: String
		
		constructor(context: Context, name: String)
		{
			this.context = context
			this.name = name
			db = createRoom(context, name)
				.build()
		}
		
		// External database, used for backup restore
		constructor(context: Context, name: String, inputStream: InputStream)
		{
			this.context = context
			this.name = name
			db = createRoom(context, name)
				.createFromInputStream { inputStream }
				.build()
		}
		
		val settings = Settings()
		val bookLibrary = BookLibrary()
		val bookChapter = BookChapter()
		val bookChapterBody = BookChapterBody()
		
		fun getDatabaseSizeBytes() = context.getDatabasePath(name).length()
		fun close() = db.close()
		fun delete() = context.deleteDatabase(name)
		fun clearAllTables() = db.clearAllTables()
		
		suspend fun <T> withTransaction(fn: suspend () -> T) = db.withTransaction(fn)
		
		inner class Settings
		{
			suspend fun clearNonLibraryData()
			{
				db.libraryDao().removeAllNonLibraryRows()
				db.chapterDao().removeAllNonLibraryRows()
				db.chapterBodyDao().removeAllNonChapterRows()
			}
			
			fun clearNonLibraryDataFlow() = flow {
				db.libraryDao().removeAllNonLibraryRows()
				db.chapterDao().removeAllNonLibraryRows()
				db.chapterBodyDao().removeAllNonChapterRows()
				emit(Unit)
			}.flowOn(Dispatchers.IO)
		}
		
		inner class BookLibrary
		{
			val booksInLibraryFlow by lazy { db.libraryDao().booksInLibraryFlow() }
			val getBooksInLibraryWithContextFlow by lazy { db.libraryDao().getBooksInLibraryWithContextFlow() }
			fun existInLibraryFlow(url: String) = db.libraryDao().existInLibraryFlow(url)
			suspend fun insert(book: Book) = if (isValid(book)) db.libraryDao().insert(book) else Unit
			suspend fun insert(books: List<Book>) = db.libraryDao().insert(books.filter(::isValid))
			suspend fun remove(book: Book) = db.libraryDao().remove(book)
			suspend fun update(book: Book) = db.libraryDao().update(book)
			suspend fun get(url: String) = db.libraryDao().get(url)
			suspend fun getAll() = db.libraryDao().getAll()
			suspend fun existInLibrary(url: String) = db.libraryDao().existInLibrary(url)
			suspend fun toggleBookmark(bookMetadata: BookMetadata) = db.withTransaction {
				val book = get(bookMetadata.url)
				if (book == null)
					insert(Book(title = bookMetadata.title, url = bookMetadata.url, inLibrary = true))
				else
					update(book.copy(inLibrary = !book.inLibrary))
			}
		}
		
		inner class BookChapter
		{
			fun numberOfUnreadChaptersFlow(bookUrl: String) = db.chapterDao().numberOfUnreadChaptersFlow(bookUrl)
			suspend fun update(chapter: Chapter) = db.chapterDao().update(chapter)
			suspend fun get(url: String) = db.chapterDao().get(url)
			suspend fun hasChapters(bookUrl: String) = db.chapterDao().hasChapters(bookUrl)
			suspend fun getAll() = db.chapterDao().getAll()
			suspend fun updateTitle(url: String, title: String) = db.chapterDao().updateTitle(url, title)
			suspend fun setAsRead(chaptersUrl: List<String>) = chaptersUrl.chunked(500).forEach { db.chapterDao().setAsRead(it) }
			suspend fun setAsUnread(chaptersUrl: List<String>) = chaptersUrl.chunked(500).forEach { db.chapterDao().setAsUnread(it) }
			suspend fun insert(chapters: List<Chapter>) = db.chapterDao().insert(chapters.filter(::isValid))
			suspend fun replace(chapters: List<Chapter>) = db.chapterDao().replace(chapters.filter(::isValid))
			suspend fun chapters(bookUrl: String) = db.chapterDao().chapters(bookUrl)
			fun getChaptersWithContexFlow(bookUrl: String) = db.chapterDao().getChaptersWithContextFlow(bookUrl)
			suspend fun merge(newChapters: List<Chapter>, bookUrl: String)
			{
				val current = chapters(bookUrl).associateBy { it.url }.toMutableMap()
				for (chapter in newChapters)
					current.merge(chapter.url, chapter) { old, new -> old.copy(position = new.position) }
				replace(current.values.toList())
			}
			
		}
		
		inner class BookChapterBody
		{
			suspend fun getAll() = db.chapterBodyDao().getAll()
			suspend fun insert(chapterBodies: List<ChapterBody>) = db.chapterBodyDao().insert(chapterBodies)
			suspend fun insert(chapterBody: ChapterBody) = db.chapterBodyDao().insert(chapterBody)
			suspend fun removeRows(chaptersUrl: List<String>) =
				chaptersUrl.chunked(500).forEach { db.chapterBodyDao().removeChapterRows(it) }
			
			suspend fun insertWithTitle(chapterBody: ChapterBody, title: String?) = db.withTransaction {
				insert(chapterBody)
				if (title != null)
					bookstore.bookChapter.updateTitle(chapterBody.url, title)
			}
			
			suspend fun fetchBody(urlChapter: String, tryCache: Boolean = true): Response<String>
			{
				if (tryCache) db.chapterBodyDao().get(urlChapter)?.let {
					return@fetchBody Response.Success(it.body)
				}
				
				return when (val res = downloadChapter(urlChapter))
				{
					is Response.Success ->
					{
						insertWithTitle(ChapterBody(url = urlChapter, body = res.data.body), res.data.title)
						return Response.Success(res.data.body)
					}
					is Response.Error ->
					{
						Response.Error(res.message)
					}
				}
			}
			
		}
	}
	
	fun isValid(book: Book): Boolean = book.url.matches("""^https?://.*""".toRegex())
	fun isValid(chapter: Chapter): Boolean = chapter.url.matches("""^https?://.*""".toRegex())
}

