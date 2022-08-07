package my.noveldokusha.ui.screens.chaptersList

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import my.noveldokusha.R
import my.noveldokusha.utils.blockInteraction
import my.noveldokusha.utils.drawBottomLine

@Composable
@Stable
fun MainToolbar(
    bookTitle: String,
    isBookmarked: Boolean,
    alpha: Float,
    onClickSortChapters: () -> Unit,
    onClickBookmark: () -> Unit,
    onClickChapterTitleSearch: () -> Unit,
    topPadding: Dp,
    height: Dp
) {
    val bookmarkColor by animateColorAsState(
        targetValue = when (isBookmarked) {
            true -> colorResource(id = R.color.dark_orange_red)
            false -> Color.Gray
        }
    )

    val primary = MaterialTheme.colors.primary
    val onPrimary = MaterialTheme.colors.onPrimary

    val backgroundColor by remember(alpha) { derivedStateOf { primary.copy(alpha = alpha) } }
    val borderColor by remember(alpha) { derivedStateOf { onPrimary.copy(alpha = alpha * 0.3f) } }
    val titleColor by remember(alpha) { derivedStateOf { onPrimary.copy(alpha = alpha) } }

    Row(
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .blockInteraction()
            .background(backgroundColor)
            .fillMaxWidth()
            .drawBottomLine(borderColor)
            .padding(top = topPadding, bottom = 0.dp, start = 12.dp, end = 12.dp)
            .height(height)
    ) {
        Text(
            text = bookTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .padding(16.dp)
                .weight(1f),
            color = titleColor
        )
        IconButton(onClick = onClickSortChapters) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_filter_list_24),
                contentDescription = stringResource(id = R.string.sort_ascending_or_descending)
            )
        }
        IconButton(onClick = onClickBookmark) {
            Icon(
                painter = painterResource(id = R.drawable.ic_twotone_bookmark_24),
                contentDescription = stringResource(id = R.string.library_bookmark),
                tint = bookmarkColor
            )
        }
        IconButton(onClick = onClickChapterTitleSearch) {
            Icon(
                painter = painterResource(id = R.drawable.ic_baseline_search_24),
                contentDescription = stringResource(id = R.string.search_by_title)
            )
        }
    }
}