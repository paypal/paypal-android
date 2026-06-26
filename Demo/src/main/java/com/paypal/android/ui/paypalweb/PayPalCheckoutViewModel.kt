package com.paypal.android.ui.paypalweb

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.models.OrderRequest
import com.paypal.android.paypalwebpayments.CreateOrderHandler
import com.paypal.android.paypalwebpayments.CreateOrderResponse
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalUserIdentity
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.uishared.enums.ReturnToAppStrategyOption
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class PayPalCheckoutViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase
) : ViewModel() {

    companion object {
        private val TAG = PayPalCheckoutViewModel::class.qualifiedName
    }

    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId)
    private val payPalDataCollector = PayPalDataCollector(coreConfig)
    private val paypalClient = PayPalWebCheckoutClient(applicationContext, coreConfig)

    private val _uiState = MutableStateFlow(PayPalUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    var returnToAppStrategyOption: ReturnToAppStrategyOption
        get() = _uiState.value.returnToAppStrategyOption
        set(value) {
            _uiState.update { it.copy(returnToAppStrategyOption = value) }
        }

    private var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    private var lastOrderResult: SDKSampleServerResult<Order, Exception>? = null

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

    var userIdentity: PayPalUserIdentity
        get() = _uiState.value.userIdentity
        set(value) {
            _uiState.update { it.copy(userIdentity = value) }
        }

    fun startCheckout(activity: ComponentActivity) {
        createOrderState = ActionState.Loading
        payPalWebCheckoutState = ActionState.Loading

        val createOrderHandler = CreateOrderHandler {
            val orderRequest = OrderRequest(
                intent = intentOption,
                shouldVaultOnSuccess = false,
                returnToAppStrategy = returnToAppStrategyOption
            )
            val result = runBlocking { createOrderUseCase(orderRequest) }
            lastOrderResult = result
            when (result) {
                is SDKSampleServerResult.Success ->
                    CreateOrderResponse.Success(result.value.id ?: "")

                is SDKSampleServerResult.Failure ->
                    CreateOrderResponse.Failure(result.value)
            }
        }

        val checkoutRequest = PayPalWebCheckoutRequest(
            userIdentity = userIdentity,
            payPalURLConfig = returnToAppStrategyOption.toReturnToAppUrlConfig()
        )

        paypalClient.start(activity, checkoutRequest, createOrderHandler) { startResult ->
            createOrderState = lastOrderResult?.mapToActionState() ?: ActionState.Idle
            when (startResult) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    // do nothing; wait for user to authenticate PayPal checkout in Chrome Custom Tab
                }

                is PayPalPresentAuthChallengeResult.Failure ->
                    payPalWebCheckoutState = ActionState.Failure(startResult.error)
            }
        }
    }

    fun completeOrder(context: Context) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            completeOrderState = ActionState.Failure(Exception("Start checkout to continue."))
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
