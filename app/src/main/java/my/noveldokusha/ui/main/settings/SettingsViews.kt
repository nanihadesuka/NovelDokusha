package my.noveldokusha.ui.main.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.ui.theme.ColorAccent
import my.noveldokusha.ui.theme.InternalTheme
import my.noveldokusha.ui.theme.Themes
import my.noveldokusha.uiUtils.drawBottomLine
import my.noveldokusha.uiUtils.ifCase
import my.noveldokusha.uiViews.MyButton

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun SettingsTheme(
    currentTheme: Themes,
    currentFollowSystem: Boolean,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
)
{
    Column(Modifier.padding(horizontal = 8.dp)) {
        Text(
            text = stringResource(id = R.string.theme),
            color = ColorAccent,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {

            val textColor = when (currentFollowSystem)
            {
                true -> Color.White
                false -> MaterialTheme.colors.onPrimary
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .padding(0.dp)
                    .clip(CircleShape)
                    .toggleable(
                        value = currentFollowSystem,
                        onValueChange = { onFollowSystem(!currentFollowSystem) }
                    )
                    .background(
                        if (currentFollowSystem) ColorAccent
                        else MaterialTheme.colors.onPrimary.copy(alpha = 0.15f)
                    )
                    .padding(8.dp)
                    .padding(start = 6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.follow_system),
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                AnimatedContent(targetState = currentFollowSystem) { follow ->
                    Icon(
                        if (follow) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel,
                        contentDescription = null,
                        tint = textColor
                    )
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            val lines = Themes.pairs.toList().chunked(2)
            for (line in lines) Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for ((theme, themeName) in line) MyButton(
                    text = themeName,
                    onClick = { onThemeSelected(theme) },
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                    outterPadding = 0.dp
                ) { _, _, _ ->
                    val textColor = when (currentTheme == theme)
                    {
                        true -> Color.White
                        false -> MaterialTheme.colors.onPrimary
                    }
                    Text(
                        text = themeName,
                        modifier = Modifier
                            .ifCase(currentTheme == theme) {
                                background(ColorAccent)
                            }
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
fun ToolbarMain(title: String)
{
    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .drawBottomLine()
            .padding(top = 8.dp, bottom = 0.dp, start = 12.dp, end = 12.dp)
            .height(56.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ClickableOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    info: String = "",
)
{
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .then(modifier)
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.padding(vertical = 2.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.caption,
            modifier = Modifier.padding(vertical = 1.dp)
        )
        if (info.isNotBlank()) Text(
            text = info,
            style = MaterialTheme.typography.caption
        )
    }
}

@Composable
private fun SettingsData(
    databaseSize: String,
    imagesFolderSize: String,
    onCleanDatabase: () -> Unit,
    onCleanImageFolder: () -> Unit
)
{
    Column {
        Text(
            text = stringResource(id = R.string.data),
            color = ColorAccent,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        ClickableOption(
            title = stringResource(id = R.string.clean_database),
            subtitle = stringResource(id = R.string.preserve_only_library_books_data),
            info = stringResource(id = R.string.database_size) + " " + databaseSize,
            onClick = onCleanDatabase
        )
        ClickableOption(
            title = stringResource(id = R.string.clean_images_folder),
            subtitle = stringResource(id = R.string.preserve_only_images_from_library_books),
            info = stringResource(id = R.string.images_total_size) + " " + imagesFolderSize,
            onClick = onCleanImageFolder
        )
    }
}

@Composable
private fun SettingsBackup(
    onBackupData: () -> Unit = {},
    onRestoreData: () -> Unit = {},
)
{
    Column {
        Text(
            text = stringResource(id = R.string.backup),
            color = ColorAccent,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        ClickableOption(
            title = stringResource(id = R.string.backup_data),
            subtitle = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_saving_location),
            onClick = onBackupData
        )
        ClickableOption(
            title = stringResource(id = R.string.restore_data),
            subtitle = stringResource(id = R.string.opens_the_file_explorer_to_select_the_backup_file),
            onClick = onRestoreData
        )
    }
}

@Composable
fun SettingsBody(
    currentFollowSystem: Boolean,
    currentTheme: Themes,
    onFollowSystem: (Boolean) -> Unit,
    onThemeSelected: (Themes) -> Unit,
    databaseSize: String,
    imagesFolderSize: String,
    onCleanDatabase: () -> Unit,
    onCleanImageFolder: () -> Unit,
    onBackupData: () -> Unit,
    onRestoreData: () -> Unit
)
{
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp)
    ) {
        SettingsTheme(
            currentFollowSystem = currentFollowSystem,
            currentTheme = currentTheme,
            onFollowSystem = onFollowSystem,
            onThemeSelected = onThemeSelected
        )
        SettingsData(
            databaseSize = databaseSize,
            imagesFolderSize = imagesFolderSize,
            onCleanDatabase = onCleanDatabase,
            onCleanImageFolder = onCleanImageFolder
        )
        SettingsBackup(
            onBackupData = onBackupData,
            onRestoreData = onRestoreData
        )
        Spacer(modifier = Modifier.height(500.dp))
        Text(
            text = "(°.°)",
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colors.onPrimary.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Preview(
    device = Devices.PIXEL_4_XL
)
@Composable
fun Preview()
{
    val currentTheme = Themes.DARK
    InternalTheme(currentTheme) {
        SettingsBody(
            currentFollowSystem = true,
            currentTheme = currentTheme,
            onFollowSystem = { },
            onThemeSelected = { },
            databaseSize = "1 MB",
            imagesFolderSize = "10 MB",
            onCleanDatabase = { },
            onCleanImageFolder = { },
            onBackupData = { },
            onRestoreData = { }
        )
    }
}