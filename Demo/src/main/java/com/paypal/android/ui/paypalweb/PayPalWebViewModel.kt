package com.paypal.android.ui.paypalweb

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.APIClientError
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.fraudprotection.PayPalDataCollector
import com.paypal.android.models.OrderRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutClient
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutListener
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutRequest
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import com.paypal.android.usecase.CompleteOrderUseCase
import com.paypal.android.usecase.CreateOrderUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException
import javax.inject.Inject

@HiltViewModel
class PayPalWebViewModel @Inject constructor(
    val sdkSampleServerAPI: SDKSampleServerAPI,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase
) : ViewModel(), PayPalWebCheckoutListener {

    companion object {
        private val TAG = PayPalWebViewModel::class.qualifiedName
        private const val URL_SCHEME = "com.paypal.android.demo"
    }

    private lateinit var paypalClient: PayPalWebCheckoutClient
    private lateinit var payPalDataCollector: PayPalDataCollector

    private val _uiState = MutableStateFlow(PayPalWebUiState())
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

    var payPalWebCheckoutResult: PayPalWebCheckoutResult?
        get() = _uiState.value.payPalWebCheckoutResult
        set(value) {
            _uiState.update { it.copy(payPalWebCheckoutResult = value) }
        }

    var payPalWebCheckoutError: PayPalSDKError?
        get() = _uiState.value.payPalWebCheckoutError
        set(value) {
            _uiState.update { it.copy(payPalWebCheckoutError = value) }
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

    var fundingSource: PayPalWebCheckoutFundingSource
        get() = _uiState.value.fundingSource
        set(value) {
            _uiState.update { it.copy(fundingSource = value) }
        }

    fun createOrder() {
        viewModelScope.launch {
            isCreateOrderLoading = true
            val orderRequest = _uiState.value.run {
                OrderRequest(orderIntent = intentOption, shouldVault = false, vaultCustomerId = "")
            }
            createdOrder = createOrderUseCase(orderRequest)
            isCreateOrderLoading = false
        }
    }

    private suspend fun fetchClientId(): String? = try {
        sdkSampleServerAPI.fetchClientId()
    } catch (e: UnknownHostException) {
        payPalWebCheckoutError = APIClientError.payPalCheckoutError(e.message!!)
        isStartCheckoutLoading = false
        null
    } catch (e: HttpException) {
        payPalWebCheckoutError = APIClientError.payPalCheckoutError(e.message!!)
        isStartCheckoutLoading = false
        null
    }

    fun startWebCheckout(activity: AppCompatActivity) {
        isStartCheckoutLoading = true
        viewModelScope.launch {
            fetchClientId()?.let { clientId ->
                val coreConfig = CoreConfig(clientId)
                payPalDataCollector = PayPalDataCollector(coreConfig)
                paypalClient = PayPalWebCheckoutClient(activity, coreConfig, URL_SCHEME)
                paypalClient.listener = this@PayPalWebViewModel

                val orderId = createdOrder!!.id!!
                paypalClient.start(PayPalWebCheckoutRequest(orderId, fundingSource))
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")
        payPalWebCheckoutResult = result
        isStartCheckoutLoading = false
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebFailure(error: PayPalSDKError) {
        Log.i(TAG, "Checkout Error: ${error.errorDescription}")
        payPalWebCheckoutError = error
        isStartCheckoutLoading = false
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebCanceled() {
        Log.i(TAG, "User cancelled")
        isCheckoutCanceled = true
        isStartCheckoutLoading = false
    }

    fun completeOrder(context: Context) {
        viewModelScope.launch {
            isCompleteOrderLoading = true

            val cmid = payPalDataCollector.collectDeviceData(context)
            val orderId = createdOrder!!.id!!
            completedOrder = completeOrderUseCase(orderId, intentOption, cmid)
            isCompleteOrderLoading = false
        }
    }
}
