package com.paypal.android.ui.vaultcard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.ui.approveorder.getActivity
import com.paypal.android.uishared.components.CardPaymentTokenView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.SetupTokenView

@ExperimentalMaterial3Api
@Composable
fun VaultCardView(
    viewModel: VaultCardViewModel,
    onUseTestCardClick: () -> Unit,
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    LaunchedEffect(uiState) {
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(scrollState)
    ) {
        CreateSetupTokenForm(
            isLoading = uiState.isCreateSetupTokenLoading,
            customerId = uiState.customerId,
            onCustomerIdValueChange = { value -> viewModel.customerId = value },
            onSubmit = { viewModel.createSetupToken() }
        )
        uiState.setupToken?.let { setupToken ->
            Spacer(modifier = Modifier.size(8.dp))
            SetupTokenView(setupToken = setupToken)
            Spacer(modifier = Modifier.size(8.dp))
            UpdateSetupTokenWithCardForm(
                uiState = uiState,
                onCardNumberChange = { viewModel.cardNumber = it },
                onExpirationDateChange = { viewModel.cardExpirationDate = it },
                onSecurityCodeChange = { viewModel.cardSecurityCode = it },
                onUseTestCardClick = { onUseTestCardClick() },
                onSubmit = { context.getActivity()?.let { viewModel.updateSetupToken(it) } }
            )
        }
        uiState.cardVaultResult?.let { vaultResult ->
            Spacer(modifier = Modifier.size(8.dp))
            VaultSuccessView(cardVaultResult = vaultResult)
            Spacer(modifier = Modifier.size(8.dp))
            CreatePaymentTokenForm(
                isLoading = uiState.isCreatePaymentTokenLoading,
                onSubmit = { viewModel.createPaymentToken() }
            )
        }
        uiState.paymentToken?.let { paymentToken ->
            Spacer(modifier = Modifier.size(8.dp))
            CardPaymentTokenView(paymentToken = paymentToken)
        }
    }
}

@Composable
fun VaultSuccessView(cardVaultResult: CardVaultResult) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = "Vault Success",
                style = MaterialTheme.typography.titleLarge,
            )
            PropertyView(name = "Setup Token Id", value = cardVaultResult.setupTokenId)
            PropertyView(name = "Status", value = cardVaultResult.status)
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
