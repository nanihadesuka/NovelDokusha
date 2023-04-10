package my.noveldokusha.ui.screens.chaptersList

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.composeViews.TernaryStateToggle
import my.noveldokusha.ui.theme.ColorAccent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    state: ChapterScreenState,
) {
    if (!visible) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(top = 16.dp, bottom = 64.dp)) {
            Text(
                text = stringResource(id = R.string.sort),
                modifier = Modifier
                    .padding(8.dp)
                    .padding(horizontal = 8.dp),
                color = ColorAccent,
                style = MaterialTheme.typography.titleMedium
            )
            TernaryStateToggle(
                text = stringResource(R.string.by_chapter_name),
                state = state.settingChapterSort.value,
                onStateChange = { state.settingChapterSort.value = it.next() },
                modifier = Modifier.fillMaxWidth(),
                activeIcon = { Icon(imageVector = Icons.Filled.ArrowUpward, null) },
                inverseIcon = { Icon(imageVector = Icons.Filled.ArrowDownward, null) },
                inactiveIcon = { Icon(imageVector = Icons.Filled.SortByAlpha, null) },
            )
        }
    }
}
