package com.paypal.android.ui.vaultcard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.ui.approveorder.getActivity
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.CardForm
import com.paypal.android.uishared.components.PaymentTokenView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.SetupTokenView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.ActionButtonState
import com.paypal.android.utils.UIConstants

@ExperimentalMaterial3Api
@Composable
fun VaultCardView(
    viewModel: VaultCardViewModel,
    onUseTestCardClick: () -> Unit,
) {
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
            Step2_VaultCard(uiState, viewModel, onUseTestCardClick)
        }
        if (uiState.isVaultCardSuccessful) {
            Step3_CreatePaymentToken(uiState, viewModel)
        }
    }
}

@Composable
fun VaultSuccessView(cardVaultResult: CardVaultResult) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Setup Token Id", value = cardVaultResult.setupTokenId)
        PropertyView(name = "Status", value = cardVaultResult.status)
    }
}

@Composable
fun Step1_CreateSetupToken(uiState: VaultCardUiState, viewModel: VaultCardViewModel) {
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
                SetupTokenView(setupToken = setupToken)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun Step2_VaultCard(
    uiState: VaultCardUiState,
    viewModel: VaultCardViewModel,
    onUseTestCardClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Vault Card")
        CardForm(
            cardNumber = uiState.cardNumber,
            expirationDate = uiState.cardExpirationDate,
            securityCode = uiState.cardSecurityCode,
            onCardNumberChange = { viewModel.cardNumber = it },
            onExpirationDateChange = { viewModel.cardExpirationDate = it },
            onSecurityCodeChange = { viewModel.cardSecurityCode = it },
            onUseTestCardClick = onUseTestCardClick
        )
        ActionButtonColumn(
            defaultTitle = "VAULT CARD",
            successTitle = "CARD VAULTED",
            state = uiState.vaultCardState,
            onClick = {
                context.getActivity()?.let { viewModel.updateSetupToken(it) }
            }
        ) {
            (uiState.vaultCardState as? ActionButtonState.Success)?.value?.let { vaultResult ->
                VaultSuccessView(cardVaultResult = vaultResult)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
fun Step3_CreatePaymentToken(
    uiState: VaultCardUiState,
    viewModel: VaultCardViewModel
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
                PaymentTokenView(paymentToken = paymentToken)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Preview
@Composable
fun VaultCardViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            VaultCardView(
                viewModel = viewModel(),
                onUseTestCardClick = {}
            )
        }
    }
}
