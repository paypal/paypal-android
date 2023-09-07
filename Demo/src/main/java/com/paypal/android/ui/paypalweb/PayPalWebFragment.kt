package com.paypal.android.ui.paypalweb

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.paypal.android.R
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import com.paypal.android.ui.WireframeButton
import com.paypal.android.ui.card.CreateOrderView
import com.paypal.android.ui.card.OrderCompleteView
import com.paypal.android.uishared.components.CreateOrderForm
import com.paypal.android.uishared.components.PayPalSDKErrorView
import com.paypal.android.uishared.components.PropertyView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class PayPalWebFragment : Fragment(), PayPalWebCheckoutListener {
    companion object {
        private val TAG = PayPalWebFragment::class.qualifiedName
    }

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    private lateinit var paypalClient: PayPalWebCheckoutClient
    private lateinit var payPalDataCollector: PayPalDataCollector

    private val viewModel by viewModels<PayPalWebViewModel>()

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
                    PayPalWebView(
                        uiState = uiState,
                        onCreateOrderClick = { createOrder() },
                        onStartCheckoutClick = { launchWebCheckout() }
                    )
                }
            }
        }
    }

    private fun createOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCreateOrderLoading = true

            val uiState = viewModel.uiState.value
            viewModel.createdOrder = uiState.run {
                sdkSampleServerAPI.createOrder(
                    orderIntent = intentOption,
                    shouldVault = shouldVault,
                    vaultCustomerId = customerId
                )
            }
            viewModel.isCreateOrderLoading = false
        }
    }

    private fun launchWebCheckout(funding: PayPalWebCheckoutFundingSource = PayPalWebCheckoutFundingSource.PAYPAL) {
        viewModel.isStartCheckoutLoading = true

        lifecycleScope.launch {
            try {
                val clientId = sdkSampleServerAPI.fetchClientId()
                val coreConfig = CoreConfig(clientId)
                payPalDataCollector = PayPalDataCollector(coreConfig)

                paypalClient = PayPalWebCheckoutClient(
                    requireActivity(),
                    coreConfig,
                    "com.paypal.android.demo"
                )
                paypalClient.listener = this@PayPalWebFragment

                val orderId = viewModel.createdOrder!!.id!!
                paypalClient.start(PayPalWebCheckoutRequest(orderId, funding))

            } catch (e: UnknownHostException) {
                viewModel.payPalWebCheckoutError = APIClientError.payPalCheckoutError(e.message!!)
                viewModel.isStartCheckoutLoading = false
            } catch (e: HttpException) {
                viewModel.payPalWebCheckoutError = APIClientError.payPalCheckoutError(e.message!!)
                viewModel.isStartCheckoutLoading = false
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")

        viewModel.payPalWebCheckoutResult = result
        viewModel.isStartCheckoutLoading = false
        completeOrder()
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebFailure(error: PayPalSDKError) {
        Log.i(TAG, "Checkout Error: ${error.errorDescription}")
        viewModel.payPalWebCheckoutError = error
        viewModel.isStartCheckoutLoading = false
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebCanceled() {
        Log.i(TAG, "User cancelled")
        viewModel.isCheckoutCanceled = true
        viewModel.isStartCheckoutLoading = false
    }

    private fun completeOrder() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isCompleteOrderLoading = true

            val cmid = payPalDataCollector.collectDeviceData(requireContext())
            val orderId = viewModel.createdOrder!!.id!!
            val orderIntent = viewModel.intentOption
            viewModel.completedOrder = when (orderIntent) {
                OrderIntent.CAPTURE -> sdkSampleServerAPI.captureOrder(orderId, cmid)
                OrderIntent.AUTHORIZE -> sdkSampleServerAPI.authorizeOrder(orderId, cmid)
            }
            viewModel.isCompleteOrderLoading = false
        }
    }

    @Composable
    fun PayPalWebView(
        uiState: PayPalWebUiState,
        onCreateOrderClick: () -> Unit,
        onStartCheckoutClick: () -> Unit
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
            CreateOrderForm(
                title = "Create an order to proceed with ${stringResource(R.string.feature_paypal_web)}:",
                orderIntent = uiState.intentOption,
                shouldVault = uiState.shouldVault,
                vaultCustomerId = uiState.customerId,
                isLoading = uiState.isCreateOrderLoading,
                onIntentOptionSelected = { value -> viewModel.intentOption = value },
                onShouldVaultChanged = { value -> viewModel.shouldVault = value },
                onVaultCustomerIdChanged = { value -> viewModel.customerId = value },
                onSubmit = { onCreateOrderClick() }
            )
            uiState.createdOrder?.let { createdOrder ->
                Spacer(modifier = Modifier.size(24.dp))
                CreateOrderView(order = createdOrder)
                Spacer(modifier = Modifier.size(24.dp))
                StartPayPalWebCheckoutForm(
                    uiState = uiState,
                    onSubmit = { onStartCheckoutClick() }
                )
            }
            uiState.payPalWebCheckoutResult?.let { result ->
                Spacer(modifier = Modifier.size(24.dp))
                PayPalWebCheckoutResultView(result = result)
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
                OrderCompleteView(order = completedOrder)
            }
            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    @Composable
    fun PayPalWebCheckoutResultView(result: PayPalWebCheckoutResult) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(stringResource(id = R.string.order_approved))
                PropertyView(name = "Order ID", value = result.orderId)
                PropertyView(name = "Payer ID", value = result.payerId)
            }
        }
    }

    @Composable
    fun PayPalWebCheckoutCanceledView() {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(stringResource(id = R.string.checkout_cancelled))
                Text(stringResource(id = R.string.user_cancelled))
            }
        }
    }

    @Composable
    fun StartPayPalWebCheckoutForm(uiState: PayPalWebUiState, onSubmit: () -> Unit) {
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "Launch PayPal Web Checkout",
                    style = MaterialTheme.typography.titleLarge
                )
                WireframeButton(
                    text = "Start Checkout",
                    isLoading = uiState.isStartCheckoutLoading,
                    onClick = { onSubmit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }
        }
    }

    @Preview
    @Composable
    fun PayPalWebViewPreview() {
        MaterialTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                PayPalWebView(
                    uiState = PayPalWebUiState(),
                    onCreateOrderClick = {},
                    onStartCheckoutClick = {}
                )
            }
        }
    }
}
