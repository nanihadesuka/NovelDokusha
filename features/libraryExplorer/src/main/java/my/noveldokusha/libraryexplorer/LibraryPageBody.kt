package my.noveldokusha.libraryexplorer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.components.BookImageButtonView
import my.noveldoksuha.coreui.modifiers.bounceOnPressed
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.ImageBorderShape
import my.noveldokusha.core.isLocalUri
import my.noveldokusha.core.rememberResolvedBookImagePath
import my.noveldokusha.feature.local_database.BookWithContext

@Composable
internal fun LibraryPageBody(
    list: List<BookWithContext>,
    onClick: (BookWithContext) -> Unit,
    onLongClick: (BookWithContext) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(160.dp),
        contentPadding = PaddingValues(top = 4.dp, bottom = 400.dp, start = 4.dp, end = 4.dp)
    ) {
        items(
            items = list,
            key = { it.book.url }
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            Box {
                BookImageButtonView(
                    title = it.book.title,
                    coverImageModel = rememberResolvedBookImagePath(
                        bookUrl = it.book.url,
                        imagePath = it.book.coverImageUrl
                    ),
                    onClick = { onClick(it) },
                    onLongClick = { onLongClick(it) },
                    interactionSource = interactionSource,
                    modifier = Modifier.bounceOnPressed(interactionSource)
                )
                val notReadCount = it.chaptersCount - it.chaptersReadCount
                AnimatedVisibility(
                    visible = notReadCount != 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = notReadCount.toString(),
                        color = Color.White,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(ColorAccent, ImageBorderShape)
                            .padding(4.dp)
                    )
                }

                if (it.book.url.isLocalUri) Text(
                    text = stringResource(R.string.local),
                    color = Color.White,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(ColorAccent, ImageBorderShape)
                        .padding(4.dp)
                )
            }
        }
    }
}