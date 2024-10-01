package com.paypal.android.ui.paypalweb

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.models.OrderRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutAuthResult
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutStartResult
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
    application: Application,
    val getClientIdUseCase: GetClientIdUseCase,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase
) : AndroidViewModel(application) {

    companion object {
        private val TAG = PayPalWebViewModel::class.qualifiedName
        private const val APP_URL_SCHEME = "com.paypal.android.demo"
    }

    private val paypalClient = PayPalWebCheckoutClient(application.applicationContext)
    private var payPalDataCollector = PayPalDataCollector(application.applicationContext)

    private val _uiState = MutableStateFlow(PayPalWebUiState())
    val uiState = _uiState.asStateFlow()

    private var coreConfig: CoreConfig? = null
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

    private var authChallengeState
        get() = _uiState.value.authChallengeState
        set(value) {
            _uiState.update { it.copy(authChallengeState = value) }
        }

    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            val orderRequest = _uiState.value.run {
                OrderRequest(orderIntent = intentOption, shouldVault = false)
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    fun startWebCheckout(activity: AppCompatActivity) {
        val orderId = createdOrder?.id
        if (orderId == null) {
            authChallengeState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                startWebCheckoutWithOrderId(activity, orderId)
            }
        }
    }

    private suspend fun startWebCheckoutWithOrderId(activity: AppCompatActivity, orderId: String) {

        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                authChallengeState = clientIdResult.mapToActionState()
            }

            is SDKSampleServerResult.Success -> {
                coreConfig = CoreConfig(clientIdResult.value)

                val request =
                    PayPalWebCheckoutRequest(coreConfig!!, orderId, APP_URL_SCHEME, fundingSource)

                when (val startResult = paypalClient.start(activity, request)) {
                    is PayPalWebCheckoutStartResult.DidLaunchAuth -> {
                        authState = startResult.authState
                    }

                    is PayPalWebCheckoutStartResult.Failure -> {
                        authChallengeState = ActionState.Failure(startResult.error)
                    }
                }
            }
        }
    }

    fun checkIntentForResult(intent: Intent) = authState?.let { state ->
        when (val result = paypalClient.getCheckoutAuthResult(intent, state)) {
            is PayPalWebCheckoutAuthResult.Success -> {
                authChallengeState = ActionState.Success(result)
            }

            is PayPalWebCheckoutAuthResult.Failure -> {
                authChallengeState = ActionState.Failure(result.error)
            }

            PayPalWebCheckoutAuthResult.NoResult -> {
                // do nothing
            }

            PayPalWebCheckoutAuthResult.Canceled -> {
                authChallengeState = ActionState.Failure(Exception("User canceled"))
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
                val dataCollectorRequest = PayPalDataCollectorRequest(
                    config = coreConfig!!,
                    hasUserLocationConsent = false
                )
                val cmid = payPalDataCollector.collectDeviceData(dataCollectorRequest)
                completeOrderState =
                    completeOrderUseCase(orderId, intentOption, cmid).mapToActionState()
            }
        }
    }
}
