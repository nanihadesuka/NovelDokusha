package my.noveldokusha.ui.theme

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.outlined.DataThresholding
import androidx.compose.material.icons.outlined.LooksOne
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TwoWheeler
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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

@OptIn(ExperimentalMaterial3Api::class)
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
            SuggestionChip(
                onClick = {},
                label = { Text(text = "SuggestionChip") }
            )
            Row {
                FilterChip(
                    selected = false,
                    onClick = {},
                    label = { Text(text = "FilterChip false") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null) },
                )
                Text(text = "-")
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(text = "FilterChip true ") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null) },
                )
            }
            AssistChip(
                onClick = {},
                label = { Text(text = "AssistChip") },
                leadingIcon = { Icon(Icons.Filled.Phone, null) },
            )
            ElevatedAssistChip(
                onClick = {},
                label = { Text(text = "ElevatedAssistChip") },
                leadingIcon = { Icon(Icons.Filled.Phone, null) },
            )
            ElevatedFilterChip(
                selected = false,
                onClick = {},
                label = { Text(text = "ElevatedFilterChip") },
                leadingIcon = { Icon(Icons.Filled.Phone, null) },
            )
            ElevatedSuggestionChip(
                onClick = {},
                label = { Text(text = "ElevatedSuggestionChip") },
            )
            Row {
                InputChip(
                    selected = false,
                    onClick = {},
                    label = { Text(text = "InputChip false") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null) },
                )
                Text(text = "-")
                InputChip(
                    selected = true,
                    onClick = {},
                    label = { Text(text = "InputChip true") },
                    leadingIcon = { Icon(Icons.Filled.Phone, null) },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(group = "miscellaneous")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, group = "miscellaneous")
@Composable
private fun PreviewMiscellaneous() {
    InternalTheme {
        Column {
            BottomAppBar {
                IconButton(onClick = { }) { Icon(Icons.Filled.Home, null) }
                IconButton(onClick = { }) { Icon(Icons.Filled.Home, null) }
                IconButton(onClick = { }) { Icon(Icons.Filled.Home, null) }
            }
            CenterAlignedTopAppBar(
                title = { Text(text = "CenterAlignedTopAppBar") },
                navigationIcon = { Icon(Icons.Filled.Menu, null) },
                actions = { Icon(Icons.Filled.Person, null) }
            )

            LargeTopAppBar(
                title = { Text(text = "LargeTopAppBar") },
                navigationIcon = { Icon(Icons.Filled.Menu, null) },
                actions = { Icon(Icons.Filled.Person, null) }
            )
            MediumTopAppBar(
                title = { Text(text = "MediumTopAppBar") },
                navigationIcon = { Icon(Icons.Filled.Menu, null) },
                actions = { Icon(Icons.Filled.Person, null) }
            )
            TabRow(selectedTabIndex = 1) {
                Tab(selected = true, onClick = {}) { Text(text = "1") }
                Tab(selected = false, onClick = {}) { Text(text = "2") }
                Tab(selected = false, onClick = {}) { Text(text = "3") }
            }
            NavigationBar {
                NavigationBarItem(
                    selected = true, onClick = { },
                    icon = { Icon(Icons.Outlined.LooksOne, null) },
                    label = { Text(text = "1") },
                )
                NavigationBarItem(
                    selected = false, onClick = { },
                    icon = { Icon(Icons.Outlined.TwoWheeler, null) },
                    label = { Text(text = "2") },
                )
                NavigationBarItem(
                    selected = false, onClick = { },
                    icon = { Icon(Icons.Outlined.DataThresholding, null) },
                    label = { Text(text = "3") },
                )
            }
            ListItem(
                overlineContent = { Text(text = "overlineContent") },
                headlineContent = { Text(text = "Headline") },
                supportingContent = { Text(text = "supportingContent") },
                leadingContent = { Icon(Icons.Filled.Face, null) },
                trailingContent = { Icon(Icons.Filled.Face, null) },
            )
            // Other stuff
            Snackbar {
                Text(text = "Snackbar")
            }
            Row {
                Switch(checked = false, onCheckedChange = {})
                Switch(checked = true, onCheckedChange = {})
            }
            Row {
                Checkbox(checked = false, onCheckedChange = {})
                Checkbox(checked = true, onCheckedChange = {})
            }
        }
    }
}


@Preview(group = "textfields")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, group = "textfields")
@Composable
private fun PreviewTextFields() {
    InternalTheme {
        Column {
            Text(text = "Text")
            TextField(
                value = "TextField",
                onValueChange = {},
                label = { Text("label") },
                placeholder = { Text("placeholder") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                trailingIcon = { Icon(Icons.Filled.Person, null) },
                isError = false,
            )
            TextField(
                value = "TextField error",
                onValueChange = {},
                label = { Text("label") },
                placeholder = { Text("placeholder") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                trailingIcon = { Icon(Icons.Filled.Person, null) },
                isError = true,
            )
            OutlinedTextField(
                value = "OutlinedTextField",
                onValueChange = {},
                label = { Text("label") },
                placeholder = { Text("placeholder") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                trailingIcon = { Icon(Icons.Filled.Person, null) },
                isError = false,
            )
            OutlinedTextField(
                value = "OutlinedTextField error",
                onValueChange = {},
                label = { Text("label") },
                placeholder = { Text("placeholder") },
                leadingIcon = { Icon(Icons.Filled.Person, null) },
                trailingIcon = { Icon(Icons.Filled.Person, null) },
                isError = true,
            )
            BasicTextField(
                value = "BasicTextField",
                onValueChange = {},
            )
        }
    }
}