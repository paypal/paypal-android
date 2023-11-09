package com.paypal.android.ui.paypalwebvault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.vault.CreateSetupTokenForm
import com.paypal.android.uishared.components.SetupTokenView

@Composable
fun PayPalWebVaultView(viewModel: PayPalWebVaultViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    LaunchedEffect(uiState) {
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        CreateSetupTokenForm(
            isLoading = uiState.isCreateSetupTokenLoading,
            customerId = uiState.vaultCustomerId,
            onCustomerIdValueChange = { value -> viewModel.vaultCustomerId = value },
            onSubmit = { viewModel.createSetupToken() }
        )
        uiState.setupToken?.let { setupToken ->
            Spacer(modifier = Modifier.size(8.dp))
            SetupTokenView(setupToken = setupToken)
            Spacer(modifier = Modifier.size(8.dp))
            AttachPayPalAccountToSetupToken(
                uiState = uiState,
                onSubmit = {}
            )
        }
    }
}

@Composable
fun AttachPayPalAccountToSetupToken(
    uiState: PayPalWebVaultUiState,
    onSubmit: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Vault PayPal",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.size(8.dp))
            WireframeButton(
                text = "Vault PayPal",
                isLoading = uiState.isUpdateSetupTokenLoading,
                onClick = { onSubmit() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
