package my.noveldokusha.uiViews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldokusha.R

@Composable
fun ErrorView(
    error: String,
    onReload: () -> Unit,
    onCopyError: (String) -> Unit
)
{
    @Composable
    fun Modifier.click(onClick: () -> Unit) = clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(color = MaterialTheme.colors.onError),
        onClick = onClick
    )

    Column(
        Modifier
            .padding(4.dp)
            .border(0.5.dp, Color.Red, RoundedCornerShape(4.dp))
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Text(
                text = stringResource(R.string.reload),
                color = MaterialTheme.colors.onError,
                modifier = Modifier
                    .click(onReload)
                    .weight(1f)
                    .padding(12.dp),
                textAlign = TextAlign.Center,
            )
            DividerVertical(
                color = MaterialTheme.colors.onError,
                thickness = 0.5.dp,
            )
            Text(
                text = stringResource(R.string.copy_error),
                color = MaterialTheme.colors.onError,
                modifier = Modifier
                    .click(onClick = { onCopyError(error) })
                    .weight(1f)
                    .padding(12.dp),
                textAlign = TextAlign.Center,
            )
        }
        Divider(color = MaterialTheme.colors.onError)
        SelectionContainer {
            Text(
                text = error,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.sp,
                color = MaterialTheme.colors.onError,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}