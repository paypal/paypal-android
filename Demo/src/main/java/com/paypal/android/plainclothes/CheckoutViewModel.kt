package com.paypal.android.plainclothes

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardAuthChallenge
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.cardpayments.threedsecure.SCA
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.Environment
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.fraudprotection.PayPalDataCollectorRequest
import com.paypal.android.models.OrderRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
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
class CheckoutViewModel @Inject constructor(
    application: Application,
    private val createOrderUseCase: CreateOrderUseCase,
    private val getClientIdUseCase: GetClientIdUseCase,
    private val completeOrderUseCase: CompleteOrderUseCase,
) : AndroidViewModel(application), ApproveOrderListener, PayPalWebCheckoutListener {

    companion object {
        const val CARD_RETURN_URL = "com.paypal.android.demo.card://"
        const val PAYPAL_RETURN_URL_SCHEME = "com.paypal.android.demo.paypal"
    }

    private val applicationContext: Context = application.applicationContext

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState = _uiState.asStateFlow()

    private var cardClient: CardClient? = null
    private var payPalClient: PayPalWebCheckoutClient? = null
    private var payPalDataCollector: PayPalDataCollector? = null

    private var isCardFormModalVisible
        get() = _uiState.value.isCardFormModalVisible
        set(value) {
            _uiState.update { it.copy(isCardFormModalVisible = value) }
        }

    private var isLoading
        get() = _uiState.value.isLoading
        set(value) {
            _uiState.update { it.copy(isLoading = value) }
        }

    private var checkoutSuccessOrderId
        get() = _uiState.value.checkoutSuccessOrderId
        set(value) {
            _uiState.update { it.copy(checkoutSuccessOrderId = value) }
        }

    private var checkoutError
        get() = _uiState.value.checkoutError
        set(value) {
            _uiState.update { it.copy(checkoutError = value) }
        }


    fun showCardFormModal() {
        isCardFormModalVisible = true
    }

    fun hideCardFormModal() {
        isCardFormModalVisible = false
    }

    fun checkoutWithPayPal(activity: FragmentActivity) {
        isLoading = true
        viewModelScope.launch {
            when (val orderResult = createOrder()) {
                is SDKSampleServerResult.Success ->
                    finishCheckoutWithPayPal(activity, orderResult.value)

                is SDKSampleServerResult.Failure -> checkoutError = orderResult.value
            }
        }
    }

    fun checkoutWithCard(activity: FragmentActivity, card: Card) {
        isLoading = true
        viewModelScope.launch {
            when (val orderResult = createOrder()) {
                is SDKSampleServerResult.Success ->
                    finishCheckoutWithCard(activity, orderResult.value, card)

                is SDKSampleServerResult.Failure -> checkoutError = orderResult.value
            }
        }
    }

    private suspend fun createOrder(): SDKSampleServerResult<Order, Exception> {
        val orderRequest = OrderRequest(intent = OrderIntent.CAPTURE, shouldVault = false)
        return createOrderUseCase(orderRequest)
    }

    private suspend fun finishCheckoutWithPayPal(activity: FragmentActivity, order: Order) {
        initializePayPalClient(activity)
        val request = PayPalWebCheckoutRequest(order.id!!)

        // clear loader since we won't know much about the state of the PayPal flow after
        // the customer leaves the app
        isLoading = false
//        payPalClient?.start(request)
    }

    private suspend fun finishCheckoutWithCard(
        activity: FragmentActivity,
        order: Order,
        card: Card
    ) {
        initializeCardClient(activity)
        val request = CardRequest(order.id!!, card, CARD_RETURN_URL, sca = SCA.SCA_ALWAYS)
//        cardClient?.approveOrder(activity, request)
    }

    private suspend fun initializeCardClient(activity: FragmentActivity) {
        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                checkoutError = clientIdResult.value
            }

            is SDKSampleServerResult.Success -> {
                val clientId = clientIdResult.value
                val coreConfig = CoreConfig(clientId = clientId, environment = Environment.SANDBOX)

                cardClient = CardClient(activity, coreConfig)
                cardClient?.approveOrderListener = this
                payPalDataCollector = PayPalDataCollector(coreConfig)
            }
        }
    }

    private suspend fun initializePayPalClient(activity: FragmentActivity) {
        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                checkoutError = clientIdResult.value
            }

            is SDKSampleServerResult.Success -> {
                val clientId = clientIdResult.value
                val coreConfig = CoreConfig(clientId = clientId, environment = Environment.SANDBOX)

                payPalClient =
                    PayPalWebCheckoutClient(activity, coreConfig, PAYPAL_RETURN_URL_SCHEME)
                payPalClient?.listener = this

                payPalDataCollector = PayPalDataCollector(coreConfig)
            }
        }
    }

    override fun onApproveOrderSuccess(result: CardResult) {
        hideCardFormModal()
        viewModelScope.launch {
            when (val completeOrderResult = completeOrder(result.orderId)) {
                is SDKSampleServerResult.Success -> {
                    checkoutSuccessOrderId = completeOrderResult.value.id
                }

                is SDKSampleServerResult.Failure -> checkoutError = completeOrderResult.value
            }
            isLoading = false
        }
    }

    override fun onApproveOrderAuthorizationRequired(authChallenge: CardAuthChallenge) {

    }

    override fun onApproveOrderFailure(error: PayPalSDKError) {
        hideCardFormModal()
        checkoutError = error
        isLoading = false
    }

    override fun onApproveOrderCanceled() {
        hideCardFormModal()
        // NOTE: the SDK cannot accurately determine implicit cancellations i.e. user returns to app
        // without completing 3DS; for this reason the canceled event type was removed in BT v5 and
        // it will also be removed in PPCP v2
        isLoading = false
    }

    override fun onApproveOrderThreeDSecureWillLaunch() {
        // do nothing
    }

    override fun onApproveOrderThreeDSecureDidFinish() {
        // do nothing
    }

    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        isLoading = true
        viewModelScope.launch {
            when (val completeOrderResult = completeOrder(result.orderId!!)) {
                is SDKSampleServerResult.Success -> {
                    checkoutSuccessOrderId = completeOrderResult.value.id
                }

                is SDKSampleServerResult.Failure -> checkoutError = completeOrderResult.value
            }
            isLoading = false
        }
    }

    override fun onPayPalWebFailure(error: PayPalSDKError) {
        checkoutError = error
        isLoading = false
    }

    override fun onPayPalWebCanceled() {
        // NOTE: the SDK cannot accurately determine implicit cancellations i.e. user returns to app
        // without completing PayPal authorization; for this reason the canceled event type was
        // removed in BT v5 and it will also be removed in PPCP v2
        isLoading = false
    }

    private suspend fun completeOrder(orderId: String): SDKSampleServerResult<Order, Exception> {
        val dataCollectorRequest = PayPalDataCollectorRequest(hasUserLocationConsent = false)
        val cmid = payPalDataCollector?.collectDeviceData(applicationContext, dataCollectorRequest)
        return completeOrderUseCase(orderId, OrderIntent.CAPTURE, cmid ?: "")
    }

    fun clearCheckoutError() {
        checkoutError = null
    }
}