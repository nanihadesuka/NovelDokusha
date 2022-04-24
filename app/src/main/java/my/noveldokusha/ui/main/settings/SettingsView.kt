package my.noveldokusha.ui.main.settings

import android.Manifest
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R

@Composable
fun SettingsView(context: Context)
{
    val viewModel = viewModel<SettingsViewModel>()
    Column {
        ToolbarMain(title = stringResource(id = R.string.app_name))
        SettingsBody(
            currentFollowSystem = viewModel.followsSystem,
            currentTheme = viewModel.theme,
            onFollowSystem = viewModel::onFollowSystem,
            onThemeSelected = viewModel::onThemeSelected,
            databaseSize = viewModel.databaseSize,
            imagesFolderSize = viewModel.imageFolderSize,
            onCleanDatabase = viewModel::cleanDatabase,
            onCleanImageFolder = viewModel::cleanImagesFolder,
            onBackupData = { doBackup(context) },
            onRestoreData = { doRestore(context) },
        )
    }
}

fun doBackup(context: Context) = with(context) {
    val read = Manifest.permission.READ_EXTERNAL_STORAGE
    val write = Manifest.permission.WRITE_EXTERNAL_STORAGE
//    TODO()
//    permissionRequest(read, write) {
//
//        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).also {
//            it.addCategory(Intent.CATEGORY_OPENABLE)
//            it.type = "application/*"
//            val date =
//                SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault()).format(Date())
//            it.putExtra(Intent.EXTRA_TITLE, "noveldokusha_backup_$date.zip")
//        }
//
//        MaterialDialog(context).show {
//            title(text = "Backup")
//            val checkImagesFolder = checkBoxPrompt(
//                text = getString(R.string.include_images_folder),
//                isCheckedDefault = true
//            ) {}
//
//            negativeButton()
//            positiveButton {
//                activityRequest(intent) { resultCode, data ->
//                    if (resultCode != RESULT_OK) return@activityRequest
//                    val uri = data?.data ?: return@activityRequest
//                    BackupDataService.start(
//                        ctx = requireContext(),
//                        uri = uri,
//                        backupImages = checkImagesFolder.isCheckPromptChecked()
//                    )
//                }
//            }
//        }
//    }
}

fun doRestore(context: Context) = with(context) {
    val read = Manifest.permission.READ_EXTERNAL_STORAGE
//    TODO()
//    permissionRequest(read) {
//
//        val intent = Intent(Intent.ACTION_GET_CONTENT).also {
//            it.addCategory(Intent.CATEGORY_OPENABLE)
//            it.type = "application/*"
//        }
//
//        activityRequest(intent) { resultCode, data ->
//            if (resultCode != RESULT_OK) return@activityRequest
//            val uri = data?.data ?: return@activityRequest
//            RestoreDataService.start(
//                ctx = this,
//                uri = uri
//            )
//        }
//    }
}
