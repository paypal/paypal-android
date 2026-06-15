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
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.models.OrderRequest
import com.paypal.android.DemoConstants
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.paypalwebpayments.CreateOrderHandler
import com.paypal.android.paypalwebpayments.CreateOrderResponse
import com.paypal.android.utils.ReturnUrlFactory
import com.paypal.android.paypalwebpayments.PayPalPresentAuthChallengeResult
import com.paypal.android.paypalwebpayments.PayPalUserIdentity
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFinishStartResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.ReturnToAppUrlConfig
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

    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId, SDKSampleServerAPI.merchantId)
    private val payPalDataCollector = PayPalDataCollector(coreConfig)
    private val paypalClient =
        PayPalWebCheckoutClient(applicationContext, coreConfig)

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
                OrderRequest(
                    intent = intentOption,
                    shouldVaultOnSuccess = false,
                    returnToAppStrategy = returnToAppStrategyOption
                )
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    fun startCheckout(activity: ComponentActivity) {
        payPalWebCheckoutState = ActionState.Loading

        val returnToAppStrategy = returnToAppStrategyOption.toReturnToAppStrategy()
        val checkoutRequest = PayPalWebCheckoutRequest(
            userIdentity = PayPalUserIdentity.Unknown,
            returnToAppUrlConfig = ReturnToAppUrlConfig(
                returnAppUrl = ReturnUrlFactory.createCheckoutSuccessUrl(returnToAppStrategy),
                cancelAppUrl = ReturnUrlFactory.createCheckoutCancelUrl(returnToAppStrategy),
                fallbackSchemeUrl = "${DemoConstants.APP_CUSTOM_URL_SCHEME}://paypal-sdk/paypal-checkout",
            )
        )

        val orderRequest = _uiState.value.run {
            OrderRequest(intent = intentOption, shouldVaultOnSuccess = false, returnToAppStrategy = returnToAppStrategyOption)
        }

        paypalClient.start(activity, checkoutRequest, CreateOrderHandler { callback ->
            viewModelScope.launch {
                when (val result = createOrderUseCase(orderRequest)) {
                    is SDKSampleServerResult.Success -> {
                        createOrderState = ActionState.Success(result.value)
                        val orderId = result.value.id
                        if (orderId == null) {
                            callback(CreateOrderResponse.Failure(Exception("Order ID is null")))
                        } else {
                            callback(CreateOrderResponse.Success(orderId))
                        }
                    }
                    is SDKSampleServerResult.Failure ->
                        callback(CreateOrderResponse.Failure(result.value))
                }
            }
        }) { startResult ->
            when (startResult) {
                is PayPalPresentAuthChallengeResult.Success -> {
                    // Browser switch launched; wait for user to complete PayPal checkout
                }
                is PayPalPresentAuthChallengeResult.Failure ->
                    payPalWebCheckoutState = ActionState.Failure(startResult.error)
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
