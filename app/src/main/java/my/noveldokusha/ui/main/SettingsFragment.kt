package my.noveldokusha.ui.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.bumptech.glide.Glide
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityMainFragmentSettingsBinding
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast
import okhttp3.internal.closeQuietly
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class SettingsFragment : BaseFragment()
{
	private val viewModel by viewModels<SettingsModel>()
	private lateinit var viewBind: ActivityMainFragmentSettingsBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewBind = ActivityMainFragmentSettingsBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		settingTheme()
		settingDatabaseClean()
		settingImagesFolderClean()
		settingDataBackup()
		settingDataRestore()
		
		return viewBind.root
	}
	
	fun settingTheme()
	{
		viewBind.settingsFollowSystemTheme.isChecked = sharedPreferences.THEME_FOLLOW_SYSTEM
		viewBind.settingsFollowSystemTheme.setOnCheckedChangeListener { _, isChecked ->
			sharedPreferences.THEME_FOLLOW_SYSTEM = isChecked
		}
		
		val themes = (globalThemeList.light + globalThemeList.dark)
		for ((id, name) in themes)
			viewBind.settingsTheme.addView(MaterialRadioButton(requireActivity()).also {
				it.id = id
				it.text = name
			})
		
		viewBind.settingsTheme.check(sharedPreferences.THEME_ID)
		viewBind.settingsTheme.setOnCheckedChangeListener { _, _ ->
			sharedPreferences.THEME_ID = viewBind.settingsTheme.checkedRadioButtonId
		}
	}
	
	fun updateDatabaseSize() = lifecycleScope.launch(Dispatchers.Main) {
		viewBind.databaseSize.text = Formatter.formatFileSize(context, bookstore.appDB.getDatabaseSizeBytes())
	}
	
	fun settingDatabaseClean()
	{
		viewBind.databaseButtonClean.setOnClickListener {
			App.scope.launch(Dispatchers.IO) {
				bookstore.settings.clearNonLibraryData()
				bookstore.appDB.vacuum()
				updateDatabaseSize()
			}
		}
		
		updateDatabaseSize()
	}
	
	fun settingDataBackup()
	{
		viewBind.backupDataButton.setOnClickListener {
			
			val read = Manifest.permission.READ_EXTERNAL_STORAGE
			val write = Manifest.permission.WRITE_EXTERNAL_STORAGE
			permissionRequest(read, write) {
				val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).also {
					it.addCategory(Intent.CATEGORY_OPENABLE)
					it.type = "application/*"
					val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
					it.putExtra(Intent.EXTRA_TITLE, "noveldokusha_backup_$date.zip")
				}
				MaterialDialog(requireContext()).show {
					title(text = "Backup")
					val checkImagesFolder =
						checkBoxPrompt(text = R.string.include_images_folder.stringRes(), isCheckedDefault = true) {}
					negativeButton()
					positiveButton {
						activityRequest(intent) { resultCode, data ->
							if (resultCode != RESULT_OK) return@activityRequest
							val uri = data?.data ?: return@activityRequest
							
							val channel_id = "Backup"
							val builder = App.showNotification(channel_id) {
								title = "Backup"
								text = "Creating backup"
								setProgress(100, 0, true)
							}
							
							val includeBookFolder = checkImagesFolder.isCheckPromptChecked()
							App.scope.launch(Dispatchers.IO) {
								App.instance.contentResolver.openOutputStream(uri)?.use { outputStream ->
									val zip = ZipOutputStream(outputStream)
									
									builder.showNotification(channel_id) {
										text = "Copying database"
									}
									// Save database
									run {
										val entry = ZipEntry("database.sqlite3")
										val file = App.getDatabasePath(bookstore.appDB.name)
										entry.method = ZipOutputStream.DEFLATED
										file.inputStream().use {
											zip.putNextEntry(entry)
											it.copyTo(zip)
										}
									}
									
									// Save books extra data (like images)
									if (includeBookFolder)
									{
										builder.showNotification(channel_id) {
											text = "Copying images"
										}
										val basePath = App.folderBooks.toPath().parent
										App.folderBooks.walkBottomUp().filterNot { it.isDirectory }.forEach { file ->
											val name = basePath.relativize(file.toPath()).toString()
											val entry = ZipEntry(name)
											entry.method = ZipOutputStream.DEFLATED
											file.inputStream().use {
												zip.putNextEntry(entry)
												it.copyTo(zip)
											}
										}
									}
									
									zip.closeQuietly()
									builder.showNotification(channel_id) {
										removeProgressBar()
										text = R.string.backup_saved.stringRes()
									}
								} ?: builder.showNotification(channel_id) {
									removeProgressBar()
									text = R.string.failed_to_make_backup.stringRes()
								}
							}
						}
					}
				}
			}
		}
	}
	
	fun settingDataRestore()
	{
		viewBind.restoreDataButton.setOnClickListener {
			
			val read = Manifest.permission.READ_EXTERNAL_STORAGE
			permissionRequest(read) {
				val intent = Intent(Intent.ACTION_GET_CONTENT).also {
					it.addCategory(Intent.CATEGORY_OPENABLE)
					it.type = "application/*"
				}
				
				activityRequest(intent) { resultCode, data ->
					if (resultCode != RESULT_OK) return@activityRequest
					val uri = data?.data ?: return@activityRequest
					
					val channel_id = "Restore backup"
					val builder = App.showNotification(channel_id) {
						title = "Restore data"
						text = "Loading data"
						setProgress(100, 0, true)
					}
					
					App.scope.launch(Dispatchers.IO) {
						
						val inputStream = App.instance.contentResolver.openInputStream(uri)
						if (inputStream == null)
						{
							builder.showNotification(channel_id) {
								removeProgressBar()
								text = R.string.failed_to_restore_cant_access_file.stringRes()
							}
							return@launch
						}
						
						suspend fun mergeToDatabase(inputStream: InputStream) = withContext(Dispatchers.IO)
						{
							try
							{
								builder.showNotification(channel_id) { text = "Loading database" }
								val backupDatabase = inputStream.use { bookstore.DBase(bookstore.db_context, "temp_database", it) }
								builder.showNotification(channel_id) { text = "Adding books" }
								bookstore.bookLibrary.insertReplace(backupDatabase.bookLibrary.getAll())
								builder.showNotification(channel_id) { text = "Adding chapters" }
								bookstore.bookChapter.insert(backupDatabase.bookChapter.getAll())
								builder.showNotification(channel_id) { text = "Adding chapters text" }
								bookstore.bookChapterBody.insertReplace(backupDatabase.bookChapterBody.getAll())
								toast(R.string.database_restored.stringRes())
								backupDatabase.close()
								backupDatabase.delete()
							}
							catch (e: Exception)
							{
								builder.showNotification(channel_id) {
									removeProgressBar()
									text = R.string.failed_to_restore_invalid_backup_database.stringRes()
								}
							}
						}
						
						suspend fun mergetoBookFolder(entry: ZipEntry, inputStream: InputStream) = withContext(Dispatchers.IO)
						{
							val file = File(App.folderBooks.parentFile, entry.name)
							if (file.isDirectory) return@withContext
							file.parentFile?.mkdirs()
							if (file.parentFile?.exists() != true) return@withContext
							builder.showNotification(channel_id) { text = "Adding image: ${file.name}" }
							file.outputStream().use { output ->
								inputStream.use { it.copyTo(output) }
							}
						}
						
						val zipSequence = ZipInputStream(inputStream).let { zipStream ->
							generateSequence { zipStream.nextEntry }
								.filterNot { it.isDirectory }
								.associateWith { zipStream.readBytes() }
						}
						
						for ((entry, file) in zipSequence) when
						{
							entry.name == "database.sqlite3" -> mergeToDatabase(file.inputStream())
							entry.name.startsWith("books/") -> mergetoBookFolder(entry, file.inputStream())
						}
						
						inputStream.closeQuietly()
						builder.showNotification(channel_id) {
							removeProgressBar()
							text = "Data restored"
						}
						updateDatabaseSize()
						updateImagesFolderSize()
					}
				}
			}
		}
	}
	
	suspend fun getFolderSize(file: File): Long = withContext(Dispatchers.IO) {
		when
		{
			!file.exists() -> 0
			file.isFile -> file.length()
			else -> file.walkBottomUp().sumOf { if (it.isDirectory) 0 else it.length() }
		}
	}
	
	fun updateImagesFolderSize() = lifecycleScope.launch(Dispatchers.Main) {
		val folderSize = getFolderSize(App.folderBooks)
		viewBind.totalBooksImagesSize.text = Formatter.formatFileSize(context, folderSize)
	}
	
	fun settingImagesFolderClean()
	{
		suspend fun cleanImageFolder() = withContext(Dispatchers.IO) {
			val libraryFolders = bookstore.bookLibrary.getAllInLibrary()
				.mapNotNull { """^local://(.+)$""".toRegex().find(it.url)?.destructured?.component1() }
				.toSet()
			
			App.folderBooks.listFiles()?.asSequence()
				?.filter { it.isDirectory && it.exists() }
				?.filter { it.name !in libraryFolders }
				?.forEach { it.deleteRecursively() }
		}
		
		viewBind.booksImagesClear.setOnClickListener {
			App.scope.launch(Dispatchers.IO) {
				cleanImageFolder()
				updateImagesFolderSize()
				Glide.get(App.instance).clearDiskCache()
			}
		}
		
		updateImagesFolderSize()
	}
}

