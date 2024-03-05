package com.paypal.android.ui.vaultcard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardVaultResult
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.BooleanOptionList
import com.paypal.android.uishared.components.CardForm
import com.paypal.android.uishared.components.CardPaymentTokenView
import com.paypal.android.uishared.components.CardSetupTokenView
import com.paypal.android.uishared.components.CardVaultResultView
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.InfoColumn
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivity

// TODO: Rename to CardVaultView
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
        Step_CreateSetupToken(stepNumber = 1, uiState, viewModel)
        if (uiState.isCreateSetupTokenSuccessful) {
            Step_VaultCard(stepNumber = 2, uiState, viewModel, onUseTestCardClick)
        }

        val authChallenge = uiState.authChallenge
        if (authChallenge != null) {
            Step_PresentAuthChallenge(stepNumber = 3, authChallenge, uiState, viewModel)
        } else if (uiState.isVaultCardSuccessful) {
            Step_CreatePaymentToken(stepNumber = 3, uiState, viewModel)
        }

        if (uiState.isVaultWith3DSSuccessful) {
            (uiState.refreshSetupTokenState as? ActionState.Success)?.value?.let { setupToken ->
                InfoColumn(title = "UPDATED SETUP TOKEN") {
                    CardSetupTokenView(setupToken = setupToken)
                }
            }
            Step_CreatePaymentToken(stepNumber = 4, uiState, viewModel)
        }

        Spacer(modifier = Modifier.size(contentPadding))
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
private fun Step_CreateSetupToken(
    stepNumber: Int,
    uiState: VaultCardUiState,
    viewModel: VaultCardViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = stepNumber, title = "Create Setup Token")
        BooleanOptionList(
            title = "SCA WHEN REQUIRED",
            selectedOption = uiState.shouldRequest3DS,
            onSelectedOptionChange = { value -> viewModel.shouldRequest3DS = value },
        )
        ActionButtonColumn(
            defaultTitle = "CREATE SETUP TOKEN",
            successTitle = "SETUP TOKEN CREATED",
            state = uiState.createSetupTokenState,
            onClick = { viewModel.createSetupToken() }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> CardSetupTokenView(setupToken = state.value)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun Step_VaultCard(
    stepNumber: Int,
    uiState: VaultCardUiState,
    viewModel: VaultCardViewModel,
    onUseTestCardClick: () -> Unit
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = stepNumber, title = "Vault Card")
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
            state = uiState.updateSetupTokenState,
            onClick = {
                context.getActivity()?.let { viewModel.updateSetupToken(it) }
            }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> VaultSuccessView(cardVaultResult = state.value)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun Step_PresentAuthChallenge(
    stepNumber: Int,
    authChallenge: CardAuthChallenge,
    uiState: VaultCardUiState,
    viewModel: VaultCardViewModel
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = stepNumber, title = "Complete Auth Challenge")
        ActionButtonColumn(
            defaultTitle = "PRESENT AUTH CHALLENGE",
            successTitle = "AUTH CHALLENGE COMPLETE",
            state = uiState.authChallengeState,
            onClick = {
                context.getActivity()?.let { viewModel.presentAuthChallenge(it, authChallenge) }
            }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> CardVaultResultView(result = state.value)
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun Step_CreatePaymentToken(
    stepNumber: Int,
    uiState: VaultCardUiState,
    viewModel: VaultCardViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = stepNumber, title = "Create Payment Token")
        ActionButtonColumn(
            defaultTitle = "CREATE PAYMENT TOKEN",
            successTitle = "PAYMENT TOKEN CREATED",
            state = uiState.createPaymentTokenState,
            onClick = { viewModel.createPaymentToken() }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> CardPaymentTokenView(paymentToken = state.value)
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
