package com.paypal.android.ui.paypalweb

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.uishared.components.ActionButtonColumn
import com.paypal.android.uishared.components.CreateOrderForm
import com.paypal.android.uishared.components.ErrorView
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.StepHeader
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.utils.UIConstants
import com.paypal.android.utils.getActivity

@Composable
fun PayPalWebView(
    viewModel: PayPalWebViewModel = hiltViewModel()
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
            Step2_StartPayPalWebCheckout(uiState, viewModel)
        }
        if (uiState.isPayPalWebCheckoutSuccessful) {
            Step3_CompleteOrder(uiState, viewModel)
        }
        Spacer(modifier = Modifier.size(24.dp))
    }
}

@Composable
private fun Step1_CreateOrder(uiState: PayPalWebUiState, viewModel: PayPalWebViewModel) {
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 1, title = "Create an Order")
        CreateOrderForm(
            orderIntent = uiState.intentOption,
            onIntentOptionSelected = { value -> viewModel.intentOption = value },
        )
        ActionButtonColumn(
            defaultTitle = "CREATE ORDER",
            successTitle = "ORDER CREATED",
            state = uiState.createOrderState,
            onClick = { viewModel.createOrder() },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            (uiState.createOrderState as? ActionState.Success)?.value?.let { order ->
                OrderView(order = order)
            }
        }
    }
}

@Composable
private fun Step2_StartPayPalWebCheckout(uiState: PayPalWebUiState, viewModel: PayPalWebViewModel) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 2, title = "Launch PayPal Web")
        StartPayPalWebCheckoutForm(
            fundingSource = uiState.fundingSource,
            onFundingSourceSelected = { value -> viewModel.fundingSource = value },
        )
        ActionButtonColumn(
            defaultTitle = "START CHECKOUT",
            successTitle = "CHECKOUT COMPLETE",
            state = uiState.payPalWebCheckoutState,
            onClick = { context.getActivity()?.let { viewModel.startWebCheckout(it) } },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            (uiState.payPalWebCheckoutState as? ActionState.Success)?.value?.let { result ->
                PayPalWebCheckoutResultView(result.orderId, result.payerId)
            }

            (uiState.payPalWebCheckoutState as? ActionState.Failure)?.value?.let { error ->
                ErrorView(error = error)
            }
        }
    }
}

@Composable
private fun Step3_CompleteOrder(uiState: PayPalWebUiState, viewModel: PayPalWebViewModel) {
    val context = LocalContext.current
    Column(
        verticalArrangement = UIConstants.spacingMedium,
    ) {
        StepHeader(stepNumber = 3, title = "Complete Order")
        ActionButtonColumn(
            defaultTitle = "COMPLETE ORDER",
            successTitle = "ORDER COMPLETED",
            state = uiState.completeOrderState,
            onClick = { context.getActivity()?.let { viewModel.completeOrder(it) } },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            (uiState.completeOrderState as? ActionState.Success)?.value?.let { completedOrder ->
                OrderView(order = completedOrder)
            }
        }
    }
}

@Preview
@Composable
fun PayPalWebViewPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            PayPalWebView()
        }
    }
}
