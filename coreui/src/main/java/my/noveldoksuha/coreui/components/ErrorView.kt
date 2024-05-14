package my.noveldoksuha.coreui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.noveldoksuha.coreui.R

@Composable
fun ErrorView(
    error: String,
    onReload: (() -> Unit)? = null,
    onCopyError: ((String) -> Unit)? = null
) {
    @Composable
    fun Modifier.click(onClick: () -> Unit) = clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(color = MaterialTheme.colorScheme.onError),
        onClick = onClick
    )

    Column(
        Modifier
            .padding(4.dp)
            .border(0.5.dp, Color.Red, RoundedCornerShape(4.dp))
    ) {
        if (onReload != null || onCopyError != null) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                if (onReload != null)
                    Text(
                        text = stringResource(R.string.reload),
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .click(onReload)
                            .weight(1f)
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
                if (onReload != null && onCopyError != null)
                    DividerVertical(
                        color = MaterialTheme.colorScheme.onError,
                        thickness = 0.5.dp,
                    )
                if (onCopyError != null)
                    Text(
                        text = stringResource(R.string.copy_error),
                        color = MaterialTheme.colorScheme.onError,
                        modifier = Modifier
                            .click(onClick = { onCopyError(error) })
                            .weight(1f)
                            .padding(12.dp),
                        textAlign = TextAlign.Center,
                    )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onError)
        }
        SelectionContainer {
            Text(
                text = error,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 0.sp,
                color = MaterialTheme.colorScheme.onError,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}