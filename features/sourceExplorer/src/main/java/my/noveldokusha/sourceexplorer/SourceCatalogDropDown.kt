package my.noveldokusha.sourceexplorer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.components.MyButton
import my.noveldokusha.core.appPreferences.ListLayoutMode

@Composable
internal fun SourceCatalogDropDown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    listLayoutMode: ListLayoutMode,
    onListLayoutModeChange: (mode: ListLayoutMode) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(8.dp)
                .widthIn(min = 128.dp)
        ) {
            Text(text = stringResource(R.string.layout))
            OutlinedCard {
                MyButton(
                    text = stringResource(id = R.string.list),
                    onClick = { onListLayoutModeChange(ListLayoutMode.VerticalList) },
                    selected = listLayoutMode == ListLayoutMode.VerticalList,
                    borderWidth = Dp.Unspecified,
                    textAlign = TextAlign.Center,
                    outerPadding = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                )
                MyButton(
                    text = stringResource(id = R.string.grid),
                    onClick = { onListLayoutModeChange(ListLayoutMode.VerticalGrid) },
                    selected = listLayoutMode == ListLayoutMode.VerticalGrid,
                    borderWidth = Dp.Unspecified,
                    textAlign = TextAlign.Center,
                    outerPadding = 0.dp,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                )
            }
        }
    }
}
