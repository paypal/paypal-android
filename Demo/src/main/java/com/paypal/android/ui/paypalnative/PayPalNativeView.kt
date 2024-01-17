package com.paypal.android.ui.paypalnative

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.ui.paypalweb.PayPalWebCheckoutResultView
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.CreateOrderWithShippingPreferenceForm
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.uishared.state.CompletedActionState
import com.paypal.android.utils.UIConstants

@Composable
fun PayPalNativeView(
    viewModel: PayPalNativeViewModel = hiltViewModel()
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
        Step1_CreateOrder(uiState, viewModel)
        if (uiState.isCreateOrderSuccessful) {
            Step2_StartPayPalNativeCheckout(uiState, viewModel)
        }
        if (uiState.isPayPalNativeCheckoutSuccessful) {
            Step3_CompleteOrder(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(contentPadding))
    }
}

@Composable
private fun Step1_CreateOrder(uiState: PayPalNativeUiState, viewModel: PayPalNativeViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create an Order")
        CreateOrderWithShippingPreferenceForm(
            orderIntent = uiState.intentOption,
            shippingPreference = uiState.shippingPreference,
            onOrderIntentChange = { value -> viewModel.intentOption = value },
            onShippingPreferenceChange = { value -> viewModel.shippingPreference = value },
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
                is CompletedActionState.Success -> OrderView(order = state.value)
                is CompletedActionState.Failure -> ErrorView(error = state.value)
            }
        }
    }
}

@Composable
private fun Step2_StartPayPalNativeCheckout(
    uiState: PayPalNativeUiState,
    viewModel: PayPalNativeViewModel
) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Launch PayPal Native")
        ActionButtonColumn(
            defaultTitle = "START CHECKOUT",
            successTitle = "CHECKOUT COMPLETE",
            state = uiState.payPalNativeCheckoutState,
            onClick = { viewModel.startNativeCheckout() },
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Success -> state.value.run {
                    PayPalWebCheckoutResultView(orderId, payerId)
                }

                is CompletedActionState.Failure -> ErrorView(error = state.value)
            }
        }
    }
}

@Composable
private fun Step3_CompleteOrder(uiState: PayPalNativeUiState, viewModel: PayPalNativeViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = "Complete Order")
        ActionButtonColumn(
            defaultTitle = "COMPLETE ORDER",
            successTitle = "ORDER COMPLETED",
            state = uiState.completeOrderState,
            onClick = { viewModel.completeOrder() },
            modifier = Modifier
                .fillMaxWidth()
        ) { state ->
            when (state) {
                is CompletedActionState.Success -> OrderView(order = state.value)
                is CompletedActionState.Failure -> ErrorView(error = state.value)
            }
        }
    }
}
