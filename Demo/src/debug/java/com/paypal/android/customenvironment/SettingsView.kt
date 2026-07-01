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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
    val config by viewModel.uiState.collectAsState()
    SettingsContent(
        config = config,
        onSdkRestUrlChange = viewModel::updateSdkRestUrl,
        onSdkGraphQLUrlChange = viewModel::updateSdkGraphQLUrl,
        onSaveClick = viewModel::saveConfig,
        onClearClick = viewModel::clearConfig
    )
}

@Composable
private fun SettingsContent(
    config: CustomEnvironmentConfig,
    onSdkRestUrlChange: (String) -> Unit,
    onSdkGraphQLUrlChange: (String) -> Unit,
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
        Text(
            text = "Custom Environment",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Point the SDK at a custom backend.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        UrlField(
            label = "SDK REST Base URL",
            placeholder = "Enter SDK REST base URL",
            value = config.sdkRestUrl,
            onValueChange = onSdkRestUrlChange
        )

        UrlField(
            label = "SDK GraphQL Base URL",
            placeholder = "Enter SDK GraphQL base URL",
            value = config.sdkGraphQLUrl,
            onValueChange = onSdkGraphQLUrlChange,
            imeAction = ImeAction.Done
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (config.isConfigured) {
            Text(
                text = "✓ Custom environment is active",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "Fill in both fields to activate. Leave blank to use Sandbox.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

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

@Preview(showBackground = true)
@Composable
private fun SettingsViewPreview() {
    MaterialTheme {
        Surface {
            SettingsContent(
                config = CustomEnvironmentConfig(
                    sdkRestUrl = "sdk-rest-url-entered-at-runtime",
                    sdkGraphQLUrl = "sdk-graphql-url-entered-at-runtime",
                ),
                onSdkRestUrlChange = {},
                onSdkGraphQLUrlChange = {},
                onSaveClick = {},
                onClearClick = {}
            )
        }
    }
}
