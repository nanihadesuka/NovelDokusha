package my.noveldokusha.composableActions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import my.noveldokusha.R
import my.noveldokusha.services.BackupDataService
import my.noveldokusha.uiViews.MyButton
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun onDoBackup(): () -> Unit
{
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var saveImages by remember { mutableStateOf(false) }

    /**
     * Steps:
     * 1) Ask permissions
     * 2) Dialog options
     * 3) Open file explorer
     * 4) Start backup
     */
    val permissions = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { areGranted ->
        if (areGranted.all { it.value })
            showDialog = true
    }

    val fileExplorer = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*"),
        onResult = {
            if (it != null) BackupDataService.start(
                ctx = context,
                uri = it,
                backupImages = saveImages
            )
        }
    )

    if (showDialog) Dialog(
        onDismissRequest = { showDialog = false },
        content = {
            Card {
                Column {
                    Text(
                        text = stringResource(R.string.backup_options),
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.h6
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clickable { saveImages = !saveImages }
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        val checkState by remember {
                            derivedStateOf { if (saveImages) Color.Green else Color.Transparent }
                        }
                        val checkedColor by animateColorAsState(
                            targetValue = checkState,
                            animationSpec = tween(250)
                        )
                        Checkbox(
                            checked = saveImages,
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = checkedColor,
                                uncheckedColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                                disabledColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.25f),
                                disabledIndeterminateColor = MaterialTheme.colors.onPrimary.copy(
                                    alpha = 0.25f
                                ),
                            )
                        )
                        Text(
                            text = stringResource(R.string.save_images),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                        )
                    }
                    MyButton(
                        text = "Backup",
                        textAlign = TextAlign.Center,
                        onClick = {
                            showDialog = false
                            val pattern = "yyyy-MM-dd_HH-mm"
                            val date = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
                            val fileName = "noveldokusha_backup_$date.zip"
                            fileExplorer.launch(fileName)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .padding(top = 12.dp)
                    )
                }
            }
        }
    )

    return {
        permissions.launch(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        )
    }
}