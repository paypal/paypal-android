package com.paypal.android.ui.googlepay

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.googlepay.GooglePayFinishStartResult
import com.paypal.android.googlepay.LaunchGooglePay
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.CreateOrderForm
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.PropertyView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.UIConstants

@Composable
fun GooglePayView(
    viewModel: GooglePayViewModel = hiltViewModel()
) {

    val googlePayLauncher = rememberLauncherForActivityResult(LaunchGooglePay()) { result ->
        viewModel.finishStart(result)
    }

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
            Step2_RequestGooglePayLaunch(
                uiState = uiState,
                onRequestGooglePayLaunch = { viewModel.requestGooglePayLaunch() }
            )
        }

        val authChallenge = uiState.authChallenge
        if (authChallenge != null) {
            Step3_LaunchGooglePay(
                uiState = uiState,
                onLaunchGooglePay = { googlePayLauncher.launch(authChallenge) }
            )
        }
        if (uiState.isGooglePayFinishSuccessful) {
            Step4_CompleteOrder(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
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
private fun Step2_RequestGooglePayLaunch(
    uiState: GooglePayUiState,
    onRequestGooglePayLaunch: () -> Unit
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Request Google Pay")
        ActionButtonColumn(
            defaultTitle = "REQUEST GOOGLE PAY",
            successTitle = "REQUEST GOOGLE PAY SUCCESS",
            state = uiState.googlePayStartState,
            onClick = onRequestGooglePayLaunch,
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> GooglePayStartSuccessView()
            }
        }
    }
}

@Composable
private fun Step3_LaunchGooglePay(uiState: GooglePayUiState, onLaunchGooglePay: () -> Unit) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = "Launch Google Pay")
        ActionButtonColumn(
            defaultTitle = "LAUNCH GOOGLE PAY",
            successTitle = "GOOGLE PAY SUCCESS",
            state = uiState.googlePayFinishStartState,
            onClick = onLaunchGooglePay,
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Failure -> ErrorView(error = state.value)
                is CompletedActionState.Success -> GooglePayFinishStartSuccessView(result = state.value)
            }
        }
    }
}

@Composable
private fun Step4_CompleteOrder(uiState: GooglePayUiState, viewModel: GooglePayViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 4, title = "Complete Order")
        ActionButtonColumn(
            defaultTitle = "COMPLETE ORDER",
            successTitle = "ORDER COMPLETED",
            state = uiState.completeOrderState,
            onClick = { viewModel.completeOrder() },
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
fun GooglePayStartSuccessView() {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        Text("Continue to Launch Google Pay")
    }
}

@Composable
fun GooglePayFinishStartSuccessView(result: GooglePayFinishStartResult.Success) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
        modifier = Modifier.padding(UIConstants.paddingMedium)
    ) {
        PropertyView(name = "Status", value = result.status)
        PropertyView(name = "Card Type", value = result.cardType)
        PropertyView(name = "Card Brand", value = result.cardBrand)
        PropertyView(name = "Card Last Digits", value = result.cardLastDigits)
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
