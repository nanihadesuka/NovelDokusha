package my.noveldokusha.composableActions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import my.noveldokusha.R
import my.noveldokusha.ui.theme.Info400

@Composable
fun rememberPermissionsDeniedDialog(): MutableState<Boolean> {
    val showDeniedDialog = rememberSaveable { mutableStateOf(false) }
    val context by rememberUpdatedState(newValue = LocalContext.current)

    if (showDeniedDialog.value) AlertDialog(
        onDismissRequest = { showDeniedDialog.value = false },
        title = { Text(text = stringResource(id = R.string.permissions_denied)) },
        icon = {
            Icon(
                imageVector = Icons.TwoTone.Info,
                contentDescription = null,
                tint = Info400
            )
        },
        text = { Text(text = stringResource(R.string.allow_the_denied_permissions_to_perform_the_action)) },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .also { it.data = Uri.parse("package:${context.packageName}") }
                        .let(context::startActivity)
                }) {
                Text(text = stringResource(R.string.app_settings))
            }

        }
    )

    return showDeniedDialog
}