package com.paypal.android.ui.paypalnative

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paypal.android.R
import com.paypal.android.ui.paypalweb.PayPalWebCheckoutCanceledView
import com.paypal.android.ui.paypalweb.PayPalWebCheckoutResultView
import com.paypal.android.uishared.components.CompleteOrderForm
import com.paypal.android.uishared.components.CreateOrderWithShippingPreferenceForm
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.PayPalSDKErrorView

@Composable
fun PayPalNativeView(
    viewModel: PayPalNativeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    LaunchedEffect(uiState) {
        // continuously scroll to bottom of the list when event state is updated
        scrollState.animateScrollTo(scrollState.maxValue)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        CreateOrderWithShippingPreferenceForm(
            title = "Create an order to proceed with ${stringResource(R.string.feature_paypal_native)}:",
            orderIntent = uiState.intentOption,
            shippingPreference = uiState.shippingPreference,
            isLoading = uiState.isCreateOrderLoading,
            onIntentOptionSelected = { value -> viewModel.intentOption = value },
            onShippingPreferenceSelected = { value -> viewModel.shippingPreference = value },
            onSubmit = { viewModel.createOrder() }
        )
        uiState.createdOrder?.let { createdOrder ->
            Spacer(modifier = Modifier.size(24.dp))
            OrderView(order = createdOrder, title = "Order Created")
            Spacer(modifier = Modifier.size(24.dp))
            StartPayPalNativeCheckoutForm(
                isLoading = uiState.isStartCheckoutLoading,
                onSubmit = { viewModel.startNativeCheckout() }
            )
        }
        uiState.payPalNativeCheckoutResult?.let { result ->
            Spacer(modifier = Modifier.size(24.dp))
            PayPalWebCheckoutResultView(result.orderId, result.payerId)
            Spacer(modifier = Modifier.size(24.dp))
//            CompleteOrderForm(
//                isLoading = uiState.isCompleteOrderLoading,
//                orderIntent = uiState.intentOption,
//                onSubmit = { viewModel.completeOrder() }
//            )
        }
        uiState.payPalNativeCheckoutError?.let { error ->
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
