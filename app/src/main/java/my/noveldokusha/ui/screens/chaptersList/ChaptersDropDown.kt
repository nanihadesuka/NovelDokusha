package my.noveldokusha.ui.screens.chaptersList

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import my.noveldokusha.R

@Composable
fun ChaptersDropDown(
    onOpenWebView: () -> Unit,
    onSearchBookInDatabase: () -> Unit,
    onResumeReading: () -> Unit,
) {
    DropdownMenuItem(
        onClick = onOpenWebView,
        text = {
            Text(text = stringResource(id = R.string.open_in_browser))
        },
        leadingIcon = {
            Icon(Icons.Outlined.Public, stringResource(R.string.open_in_browser))
        }
    )
    DropdownMenuItem(
        onClick = onSearchBookInDatabase,
        text = {
            Text(text = stringResource(R.string.find_in_database))
        },
        leadingIcon = {
            Icon(
                Icons.Outlined.Search,
                stringResource(R.string.find_in_database)
            )
        }
    )
    DropdownMenuItem(
        onClick = onResumeReading,
        text = {
            Text(text = stringResource(id = R.string.resume_reading))
        },
        leadingIcon = {
            Icon(
                Icons.Filled.PlayArrow,
                stringResource(R.string.resume_reading),
            )
        }
    )
}