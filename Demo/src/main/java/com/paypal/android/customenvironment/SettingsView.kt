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
        onClientIdChange = viewModel::updateClientId,
        onSdkRestUrlChange = viewModel::updateSdkRestUrl,
        onSdkGraphQLUrlChange = viewModel::updateSdkGraphQLUrl,
        onMerchantServerUrlChange = viewModel::updateMerchantServerUrl,
        onCreateOrderPathChange = viewModel::updateCreateOrderPath,
        onCreateSetupTokenPathChange = viewModel::updateCreateSetupTokenPath,
        onCreatePaymentTokenPathChange = viewModel::updateCreatePaymentTokenPath,
        onSaveClick = viewModel::saveConfig,
        onClearClick = viewModel::clearConfig
    )
}

@Composable
private fun SettingsContent(
    config: CustomEnvironmentConfig,
    onClientIdChange: (String) -> Unit,
    onSdkRestUrlChange: (String) -> Unit,
    onSdkGraphQLUrlChange: (String) -> Unit,
    onMerchantServerUrlChange: (String) -> Unit,
    onCreateOrderPathChange: (String) -> Unit,
    onCreateSetupTokenPathChange: (String) -> Unit,
    onCreatePaymentTokenPathChange: (String) -> Unit,
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
            text = "Enter your environment URLs below. These are stored on-device only and never committed to source control.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        UrlField(
            label = "Client ID",
            placeholder = "Your PayPal client ID",
            value = config.clientId,
            onValueChange = onClientIdChange
        )

        UrlField(
            label = "SDK REST Base URL",
            placeholder = "e.g. https://api.msmaster.qa.paypal.com",
            value = config.sdkRestUrl,
            onValueChange = onSdkRestUrlChange
        )

        UrlField(
            label = "SDK GraphQL Base URL",
            placeholder = "e.g. https://www.braintree.stage.paypal.com",
            value = config.sdkGraphQLUrl,
            onValueChange = onSdkGraphQLUrlChange
        )

        UrlField(
            label = "Merchant Server URL",
            placeholder = "e.g. https://www.braintree.stage.paypal.com/mockmerchantnodeweb/",
            value = config.merchantServerUrl,
            onValueChange = onMerchantServerUrlChange
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Path Overrides (optional)",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Leave blank to use standard paths (/orders, /setup-tokens, /payment-tokens). " +
                "Capture, authorize, and get-setup-token paths are derived automatically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        UrlField(
            label = "POST /orders path",
            placeholder = "e.g. /PPCP/stage_modxo/v2/checkout/orders",
            value = config.createOrderPath,
            onValueChange = onCreateOrderPathChange,
            keyboardType = KeyboardType.Text
        )

        UrlField(
            label = "POST /setup-tokens path",
            placeholder = "e.g. /PPCP/stage_modxo/v3/vault/setup-tokens",
            value = config.createSetupTokenPath,
            onValueChange = onCreateSetupTokenPathChange,
            keyboardType = KeyboardType.Text
        )

        UrlField(
            label = "POST /payment-tokens path",
            placeholder = "e.g. /PPCP/stage_modxo/v3/vault/payment-tokens",
            value = config.createPaymentTokenPath,
            onValueChange = onCreatePaymentTokenPathChange,
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Text
        )

        if (config.isConfigured) {
            Text(
                text = "✓ Custom environment is active",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Text(
                text = "Fill in all fields to activate. Leave blank to use Sandbox.",
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
    keyboardType: KeyboardType = KeyboardType.Uri
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
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
                    clientId = "AQTfw2irFfemo-eWG4H5",
                    sdkRestUrl = "https://api.msmaster.qa.paypal.com",
                    sdkGraphQLUrl = "https://www.braintree.stage.paypal.com",
                    merchantServerUrl = "https://www.braintree.stage.paypal.com/mockmerchantnodeweb/",
                    createOrderPath = "/PPCP/stage_modxo/v2/checkout/orders",
                    createSetupTokenPath = "/PPCP/stage_modxo/v3/vault/setup-tokens",
                    createPaymentTokenPath = "/PPCP/stage_modxo/v3/vault/payment-tokens"
                ),
                onClientIdChange = {},
                onSdkRestUrlChange = {},
                onSdkGraphQLUrlChange = {},
                onMerchantServerUrlChange = {},
                onCreateOrderPathChange = {},
                onCreateSetupTokenPathChange = {},
                onCreatePaymentTokenPathChange = {},
                onSaveClick = {},
                onClearClick = {}
            )
        }
    }
}
