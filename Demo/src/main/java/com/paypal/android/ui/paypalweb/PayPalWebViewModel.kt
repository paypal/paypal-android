package com.paypal.android.ui.paypalweb

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.ChromeCustomTabsResult
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalWebViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val getClientIdUseCase: GetClientIdUseCase,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    companion object {
        private val TAG = PayPalWebViewModel::class.qualifiedName
    }

    private var paypalClient: PayPalWebCheckoutClient? = null
    private lateinit var payPalDataCollector: PayPalDataCollector

    private val _uiState = MutableStateFlow(PayPalWebUiState())
    val uiState = _uiState.asStateFlow()

    init {
        registerPayPalWebCheckoutClientSaveInstanceStateHandler()
    }

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

    private fun registerPayPalWebCheckoutClientSaveInstanceStateHandler() {
        savedStateHandle.setSavedStateProvider("pay_pal_web_view_model") {
            val bundle = Bundle()
            paypalClient?.instanceState?.let { instanceState ->
                bundle.putString("instance_state", instanceState)
            }
            bundle
        }
    }

    private fun restorePayPalWebCheckoutClientFromSavedInstanceState() {
        // restore instance state
        val savedStateBundle = savedStateHandle.get<Bundle>("pay_pal_web_view_model")
        savedStateBundle?.let { bundle ->
            bundle.getString("instance_state")?.let { instanceState ->
                paypalClient?.restore(instanceState)
            }
        }
        // make sure saved instance state is only restored once
        savedStateHandle.remove<Bundle>("pay_pal_web_view_model")
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

    suspend fun startWebCheckout(): Uri? {
        val orderId = createdOrder?.id
        if (orderId == null) {
            payPalWebCheckoutState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            return startWebCheckoutWithOrderId(orderId)
        }
        return null
    }

    private suspend fun startWebCheckoutWithOrderId(orderId: String): Uri? {
        payPalWebCheckoutState = ActionState.Loading

        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                payPalWebCheckoutState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {
                val coreConfig = CoreConfig(clientIdResult.value)
                payPalDataCollector = PayPalDataCollector(coreConfig)

                paypalClient =
                    PayPalWebCheckoutClient(applicationContext, coreConfig, "com.paypal.android.demo")
                restorePayPalWebCheckoutClientFromSavedInstanceState()

                val checkoutRequest = PayPalWebCheckoutRequest(orderId, fundingSource)
                when (val startResult = paypalClient?.start(checkoutRequest)) {
                    is PayPalPresentAuthChallengeResult.Success -> {
                        return startResult.uri
                    }

                    is PayPalPresentAuthChallengeResult.Failure ->
                        payPalWebCheckoutState = ActionState.Failure(startResult.error)

                    null -> {
                        // do nothing
                    }
                }
            }
        }
        return null
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
         paypalClient?.finishStart(intent)

    fun completeAuthChallenge(intent: Intent) {
        checkIfPayPalAuthFinished(intent)?.let { payPalAuthResult ->
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

    fun completeAuthChallenge(chromeCustomTabsResult: ChromeCustomTabsResult) {
//        if (chromeCustomTabsResult.resultCode == Activity.RESULT_CANCELED) {
//            val error = Exception("USER CANCELED")
//            payPalWebCheckoutState = ActionState.Failure(error)
//            discardAuthState()
//        }
    }
}
