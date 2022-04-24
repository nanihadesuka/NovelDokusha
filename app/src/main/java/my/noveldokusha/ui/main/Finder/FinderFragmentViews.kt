package my.noveldokusha.ui.main.Finder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.scraper.DatabaseInterface
import my.noveldokusha.scraper.SourceInterface
import my.noveldokusha.scraper.scraper
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.uiViews.MyButton

@Composable
private fun Button(
    text: String,
    onClick: () -> Unit,
)
{
    MyButton(
        text = text,
        onClick = onClick,
        outterPadding = 0.dp,
        backgroundColor = MaterialTheme.colors.primary,
        modifier = Modifier.fillMaxWidth(),
        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.Bold)
    )
}

@Composable
fun FinderBody(
    databasesList: List<DatabaseInterface>,
    sourcesList: List<SourceInterface.catalog>,
    onDatabaseClick: (DatabaseInterface) -> Unit,
    onSourceClick: (SourceInterface.catalog) -> Unit
)
{
    LazyColumn(
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 200.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            Text(
                text = stringResource(id = R.string.database),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.subtitle1,
                color = ColorAccent,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        items(databasesList) {
            Button(
                text = it.name,
                onClick = { onDatabaseClick(it) },
            )
        }

        item {
            Text(
                text = stringResource(id = R.string.sources),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.subtitle1,
                color = ColorAccent,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        items(sourcesList) {
            Button(
                text = it.name,
                onClick = { onSourceClick(it) },
            )
        }
    }
}

@Composable
fun ToolbarMain(
    title: String,
    onSearchPress: () -> Unit,
    onLanguagesOptionsPress: () -> Unit,
    languagesDropDownView: @Composable () -> Unit
)
{
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 0.dp, start = 12.dp, end = 12.dp)
            .height(56.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onSearchPress) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_search_24),
                contentDescription = stringResource(R.string.search_for_title)
            )
        }

        IconButton(onClick = onLanguagesOptionsPress) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_languages_24),
                contentDescription = stringResource(R.string.open_for_more_options)
            )
            languagesDropDownView()
        }
    }
}

data class LanguagesActive(val language: String, val active: Boolean)

@Composable
fun LanguagesDropDown(
    expanded: Boolean,
    list: List<LanguagesActive>,
    onDismiss: () -> Unit,
    onToggleLanguage: (LanguagesActive) -> Unit
)
{
    @Composable
    fun colorBackground(active: Boolean) =
        if (active) ColorAccent else MaterialTheme.colors.surface

    @Composable
    fun colorText(active: Boolean) =
        if (active) Color.White else MaterialTheme.colors.onPrimary

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Column {
            Text(
                text = stringResource(R.string.sources_languages),
                Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Column(
                Modifier
                    .padding(horizontal = 8.dp)
                    .border(
                        Dp.Hairline,
                        MaterialTheme.colors.onPrimary.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .clip(RoundedCornerShape(8.dp))
            ) {
                list.forEach { lang ->
                    Box(
                        Modifier
                            .background(colorBackground(lang.active))
                            .clickable { onToggleLanguage(lang) }
                    ) {
                        Text(
                            text = lang.language,
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            color = colorText(lang.active)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewView()
{
    InternalTheme {
        FinderBody(
            databasesList = scraper.databasesList.toList(),
            sourcesList = scraper.sourcesListCatalog.toList(),
            onDatabaseClick = {},
            onSourceClick = {}
        )
    }
}





