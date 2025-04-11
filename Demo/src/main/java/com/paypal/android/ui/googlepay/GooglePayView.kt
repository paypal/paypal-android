package com.paypal.android.ui.googlepay

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.wallet.contract.TaskResultContracts
import com.paypal.android.corepayments.ApproveGooglePayPaymentResult
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.CreateOrderForm
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivityOrNull
import kotlinx.coroutines.launch

@Composable
fun GooglePayView(
    viewModel: GooglePayViewModel = hiltViewModel()
) {
    // Ref: https://stackoverflow.com/a/67156998
    val googlePayLauncher =
        rememberLauncherForActivityResult(TaskResultContracts.GetPaymentDataResult()) { taskResult ->
            taskResult?.let { viewModel.completeGooglePayLaunch(it) }
        }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberScrollState()
    LaunchedEffect(scrollState.maxValue) {
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contentPadding = UIConstants.paddingMedium
    Column(
        verticalArrangement = UIConstants.spacingLarge,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding)
            .verticalScroll(scrollState)
    ) {
        Step1_CreateOrder(uiState, viewModel)
        if (uiState.isCreateOrderSuccessful) {
            Step2_LaunchGooglePay(
                uiState = uiState,
                onLaunchGooglePay = {
                    context.getActivityOrNull()?.let { activity ->
                        coroutineScope.launch {
                            // Ref: https://developers.google.com/pay/api/android/guides/tutorial#initiate-payment
                            val task = viewModel.launchGooglePay(activity)
                            task.addOnCompleteListener(googlePayLauncher::launch)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun Step1_CreateOrder(uiState: GooglePayUiState, viewModel: GooglePayViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create an Order")
        CreateOrderForm(
            orderIntent = uiState.intentOption,
            onOrderIntentChange = { value -> viewModel.intentOption = value },
        )
        ActionButtonColumn(
            defaultTitle = "CREATE ORDER",
            successTitle = "ORDER CREATED",
            state = uiState.createOrderState,
            onClick = { viewModel.createOrder() },
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> OrderView(order = state.value)
            }
        }
    }
}

@Composable
private fun Step2_LaunchGooglePay(uiState: GooglePayUiState, onLaunchGooglePay: () -> Unit) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Launch Google Pay")
        ActionButtonColumn(
            defaultTitle = "LAUNCH GOOGLE PAY",
            successTitle = "GOOGLE PAY SUCCESS",
            state = uiState.googlePayState,
            onClick = onLaunchGooglePay,
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> GooglePayTaskResultView(result = state.value)
            }
        }
    }
}

@Composable
fun GooglePayTaskResultView(result: ApproveGooglePayPaymentResult.Success) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Status", value = result.status)
    }
}

@Preview
@Composable
fun GooglePayViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            GooglePayView()
        }
    }
}