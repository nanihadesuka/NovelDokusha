package my.noveldokusha.settings.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import my.noveldoksuha.coreui.theme.ColorAccent
import my.noveldoksuha.coreui.theme.textPadding
import my.noveldokusha.settings.R
import my.noveldokusha.settings.SettingsScreenState

// ── Prompt presets ──────────────────────────────────────────────────────
// Names must match the keys the translator_nop module recognises.
// When a preset is selected, the user can also override with a custom prompt
// via the "System Prompt" field below — empty = use preset default.

private data class ProviderOption(val key: String, val labelRes: Int)

private val PROVIDERS = listOf(
    ProviderOption("GOOGLE_PA", R.string.translation_provider_google_pa),
    ProviderOption("GOOGLE_FREE", R.string.translation_provider_google_free),
    ProviderOption("GEMINI", R.string.translation_provider_gemini),
    ProviderOption("OPENAI", R.string.translation_provider_openai),
)

private val PROMPT_PRESETS = listOf(
    "Balanced (Default)",
    "Minimal",
    "Detailed",
    "Adult (18+)",
    "Direct Asian",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTranslation(
    state: SettingsScreenState.TranslationSettings,
) {
    val ctx = LocalContext.current
    var providerExpanded by remember { mutableStateOf(false) }
    var presetExpanded by remember { mutableStateOf(false) }

    Column {
        Text(
            text = stringResource(R.string.settings_title_translation_models),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.textPadding(),
            color = ColorAccent
        )

        // ── Provider selector ────────────────────────────────────────────
        ListItem(
            headlineContent = {
                Text(text = stringResource(R.string.settings_translation_provider))
            },
            supportingContent = {
                Text(text = stringResource(R.string.settings_translation_provider_description))
            },
            leadingContent = {
                Icon(Icons.Outlined.Translate, null, tint = MaterialTheme.colorScheme.onPrimary)
            }
        )
        ExposedDropdownMenuBox(
            expanded = providerExpanded,
            onExpandedChange = { providerExpanded = it },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = PROVIDERS.firstOrNull { it.key == state.provider.value }?.let {
                    ctx.getString(it.labelRes)
                } ?: state.provider.value,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.translation_active_provider)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = providerExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = providerExpanded,
                onDismissRequest = { providerExpanded = false }
            ) {
                PROVIDERS.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(ctx.getString(opt.labelRes)) },
                        onClick = {
                            state.provider.value = opt.key
                            providerExpanded = false
                        }
                    )
                }
            }
        }

        val provider = state.provider.value

        // ── Provider-specific config ─────────────────────────────────────
        when (provider) {
            "GOOGLE_PA" -> GooglePaSection(state)
            "GOOGLE_FREE" -> GoogleFreeSection()
            "GEMINI" -> {
                GeminiSection(state)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LlmSection(state, presetExpanded = presetExpanded, onPresetExpandedChange = { presetExpanded = it })
            }
            "OPENAI" -> {
                OpenAiSection(state)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LlmSection(state, presetExpanded = presetExpanded, onPresetExpandedChange = { presetExpanded = it })
            }
        }
    }
}

// ── Google PA ───────────────────────────────────────────────────────────
@Composable
private fun GooglePaSection(state: SettingsScreenState.TranslationSettings) {
    Section(title = stringResource(R.string.translation_section_google_pa)) {
        Text(
            text = stringResource(R.string.translation_google_pa_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        OutlinedTextField(
            value = state.googlePaApiKeys.value,
            onValueChange = { state.googlePaApiKeys.value = it },
            label = { Text(stringResource(R.string.translation_api_key_optional)) },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            minLines = 1,
            maxLines = 3
        )
    }
}

// ── Google Free ─────────────────────────────────────────────────────────
@Composable
private fun GoogleFreeSection() {
    Section(title = stringResource(R.string.translation_provider_google_free)) {
        Text(
            text = stringResource(R.string.translation_google_free_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Text(
            text = stringResource(R.string.translation_no_config_needed),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

// ── Gemini ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GeminiSection(state: SettingsScreenState.TranslationSettings) {
    Section(title = stringResource(R.string.translation_section_gemini)) {
        var showApiKey by remember { mutableStateOf(false) }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.geminiApiKey.value,
                onValueChange = { state.geminiApiKey.value = it },
                label = { Text(stringResource(R.string.translation_api_key)) },
                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    TextButton(onClick = { showApiKey = !showApiKey }) {
                        Text(if (showApiKey) "Hide" else "Show")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = state.geminiModel.value,
                onValueChange = { state.geminiModel.value = it },
                label = { Text(stringResource(R.string.translation_model)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                singleLine = true
            )
            val tempLabel = stringResource(R.string.translation_temperature)
            Text(
                text = "$tempLabel: ${"%.2f".format(state.geminiTemperature.value)}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
            Slider(
                value = state.geminiTemperature.value,
                onValueChange = { state.geminiTemperature.value = it },
                valueRange = 0f..2f,
                steps = 39,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ── OpenAI ──────────────────────────────────────────────────────────────
@Composable
private fun OpenAiSection(state: SettingsScreenState.TranslationSettings) {
    Section(title = stringResource(R.string.translation_section_openai)) {
        var showKeys by remember { mutableStateOf(false) }
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.openAiApiKeys.value,
                onValueChange = { state.openAiApiKeys.value = it },
                label = { Text(stringResource(R.string.translation_api_keys)) },
                supportingText = { Text(stringResource(R.string.translation_api_keys_hint)) },
                visualTransformation = if (showKeys) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { showKeys = !showKeys }) {
                        Text(if (showKeys) "Hide" else "Show")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                minLines = 1,
                maxLines = 4
            )
            OutlinedTextField(
                value = state.openAiBaseUrl.value,
                onValueChange = { state.openAiBaseUrl.value = it },
                label = { Text(stringResource(R.string.translation_base_url)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                singleLine = true
            )
            OutlinedTextField(
                value = state.openAiModel.value,
                onValueChange = { state.openAiModel.value = it },
                label = { Text(stringResource(R.string.translation_model)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                singleLine = true
            )
        }
    }
}

// ── Shared LLM settings (Gemini + OpenAI) ───────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LlmSection(
    state: SettingsScreenState.TranslationSettings,
    presetExpanded: Boolean,
    onPresetExpandedChange: (Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    Section(title = stringResource(R.string.translation_section_llm_settings)) {
        // Prompt preset dropdown
        ExposedDropdownMenuBox(
            expanded = presetExpanded,
            onExpandedChange = { onPresetExpandedChange(it) },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.promptPreset.value.ifBlank { "Balanced (Default)" },
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.translation_prompt_preset)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = presetExpanded,
                onDismissRequest = { onPresetExpandedChange(false) }
            ) {
                PROMPT_PRESETS.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            state.promptPreset.value = name
                            onPresetExpandedChange(false)
                        }
                    )
                }
            }
        }

        // System prompt override
        OutlinedTextField(
            value = state.activeSystemPrompt.value,
            onValueChange = { state.activeSystemPrompt.value = it },
            label = { Text(stringResource(R.string.translation_system_prompt)) },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            minLines = 2,
            maxLines = 6,
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
        )

        // Batch size
        OutlinedTextField(
            value = state.batchSize.value.toString(),
            onValueChange = { it.toIntOrNull()?.let { v -> state.batchSize.value = v.coerceIn(1, 50) } },
            label = { Text(stringResource(R.string.translation_batch_size)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            singleLine = true
        )

        // Max output tokens
        OutlinedTextField(
            value = state.maxOutputTokens.value.toString(),
            onValueChange = { it.toIntOrNull()?.let { v -> state.maxOutputTokens.value = v.coerceIn(1024, 65536) } },
            label = { Text(stringResource(R.string.translation_max_output_tokens)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth(),
            singleLine = true
        )

        // Use English locale toggle
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.translation_use_english_locale),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = state.useEnglishLocale.value,
                onCheckedChange = { state.useEnglishLocale.value = it }
            )
        }
    }
}

// ── Shared Section helper ───────────────────────────────────────────────
@Composable
private fun Section(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        content()
    }
}
