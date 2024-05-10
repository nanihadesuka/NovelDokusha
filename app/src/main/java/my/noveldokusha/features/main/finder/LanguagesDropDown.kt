package my.noveldokusha.features.main.finder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldoksuha.data.LanguageItem
import my.noveldokusha.R
import my.noveldokusha.ui.composeViews.MyButton

@Composable
fun LanguagesDropDown(
    expanded: Boolean,
    onDismiss: () -> Unit,
    languageItemList: List<LanguageItem>,
    onSourceLanguageItemToggle: (LanguageItem) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(8.dp)
                .widthIn(min = 128.dp)
        ) {
            Text(text = stringResource(R.string.sources_languages))
            OutlinedCard {
                languageItemList.forEach { lang ->
                    MyButton(
                        text = stringResource(id = lang.language.nameResId),
                        onClick = { onSourceLanguageItemToggle(lang) },
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
}