package com.paypal.android.plainclothes

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.cardpayments.ApproveOrderListener
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.CardClient
import com.paypal.android.cardpayments.CardRequest
import com.paypal.android.cardpayments.CardResult
import com.paypal.android.cardpayments.CardVaultListener
import com.paypal.android.cardpayments.CardVaultResult
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
import com.paypal.android.paypalwebpayments.PayPalWebVaultListener
import com.paypal.android.paypalwebpayments.PayPalWebVaultResult
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
) : AndroidViewModel(application), ApproveOrderListener, CardVaultListener,
    PayPalWebCheckoutListener,
    PayPalWebVaultListener {

    companion object {
        const val APP_RETURN_URL = "com.paypal.android.demo://example.com/returnUrl"
    }

    val applicationContext: Context = application.applicationContext

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState = _uiState.asStateFlow()

    private lateinit var cardClient: CardClient
    private lateinit var payPalClient: PayPalWebCheckoutClient
    private lateinit var payPalDataCollector: PayPalDataCollector

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
        initializePaymentsSDK(activity)
        val request = PayPalWebCheckoutRequest(order.id!!)

        // clear loader since we won't know much about the state of the PayPal flow after
        // the customer leaves the app
        isLoading = false
        payPalClient.start(request)
    }

    private suspend fun finishCheckoutWithCard(
        activity: FragmentActivity,
        order: Order,
        card: Card
    ) {
        initializePaymentsSDK(activity)
        val request = CardRequest(order.id!!, card, APP_RETURN_URL, sca = SCA.SCA_ALWAYS)
        cardClient.approveOrder(activity, request)
    }

    private suspend fun initializePaymentsSDK(activity: FragmentActivity) {
        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> {
                checkoutError = clientIdResult.value
            }

            is SDKSampleServerResult.Success -> {
                val clientId = clientIdResult.value
                val coreConfig = CoreConfig(clientId = clientId, environment = Environment.SANDBOX)

                cardClient = CardClient(activity, coreConfig)
                cardClient.approveOrderListener = this
                cardClient.cardVaultListener = this

                payPalClient = PayPalWebCheckoutClient(activity, coreConfig, APP_RETURN_URL)
                payPalClient.listener = this
                payPalClient.vaultListener = this

                payPalDataCollector = PayPalDataCollector(coreConfig)
            }
        }
    }

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

    private var checkoutError
        get() = _uiState.value.checkoutError
        set(value) {
            _uiState.update { it.copy(checkoutError = value) }
        }

    override fun onApproveOrderSuccess(result: CardResult) {
        hideCardFormModal()
        viewModelScope.launch {
            when (val completeOrderResult = completeOrder(result.orderId)) {
                is SDKSampleServerResult.Success -> {
                    // TODO: navigate to checkout success view
                }

                is SDKSampleServerResult.Failure -> checkoutError = completeOrderResult.value
            }
            isLoading = false
        }
    }

    override fun onApproveOrderFailure(error: PayPalSDKError) {
        hideCardFormModal()
        checkoutError = error
        isLoading = false
    }

    override fun onApproveOrderCanceled() {
        hideCardFormModal()
        checkoutError = Exception("User Canceled")
        isLoading = false
    }

    override fun onApproveOrderThreeDSecureWillLaunch() {
        // do nothing
    }

    override fun onApproveOrderThreeDSecureDidFinish() {
        // do nothing
    }

    override fun onVaultSuccess(result: CardVaultResult) {
        TODO("Not yet implemented")
    }

    override fun onVaultFailure(error: PayPalSDKError) {
        TODO("Not yet implemented")
    }

    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        TODO("Not yet implemented")
    }

    override fun onPayPalWebFailure(error: PayPalSDKError) {
        TODO("Not yet implemented")
    }

    override fun onPayPalWebCanceled() {
        TODO("Not yet implemented")
    }

    override fun onPayPalWebVaultSuccess(result: PayPalWebVaultResult) {
        TODO("Not yet implemented")
    }

    override fun onPayPalWebVaultFailure(error: PayPalSDKError) {
        TODO("Not yet implemented")
    }

    override fun onPayPalWebVaultCanceled() {
        TODO("Not yet implemented")
    }

    private suspend fun completeOrder(orderId: String): SDKSampleServerResult<Order, Exception> {
        val dataCollectorRequest = PayPalDataCollectorRequest(hasUserLocationConsent = false)
        val cmid = payPalDataCollector.collectDeviceData(applicationContext, dataCollectorRequest)
        return completeOrderUseCase(orderId, OrderIntent.CAPTURE, cmid)
    }
}