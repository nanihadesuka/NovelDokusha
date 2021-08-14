package my.noveldokusha.ui.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import com.bumptech.glide.Glide
import com.google.android.material.composethemeadapter.MdcTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.*
import my.noveldokusha.R
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
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		return ComposeView(requireContext()).apply {
			setContent {
				MdcTheme(setTextColors = true) {
					SettingsCompose()
				}
			}
		}
	}
	
	@Preview
	@Composable
	fun SettingsCompose()
	{
		val databaseSize = remember { mutableStateOf<Long>(1) }
		val imagesFolderSize = remember { mutableStateOf<Long>(0) }
		
		LaunchedEffect(0) {
			databaseSize.value = withContext(Dispatchers.IO) { bookstore.appDB.getDatabaseSizeBytes() }
			imagesFolderSize.value = withContext(Dispatchers.IO) { getFolderSize(App.folderBooks) }
		}
		
		fun cleanDatabase() = App.scope.launch(Dispatchers.IO) {
			bookstore.settings.clearNonLibraryData()
			bookstore.appDB.vacuum()
			databaseSize.value = bookstore.appDB.getDatabaseSizeBytes()
		}
		
		fun cleanImageFolder() = App.scope.launch(Dispatchers.IO) {
			val libraryFolders = bookstore.bookLibrary.getAllInLibrary()
				.mapNotNull { """^local://(.+)$""".toRegex().find(it.url)?.destructured?.component1() }
				.toSet()
			
			App.folderBooks.listFiles()?.asSequence()
				?.filter { it.isDirectory && it.exists() }
				?.filter { it.name !in libraryFolders }
				?.forEach { it.deleteRecursively() }
			
			Glide.get(App.instance).clearDiskCache()
			
			imagesFolderSize.value = withContext(Dispatchers.IO) { getFolderSize(App.folderBooks) }
		}
		
		val accent = Color(0xFF2A59B6)
		
		Column(Modifier.verticalScroll(rememberScrollState())) {
			// Section: Theme
			Text(
				text = stringResource(R.string.theme),
				Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
				color = accent,
				fontWeight = FontWeight.Bold
			)
			
			// Follow system theme
			Row(
				Modifier
					.fillMaxSize()
					.clickable { sharedPreferences.THEME_FOLLOW_SYSTEM = !sharedPreferences.THEME_FOLLOW_SYSTEM }
			) {
				Text(text = stringResource(id = R.string.follow_system), modifier = Modifier.padding(12.dp))
				Switch(
					checked = sharedPreferences.THEME_FOLLOW_SYSTEM,
					onCheckedChange = { sharedPreferences.THEME_FOLLOW_SYSTEM = it },
					colors = SwitchDefaults.colors(checkedThumbColor = accent),
					modifier = Modifier.padding(12.dp)
				)
			}
			
			// Themes list
			var selectedTheme by remember { mutableStateOf(sharedPreferences.THEME_ID) }
			(globalThemeList.light + globalThemeList.dark).forEach { (id, name) ->
				
				fun onClick()
				{
					selectedTheme = id
					sharedPreferences.THEME_ID = id
				}
				
				Row(
					Modifier
						.fillMaxWidth()
						.clickable { onClick() }
				) {
					RadioButton(
						selected = (id == selectedTheme),
						onClick = { onClick() },
						colors = RadioButtonDefaults.colors(selectedColor = accent),
						modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
					)
					Text(text = name, Modifier.padding(6.dp))
				}
			}
			
			// Section: Data
			Text(
				text = stringResource(R.string.data),
				Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
				color = accent,
				fontWeight = FontWeight.Bold
			)
			
			// Clean database
			Column(Modifier
				.fillMaxWidth()
				.clickable { cleanDatabase() }
				.padding(12.dp)
			) {
				Text(text = stringResource(R.string.clean_database))
				Text(text = stringResource(R.string.preserve_only_library_books_data), Modifier.alpha(0.7f))
				Row(Modifier.alpha(0.7f)) {
					Text(text = stringResource(R.string.database_size))
					Text(text = Formatter.formatFileSize(context, databaseSize.value), Modifier.padding(horizontal = 2.dp))
				}
			}
			
			// Clean images folder
			Column(Modifier
				.fillMaxWidth()
				.clickable { cleanImageFolder() }
				.padding(12.dp)
			) {
				Text(text = stringResource(R.string.clean_images_folder))
				Text(text = stringResource(R.string.preserve_only_images_from_library_books), Modifier.alpha(0.7f))
				Row(Modifier.alpha(0.7f)) {
					Text(text = stringResource(R.string.images_total_size))
					Text(text = Formatter.formatFileSize(context, imagesFolderSize.value), Modifier.padding(horizontal = 2.dp))
				}
			}
			
			// Section: Backup
			Text(
				text = stringResource(R.string.data),
				Modifier.padding(vertical = 6.dp, horizontal = 12.dp),
				color = accent,
				fontWeight = FontWeight.Bold
			)
			
			// Make backup
			Column(Modifier
				.fillMaxWidth()
				.clickable { createBackup() }
				.padding(12.dp)
			) {
				Text(text = stringResource(R.string.backup_data))
				Text(text = stringResource(R.string.opens_the_file_explorer_to_select_the_backup_saving_location), Modifier.alpha(0.7f))
			}
			
			// Restore backup
			Column(Modifier
				.fillMaxWidth()
				.clickable { restoreBackup(databaseSize = databaseSize, imagesFolderSize = imagesFolderSize) }
				.padding(12.dp)
			) {
				Text(text = stringResource(R.string.restore_data))
				Text(text = stringResource(R.string.opens_the_file_explorer_to_select_the_backup_file), Modifier.alpha(0.7f))
			}
		}
	}
	
	fun createBackup()
	{
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
	
	fun restoreBackup(databaseSize: MutableState<Long>, imagesFolderSize: MutableState<Long>)
	{
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
					
					suspend fun mergeToBookFolder(entry: ZipEntry, inputStream: InputStream) = withContext(Dispatchers.IO)
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
						entry.name.startsWith("books/") -> mergeToBookFolder(entry, file.inputStream())
					}
					
					inputStream.closeQuietly()
					builder.showNotification(channel_id) {
						removeProgressBar()
						text = "Data restored"
					}
					databaseSize.value = withContext(Dispatchers.IO) { bookstore.appDB.getDatabaseSizeBytes() }
					imagesFolderSize.value = withContext(Dispatchers.IO) { getFolderSize(App.folderBooks) }
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
}

