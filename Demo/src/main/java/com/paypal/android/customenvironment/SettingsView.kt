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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private val successGreen = Color(color = 0xff007f5f)

@Composable
fun SettingsView(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    SettingsContent(
        uiState = uiState,
        onEnvironmentChange = viewModel::updateSelectedEnvironment,
        onCustomSdkRestUrlChange = viewModel::updateCustomSdkRestUrl,
        onCustomSdkGraphQLUrlChange = viewModel::updateCustomSdkGraphQLUrl,
        onCustomClientIdChange = viewModel::updateCustomClientId,
        onCustomMerchantBaseUrlChange = viewModel::updateCustomMerchantBaseUrl,
        onCustomVenmoEnvironmentChange = viewModel::updateCustomVenmoEnvironment,
        onCustomVenmoCheckoutUrlPrefixChange = viewModel::updateCustomVenmoCheckoutUrlPrefix,
        onCustomMerchantIdChange = viewModel::updateCustomMerchantId,
        onSaveClick = viewModel::saveConfig,
        onClearClick = viewModel::clearConfig
    )
}

@Suppress("LongParameterList")
@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onEnvironmentChange: (SelectedEnvironment) -> Unit,
    onCustomSdkRestUrlChange: (String) -> Unit,
    onCustomSdkGraphQLUrlChange: (String) -> Unit,
    onCustomClientIdChange: (String) -> Unit,
    onCustomMerchantBaseUrlChange: (String) -> Unit,
    onCustomVenmoEnvironmentChange: (String) -> Unit,
    onCustomVenmoCheckoutUrlPrefixChange: (String) -> Unit,
    onCustomMerchantIdChange: (String) -> Unit,
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
        EnvironmentSelector(
            selected = uiState.settings.selectedEnvironment,
            onSelect = onEnvironmentChange
        )
        if (uiState.settings.selectedEnvironment == SelectedEnvironment.CUSTOM) {
            CustomEnvironmentFields(
                uiState = uiState,
                onCustomSdkRestUrlChange = onCustomSdkRestUrlChange,
                onCustomSdkGraphQLUrlChange = onCustomSdkGraphQLUrlChange,
                onCustomClientIdChange = onCustomClientIdChange,
                onCustomMerchantBaseUrlChange = onCustomMerchantBaseUrlChange,
                onCustomVenmoEnvironmentChange = onCustomVenmoEnvironmentChange,
                onCustomVenmoCheckoutUrlPrefixChange = onCustomVenmoCheckoutUrlPrefixChange,
                onCustomMerchantIdChange = onCustomMerchantIdChange
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        SettingsStatus(settings = uiState.settings)
        Spacer(modifier = Modifier.height(8.dp))
        SettingsActionButtons(onSaveClick = onSaveClick, onClearClick = onClearClick)
        if (uiState.showSaveSuccess) {
            Text(
                text = "Custom URLs saved successfully.",
                style = MaterialTheme.typography.bodySmall,
                color = successGreen,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun CustomEnvironmentFields(
    uiState: SettingsUiState,
    onCustomSdkRestUrlChange: (String) -> Unit,
    onCustomSdkGraphQLUrlChange: (String) -> Unit,
    onCustomClientIdChange: (String) -> Unit,
    onCustomMerchantBaseUrlChange: (String) -> Unit,
    onCustomVenmoEnvironmentChange: (String) -> Unit,
    onCustomVenmoCheckoutUrlPrefixChange: (String) -> Unit,
    onCustomMerchantIdChange: (String) -> Unit
) {
    val settings = uiState.settings
    UrlField(
        label = "SDK REST Base URL",
        placeholder = "Enter SDK REST base URL",
        value = settings.customSdkRestUrl,
        onValueChange = onCustomSdkRestUrlChange,
        error = uiState.restUrlError
    )
    UrlField(
        label = "SDK GraphQL Base URL",
        placeholder = "Enter SDK GraphQL base URL",
        value = settings.customSdkGraphQLUrl,
        onValueChange = onCustomSdkGraphQLUrlChange,
        error = uiState.graphQLUrlError
    )
    UrlField(
        label = "Merchant Server Base URL",
        placeholder = "Enter merchant server base URL",
        value = settings.customMerchantBaseUrl,
        onValueChange = onCustomMerchantBaseUrlChange,
        imeAction = ImeAction.Done,
        error = uiState.merchantBaseUrlError
    )
    UrlField(
        label = "Client ID",
        placeholder = "Enter PayPal client ID (optional)",
        value = settings.customClientId,
        onValueChange = onCustomClientIdChange,
    )
    UrlField(
        label = "Venmo Environment (Optional)",
        placeholder = "sandbox, live, or qa",
        value = settings.customVenmoEnvironment,
        onValueChange = onCustomVenmoEnvironmentChange,
        imeAction = ImeAction.Next,
        error = uiState.venmoEnvironmentError
    )
    UrlField(
        label = "Venmo Checkout URL Prefix (Optional)",
        placeholder = "e.g., qa, staging (formats as https://account.\$prefix.venmo.com/go/web/paypal)",
        value = settings.customVenmoCheckoutUrlPrefix,
        onValueChange = onCustomVenmoCheckoutUrlPrefixChange,
        imeAction = ImeAction.Next,
        error = uiState.venmoCheckoutUrlPrefixError
    )
    UrlField(
        label = "Merchant ID (Optional)",
        placeholder = "Enter merchant ID",
        value = settings.customMerchantId,
        onValueChange = onCustomMerchantIdChange,
        imeAction = ImeAction.Done,
        error = uiState.merchantIdError
    )
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
    if (!settings.isValidEnvironment) {
        Text(
            text = "Fill in all URL fields to use the Custom environment. Will default to Sandbox.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
    error: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = error != null,
        supportingText = error?.let { msg -> { Text(msg) } },
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
                uiState = SettingsUiState(
                    settings = DemoEnvironmentSettings(
                        selectedEnvironment = SelectedEnvironment.CUSTOM,
                        customSdkRestUrl = "not-a-valid-url",
                        customSdkGraphQLUrl = "",
                        customClientId = "client-id-entered-at-runtime",
                        customMerchantBaseUrl = "bad merchant url",
                    ),
                    restUrlError = "URL must start with https://",
                    graphQLUrlError = "URL is required",
                    merchantBaseUrlError = "URL must not contain spaces"
                ),
                onEnvironmentChange = {},
                onCustomSdkRestUrlChange = {},
                onCustomSdkGraphQLUrlChange = {},
                onCustomClientIdChange = {},
                onCustomMerchantBaseUrlChange = {},
                onCustomVenmoEnvironmentChange = {},
                onCustomVenmoCheckoutUrlPrefixChange = {},
                onCustomMerchantIdChange = {},
                onSaveClick = {},
                onClearClick = {}
            )
        }
    }
}
