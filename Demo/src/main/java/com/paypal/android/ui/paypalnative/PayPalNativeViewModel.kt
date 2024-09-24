package com.paypal.android.ui.paypalnative

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.BuildConfig
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutClient
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutListener
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutRequest
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult
import com.paypal.android.paypalnativepayments.PayPalNativePaysheetActions
import com.paypal.android.paypalnativepayments.PayPalNativeShippingAddress
import com.paypal.android.paypalnativepayments.PayPalNativeShippingListener
import com.paypal.android.paypalnativepayments.PayPalNativeShippingMethod
import com.paypal.android.uishared.state.ActionState
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import com.paypal.android.usecase.GetOrderUseCase
import com.paypal.android.usecase.UpdateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalNativeViewModel @Inject constructor(
    application: Application,
    private val getClientIdUseCase: GetClientIdUseCase,
    private val getOrderUseCase: GetOrderUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
    private val updateOrderUseCase: UpdateOrderUseCase,
) : AndroidViewModel(application) {

    private var orderId: String? = null

    private val _uiState = MutableStateFlow(PayPalNativeUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var payPalClient: PayPalNativeCheckoutClient
    private lateinit var payPalDataCollector: PayPalDataCollector

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    private var payPalNativeCheckoutState
        get() = _uiState.value.payPalNativeCheckoutState
        set(value) {
            _uiState.update { it.copy(payPalNativeCheckoutState = value) }
        }

    private var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private var completeOrderState
        get() = _uiState.value.completeOrderState
        set(value) {
            _uiState.update { it.copy(completeOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    var shippingPreference: ShippingPreferenceType
        get() = _uiState.value.shippingPreference
        set(value) {
            _uiState.update { it.copy(shippingPreference = value) }
        }

    private val payPalListener = object : PayPalNativeCheckoutListener {
        override fun onPayPalCheckoutStart() {
            payPalNativeCheckoutState = ActionState.Loading
        }

        override fun onPayPalCheckoutSuccess(result: PayPalNativeCheckoutResult) {
            payPalNativeCheckoutState = ActionState.Success(result)
        }

        override fun onPayPalCheckoutFailure(error: PayPalSDKError) {
            payPalNativeCheckoutState = ActionState.Failure(error)
        }

        override fun onPayPalCheckoutCanceled() {
            val error = Exception("USER CANCELED")
            payPalNativeCheckoutState = ActionState.Failure(error)
        }
    }

    private val shippingListener = object : PayPalNativeShippingListener {

        override fun onPayPalNativeShippingAddressChange(
            actions: PayPalNativePaysheetActions,
            shippingAddress: PayPalNativeShippingAddress
        ) {
            if (shippingAddress.adminArea1.isNullOrBlank() || shippingAddress.adminArea1 == "NV") {
                actions.reject()
            } else {
                actions.approve()
            }
        }

        override fun onPayPalNativeShippingMethodChange(
            actions: PayPalNativePaysheetActions,
            shippingMethod: PayPalNativeShippingMethod
        ) {
            orderId?.let { orderId ->
                viewModelScope.launch {
                    when (updateOrderUseCase(orderId, shippingMethod)) {
                        is SDKSampleServerResult.Failure -> actions.reject()
                        is SDKSampleServerResult.Success -> actions.approve()
                    }
                }
            }
        }
    }

    fun startNativeCheckout() {
        val orderId = createdOrder?.id
        if (orderId == null) {
            payPalNativeCheckoutState =
                ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                startNativeCheckoutWithOrderId(orderId)
            }
        }
    }

    private suspend fun startNativeCheckoutWithOrderId(orderId: String) {
        payPalNativeCheckoutState = ActionState.Loading
        when (val getClientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                payPalNativeCheckoutState = ActionState.Failure(getClientIdResult.value)
            }

            is SDKSampleServerResult.Success -> {
                val returnUrl = "${BuildConfig.APPLICATION_ID}://paypalpay"
                val coreConfig = CoreConfig(getClientIdResult.value)
                payPalClient = PayPalNativeCheckoutClient(getApplication(), coreConfig, returnUrl)

                payPalClient.listener = payPalListener
                payPalClient.shippingListener = shippingListener

                payPalClient.startCheckout(PayPalNativeCheckoutRequest(orderId))
            }
        }
    }

    fun completeOrder() {
        viewModelScope.launch {
            val orderId = createdOrder?.id
            if (orderId == null) {
                completeOrderState = ActionState.Failure(Exception("Create an order to continue."))
            } else {
                completeOrderState = ActionState.Loading
                // TODO: fix once data collector semantics are determined
//                val dataCollectorRequest =
//                    PayPalDataCollectorRequest(hasUserLocationConsent = false)
//                val cmid = payPalDataCollector.collectDeviceData(dataCollectorRequest)
//                completeOrderState =
//                    completeOrderUseCase(orderId, intentOption, cmid).mapToActionState()
            }
        }
    }

    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            createOrderState = getOrderUseCase(shippingPreference, intentOption).mapToActionState()
        }
    }
}
