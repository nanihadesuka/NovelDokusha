package my.noveldokusha.libraryexplorer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import my.noveldokusha.tooling.epub_importer.onDoImportEPUB

@Composable
internal fun LibraryDropDown(
    expanded: Boolean,
    onDismiss: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            leadingIcon = {
                Icon(Icons.Filled.FileOpen, stringResource(id = R.string.import_epub))
            },
            text = { Text(stringResource(id = R.string.import_epub)) },
            onClick = onDoImportEPUB()
        )
    }
}