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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.approveorder.getActivity
import com.paypal.android.ui.paypalweb.PayPalWebCheckoutCanceledView
import com.paypal.android.ui.vaultcard.CreatePaymentTokenForm
import com.paypal.android.ui.vaultcard.CreateSetupTokenForm
import com.paypal.android.uishared.components.PayPalPaymentTokenView
import com.paypal.android.uishared.components.PayPalSDKErrorView
import com.paypal.android.uishared.components.PayPalSetupTokenView
import com.paypal.android.uishared.components.PropertyView

@Composable
fun PayPalWebVaultView(viewModel: PayPalWebVaultViewModel = hiltViewModel()) {
    val context = LocalContext.current
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
            onSubmit = {
                viewModel.createSetupToken()
            }
        )
        uiState.setupToken?.let { setupToken ->
            Spacer(modifier = Modifier.size(8.dp))
            PayPalSetupTokenView(setupToken = setupToken)
            Spacer(modifier = Modifier.size(8.dp))
            VaultPayPal(
                isLoading = uiState.isVaultPayPalLoading,
                onSubmit = {
                    context.getActivity()?.let { activity ->
                        viewModel.updateSetupToken(activity)
                    }
                }
            )
        }
        uiState.payPalWebVaultResult?.let { vaultResult ->
            Spacer(modifier = Modifier.size(8.dp))
            PayPalWebVaultResultView(vaultResult)
            Spacer(modifier = Modifier.size(8.dp))
            CreatePaymentTokenForm(
                isLoading = uiState.isCreatePaymentTokenLoading,
                onSubmit = { viewModel.createPaymentToken() }
            )
        }
        uiState.payPalWebVaultError?.let { error ->
            Spacer(modifier = Modifier.size(24.dp))
            PayPalSDKErrorView(error = error)
        }
        if (uiState.isVaultingCanceled) {
            Spacer(modifier = Modifier.size(24.dp))
            PayPalWebCheckoutCanceledView()
        }
        uiState.paymentToken?.let { paymentToken ->
            Spacer(modifier = Modifier.size(8.dp))
            PayPalPaymentTokenView(paymentToken = paymentToken)
        }
    }
}

@Composable
fun VaultPayPal(
    isLoading: Boolean,
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
                isLoading = isLoading,
                onClick = { onSubmit() },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun PayPalWebVaultResultView(result: PayPalWebVaultResult) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "PayPal Web Vault Result",
                style = MaterialTheme.typography.titleLarge
            )
            PropertyView(name = "Approval Session ID", value = result.approvalSessionId)
        }
    }
}
