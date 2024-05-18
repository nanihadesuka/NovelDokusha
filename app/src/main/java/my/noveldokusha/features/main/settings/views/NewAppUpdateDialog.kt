package my.noveldokusha.features.main.settings.views

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import my.noveldokusha.R
import my.noveldokusha.core.domain.AppVersion
import my.noveldokusha.core.domain.RemoteAppVersion
import my.noveldokusha.features.main.settings.SettingsScreenState
import my.noveldoksuha.coreui.components.ImageView
import my.noveldoksuha.coreui.components.MyButton
import my.noveldoksuha.coreui.theme.InternalTheme

@Composable
fun NewAppUpdateDialog(
    updateApp: SettingsScreenState.UpdateApp,
) {
    val newVersion = updateApp.showNewVersionDialog.value
    val context = LocalContext.current

    if (newVersion != null) {
        Dialog(
            onDismissRequest = { updateApp.showNewVersionDialog.value = null },
            content = {
                Card(
                    elevation = CardDefaults.elevatedCardElevation(
                        defaultElevation = 10.dp,
                        pressedElevation = 10.dp,
                        focusedElevation = 10.dp,
                        hoveredElevation = 10.dp,
                        draggedElevation = 10.dp,
                        disabledElevation = 10.dp,
                    )

                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ImageView(
                            imageModel = R.drawable.default_icon,
                            modifier = Modifier.size(48.dp),
                            contentScale = ContentScale.Fit
                        )
                        Text(
                            text = stringResource(
                                R.string.new_app_version_found_s,
                                newVersion.version.toString()
                            ),
                            modifier = Modifier.padding(0.dp),
                            style = MaterialTheme.typography.headlineSmall
                                .copy(textAlign = TextAlign.Center)
                        )
                        MyButton(
                            text = stringResource(R.string.download),
                            textAlign = TextAlign.Center,
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW)
                                    .also { it.data = Uri.parse(newVersion.sourceUrl) })
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        )
    }
}

@Preview
@Composable
private fun PreviewView() {
    InternalTheme {
        NewAppUpdateDialog(
            updateApp = SettingsScreenState.UpdateApp(
                currentAppVersion = "1.2.3",
                appUpdateCheckerEnabled = remember { mutableStateOf(true) },
                showNewVersionDialog = remember {
                    mutableStateOf(
                        RemoteAppVersion(
                            sourceUrl = "url",
                            version = AppVersion(1, 4, 5)
                        )
                    )
                },
                checkingForNewVersion = remember { mutableStateOf(true) }
            )
        )
    }
}