package com.paypal.android.ui.paypal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.paypal.android.R
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.ui.paypalweb.PayPalWebCheckoutCanceledView
import com.paypal.android.ui.paypalweb.PayPalWebCheckoutResultView
import com.paypal.android.uishared.components.CompleteOrderForm
import com.paypal.android.uishared.components.CreateOrderWithShippingPreferenceForm
import com.paypal.android.uishared.components.OrderView
import com.paypal.android.uishared.components.PayPalSDKErrorView
import com.paypal.android.usecase.GetOrderUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
@Suppress("TooManyFunctions")
class PayPalNativeFragment : Fragment() {

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    @Inject
    lateinit var getOrderUseCase: GetOrderUseCase

    private val viewModel: PayPalNativeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PayPalNativeView(
                        uiState,
                        onCreateOrderClick = { createOrder() },
                        onCompleteOrderClick = { viewModel.completeOrder() }
                    )
                }
            }
        }
    }

    private fun createOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCreateOrderLoading = true

            val shippingPreference = viewModel.shippingPreference
            val orderIntent = viewModel.intentOption
            viewModel.createdOrder = getOrderUseCase(shippingPreference, orderIntent)

            viewModel.isCreateOrderLoading = false
        }
    }

    private fun startCheckout() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.startNativeCheckout()
        }
    }

    @Composable
    fun PayPalNativeView(
        uiState: PayPalNativeUiState,
        onCreateOrderClick: () -> Unit,
        onCompleteOrderClick: () -> Unit
    ) {
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
            CreateOrderWithShippingPreferenceForm(
                title = "Create an order to proceed with ${stringResource(R.string.feature_paypal_native)}:",
                orderIntent = uiState.intentOption,
                shippingPreference = uiState.shippingPreference,
                isLoading = uiState.isCreateOrderLoading,
                onIntentOptionSelected = { value -> viewModel.intentOption = value },
                onShippingPreferenceSelected = { value -> viewModel.shippingPreference = value },
                onSubmit = { onCreateOrderClick() }
            )
            uiState.createdOrder?.let { createdOrder ->
                Spacer(modifier = Modifier.size(24.dp))
                OrderView(order = createdOrder, title = "Order Created")
                Spacer(modifier = Modifier.size(24.dp))
                StartPayPalNativeCheckoutForm(
                    isLoading = uiState.isStartCheckoutLoading,
                    onSubmit = { startCheckout() }
                )
            }
            uiState.payPalNativeCheckoutResult?.let { result ->
                Spacer(modifier = Modifier.size(24.dp))
                PayPalWebCheckoutResultView(result.orderId, result.payerId)
                Spacer(modifier = Modifier.size(24.dp))
                CompleteOrderForm(
                    isLoading = uiState.isCompleteOrderLoading,
                    orderIntent = uiState.intentOption,
                    onSubmit = { onCompleteOrderClick() }
                )
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
}
