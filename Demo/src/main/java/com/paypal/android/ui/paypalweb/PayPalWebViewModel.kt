package com.paypal.android.ui.paypalweb

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.models.OrderRequest
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalWebViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase
) : ViewModel() {

    companion object {
        private val TAG = PayPalWebViewModel::class.qualifiedName
    }

    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId)
    private val payPalDataCollector = PayPalDataCollector(coreConfig)
    private val paypalClient =
        PayPalWebCheckoutClient(applicationContext, coreConfig, "com.paypal.android.demo")

    private val _uiState = MutableStateFlow(PayPalWebUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    private var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    private var payPalWebCheckoutState
        get() = _uiState.value.payPalWebCheckoutState
        set(value) {
            _uiState.update { it.copy(payPalWebCheckoutState = value) }
        }

    private var completeOrderState
        get() = _uiState.value.completeOrderState
        set(value) {
            _uiState.update { it.copy(completeOrderState = value) }
        }

    var fundingSource: PayPalWebCheckoutFundingSource
        get() = _uiState.value.fundingSource
        set(value) {
            _uiState.update { it.copy(fundingSource = value) }
        }

    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            val orderRequest = _uiState.value.run {
                OrderRequest(intent = intentOption, shouldVault = false)
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    fun startWebCheckout(context: Context) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            payPalWebCheckoutState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                startWebCheckoutWithOrderId(context, orderId)
            }
        }
    }

    private fun startWebCheckoutWithOrderId(context: Context, orderId: String) {
        payPalWebCheckoutState = ActionState.Loading

        val checkoutRequest = PayPalWebCheckoutRequest(orderId, fundingSource)
        when (val startResult = paypalClient.start(context, checkoutRequest)) {
            is PayPalPresentAuthChallengeResult.Success -> {
                // do nothing; wait for user to authenticate PayPal checkout in Chrome Custom Tab
            }

            is PayPalPresentAuthChallengeResult.Failure ->
                payPalWebCheckoutState = ActionState.Failure(startResult.error)
        }
    }

    fun completeOrder() {
        val orderId = createdOrder?.id
        if (orderId == null) {
            completeOrderState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                completeOrderState = ActionState.Loading
                val dataCollectorRequest =
                    PayPalDataCollectorRequest(hasUserLocationConsent = false)
                val cmid =
                    payPalDataCollector.collectDeviceData(applicationContext, dataCollectorRequest)
                completeOrderState =
                    completeOrderUseCase(orderId, intentOption, cmid).mapToActionState()
            }
        }
    }

    fun completeAuthChallenge(intent: Intent) =
        paypalClient.finishStart(intent)?.let { payPalAuthResult ->
            when (payPalAuthResult) {
                is PayPalWebCheckoutFinishStartResult.Success -> {
                    payPalWebCheckoutState = ActionState.Success(payPalAuthResult)
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    val error = Exception("USER CANCELED")
                    payPalWebCheckoutState = ActionState.Failure(error)
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    Log.i(TAG, "Checkout Error: ${payPalAuthResult.error.errorDescription}")
                    payPalWebCheckoutState = ActionState.Failure(payPalAuthResult.error)
                }

                PayPalWebCheckoutFinishStartResult.NoResult -> {
                    // no result; re-enable PayPal button so user can retry
                    payPalWebCheckoutState = ActionState.Idle
                }
            }
        }
}
