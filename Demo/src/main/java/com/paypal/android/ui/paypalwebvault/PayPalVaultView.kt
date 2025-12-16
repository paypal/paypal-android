package com.paypal.android.ui.paypalwebvault

import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.BuildConfig
import com.paypal.android.DemoConstants
import com.paypal.android.R
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.paypalwebpayments.PayPalWebVaultRequest
import com.paypal.android.paypalwebpayments.compose.rememberPayPalCheckoutLauncher
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.BooleanOptionList
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.PayPalPaymentTokenView
import com.paypal.android.uishared.components.PayPalSetupTokenView
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

    // State for Composable API vault
    var composableApiVaultState: ActionState<PayPalVaultResult, Exception> by remember {
        mutableStateOf(ActionState.Idle)
    }

    // Setup Composable API launcher
    val coreConfig = remember {
        CoreConfig(BuildConfig.CLIENT_ID, Environment.SANDBOX)
    }

    val payPalLauncher = rememberPayPalCheckoutLauncher(
        configuration = coreConfig,
        onVaultSuccess = { result ->
            Log.d(
                TAG,
                "Composable API: onVaultSuccess - approvalSessionId: ${result.approvalSessionId}"
            )
            composableApiVaultState = ActionState.Success(
                PayPalVaultResult(approvalSessionId = result.approvalSessionId)
            )
        },
        onVaultCanceled = {
            Log.d(TAG, "Composable API: onVaultCanceled - User canceled vault")
            composableApiVaultState = ActionState.Failure(Exception("USER CANCELED"))
        },
        onVaultError = { error ->
            Log.e(TAG, "Composable API: onVaultError - ${error.message}", error)
            composableApiVaultState = ActionState.Failure(
                if (error is Exception) error else Exception(error.message, error)
            )
        }
    )

    // Only setup lifecycle hooks for standard API
    if (!uiState.useComposableApi) {
        OnLifecycleOwnerResumeEffect {
            val intent = context.getActivityOrNull()?.intent
            intent?.let { viewModel.completeAuthChallenge(it) }
        }

        OnNewIntentEffect { newIntent ->
            viewModel.completeAuthChallenge(newIntent)
        }
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
            Step2_VaultPayPal(
                uiState = uiState,
                viewModel = viewModel,
                payPalLauncher = payPalLauncher,
                composableApiVaultState = composableApiVaultState,
                onComposableApiVaultStateChange = { composableApiVaultState = it }
            )
        }
        val isVaultSuccessful = if (uiState.useComposableApi) {
            composableApiVaultState is ActionState.Success
        } else {
            uiState.isVaultPayPalSuccessful
        }
        if (isVaultSuccessful) {
            Step3_CreatePaymentToken(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

// Data class to hold vault result for Composable API
data class PayPalVaultResult(
    val approvalSessionId: String
)

private const val TAG = "PayPalVaultView"

@Composable
private fun Step1_CreateSetupToken(
    uiState: PayPalVaultUiState,
    viewModel: PayPalVaultViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create Setup Token")
        BooleanOptionList(
            title = stringResource(id = R.string.use_composable_api),
            selectedOption = uiState.useComposableApi,
            onSelectedOptionChange = { value -> viewModel.useComposableApi = value },
            modifier = Modifier.fillMaxWidth()
        )
        BooleanOptionList(
            title = stringResource(id = R.string.app_switch_when_available),
            selectedOption = uiState.appSwitchWhenEligible,
            onSelectedOptionChange = { value -> viewModel.appSwitchWhenEligible = value },
            modifier = Modifier.fillMaxWidth()
        )
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
private fun Step2_VaultPayPal(
    uiState: PayPalVaultUiState,
    viewModel: PayPalVaultViewModel,
    payPalLauncher: com.paypal.android.paypalwebpayments.compose.PayPalCheckoutLauncher,
    composableApiVaultState: ActionState<PayPalVaultResult, Exception>,
    onComposableApiVaultStateChange: (ActionState<PayPalVaultResult, Exception>) -> Unit
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Vault PayPal")

        if (uiState.useComposableApi) {
            // Use Composable API launcher
            ActionButtonColumn(
                defaultTitle = "VAULT PAYPAL (Composable API)",
                successTitle = "PAYPAL VAULTED",
                state = composableApiVaultState,
                onClick = {
                    val setupTokenId = viewModel.createdSetupToken?.id
                    if (setupTokenId == null) {
                        onComposableApiVaultStateChange(
                            ActionState.Failure(Exception("Create a setup token to continue."))
                        )
                    } else {
                        Log.d(
                            TAG,
                            "Composable API: Launching vault for setupTokenId: $setupTokenId"
                        )
                        onComposableApiVaultStateChange(ActionState.Loading)
                        val request = PayPalWebVaultRequest(
                            setupTokenId = setupTokenId,
                            appSwitchWhenEligible = uiState.appSwitchWhenEligible,
                            appLinkUrl = DemoConstants.APP_URL,
                            fallbackUrlScheme = DemoConstants.APP_FALLBACK_URL_SCHEME
                        )
                        payPalLauncher.launchVault(request) { result ->
                            Log.d(TAG, "Composable API: Presentation result: $result")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { state ->
                when (state) {
                    is CompletedActionState.Failure -> ErrorView(error = state.value)
                    is CompletedActionState.Success -> state.value.run {
                        PayPalWebVaultResultView(
                            result = PayPalWebCheckoutFinishVaultResult.Success(approvalSessionId)
                        )
                    }
                }
            }
        } else {
            // Use standard API
            ActionButtonColumn(
                defaultTitle = "VAULT PAYPAL",
                successTitle = "PAYPAL VAULTED",
                state = uiState.vaultPayPalState,
                onClick = {
                    context.getActivityOrNull()?.let { activity ->
                        viewModel.vaultSetupToken(activity)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { state ->
                when (state) {
                    is CompletedActionState.Failure -> ErrorView(error = state.value)
                    is CompletedActionState.Success -> PayPalWebVaultResultView(result = state.value)
                }
            }
        }
    }
}

@Composable
private fun Step3_CreatePaymentToken(
    uiState: PayPalVaultUiState,
    viewModel: PayPalVaultViewModel
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
