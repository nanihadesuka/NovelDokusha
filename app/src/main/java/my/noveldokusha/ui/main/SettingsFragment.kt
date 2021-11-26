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
import com.google.android.material.radiobutton.MaterialRadioButton
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.*
import my.noveldokusha.services.BackupDataService
import my.noveldokusha.services.RestoreDataService
import my.noveldokusha.databinding.ActivityMainFragmentSettingsBinding
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.uiUtils.stringRes
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
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

        val buttons = viewModel.themes.map { (id, name) ->
            MaterialRadioButton(requireActivity()).also {
                it.id = id
                it.text = name
            }
        }

        for (button in buttons)
            viewBind.settingsTheme.addView(button)

        viewBind.settingsTheme.check(sharedPreferences.THEME_ID)
        viewBind.settingsTheme.setOnCheckedChangeListener { _, _ ->
            sharedPreferences.THEME_ID = viewBind.settingsTheme.checkedRadioButtonId
        }
    }

    fun settingDatabaseClean()
    {
        viewModel.eventDataRestored.observe(viewLifecycleOwner){
            viewModel.updateDatabaseSize()
        }

        viewBind.databaseButtonClean.setOnClickListener {
            viewModel.cleanDatabase()
        }

        viewModel.databseSizeBytes.observe(viewLifecycleOwner) {
            viewBind.databaseSize.text = Formatter.formatFileSize(context, it)
        }
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
                    val checkImagesFolder = checkBoxPrompt(
                        text = R.string.include_images_folder.stringRes(),
                        isCheckedDefault = true
                    ) {}

                    negativeButton()
                    positiveButton {
                        activityRequest(intent) { resultCode, data ->
                            if (resultCode != RESULT_OK) return@activityRequest
                            val uri = data?.data ?: return@activityRequest
                            BackupDataService.start(
                                ctx =  requireContext(),
                                uri = uri,
                                backupImages = checkImagesFolder.isCheckPromptChecked()
                            )
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
                    RestoreDataService.start(
                        ctx = requireContext(),
                        uri = uri
                    )
                }
            }
        }
    }

    fun settingImagesFolderClean()
    {
        viewModel.eventDataRestored.observe(viewLifecycleOwner){
            viewModel.updateImagesFolderSize()
        }

        viewModel.imagesFolderSizeBytes.observe(viewLifecycleOwner) {
            viewBind.totalBooksImagesSize.text = Formatter.formatFileSize(context, it)
        }
        viewBind.booksImagesClear.setOnClickListener {
            viewModel.cleanImagesFolder()
        }
    }
}
