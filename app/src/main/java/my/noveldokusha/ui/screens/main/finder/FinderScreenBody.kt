package my.noveldokusha.ui.screens.main.finder

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.network.NetworkClient
import my.noveldokusha.repository.SourceCatalogItem
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.Scraper
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.ui.composeViews.AnimatedTransition
import my.noveldokusha.ui.composeViews.ImageViewGlide
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.PreviewThemes
import okhttp3.Request
import okhttp3.Response

@OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)
@Composable
fun FinderScreenBody(
    innerPadding: PaddingValues,
    databasesList: List<DatabaseInterface>,
    sourcesList: List<SourceCatalogItem>,
    onDatabaseClick: (DatabaseInterface) -> Unit,
    onSourceClick: (SourceInterface.Catalog) -> Unit,
    onSourceSetPinned: (id: String, pinned: Boolean) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 300.dp),
        modifier = Modifier.padding(paddingValues = innerPadding)
    ) {
        item {
            Text(
                text = stringResource(id = R.string.database),
                style = MaterialTheme.typography.titleMedium,
                color = ColorAccent,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
            )
        }

        items(databasesList) {
            ListItem(
                modifier = Modifier
                    .clickable { onDatabaseClick(it) },
                headlineContent = {
                    Text(
                        text = it.name,
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                supportingContent = {
                    Text(
                        text = stringResource(R.string.english),
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                leadingContent = {
                    ImageViewGlide(
                        imageModel = it.iconUrl,
                        modifier = Modifier.size(28.dp),
                        error = R.drawable.default_icon
                    )
                }
            )
        }

        item {
            Text(
                text = stringResource(id = R.string.sources),
                style = MaterialTheme.typography.titleMedium,
                color = ColorAccent,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp),
            )
        }

        items(
            items = sourcesList,
            key = { it.catalog.id }
        ) {
            ListItem(
                modifier = Modifier
                    .clickable { onSourceClick(it.catalog) }
                    .animateItemPlacement(),
                headlineContent = {
                    Text(
                        text = it.catalog.name,
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                supportingContent = {
                    Text(
                        text = it.catalog.language,
                        style = MaterialTheme.typography.bodySmall,
                    )
                },
                leadingContent = {
                    ImageViewGlide(
                        imageModel = it.catalog.iconUrl,
                        modifier = Modifier.size(28.dp),
                        error = R.drawable.default_icon
                    )
                },
                trailingContent = {
                    IconButton(
                        onClick = { onSourceSetPinned(it.catalog.id, !it.pinned) },
                    ) {
                        AnimatedTransition(targetState = it.pinned) { pinned ->
                            Icon(
                                imageVector = if (pinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                contentDescription = stringResource(R.string.pin_or_unpin_source),
                            )
                        }
                    }
                }
            )
        }
    }
}

@PreviewThemes
@Composable
private fun PreviewView() {

    val scraper = Scraper(object : NetworkClient {
        override suspend fun get(url: String) = Response.Builder().build()
        override suspend fun get(url: Uri.Builder) = Response.Builder().build()
        override suspend fun call(request: Request.Builder, followRedirects: Boolean) =
            Response.Builder().build()
    })

    val sourcesList = scraper.sourcesListCatalog.toList().mapIndexed { index, it ->
        SourceCatalogItem(
            catalog = it, pinned = index < 3
        )
    }

    InternalTheme {
        FinderScreenBody(
            innerPadding = PaddingValues(),
            databasesList = scraper.databasesList.toList(),
            sourcesList = sourcesList,
            onDatabaseClick = {},
            onSourceClick = {},
            onSourceSetPinned = { _, _ -> },
        )
    }
}