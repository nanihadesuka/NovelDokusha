package my.noveldokusha.ui.main.settings

import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.checkbox.checkBoxPrompt
import com.afollestad.materialdialogs.checkbox.isCheckPromptChecked
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.R
import my.noveldokusha.services.BackupDataService
import my.noveldokusha.services.RestoreDataService
import my.noveldokusha.ui.BaseFragment
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.ui.theme.Themes
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class SettingsFragment : BaseFragment()
{
    private val viewModel by viewModels<SettingsViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View
    {
        val view = ComposeView(requireContext())
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        view.setContent {
            Theme(appPreferences = appPreferences) {
                SettingsView(
                    currentFollowSystem = viewModel.followsSystem,
                    currentTheme = viewModel.theme,
                    onFollowSystem = { appPreferences.THEME_FOLLOW_SYSTEM.value= it },
                    onThemeSelected = { appPreferences.THEME_ID.value = Themes.toIDTheme(it) },
                    databaseSize = viewModel.databaseSize,
                    imagesFolderSize = viewModel.imageFolderSize,
                    onCleanDatabase = { viewModel.cleanDatabase() },
                    onCleanImageFolder = { viewModel.cleanImagesFolder() },
                    onBackupData = { doBackup() },
                    onRestoreData = { doRestore() }
                )
            }
        }
        return view
    }

    fun doBackup()
    {
        val read = Manifest.permission.READ_EXTERNAL_STORAGE
        val write = Manifest.permission.WRITE_EXTERNAL_STORAGE
        permissionRequest(read, write) {

            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).also {
                it.addCategory(Intent.CATEGORY_OPENABLE)
                it.type = "application/*"
                val date =
                    SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
                it.putExtra(Intent.EXTRA_TITLE, "noveldokusha_backup_$date.zip")
            }

            MaterialDialog(requireContext()).show {
                title(text = "Backup")
                val checkImagesFolder = checkBoxPrompt(
                    text = getString(R.string.include_images_folder),
                    isCheckedDefault = true
                ) {}

                negativeButton()
                positiveButton {
                    activityRequest(intent) { resultCode, data ->
                        if (resultCode != RESULT_OK) return@activityRequest
                        val uri = data?.data ?: return@activityRequest
                        BackupDataService.start(
                            ctx = requireContext(),
                            uri = uri,
                            backupImages = checkImagesFolder.isCheckPromptChecked()
                        )
                    }
                }
            }
        }
    }

    fun doRestore()
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
                RestoreDataService.start(
                    ctx = requireContext(),
                    uri = uri
                )
            }
        }
    }

    override fun onResume()
    {
        super.onResume()
        viewModel.updateImagesFolderSize()
        viewModel.updateDatabaseSize()
    }
}
