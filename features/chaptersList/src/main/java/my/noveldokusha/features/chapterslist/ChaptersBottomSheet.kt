package my.noveldokusha.features.chapterslist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.components.TernaryStateToggle
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldokusha.chapterslist.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChaptersBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    state: ChaptersScreenState,
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
