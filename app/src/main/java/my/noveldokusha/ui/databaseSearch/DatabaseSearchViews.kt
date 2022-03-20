package my.noveldokusha.ui.databaseSearch

import android.util.Log
import android.view.KeyEvent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.Blue600
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.Themes

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CheckBoxCategory(
    state: ToggleableState,
    text: String,
    onStateChanged: (state: ToggleableState) -> Unit,
    modifier: Modifier = Modifier
) {
    val checkedColor by animateColorAsState(
        targetValue = when (state) {
            ToggleableState.Off -> Color.Green
            ToggleableState.On -> Color.Green
            ToggleableState.Indeterminate -> Color.Red
        },
        animationSpec = tween(250)
    )

    val onClick = {
        val nextState = when (state) {
            ToggleableState.Off -> ToggleableState.On
            ToggleableState.On -> ToggleableState.Indeterminate
            ToggleableState.Indeterminate -> ToggleableState.Off
        }
        onStateChanged(nextState)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onClick() }
    ) {
        TriStateCheckbox(
            state = state,
            onClick = onClick,
            colors = CheckboxDefaults.colors(
                checkedColor = checkedColor,
                uncheckedColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                disabledColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.25f),
                disabledIndeterminateColor = MaterialTheme.colors.onPrimary.copy(alpha = 0.25f),
            )
        )
        Text(
            text = text,
            modifier = Modifier
                .padding(8.dp)
                .weight(1f)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchGenres(
    list: SnapshotStateList<GenreItem>,
    onSearchClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Button(
            onClick = onSearchClick,
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
        ) {
            Text(text = stringResource(id = R.string.search_with_filters))
        }

        LazyVerticalGrid(
            cells = GridCells.Fixed(2)
        ) {
            itemsIndexed(list) { index, it ->
                CheckBoxCategory(
                    state = it.state,
                    text = it.genre,
                    onStateChanged = { state -> list[index] = it.copy(state = state) }
                )
            }
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DatabaseSearchView(
    onBackStack: () -> Unit,
    title: String,
    subtitle: String,
    searchText: MutableState<String>,
    genresList: SnapshotStateList<GenreItem>,
    onTitleSearchClick: (text: String) -> Unit,
    onGenreSearchClick: () -> Unit,
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(showSearch) {
        if (showSearch)
            focusRequester.requestFocus()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearch) {
                        BackHandler { showSearch = false }
                        BasicTextField(
                            value = searchText.value,
                            onValueChange = { searchText.value = it },
                            singleLine = true,
                            maxLines = 1,
                            textStyle = LocalTextStyle.current.copy(
                                color = MaterialTheme.colors.onPrimary
                            ),
                            cursorBrush = SolidColor(Blue600),
                            keyboardActions = KeyboardActions(
                                onDone = { onTitleSearchClick(searchText.value) }
                            ),
                            decorationBox = {
                                if (searchText.value.isBlank()) Text(
                                    text = "Search by title",
                                    color = MaterialTheme.colors.onPrimary.copy(alpha = 0.3f)
                                ) else it()
                            },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .fillMaxWidth(),
                        )
                    } else {
                        Column {
                            Text(text = title)
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.subtitle1
                            )
                        }
                    }
                },
                navigationIcon = {
                    when {
                        showSearch ->
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search by title",
                                modifier = Modifier.padding(start = 14.dp)
                            )
                        else ->
                            IconButton(
                                onClick = when (showSearch) {
                                    true -> {
                                        { showSearch = false }
                                    }
                                    false -> onBackStack
                                }
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Go back"
                                )
                            }
                    }
                },
                elevation = 0.dp,
                backgroundColor = Color.Transparent,
                actions = {
                    when {
                        !showSearch ->
                            IconButton(onClick = { showSearch = true }) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search by title"
                                )
                            }
                        else ->
                            IconButton(onClick = {
                                if (searchText.value.isBlank())
                                    showSearch = false
                                else searchText.value = ""
                            }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Search by title"
                                )
                            }
                    }
                }
            )
        },
        content = {
            SearchGenres(list = genresList, onSearchClick = onGenreSearchClick)
        }
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewView() {
    val list = remember {
        mutableStateListOf<GenreItem>(
            GenreItem("Fantasy", "fn", ToggleableState.Off),
            GenreItem("Horror", "fn", ToggleableState.Off),
            GenreItem("Honesty", "fn", ToggleableState.Off),
            GenreItem("Drama", "fn", ToggleableState.Off),
            GenreItem("Historical", "fn", ToggleableState.Off),
            GenreItem("Crime", "fn", ToggleableState.Off),
            GenreItem("Seinen", "fn", ToggleableState.Off),
            GenreItem("Action", "fn", ToggleableState.Off),
            GenreItem("Fantasy", "fn", ToggleableState.Off),
            GenreItem("Horror", "fn", ToggleableState.Off),
            GenreItem("Honesty", "fn", ToggleableState.Off),
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

    InternalTheme(Themes.LIGHT) {
        DatabaseSearchView(
            onBackStack = { },
            title = "Database",
            subtitle = "Baka-Updates",
            searchText = remember { mutableStateOf("hero") },
            genresList = list,
            onTitleSearchClick = {},
            onGenreSearchClick = {}
        )
    }
}

