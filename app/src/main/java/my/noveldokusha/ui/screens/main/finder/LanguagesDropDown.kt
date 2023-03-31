package my.noveldokusha.ui.screens.main.finder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.repository.SourceLanguageItem
import my.noveldokusha.ui.composeViews.MyButton
import my.noveldokusha.utils.backgroundRounded
import my.noveldokusha.utils.outlineRounded

@Composable
fun LanguagesDropDown(
    expanded: Boolean,
    list: List<SourceLanguageItem>,
    onDismiss: () -> Unit,
    onToggleLanguage: (SourceLanguageItem) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Text(
            text = stringResource(R.string.sources_languages),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
        )
        Column(
            Modifier
                .padding(horizontal = 12.dp)
                .backgroundRounded()
                .outlineRounded(width = Dp.Hairline)
                .widthIn(min = 128.dp)
        ) {
            list.forEach { lang ->
                MyButton(
                    text = lang.language,
                    onClick = { onToggleLanguage(lang) },
                    selected = lang.active,
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