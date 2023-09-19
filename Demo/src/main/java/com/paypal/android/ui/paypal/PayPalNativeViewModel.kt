package com.paypal.android.ui.paypal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.BuildConfig
import com.paypal.android.api.model.Order
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.cardpayments.OrderIntent
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
import com.paypal.android.usecase.AuthorizeOrderUseCase
import com.paypal.android.usecase.CaptureOrderUseCase
import com.paypal.android.usecase.GetClientIdUseCase
import com.paypal.android.usecase.GetOrderUseCase
import com.paypal.android.usecase.UpdateOrderUseCase
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
    application: Application
) : AndroidViewModel(application) {

    @Inject
    lateinit var getClientIdUseCase: GetClientIdUseCase

    @Inject
    lateinit var getOrderUseCase: GetOrderUseCase

    @Inject
    lateinit var completeOrderUseCase: CompleteOrderUseCase

    @Inject
    lateinit var updateOrderUseCase: UpdateOrderUseCase

    @Inject
    lateinit var sdkSampleServerAPI: SDKSampleServerAPI

    private var orderId: String? = null

    private val _uiState = MutableStateFlow(PayPalNativeUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    var isCreateOrderLoading: Boolean
        get() = _uiState.value.isCreateOrderLoading
        set(value) {
            _uiState.update { it.copy(isCreateOrderLoading = value) }
        }

    var isStartCheckoutLoading: Boolean
        get() = _uiState.value.isStartCheckoutLoading
        set(value) {
            _uiState.update { it.copy(isStartCheckoutLoading = value) }
        }

    var isCheckoutCanceled: Boolean
        get() = _uiState.value.isCheckoutCanceled
        set(value) {
            _uiState.update { it.copy(isCheckoutCanceled = value) }
        }

    var isCompleteOrderLoading: Boolean
        get() = _uiState.value.isCompleteOrderLoading
        set(value) {
            _uiState.update { it.copy(isCompleteOrderLoading = value) }
        }

    var createdOrder: Order?
        get() = _uiState.value.createdOrder
        set(value) {
            _uiState.update { it.copy(createdOrder = value) }
        }

    var completedOrder: Order?
        get() = _uiState.value.completedOrder
        set(value) {
            _uiState.update { it.copy(completedOrder = value) }
        }

    var shippingPreference: ShippingPreferenceType
        get() = _uiState.value.shippingPreference
        set(value) {
            _uiState.update { it.copy(shippingPreference = value) }
        }

    var payPalNativeCheckoutResult: PayPalNativeCheckoutResult?
        get() = _uiState.value.payPalNativeCheckoutResult
        set(value) {
            _uiState.update { it.copy(payPalNativeCheckoutResult = value) }
        }

    var payPalNativeCheckoutError: PayPalSDKError?
        get() = _uiState.value.payPalNativeCheckoutError
        set(value) {
            _uiState.update { it.copy(payPalNativeCheckoutError = value) }
        }

    private val payPalListener = object : PayPalNativeCheckoutListener {
        override fun onPayPalCheckoutStart() {
            isStartCheckoutLoading = true
        }

        override fun onPayPalCheckoutSuccess(result: PayPalNativeCheckoutResult) {
            isStartCheckoutLoading = false
            payPalNativeCheckoutResult = result
        }

        override fun onPayPalCheckoutFailure(error: PayPalSDKError) {
            isStartCheckoutLoading = false
            payPalNativeCheckoutError = error
        }

        override fun onPayPalCheckoutCanceled() {
            isStartCheckoutLoading = false
            isCheckoutCanceled = true
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
        // TODO: propagate error to UI
    }

    suspend fun startNativeCheckout() {
        val clientId = getClientIdUseCase()

        val coreConfig = CoreConfig(clientId)
        val returnUrl = "${BuildConfig.APPLICATION_ID}://paypalpay"
        payPalClient = PayPalNativeCheckoutClient(getApplication(), coreConfig, returnUrl)
        payPalClient.listener = payPalListener
        payPalClient.shippingListener = shippingListener

        payPalDataCollector = PayPalDataCollector(coreConfig)

        createdOrder?.id?.also { orderId ->
            payPalClient.startCheckout(PayPalNativeCheckoutRequest(orderId))
        }
    }

    fun completeOrder() {
        viewModelScope.launch {
            isCompleteOrderLoading = true

            val cmid = payPalDataCollector.collectDeviceData(getApplication())
            val orderId = createdOrder!!.id!!
            val orderIntent = intentOption
            completedOrder = when (orderIntent) {
                OrderIntent.CAPTURE -> sdkSampleServerAPI.captureOrder(orderId, cmid)
                OrderIntent.AUTHORIZE -> sdkSampleServerAPI.authorizeOrder(orderId, cmid)
            }
            isCompleteOrderLoading = false
        }
    }
}
