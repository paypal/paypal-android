package com.paypal.android.ui.paypalweb

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
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
import com.paypal.android.usecase.GetClientIdUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalWebViewModel @Inject constructor(
    val getClientIdUseCase: GetClientIdUseCase,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase
) : ViewModel() {

    companion object {
        private val TAG = PayPalWebViewModel::class.qualifiedName
    }

    private var paypalClient: PayPalWebCheckoutClient? = null
    private lateinit var payPalDataCollector: PayPalDataCollector

    private val _uiState = MutableStateFlow(PayPalWebUiState())
    val uiState = _uiState.asStateFlow()

    private var authState: String? = null

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

    fun startWebCheckout(activity: ComponentActivity) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            payPalWebCheckoutState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                startWebCheckoutWithOrderId(activity, orderId)
            }
        }
    }

    private suspend fun startWebCheckoutWithOrderId(activity: ComponentActivity, orderId: String) {
        payPalWebCheckoutState = ActionState.Loading

        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                payPalWebCheckoutState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {
                val coreConfig = CoreConfig(clientIdResult.value)
                payPalDataCollector = PayPalDataCollector(coreConfig)

                paypalClient =
                    PayPalWebCheckoutClient(activity, coreConfig, "com.paypal.android.demo")

                val checkoutRequest = PayPalWebCheckoutRequest(orderId, fundingSource)
                when (val startResult = paypalClient?.start(activity, checkoutRequest)) {
                    is PayPalPresentAuthChallengeResult.Success ->
                        authState = startResult.authState

                    is PayPalPresentAuthChallengeResult.Failure ->
                        payPalWebCheckoutState = ActionState.Failure(startResult.error)

                    null -> {
                        // do nothing
                    }
                }
            }
        }
    }

    fun completeOrder(context: Context) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            completeOrderState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                completeOrderState = ActionState.Loading
                val dataCollectorRequest =
                    PayPalDataCollectorRequest(hasUserLocationConsent = false)
                val cmid = payPalDataCollector.collectDeviceData(context, dataCollectorRequest)
                completeOrderState =
                    completeOrderUseCase(orderId, intentOption, cmid).mapToActionState()
            }
        }
    }

    private fun checkIfPayPalAuthFinished(intent: Intent): PayPalWebCheckoutFinishStartResult? =
        authState?.let { paypalClient?.finishStart(intent, it) }

    fun completeAuthChallenge(intent: Intent) {
        checkIfPayPalAuthFinished(intent)?.let { payPalAuthResult ->
            when (payPalAuthResult) {
                is PayPalWebCheckoutFinishStartResult.Success -> {
                    payPalWebCheckoutState = ActionState.Success(payPalAuthResult)
                    discardAuthState()
                }

                is PayPalWebCheckoutFinishStartResult.Canceled -> {
                    val error = Exception("USER CANCELED")
                    payPalWebCheckoutState = ActionState.Failure(error)
                    discardAuthState()
                }

                is PayPalWebCheckoutFinishStartResult.Failure -> {
                    Log.i(TAG, "Checkout Error: ${payPalAuthResult.error.errorDescription}")
                    payPalWebCheckoutState = ActionState.Failure(payPalAuthResult.error)
                    discardAuthState()
                }

                PayPalWebCheckoutFinishStartResult.NoResult -> {
                    // no result; re-enable PayPal button so user can retry
                    payPalWebCheckoutState = ActionState.Idle
                }
            }
        }
    }

    private fun discardAuthState() {
        authState = null
    }
}
