package my.noveldokusha.ui.screens.databaseSearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.composeViews.CollapsibleDivider
import my.noveldokusha.ui.composeViews.next
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.PreviewThemes


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DatabaseSearchBottomSheet(
    genresList: SnapshotStateList<GenreItem>,
    onGenresFiltersSubmit: () -> Unit,
) {
    Column {
        val scrollState = rememberScrollState()

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Spacer(
                    modifier = Modifier
                        .size(1.dp)
                        .weight(1f)
                )

                FilledTonalButton(onClick = onGenresFiltersSubmit) {
                    Text(text = stringResource(R.string.search_with_filters))
                }

                TextButton(
                    onClick = {
                        val newList = genresList.map { it.copy(state = ToggleableState.Off) }
                        genresList.clear()
                        genresList.addAll(newList)
                    },
                    content = {
                        Text(
                            text = stringResource(R.string.clear_filters),
                            color = ColorAccent
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            CollapsibleDivider(scrollState = scrollState)
        }

        FlowRow(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            genresList.forEachIndexed { index, genreItem ->
                FilterChip(
                    selected = genreItem.state != ToggleableState.Off,
                    onClick = {
                        val item = genresList[index]
                        genresList[index] = item.copy(state = item.state.next())
                    },
                    label = { Text(text = genreItem.genre) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = when (genreItem.state) {
                            ToggleableState.On -> MaterialTheme.colorScheme.secondaryContainer
                            ToggleableState.Off -> MaterialTheme.colorScheme.primary
                            ToggleableState.Indeterminate -> MaterialTheme.colorScheme.error
                        }
                    ),
                    leadingIcon = {
                        when (genreItem.state) {
                            ToggleableState.On -> Icon(Icons.Filled.CheckCircle, null)
                            ToggleableState.Indeterminate -> Icon(Icons.Filled.RemoveCircle, null)
                            ToggleableState.Off -> Icon(
                                Icons.Filled.Add,
                                null,
                                tint = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(300.dp))
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView() {
    val list = remember {
        mutableStateListOf<GenreItem>(
            GenreItem("Fantasy", "fn", ToggleableState.On),
            GenreItem("Horror", "fn", ToggleableState.Off),
            GenreItem("Honesty", "fn", ToggleableState.Off),
            GenreItem("Drama", "fn", ToggleableState.Indeterminate),
            GenreItem("Historical", "fn", ToggleableState.Off),
            GenreItem("Crime", "fn", ToggleableState.Off),
            GenreItem("Seinen", "fn", ToggleableState.Off),
            GenreItem("Action", "fn", ToggleableState.Off),
            GenreItem("Fantasy", "fn", ToggleableState.Off),
            GenreItem("Horror", "fn", ToggleableState.On),
            GenreItem("Honesty", "fn", ToggleableState.On),
            GenreItem("Drama", "fn", ToggleableState.Off),
            GenreItem("Historical", "fn", ToggleableState.Off),
            GenreItem("Crime", "fn", ToggleableState.Off),
            GenreItem("Seinen", "fn", ToggleableState.Off),
            GenreItem("Action", "fn", ToggleableState.Off),
            GenreItem("Horror", "fn", ToggleableState.Off),
            GenreItem("Honesty", "fn", ToggleableState.Off),
            GenreItem("Drama", "fn", ToggleableState.Off),
            GenreItem("Historical", "fn", ToggleableState.Off),
            GenreItem("Crime", "fn", ToggleableState.Off),
            GenreItem("Seinen", "fn", ToggleableState.Off),
            GenreItem("Action", "fn", ToggleableState.Off)
        )
    }
    InternalTheme {
        DatabaseSearchBottomSheet(
            genresList = list,
            onGenresFiltersSubmit = { }
        )
    }
}