package my.noveldokusha.ui.screens.databaseSearchResults

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import my.noveldokusha.ui.theme.InternalTheme

@Composable
fun ToolbarMain(
    title: String,
    subtitle: String
){
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .fillMaxWidth()
            .padding(8.dp)
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.h6,
            fontWeight = FontWeight.Bold,
        )
        Text(text = subtitle, style = MaterialTheme.typography.subtitle1)
    }
}

@Preview
@Composable
fun Preview() {
    InternalTheme {
        ToolbarMain(
            title = "Database: Baka-Updates",
            subtitle = "Search by genre",
        )
    }
}