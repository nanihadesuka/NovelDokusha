package my.noveldokusha.features.main.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldoksuha.coreui.components.TernaryStateToggle
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldokusha.R
import my.noveldokusha.core.utils.toToggleableState
import my.noveldoksuha.coreui.components.PosNegCheckbox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    model: LibraryViewModel = viewModel()
) {
    if (!visible) return

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(top = 16.dp, bottom = 64.dp)) {
            Text(
                text = stringResource(id = R.string.filter),
                modifier = Modifier
                    .padding(8.dp)
                    .padding(horizontal = 8.dp),
                color = ColorAccent,
                style = MaterialTheme.typography.titleMedium
            )
            PosNegCheckbox(
                text = stringResource(id = R.string.read),
                state = model.readFilter.toToggleableState(),
                onStateChange = { model.readFilterToggle() },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(id = R.string.sort),
                modifier = Modifier
                    .padding(8.dp)
                    .padding(horizontal = 8.dp),
                color = ColorAccent,
                style = MaterialTheme.typography.titleMedium
            )
            TernaryStateToggle(
                text = stringResource(id = R.string.last_read),
                state = model.readSort,
                onStateChange = { model.readSortToggle() },
                modifier = Modifier.fillMaxWidth(),
                activeIcon = { Icon(imageVector = Icons.Filled.ArrowUpward, null) },
                inverseIcon = { Icon(imageVector = Icons.Filled.ArrowDownward, null) },
                inactiveIcon = { Icon(imageVector = Icons.Filled.SwapVert, null) },
            )
        }
    }
}
