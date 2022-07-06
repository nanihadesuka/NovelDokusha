package my.noveldokusha.ui.screens.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import my.noveldokusha.R
import my.noveldokusha.data.database.tables.Book
import my.noveldokusha.ui.screens.main.library.LibraryViewModel
import my.noveldokusha.uiUtils.drawBottomLine
import my.noveldokusha.uiViews.ImageView

@Composable
fun OptionsDialogBookLibrary(book: Book)
{
    val model = viewModel<LibraryViewModel>()
    Card {
        Column(Modifier.padding(top = 0.dp, bottom = 16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .drawBottomLine()
                    .padding(8.dp)
            ) {
                ImageView(
                    imageModel = book.coverImageUrl,
                    error = R.drawable.default_book_cover,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                Text(
                    text = book.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.h6,
                    modifier = Modifier
                        .padding(8.dp)
                        .weight(1f)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clickable { model.bookCompletedToggle(book.url) }
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                val checkState by remember {
                    derivedStateOf {
                        when (model.readFilter.toToggleableState())
                        {
                            ToggleableState.Off -> Color.Green
                            ToggleableState.On -> Color.Green
                            ToggleableState.Indeterminate -> Color.Red
                        }
                    }
                }
                val checkedColor by animateColorAsState(
                    targetValue = checkState,
                    animationSpec = tween(250)
                )
                Checkbox(
                    checked = book.completed,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = checkedColor,
                        uncheckedColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                        disabledColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.25f),
                        disabledIndeterminateColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.25f),
                    )
                )
                Text(
                    text = "Completed",
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .weight(1f)
                )
            }
        }
    }
}