package my.noveldokusha.ui.screens.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.ui.screens.main.library.LibraryViewModel
import my.noveldokusha.ui.theme.ColorAccent

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OptionsSheetLibrary() {
    val model = viewModel<LibraryViewModel>()
    Column(Modifier.padding(top = 16.dp, bottom = 64.dp)) {
        Text(
            text = stringResource(id = R.string.filter),
            modifier = Modifier
                .padding(8.dp)
                .padding(horizontal = 8.dp),
            color = ColorAccent,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { model.readFilterToggle() }
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(8.dp)
                .padding(horizontal = 8.dp)
        ) {
            val checkedColor by animateColorAsState(
                targetValue = when (model.readFilter.toToggleableState()) {
                    ToggleableState.Off -> Color.Green
                    ToggleableState.On -> Color.Green
                    ToggleableState.Indeterminate -> Color.Red
                },
                animationSpec = tween(250)
            )
            TriStateCheckbox(
                state = model.readFilter.toToggleableState(),
                onClick = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = checkedColor,
                    uncheckedColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                    disabledColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.25f),
                    disabledIndeterminateColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.25f),
                )
            )
            Text(text = stringResource(id = R.string.read), modifier = Modifier.padding(8.dp))
        }


        Text(
            text = stringResource(id = R.string.sort),
            modifier = Modifier
                .padding(8.dp)
                .padding(horizontal = 8.dp),
            color = ColorAccent,
            fontWeight = FontWeight.Bold
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { model.readSortToggle() }
                .fillMaxWidth()
                .heightIn(min = 40.dp)
                .padding(8.dp)
                .padding(horizontal = 8.dp)
        ) {
            AnimatedContent(targetState = model.readSort) {
                Icon(
                    imageVector = when (it) {
                        AppPreferences.TERNARY_STATE.active -> Icons.Default.ArrowUpward
                        AppPreferences.TERNARY_STATE.inverse -> Icons.Default.ArrowDownward
                        AppPreferences.TERNARY_STATE.inactive -> Icons.Default.SwapVert
                    },
                    contentDescription = null
                )
            }
            Text(text = stringResource(id = R.string.read), modifier = Modifier.padding(8.dp))
        }
    }
}