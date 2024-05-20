package my.noveldokusha.webview

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme.colors
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import my.noveldoksuha.coreui.theme.InternalTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun <T : View> WebViewScreen(
    toolbarTitle: String,
    webViewFactory: (Context) -> T,
    onBackClicked: () -> Unit,
    onReloadClicked: () -> Unit,
) {
    Scaffold(
        topBar = {
            Surface {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Unspecified,
                        scrolledContainerColor = Color.Unspecified,
                    ),
                    title = {
                        Text(
                            text = toolbarTitle,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClicked) {
                            Icon(Icons.Default.Close, null)
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = onReloadClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.reload),
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                )
            }
        },
        content = {
            AndroidView(
                modifier = Modifier.padding(it),
                factory = webViewFactory
            )
        }
    )
}

@Preview
@Composable
private fun WebViewScreenPreview() {
    InternalTheme {
        WebViewScreen(
            toolbarTitle = "Title",
            webViewFactory = { View(it) },
            onBackClicked = {}
        )
    }
}