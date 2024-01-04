package com.paypal.android.ui.paypalwebvault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.PayPalPaymentTokenView
import com.paypal.android.uishared.components.PayPalSetupTokenView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.ActionButtonState
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivity

@Composable
fun PayPalWebVaultView(viewModel: PayPalWebVaultViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
    ) {
        Step1_CreateSetupToken(uiState, viewModel)
        if (uiState.isCreateSetupTokenSuccessful) {
            Step2_VaultPayPal(uiState, viewModel)
        }
        if (uiState.isVaultPayPalSuccessful) {
            Step3_CreatePaymentToken(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

@Composable
private fun Step1_CreateSetupToken(
    uiState: PayPalWebVaultUiState,
    viewModel: PayPalWebVaultViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create Setup Token")
        ActionButtonColumn(
            defaultTitle = "CREATE SETUP TOKEN",
            successTitle = "SETUP TOKEN CREATED",
            state = uiState.createSetupTokenState,
            onClick = { viewModel.createSetupToken() }
        ) {
            (uiState.createSetupTokenState as? ActionButtonState.Success)?.value?.let { setupToken ->
                PayPalSetupTokenView(setupToken = setupToken)
            }
        }
    }
}

@Composable
private fun Step2_VaultPayPal(
    uiState: PayPalWebVaultUiState,
    viewModel: PayPalWebVaultViewModel
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Vault PayPal")
        ActionButtonColumn(
            defaultTitle = "VAULT PAYPAL",
            successTitle = "PAYPAL VAULTED",
            state = uiState.vaultPayPalState,
            onClick = {
                context.getActivity()?.let { activity ->
                    viewModel.vaultSetupToken(activity)
                }
            }
        ) {
            (uiState.vaultPayPalState as? ActionButtonState.Success)?.value?.let { vaultResult ->
                PayPalWebVaultResultView(result = vaultResult)
            }
            (uiState.vaultPayPalState as? ActionButtonState.Failure)?.value?.let { error ->
                ErrorView(error = error)
            }
        }
    }
}

@Composable
private fun Step3_CreatePaymentToken(
    uiState: PayPalWebVaultUiState,
    viewModel: PayPalWebVaultViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = "Create Payment Token")
        ActionButtonColumn(
            defaultTitle = "CREATE PAYMENT TOKEN",
            successTitle = "PAYMENT TOKEN CREATED",
            state = uiState.createPaymentTokenState,
            onClick = { viewModel.createPaymentToken() }
        ) {
            (uiState.createPaymentTokenState as? ActionButtonState.Success)?.value?.let { paymentToken ->
                PayPalPaymentTokenView(paymentToken = paymentToken)
            }
        }
    }
}

@Composable
fun PayPalWebVaultResultView(result: PayPalWebVaultResult) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Approval Session ID", value = result.approvalSessionId)
    }
}
