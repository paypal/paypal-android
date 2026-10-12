package com.paypal.android.ui.venmovault

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
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.components.VenmoPaymentTokenView
import com.paypal.android.uishared.components.VenmoSetupTokenView
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.OnLifecycleOwnerResumeEffect
import com.paypal.android.utils.OnNewIntentEffect
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull
import com.paypal.android.venmo.VenmoEligibilityResult
import com.paypal.android.venmo.VenmoFinishVaultResult

@Composable
fun VenmoVaultView(viewModel: VenmoVaultViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        // Auto-scroll once the user has started the flow; avoids scrolling on initial render
        if (uiState.createSetupTokenState !is ActionState.Idle) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
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
        Step1_CheckEligibility(uiState, viewModel)
        if (uiState.isEligibilityCheckSuccessful) {
            Step2_CreateSetupToken(uiState, viewModel)
            if (uiState.isCreateSetupTokenSuccessful) {
                Step3_VaultVenmo(uiState, viewModel)
            }
            if (uiState.isVaultVenmoSuccessful) {
                Step4_CreatePaymentToken(uiState, viewModel)
            }
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

@Composable
private fun Step1_CheckEligibility(
    uiState: VenmoVaultUiState,
    viewModel: VenmoVaultViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Check Eligibility")

        // Map eligibility result to failure state if not eligible
        val displayState = when (val checkState = uiState.checkEligibilityState) {
            is ActionState.Success -> {
                val result = checkState.value
                if (result is VenmoEligibilityResult.Eligible) {
                    checkState
                } else {
                    val errorMessage = when (result) {
                        is VenmoEligibilityResult.Ineligible -> result.reason
                        is VenmoEligibilityResult.Error -> result.error.errorDescription
                        else -> "Venmo is not eligible"
                    }
                    ActionState.Failure(Exception(errorMessage))
                }
            }
            else -> checkState
        }

        ActionButtonColumn(
            defaultTitle = "CHECK ELIGIBILITY",
            successTitle = "ELIGIBILITY CHECKED",
            state = displayState,
            onClick = { viewModel.checkEligibility() }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> VenmoVaultEligibilityResultView(result = state.value)
            }
        }
    }
}

@Composable
private fun Step2_CreateSetupToken(
    uiState: VenmoVaultUiState,
    viewModel: VenmoVaultViewModel
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
                is CompletedActionState.Success -> VenmoSetupTokenView(setupToken = state.value)
            }
        }
    }
}

@Composable
private fun Step3_VaultVenmo(
    uiState: VenmoVaultUiState,
    viewModel: VenmoVaultViewModel
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = "Vault Venmo")
        ActionButtonColumn(
            defaultTitle = "VAULT VENMO",
            successTitle = "VENMO VAULTED",
            state = uiState.vaultVenmoState,
            onClick = {
                context.getActivityOrNull()?.let { activity ->
                    viewModel.vaultSetupToken(activity)
                }
            }
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> VenmoVaultResultView(result = state.value)
            }
        }
    }
}

@Composable
private fun Step4_CreatePaymentToken(
    uiState: VenmoVaultUiState,
    viewModel: VenmoVaultViewModel
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
                is CompletedActionState.Success -> VenmoPaymentTokenView(paymentToken = state.value)
            }
        }
    }
}

@Composable
fun VenmoVaultEligibilityResultView(result: VenmoEligibilityResult) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        when (result) {
            VenmoEligibilityResult.Eligible -> {
                PropertyView(name = "Status", value = "Venmo is eligible")
            }
            is VenmoEligibilityResult.Ineligible -> {
                PropertyView(name = "Status", value = "Not Eligible")
                PropertyView(name = "Reason", value = result.reason)
            }
            is VenmoEligibilityResult.Error -> {
                PropertyView(name = "Status", value = "Error")
                PropertyView(name = "Error", value = result.error.errorDescription)
            }
        }
    }
}

@Composable
fun VenmoVaultResultView(result: VenmoFinishVaultResult.Success) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(
            name = "Approval Session ID",
            value = result.approvalSessionId ?: result.approvalTokenId
        )
    }
}
