package com.paypal.android.ui.paypalwebvault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.R
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.EnumOptionList
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.PayPalPaymentTokenView
import com.paypal.android.uishared.components.PayPalSetupTokenView
import com.paypal.android.uishared.components.PayPalUserIdentityForm
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.OnLifecycleOwnerResumeEffect
import com.paypal.android.utils.OnNewIntentEffect
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull

@Composable
fun PayPalVaultView(viewModel: PayPalVaultViewModel = hiltViewModel()) {
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
        Step1_CreatePayPalSession(uiState, viewModel)
        Step2_CreateSetupToken(uiState, viewModel)
        if (uiState.isCreateSetupTokenSuccessful) {
            Step3_VaultPayPal(uiState, viewModel)
        }
        if (uiState.isVaultPayPalSuccessful) {
            Step4_CreatePaymentToken(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

@Composable
private fun Step1_CreatePayPalSession(
    uiState: PayPalVaultUiState,
    viewModel: PayPalVaultViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create PayPal Session")
        PayPalUserIdentityForm(
            userIdentity = uiState.userIdentity,
            onUserIdentityChange = { value -> viewModel.userIdentity = value },
            modifier = Modifier.fillMaxWidth()
        )
        EnumOptionList(
            title = stringResource(R.string.user_action_title),
            stringArrayResId = R.array.user_action_options,
            onSelectedOptionChange = { value -> viewModel.userAction = value },
            selectedOption = uiState.userAction,
            modifier = Modifier.fillMaxWidth()
        )
        ActionButtonColumn(
            defaultTitle = "CREATE PAYPAL SESSION",
            successTitle = "SESSION CREATION INITIATED",
            state = if (uiState.isPayPalSessionCreated) ActionState.Success(Unit) else ActionState.Idle,
            onClick = { viewModel.createPayPalSession() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Step2_CreateSetupToken(
    uiState: PayPalVaultUiState,
    viewModel: PayPalVaultViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Create Setup Token")
        ActionButtonColumn(
            defaultTitle = "CREATE SETUP TOKEN",
            successTitle = "SETUP TOKEN CREATED",
            state = uiState.createSetupTokenState,
            onClick = { viewModel.createSetupToken() }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> PayPalSetupTokenView(setupToken = state.value)
            }
        }
    }
}

@Composable
private fun Step3_VaultPayPal(
    uiState: PayPalVaultUiState,
    viewModel: PayPalVaultViewModel
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = "Vault PayPal")
        ActionButtonColumn(
            defaultTitle = "VAULT PAYPAL",
            successTitle = "PAYPAL VAULTED",
            state = uiState.vaultPayPalState,
            onClick = {
                context.getActivityOrNull()?.let { activity ->
                    viewModel.vaultSetupToken(activity)
                }
            }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> PayPalWebVaultResultView(result = state.value)
            }
        }
    }
}

@Composable
private fun Step4_CreatePaymentToken(
    uiState: PayPalVaultUiState,
    viewModel: PayPalVaultViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 4, title = "Create Payment Token")
        ActionButtonColumn(
            defaultTitle = "CREATE PAYMENT TOKEN",
            successTitle = "PAYMENT TOKEN CREATED",
            state = uiState.createPaymentTokenState,
            onClick = { viewModel.createPaymentToken() }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> PayPalPaymentTokenView(paymentToken = state.value)
            }
        }
    }
}

@Composable
fun PayPalWebVaultResultView(result: PayPalWebCheckoutFinishVaultResult.Success) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Approval Session ID", value = result.approvalSessionId)
    }
}
