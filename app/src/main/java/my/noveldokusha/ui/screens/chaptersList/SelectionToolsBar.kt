package my.noveldokusha.ui.screens.chaptersList

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.R

@Composable
fun SelectionToolsBar(
    onDeleteDownload: () -> Unit,
    onDownload: () -> Unit,
    onSetRead: () -> Unit,
    onSetUnread: () -> Unit,
    onSelectAllChapters: () -> Unit,
    onSelectAllChaptersAfterSelectedOnes: () -> Unit,
    onCloseSelectionbar: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun LazyGridScope.buttonItem(
        @DrawableRes id: Int,
        @StringRes description: Int,
        onClick: () -> Unit
    ) {
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable(onClick = onClick)
                    .padding(12.dp)
            ) {
                Icon(
                    painter = painterResource(id = id),
                    contentDescription = stringResource(description)
                )
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .shadow(6.dp)
            .background(MaterialTheme.colorScheme.primary)
    ) {
        buttonItem(
            id = R.drawable.ic_outline_delete_24,
            description = R.string.remove_selected_chapters_dowloads,
            onClick = onDeleteDownload
        )
        buttonItem(
            id = R.drawable.ic_outline_cloud_download_24,
            description = R.string.download_selected_chapters,
            onClick = onDownload
        )
        buttonItem(
            id = R.drawable.ic_looking_24,
            description = R.string.set_as_read_selected_chapters,
            onClick = onSetRead
        )
        buttonItem(
            id = R.drawable.ic_not_looking_24,
            description = R.string.set_as_not_read_selected_chapters,
            onClick = onSetUnread
        )
        buttonItem(
            id = R.drawable.ic_baseline_select_all_24,
            description = R.string.select_all_chapters,
            onClick = onSelectAllChapters
        )
        buttonItem(
            id = R.drawable.ic_baseline_select_all_24_bottom,
            description = R.string.select_all_chapters_after_currently_selected_ones,
            onClick = onSelectAllChaptersAfterSelectedOnes
        )
        buttonItem(
            id = R.drawable.ic_baseline_close_24,
            description = R.string.close_selection_bar,
            onClick = onCloseSelectionbar
        )
    }
}