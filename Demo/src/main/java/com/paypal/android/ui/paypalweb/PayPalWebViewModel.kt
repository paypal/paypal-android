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
import com.paypal.android.uishared.state.ActionButtonState
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

    private var createOrderState
        get() = _uiState.value.createOrderState
        set(value) {
            _uiState.update { it.copy(createOrderState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionButtonState.Success)?.value

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
            createOrderState = ActionButtonState.Loading
            val orderRequest = _uiState.value.run {
                OrderRequest(orderIntent = intentOption, shouldVault = false)
            }
            val createdOrder = createOrderUseCase(orderRequest)
            createOrderState = ActionButtonState.Success(createdOrder)
        }
    }

    fun startWebCheckout(activity: AppCompatActivity) {
        payPalWebCheckoutState = ActionButtonState.Loading
        viewModelScope.launch {
            try {
                val clientId = sdkSampleServerAPI.fetchClientId()
                val coreConfig = CoreConfig(clientId)
                payPalDataCollector = PayPalDataCollector(coreConfig)

                paypalClient =
                    PayPalWebCheckoutClient(activity, coreConfig, "com.paypal.android.demo")
                paypalClient.listener = this@PayPalWebViewModel

                val orderId = createdOrder!!.id!!
                paypalClient.start(PayPalWebCheckoutRequest(orderId, fundingSource))
            } catch (e: UnknownHostException) {
                val error = APIClientError.payPalCheckoutError(e.message!!)
                payPalWebCheckoutState = ActionButtonState.Failure(error)
            } catch (e: HttpException) {
                val error = APIClientError.payPalCheckoutError(e.message!!)
                payPalWebCheckoutState = ActionButtonState.Failure(error)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebSuccess(result: PayPalWebCheckoutResult) {
        Log.i(TAG, "Order Approved: ${result.orderId} && ${result.payerId}")
        payPalWebCheckoutState = ActionButtonState.Success(result)
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebFailure(error: PayPalSDKError) {
        Log.i(TAG, "Checkout Error: ${error.errorDescription}")
        payPalWebCheckoutState = ActionButtonState.Failure(error)
    }

    @SuppressLint("SetTextI18n")
    override fun onPayPalWebCanceled() {
        Log.i(TAG, "User cancelled")
        val error = Exception("USER CANCELED")
        payPalWebCheckoutState = ActionButtonState.Failure(error)
    }

    fun completeOrder(context: Context) {
        viewModelScope.launch {
            completeOrderState = ActionButtonState.Loading
            val cmid = payPalDataCollector.collectDeviceData(context)
            val orderId = createdOrder!!.id!!
            val completedOrder = completeOrderUseCase(orderId, intentOption, cmid)
            completeOrderState = ActionButtonState.Success(completedOrder)
        }
    }
}
