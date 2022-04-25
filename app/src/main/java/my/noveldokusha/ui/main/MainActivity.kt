package my.noveldokusha.ui.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.insets.navigationBarsPadding
import dagger.hilt.android.AndroidEntryPoint
import my.noveldokusha.AppPreferences
import my.noveldokusha.R
import my.noveldokusha.ui.main.finder.FinderView
import my.noveldokusha.ui.main.library.LibraryView
import my.noveldokusha.ui.main.library.LibraryViewModel
import my.noveldokusha.ui.main.settings.SettingsView
import my.noveldokusha.ui.theme.Theme
import my.noveldokusha.uiUtils.drawTopLine
import my.noveldokusha.uiUtils.mix
import javax.inject.Inject

@AndroidEntryPoint
open class MainActivity : ComponentActivity()
{
    enum class PAGE
    {
        LIBRARY, FINDER, SETTINGS
    }

    @Inject
    lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContent {
            var page by rememberSaveable { mutableStateOf(PAGE.LIBRARY) }

            Theme(appPreferences = appPreferences) {
                BottomSheetMain {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f)) {
                            when (page)
                            {
                                PAGE.LIBRARY -> LibraryView(context = this@MainActivity)
                                PAGE.FINDER -> FinderView(context = this@MainActivity)
                                PAGE.SETTINGS -> SettingsView(context = this@MainActivity)
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            modifier = Modifier
                                .background(MaterialTheme.colors.surface)
                                .drawTopLine()
                        ) {
                            NavItem(
                                text = stringResource(id = R.string.title_library),
                                painter = painterResource(id = R.drawable.ic_baseline_home_24),
                                onClick = { page = PAGE.LIBRARY },
                                selected = page == PAGE.LIBRARY,
                            )
                            NavItem(
                                text = stringResource(id = R.string.title_finder),
                                painter = painterResource(id = R.drawable.ic_baseline_menu_book_24),
                                onClick = { page = PAGE.FINDER },
                                selected = page == PAGE.FINDER,
                            )
                            NavItem(
                                text = stringResource(id = R.string.title_settings),
                                painter = painterResource(id = R.drawable.ic_twotone_settings_24),
                                onClick = { page = PAGE.SETTINGS },
                                selected = page == PAGE.SETTINGS,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetMain(body: @Composable () -> Unit)
{
    val model = viewModel<LibraryViewModel>()
    ModalBottomSheetLayout(
        sheetState = model.bottomSheetState,
        sheetBackgroundColor = MaterialTheme.colors.primary,
        scrimColor = MaterialTheme.colors.primary.copy(alpha = 0.4f),
        sheetContentColor = MaterialTheme.colors.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        sheetElevation = 20.dp,
        sheetContent = { OptionsSheetLibrary() },
        content = body
    )
}

fun AppPreferences.TERNARY_STATE.toToggleableState() = when (this)
{
    AppPreferences.TERNARY_STATE.active -> ToggleableState.On
    AppPreferences.TERNARY_STATE.inverse -> ToggleableState.Indeterminate
    AppPreferences.TERNARY_STATE.inactive -> ToggleableState.Off
}


@Composable
private fun RowScope.NavItem(
    text: String,
    painter: Painter,
    onClick: () -> Unit,
    selected: Boolean,
)
{
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(radius = 32.dp, bounded = false),
                onClick = onClick
            )
            .padding(vertical = 8.dp)
            .weight(1f)
    ) {
        val color = MaterialTheme.colors.onPrimary.mix(
            color = MaterialTheme.colors.primary,
            fraction = if (selected) 1f else 0.5f
        )
        Icon(
            painter = painter,
            contentDescription = text,
            tint = color
        )
        Text(
            text = text,
            modifier = Modifier.padding(4.dp),
            color = color
        )
    }
}
