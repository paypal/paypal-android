package com.paypal.android.ui.paypalwebvault

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.R
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishVaultResult
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.BooleanOptionList
import com.paypal.android.uishared.components.EnumOptionList
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.PayPalPaymentTokenView
import com.paypal.android.uishared.components.PayPalSetupTokenView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.StepHeader
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
    uiState: PayPalVaultUiState,
    viewModel: PayPalVaultViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create Setup Token")
        BooleanOptionList(
            title = stringResource(id = R.string.app_switch_when_available),
            selectedOption = uiState.appSwitchWhenEligible,
            onSelectedOptionChange = { value -> viewModel.appSwitchWhenEligible = value },
            modifier = Modifier.fillMaxWidth()
        )
        if (uiState.appSwitchWhenEligible) {
            if (uiState.buyerEmailAddress != null) {
                TextButton(
                    onClick = { viewModel.buyerEmailAddress = null },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear Buyer Email Address (${uiState.buyerEmailAddress})")
                }
            } else {
                TextButton(
                    onClick = { viewModel.showBuyerEmailDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Enter Buyer Email Address")
                }
            }
        }
        if (uiState.showBuyerEmailDialog) {
            BuyerEmailDialog(
                onConfirm = { email ->
                    viewModel.buyerEmailAddress = email
                    viewModel.showBuyerEmailDialog = false
                },
                onDismiss = { viewModel.showBuyerEmailDialog = false }
            )
        }
        EnumOptionList(
            title = stringResource(id = R.string.return_to_app_strategy_title),
            stringArrayResId = R.array.deep_link_strategy_options,
            onSelectedOptionChange = { value -> viewModel.returnToAppStrategy = value },
            selectedOption = uiState.returnToAppStrategy
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
    viewModel: PayPalVaultViewModel
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

@Composable
private fun BuyerEmailDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emailInput = remember { mutableStateOf("") }
    val emailError = remember { mutableStateOf<String?>(null) }

    fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buyer Email Address") },
        text = {
            Column {
                OutlinedTextField(
                    value = emailInput.value,
                    onValueChange = {
                        emailInput.value = it
                        emailError.value = null
                    },
                    label = { Text("Email Address") },
                    isError = emailError.value != null,
                    supportingText = emailError.value?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    emailInput.value.isBlank() -> emailError.value = "Email address is required"
                    !isValidEmail(emailInput.value) -> emailError.value =
                        "Enter a valid email address"

                    else -> onConfirm(emailInput.value.trim())
                }
            }) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
