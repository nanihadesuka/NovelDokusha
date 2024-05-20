package my.noveldokusha.tooling.backup_create

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import my.noveldoksuha.coreui.components.MyButton
import my.noveldokusha.tooling.backup_create.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun onBackupCreate(): () -> Unit {
    val context = LocalContext.current
    var showDialog by rememberSaveable { mutableStateOf(false) }
    var saveImages by rememberSaveable { mutableStateOf(false) }

    /**
     * Steps:
     * 2) Dialog options
     * 3) Open file explorer
     * 4) Start backup
     */
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
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .clickable { saveImages = !saveImages }
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Checkbox(
                            checked = saveImages,
                            onCheckedChange = null
                        )
                        Text(
                            text = stringResource(R.string.save_images),
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .weight(1f)
                        )
                    }
                    MyButton(
                        text = stringResource(id = R.string.backup),
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

    return { showDialog = true }
}