package my.noveldokusha.ui.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import my.noveldokusha.*
import my.noveldokusha.databinding.ActivityMainFragmentSettingsBinding
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.uiUtils.stringRes
import my.noveldokusha.uiUtils.toast
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : BaseFragment()
{
	private val viewModel by viewModels<SettingsModel>()
	private lateinit var viewHolder: ActivityMainFragmentSettingsBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	
	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View
	{
		viewHolder = ActivityMainFragmentSettingsBinding.inflate(inflater, container, false)
		viewAdapter = Adapter()
		
		settingTheme()
		settingDatabaseClean()
		settingDatabaseBackup()
		settingDatabaseRestore()
		
		return viewHolder.root
	}
	
	fun settingTheme()
	{
		viewHolder.settingsFollowSystemTheme.isChecked = sharedPreferences.THEME_FOLLOW_SYSTEM
		viewHolder.settingsFollowSystemTheme.setOnCheckedChangeListener { _, isChecked ->
			sharedPreferences.THEME_FOLLOW_SYSTEM = isChecked
		}
		
		val themes = (globalThemeList.light + globalThemeList.dark)
		for ((id, name) in themes)
			viewHolder.settingsTheme.addView(MaterialRadioButton(requireActivity()).also {
				it.id = id
				it.text = name
			})
		
		viewHolder.settingsTheme.check(sharedPreferences.THEME_ID)
		viewHolder.settingsTheme.setOnCheckedChangeListener { _, _ ->
			sharedPreferences.THEME_ID = viewHolder.settingsTheme.checkedRadioButtonId
		}
	}
	
	fun settingDatabaseClean()
	{
		viewHolder.databaseSize.text = Formatter.formatFileSize(context, bookstore.appDB.getDatabaseSizeBytes())
		viewHolder.databaseButtonClean.setOnClickListener {
			bookstore.settings.clearNonLibraryDataFlow().asLiveData().observe(viewLifecycleOwner) {
				viewHolder.databaseSize.text = Formatter.formatFileSize(context, bookstore.appDB.getDatabaseSizeBytes())
			}
		}
	}
	
	fun settingDatabaseBackup()
	{
		viewHolder.backupDatabaseButton.setOnClickListener {
			
			val read = Manifest.permission.READ_EXTERNAL_STORAGE
			val write = Manifest.permission.WRITE_EXTERNAL_STORAGE
			permissionRequest(read, write) {
				val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).also {
					it.addCategory(Intent.CATEGORY_OPENABLE)
					it.type = "application/*"
					val date = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
					it.putExtra(Intent.EXTRA_TITLE, "noveldokusha_$date.sqlite3")
				}
				
				activityRequest(intent) { resultCode, data ->
					if (resultCode != RESULT_OK) return@activityRequest
					val uri = data?.data ?: return@activityRequest
					
					val inputStream = requireActivity().applicationContext.getDatabasePath(bookstore.appDB.name).inputStream()
					requireActivity().contentResolver.openOutputStream(uri)?.use { outputStream ->
						inputStream.copyTo(outputStream)
						toast(R.string.backup_saved.stringRes())
					} ?: toast(R.string.failed_to_make_backup.stringRes())
					inputStream.close()
				}
			}
		}
	}
	
	fun settingDatabaseRestore()
	{
		viewHolder.restoreDatabaseButton.setOnClickListener {
			
			val read = Manifest.permission.READ_EXTERNAL_STORAGE
			permissionRequest(read) {
				val intent = Intent(Intent.ACTION_GET_CONTENT).also {
					it.addCategory(Intent.CATEGORY_OPENABLE)
					it.type = "application/*"
				}
				
				activityRequest(intent) { resultCode, data ->
					if (resultCode != RESULT_OK) return@activityRequest
					val uri = data?.data ?: return@activityRequest
					val inputStream = requireActivity().contentResolver.openInputStream(uri)
					if (inputStream == null)
					{
						toast(R.string.failed_to_restore_cant_access_file.stringRes())
						return@activityRequest
					}
					
					val backupDatabase = try
					{
						bookstore.DBase(requireContext(), "temp_database", inputStream)
					}
					catch (e: Exception)
					{
						toast(R.string.failed_to_restore_invalid_backup.stringRes())
						val stacktrace = StringWriter().apply { e.printStackTrace(PrintWriter(this)) }
						Log.e("ERROR", "Message:\n${e.message}\n\nStacktrace:\n$stacktrace")
						return@activityRequest
					}
					
					CoroutineScope(Dispatchers.IO).launch {
						bookstore.bookLibrary.insert(backupDatabase.bookLibrary.getAll())
						bookstore.bookChapter.insert(backupDatabase.bookChapter.getAll())
						bookstore.bookChapterBody.insert(backupDatabase.bookChapterBody.getAll())
						toast(R.string.database_restored.stringRes())
						backupDatabase.close()
						backupDatabase.delete()
					}
				}
			}
		}
	}
}

