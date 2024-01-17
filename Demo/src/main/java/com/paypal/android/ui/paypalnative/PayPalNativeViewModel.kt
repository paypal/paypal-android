package com.paypal.android.ui.paypalnative

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.BuildConfig
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
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
import com.paypal.android.api.services.SDKSampleServerResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.IOException
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

            viewModelScope.launch(exceptionHandler) {
                orderId?.also {
                    try {
                        updateOrderUseCase(it, shippingMethod)
                        actions.approve()
                    } catch (e: IOException) {
                        actions.reject()
                        throw e
                    }
                }
            }
        }
    }

    private lateinit var payPalClient: PayPalNativeCheckoutClient
    private lateinit var payPalDataCollector: PayPalDataCollector

    private val exceptionHandler = CoroutineExceptionHandler { _, e ->
        // TODO: show error in UI using a Compose UI Alert Dialog to improve error messaging UX
        Toast.makeText(getApplication(), e.message, Toast.LENGTH_LONG).show()
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

                payPalDataCollector = PayPalDataCollector(coreConfig)
                payPalClient.startCheckout(PayPalNativeCheckoutRequest(orderId))
            }
        }
    }

    fun completeOrder() {
        completeOrderState = ActionState.Loading
        viewModelScope.launch {
            val orderId = createdOrder?.id
            completeOrderState = if (orderId == null) {
                ActionState.Failure(Exception("Create an order to continue."))
            } else {
                val cmid = payPalDataCollector.collectDeviceData(getApplication())
                completeOrderUseCase(orderId, intentOption, cmid).mapToActionState()
            }
        }
    }

    fun createOrder() {
        createOrderState = ActionState.Loading
        viewModelScope.launch {
            createOrderState = getOrderUseCase(shippingPreference, intentOption).mapToActionState()
        }
    }
}
