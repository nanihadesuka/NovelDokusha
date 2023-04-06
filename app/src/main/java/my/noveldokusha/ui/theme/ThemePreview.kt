package my.noveldokusha.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Chip
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ElevatedSuggestionChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LargeFloatingActionButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(group = "button")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, group = "button")
@Composable
private fun PreviewButtons() {
    InternalTheme {
        Column {
            Button(onClick = {}) {
                Text("Button")
            }
            TextButton(onClick = {}) {
                Text("TextButton")
            }
            ElevatedButton(onClick = {}) {
                Text("ElevatedButton")
            }
            OutlinedButton(onClick = {}) {
                Text("OutlinedButton")
            }
            OutlinedIconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.Settings, null)
            }
            Row {
                OutlinedIconToggleButton(checked = false, onCheckedChange = { }) {
                    Icon(imageVector = Icons.Outlined.Settings, null)
                }
                Text(text = " - ")
                OutlinedIconToggleButton(checked = true, onCheckedChange = { }) {
                    Icon(imageVector = Icons.Outlined.Settings, null)
                }
            }
            ExtendedFloatingActionButton(onClick = {}) {
                Text("ExtendedFloatingActionButton")
            }
            FilledIconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.Settings, null)
            }
            Row {
                FilledIconToggleButton(checked = false, onCheckedChange = {}) {
                    Icon(imageVector = Icons.Outlined.Settings, null)
                }
                Text(text = " - ")
                FilledIconToggleButton(checked = true, onCheckedChange = {}) {
                    Icon(imageVector = Icons.Outlined.Settings, null)
                }
            }
            FilledTonalButton(onClick = {}) {
                Text("FilledTonalButton")
            }
            FilledTonalIconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.Settings, null)
            }
            Row {
                FilledTonalIconToggleButton(checked = false, onCheckedChange = {}) {
                    Icon(imageVector = Icons.Outlined.Settings, null)
                }
                Text(text = " - ")
                FilledTonalIconToggleButton(checked = true, onCheckedChange = {}) {
                    Icon(imageVector = Icons.Outlined.Settings, null)
                }
            }
            FloatingActionButton(onClick = {}) {
                Text("FloatingActionButton")
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.Settings, null)
            }
            Row {
                IconToggleButton(checked = false, onCheckedChange = {}) {
                    Icon(imageVector = Icons.Outlined.Settings, null)
                }
                Text(text = " - ")
                IconToggleButton(checked = true, onCheckedChange = {}) {
                    Icon(imageVector = Icons.Outlined.Settings, null)
                }
            }
            LargeFloatingActionButton(onClick = {}) {
                Text("LargeFloatingActionButton")
            }
            Row {
                RadioButton(selected = false, onClick = {})
                Text(text = " - ")
                RadioButton(selected = true, onClick = {})
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Preview(group = "card & chip")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, group = "card & chip")
@Composable
private fun PreviewCards() {
    InternalTheme {
        Column {
            // Cards
            Card {
                Text(text = "Card")
            }
            Card(onClick = {}) {
                Text(text = "Card clickable")
            }
            ElevatedCard {
                Text(text = "ElevatedCard")
            }
            ElevatedCard(onClick = {}) {
                Text(text = "ElevatedCard clickable")
            }
            OutlinedCard {
                Text(text = "OutlinedCard")
            }
            OutlinedCard(onClick = {}) {
                Text(text = "OutlinedCard clickable")
            }

            Divider()

            // Chips
            Chip(onClick = {}) {
                Text(text = "Card")
            }
            SuggestionChip(
                onClick = {},
                label = { Text(text = "SuggestionChip") }
            )
            Row {
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(text = "FilterChip false") }
                )
                Text(text = "-")
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(text = "FilterChip true ") }
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(text = "AssistChip") }
            )
            ElevatedAssistChip(
                onClick = {},
                label = { Text(text = "ElevatedAssistChip") }
            )
            ElevatedFilterChip(
                selected = false,
                onClick = {},
                label = { Text(text = "ElevatedFilterChip") }
            )
            ElevatedSuggestionChip(
                onClick = {},
                label = { Text(text = "ElevatedSuggestionChip") }
            )
            Row {
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(text = "InputChip false") }
                )
                Text(text = "-")
                InputChip(
                    selected = true,
                    onClick = {},
                    label = { Text(text = "InputChip true") }
                )
            }
        }
    }
}