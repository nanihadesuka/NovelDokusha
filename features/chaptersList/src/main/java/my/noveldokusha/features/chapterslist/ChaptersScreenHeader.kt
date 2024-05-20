package my.noveldokusha.features.chapterslist

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import my.noveldoksuha.coreui.components.BookImageButtonView
import my.noveldoksuha.coreui.components.BookTitlePosition
import my.noveldoksuha.coreui.components.ExpandableText
import my.noveldoksuha.coreui.components.ImageView
import my.noveldoksuha.coreui.modifiers.bounceOnPressed
import my.noveldoksuha.coreui.theme.clickableNoIndicator
import my.noveldokusha.chapterslist.R
import my.noveldokusha.core.rememberResolvedBookImagePath

@Composable
internal fun ChaptersScreenHeader(
    bookState: ChaptersScreenState.BookState,
    sourceCatalogName: String,
    numberOfChapters: Int,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier,
    onCoverLongClick: () -> Unit,
    onGlobalSearchClick: (input: String) -> Unit,
) {
    val coverImageModel = bookState.coverImageUrl?.let {
        rememberResolvedBookImagePath(
            bookUrl = bookState.url,
            imagePath = it
        )
    } ?: R.drawable.ic_baseline_empty_24

    Box(modifier = modifier) {
        Box {
            ImageView(
                imageModel = coverImageModel,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .alpha(0.2f)
                    .fillMaxWidth()
                    .height(200.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            0f to MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                            1f to MaterialTheme.colorScheme.primary,
                        )
                    )
            )
        }
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                var showImageFullScreen by rememberSaveable { mutableStateOf(false) }
                val interactionSource = remember { MutableInteractionSource() }
                BookImageButtonView(
                    title = "",
                    coverImageModel = coverImageModel,
                    onClick = { showImageFullScreen = true },
                    onLongClick = onCoverLongClick,
                    bookTitlePosition = BookTitlePosition.Hidden,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .weight(1f)
                        .bounceOnPressed(interactionSource)
                )
                if (showImageFullScreen) Dialog(
                    onDismissRequest = { showImageFullScreen = false },
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = true
                    )
                ) {
                    ImageView(
                        imageModel = coverImageModel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickableNoIndicator { showImageFullScreen = false },
                        contentScale = ContentScale.Fit
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxHeight()
                        .weight(1f),
                ) {
                    SelectionContainer {
                        Text(
                            text = bookState.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 5,
                            modifier = Modifier.clickableNoIndicator {
                                onGlobalSearchClick(bookState.title)
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    SelectionContainer {
                        Text(
                            text = sourceCatalogName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                    SelectionContainer {
                        Text(
                            text = stringResource(id = R.string.chapters) + " " + numberOfChapters.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiary,
                        )
                    }
                }
            }
            SelectionContainer {
                val text by remember(bookState.description) { derivedStateOf { bookState.description.trim() } }
                ExpandableText(
                    text = text,
                    linesForExpand = 4
                )
            }
            HorizontalDivider(
                Modifier
                    .padding(horizontal = 40.dp)
                    .alpha(0.5f), thickness = Dp.Hairline
            )
        }
    }
}