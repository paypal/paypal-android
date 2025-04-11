package com.paypal.android.ui.googlepay

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerResult
import com.paypal.android.corepayments.ApproveGooglePayPaymentResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.GooglePayClient
import com.paypal.android.models.OrderRequest
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
class GooglePayViewModel @Inject constructor(
    val getClientIdUseCase: GetClientIdUseCase,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase
) : ViewModel() {

    private var googlePayClient: GooglePayClient? = null

    private val _uiState = MutableStateFlow(GooglePayUiState())
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

    private var googlePayState
        get() = _uiState.value.googlePayState
        set(value) {
            _uiState.update { it.copy(googlePayState = value) }
        }

    private val createdOrder: Order?
        get() = (createOrderState as? ActionState.Success)?.value

    fun createOrder() {
        viewModelScope.launch {
            createOrderState = ActionState.Loading
            val orderRequest = _uiState.value.run {
                OrderRequest(intent = intentOption, shouldVault = false)
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    suspend fun launchGooglePay(activity: ComponentActivity): Task<PaymentData> {
        googlePayState = ActionState.Loading
        when (val clientIdResult = getClientIdUseCase()) {
            is SDKSampleServerResult.Failure -> TODO("handle failure")
            is SDKSampleServerResult.Success -> {
                val coreConfig = CoreConfig(clientIdResult.value)
                googlePayClient = GooglePayClient(activity, coreConfig)
                return googlePayClient!!.start()
            }
        }
    }

    fun completeGooglePayLaunch(result: ApiTaskResult<PaymentData>) {
        viewModelScope.launch {
            val orderId = createdOrder!!.id!!
            when (val confirmOrderResult = googlePayClient!!.confirmOrder(orderId, result)) {
                is ApproveGooglePayPaymentResult.Success -> {
                    googlePayState = ActionState.Success(confirmOrderResult)
                }

                is ApproveGooglePayPaymentResult.Failure -> {
                    // TODO: handle error
                }
            }
        }
    }

    private suspend fun completeOrder(orderId: String) {
        val orderIntent = OrderIntent.CAPTURE
        val completeOrderResult =
            completeOrderUseCase(orderId = orderId, intent = orderIntent, clientMetadataId = "")
        when (completeOrderResult) {
            is SDKSampleServerResult.Success -> {
                val order = completeOrderResult.value
                Log.d("GooglePayViewModel", "Order Complete: $order")
            }

            is SDKSampleServerResult.Failure -> {
                // TODO: handle error
            }
        }
    }
}