package my.noveldokusha.features.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.IntentCompat
import dagger.hilt.android.AndroidEntryPoint
import my.noveldoksuha.coreui.BaseActivity
import my.noveldoksuha.coreui.components.AnimatedTransition
import my.noveldoksuha.coreui.theme.Theme
import my.noveldokusha.R
import my.noveldokusha.catalogexplorer.CatalogExplorerScreen
import my.noveldokusha.libraryexplorer.LibraryScreen
import my.noveldokusha.settings.SettingsScreen
import my.noveldokusha.tooling.epub_importer.EpubImportService

private data class Page(
    @DrawableRes val iconRes: Int,
    @StringRes val stringRes: Int,
)

private val pages = listOf(
    Page(iconRes = R.drawable.ic_baseline_home_24, stringRes = R.string.title_library),
    Page(iconRes = R.drawable.ic_baseline_menu_book_24, stringRes = R.string.title_finder),
    Page(iconRes = R.drawable.ic_twotone_settings_24, stringRes = R.string.title_settings),
)


@OptIn(ExperimentalAnimationApi::class)
@AndroidEntryPoint
open class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var activePageIndex by rememberSaveable { mutableIntStateOf(0) }

            BackHandler(enabled = activePageIndex != 0) {
                activePageIndex = 0
            }

            Theme(themeProvider = themeProvider) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        AnimatedTransition(targetState = activePageIndex) {
                            when (it) {
                                0 -> LibraryScreen()
                                1 -> CatalogExplorerScreen()
                                2 -> SettingsScreen()
                            }
                        }
                    }
                    NavigationBar {
                        pages.forEachIndexed { pageIndex, page ->
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        painter = painterResource(id = page.iconRes),
                                        contentDescription = stringResource(id = page.stringRes)
                                    )
                                },
                                label = { Text(stringResource(id = page.stringRes)) },
                                selected = activePageIndex == pageIndex,
                                onClick = {
                                    activePageIndex = pageIndex
                                },
                            )
                        }
                    }
                }
            }
        }

        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val action = intent.action ?: return
        val type = intent.type

        when (action) {
            Intent.ACTION_SEND -> {
                if (type == "application/epub+zip") {
                    handleSharedEpub(intent)
                }
            }

            Intent.ACTION_VIEW -> {
                handleViewedEpub(intent)
            }
        }
    }

    private fun handleViewedEpub(intent: Intent) {
        val epubUri: Uri? = intent.data
        if (epubUri != null) {
            EpubImportService.start(ctx = this, uri = epubUri)
        }
    }

    private fun handleSharedEpub(intent: Intent) {
        val epubUri: Uri? = IntentCompat.getParcelableExtra(
            intent, Intent.EXTRA_STREAM, Uri::class.java
        )
        if (epubUri != null) {
            EpubImportService.start(ctx = this, uri = epubUri)
        }
    }
}

