package com.paypal.android.customenvironment

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsView(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settings by viewModel.uiState.collectAsState()
    SettingsContent(
        settings = settings,
        onEnvironmentChange = viewModel::updateSelectedEnvironment,
        onCustomSdkRestUrlChange = viewModel::updateCustomSdkRestUrl,
        onCustomSdkGraphQLUrlChange = viewModel::updateCustomSdkGraphQLUrl,
        onCustomClientIdChange = viewModel::updateCustomClientId,
        onSaveClick = viewModel::saveConfig,
        onClearClick = viewModel::clearConfig
    )
}

@Composable
private fun SettingsContent(
    settings: DemoEnvironmentSettings,
    onEnvironmentChange: (SelectedEnvironment) -> Unit,
    onCustomSdkRestUrlChange: (String) -> Unit,
    onCustomSdkGraphQLUrlChange: (String) -> Unit,
    onCustomClientIdChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onClearClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsHeader()
        EnvironmentSelector(selected = settings.selectedEnvironment, onSelect = onEnvironmentChange)
        if (settings.selectedEnvironment == SelectedEnvironment.CUSTOM) {
            UrlField(
                label = "SDK REST Base URL",
                placeholder = "Enter SDK REST base URL",
                value = settings.customSdkRestUrl,
                onValueChange = onCustomSdkRestUrlChange
            )
            UrlField(
                label = "SDK GraphQL Base URL",
                placeholder = "Enter SDK GraphQL base URL",
                value = settings.customSdkGraphQLUrl,
                onValueChange = onCustomSdkGraphQLUrlChange
            )
            UrlField(
                label = "Client ID",
                placeholder = "Enter PayPal client ID (optional)",
                value = settings.customClientId,
                onValueChange = onCustomClientIdChange,
                imeAction = ImeAction.Done
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingsStatus(settings = settings)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsActionButtons(onSaveClick = onSaveClick, onClearClick = onClearClick)
    }
}

@Composable
private fun SettingsHeader() {
    Text(
        text = "Custom Environment",
        style = MaterialTheme.typography.titleLarge
    )
    Text(
        text = "Select an environment and point the SDK at the right backend.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnvironmentSelector(
    selected: SelectedEnvironment,
    onSelect: (SelectedEnvironment) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        SelectedEnvironment.entries.forEachIndexed { index, env ->
            SegmentedButton(
                selected = selected == env,
                onClick = { onSelect(env) },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = SelectedEnvironment.entries.size
                ),
                label = { Text(env.name.lowercase().replaceFirstChar { it.uppercase() }) }
            )
        }
    }
}

@Composable
private fun SettingsStatus(settings: DemoEnvironmentSettings) {
    val (text, color) = if (settings.isValidEnvironment) {
        val envName = settings.selectedEnvironment.name
            .lowercase()
            .replaceFirstChar { it.uppercase() }
        "✓ $envName environment selected" to MaterialTheme.colorScheme.primary
    } else {
        "Fill in both URL fields to use the Custom environment. " +
                "Otherwise, Environment is Sandbox by default." to
            MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color
    )
}

@Composable
private fun SettingsActionButtons(onSaveClick: () -> Unit, onClearClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onSaveClick,
            modifier = Modifier.weight(1f)
        ) {
            Text("Save")
        }
        OutlinedButton(
            onClick = onClearClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("Clear")
        }
    }
}

@Composable
private fun UrlField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    imeAction: ImeAction = ImeAction.Next,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Uri,
            imeAction = imeAction
        )
    )
}

@Suppress("UnusedPrivateMember")
@Preview(showBackground = true)
@Composable
private fun SettingsViewPreview() {
    MaterialTheme {
        Surface {
            SettingsContent(
                settings = DemoEnvironmentSettings(
                    selectedEnvironment = SelectedEnvironment.CUSTOM,
                    customSdkRestUrl = "sdk-rest-url-entered-at-runtime",
                    customSdkGraphQLUrl = "sdk-graphql-url-entered-at-runtime",
                    customClientId = "client-id-entered-at-runtime",
                ),
                onEnvironmentChange = {},
                onCustomSdkRestUrlChange = {},
                onCustomSdkGraphQLUrlChange = {},
                onCustomClientIdChange = {},
                onSaveClick = {},
                onClearClick = {}
            )
        }
    }
}
