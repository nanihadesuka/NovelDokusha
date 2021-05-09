package my.noveldokusha.ui.main

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.format.Formatter
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import androidx.lifecycle.asLiveData
import com.google.android.material.radiobutton.MaterialRadioButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import my.noveldokusha.bookstore
import my.noveldokusha.databinding.ActivityMainFragmentSettingsBinding
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.uiUtils.toast
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

class SettingsFragment : BaseFragment()
{
	class Extras
	{
		fun intent(ctx: Context) = Intent(ctx, SettingsFragment::class.java)
	}
	
	private val viewModel by viewModels<SettingsModel>()
	private lateinit var viewHolder: ActivityMainFragmentSettingsBinding
	private lateinit var viewAdapter: Adapter
	
	private inner class Adapter
	
	val preferences: SharedPreferences get() = requireActivity().getSharedPreferences("SETTINGS", AppCompatActivity.MODE_PRIVATE)
	
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
		val currentThemeId = preferencesGetThemeId()
		globalThemeList.forEach { (name, id) ->
			viewHolder.settingsTheme.addView(MaterialRadioButton(requireActivity()).also {
				it.text = name
				it.setOnCheckedChangeListener { _, _ -> preferencesSetThemeId(id) }
				it.isChecked = currentThemeId == id
			})
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
						toast("Backup saved")
					} ?: toast("Failed to make backup")
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
						toast("Failed to restore, can't access file")
						return@activityRequest
					}
					
					val backupDatabase = try
					{
						bookstore.DBase(requireContext(), "temp_database", inputStream)
					}
					catch (e: Exception)
					{
						toast("Failed to restore, invalid backup")
						val stacktrace = StringWriter().apply { e.printStackTrace(PrintWriter(this)) }
						Log.e("ERROR", "Message:\n${e.message}\n\nStacktrace:\n$stacktrace")
						return@activityRequest
					}
					
					GlobalScope.launch(Dispatchers.IO) {
						bookstore.bookLibrary.insert(backupDatabase.bookLibrary.getAll())
						bookstore.bookChapter.insert(backupDatabase.bookChapter.getAll())
						bookstore.bookChapterBody.insert(backupDatabase.bookChapterBody.getAll())
						withContext(Dispatchers.Main) { toast("Database restored") }
					}
				}
			}
		}
	}
}

