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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.paypal.android.R
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.CardForm
import com.paypal.android.uishared.components.CardPaymentTokenView
import com.paypal.android.uishared.components.CardSetupTokenView
import com.paypal.android.uishared.components.CardVaultResultView
import com.paypal.android.uishared.components.EnumOptionList
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.OnLifecycleOwnerResumeEffect
import com.paypal.android.utils.OnNewIntentEffect
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull

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

    val context = LocalContext.current
    OnLifecycleOwnerResumeEffect {
        val intent = context.getActivityOrNull()?.intent
        intent?.let { viewModel.completeAuthChallenge(it) }
    }

    OnNewIntentEffect { newIntent ->
        viewModel.completeAuthChallenge(newIntent)
    }

    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
    ) {
        Step_CreateSetupToken(uiState, viewModel)
        if (uiState.isCreateSetupTokenSuccessful) {
            Step_VaultCard(uiState, viewModel, onUseTestCardClick)
        }
        if (uiState.isVaultCardSuccessful) {
            Step_CreatePaymentToken(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

@Composable
private fun Step_CreateSetupToken(
    uiState: VaultCardUiState,
    viewModel: VaultCardViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create Setup Token")
        EnumOptionList(
            title = stringResource(id = R.string.sca_title),
            stringArrayResId = R.array.sca_options,
            onSelectedOptionChange = { value -> viewModel.scaOption = value },
            selectedOption = uiState.scaOption
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
            state = uiState.updateSetupTokenState,
            onClick = {
                context.getActivityOrNull()?.let { viewModel.updateSetupToken(it) }
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
