package com.paypal.android.ui.paypalweb

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.R
import com.paypal.android.ui.approveorder.getActivity
import com.paypal.android.uishared.components.CompleteOrderForm
import com.paypal.android.uishared.components.CreateOrderForm
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.PayPalSDKErrorView

@Composable
fun PayPalWebView(
    viewModel: PayPalWebViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    LaunchedEffect(uiState) {
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        CreateOrderForm(
            title = "Create an order to proceed with ${stringResource(R.string.feature_paypal_web)}:",
            orderIntent = uiState.intentOption,
            isLoading = uiState.isCreateOrderLoading,
            onIntentOptionSelected = { value -> viewModel.intentOption = value },
            onSubmit = { viewModel.createOrder() }
        )
        uiState.createdOrder?.let { createdOrder ->
            Spacer(modifier = Modifier.size(24.dp))
            OrderView(order = createdOrder, title = "Order Created")
            Spacer(modifier = Modifier.size(24.dp))
            StartPayPalWebCheckoutForm(
                fundingSource = uiState.fundingSource,
                isLoading = uiState.isStartCheckoutLoading,
                onFundingSourceSelected = { value -> viewModel.fundingSource = value },
                onSubmit = { context.getActivity()?.let { viewModel.startWebCheckout(it) } }
            )
        }
        uiState.payPalWebCheckoutResult?.let { result ->
            Spacer(modifier = Modifier.size(24.dp))
            PayPalWebCheckoutResultView(result.orderId, result.payerId)
            Spacer(modifier = Modifier.size(24.dp))
            CompleteOrderForm(
                isLoading = uiState.isCompleteOrderLoading,
                orderIntent = uiState.intentOption,
                onSubmit = { viewModel.completeOrder(context) }
            )
        }
        uiState.payPalWebCheckoutError?.let { error ->
            Spacer(modifier = Modifier.size(24.dp))
            PayPalSDKErrorView(error = error)
        }
        if (uiState.isCheckoutCanceled) {
            Spacer(modifier = Modifier.size(24.dp))
            PayPalWebCheckoutCanceledView()
        }
        uiState.completedOrder?.let { completedOrder ->
            Spacer(modifier = Modifier.size(24.dp))
            OrderView(order = completedOrder, title = "Order Completed")
        }
        Spacer(modifier = Modifier.size(24.dp))
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
