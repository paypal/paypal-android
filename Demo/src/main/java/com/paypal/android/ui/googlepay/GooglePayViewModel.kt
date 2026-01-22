package com.paypal.android.ui.googlepay

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.contract.ApiTaskResult
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.ApproveGooglePayPaymentResult
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.corepayments.GooglePayClient
import com.paypal.android.models.OrderRequest
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
class GooglePayViewModel @Inject constructor(
    @ApplicationContext val applicationContext: Context,
    val createOrderUseCase: CreateOrderUseCase,
    val completeOrderUseCase: CompleteOrderUseCase
) : ViewModel() {

    private val coreConfig = CoreConfig(SDKSampleServerAPI.clientId)
    private val googlePayClient = GooglePayClient(applicationContext, coreConfig)

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

    private var completeOrderState
        get() = _uiState.value.completeOrderState
        set(value) {
            _uiState.update { it.copy(completeOrderState = value) }
        }

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
        return googlePayClient.start()
    }

    fun completeGooglePayLaunch(result: ApiTaskResult<PaymentData>) {
        viewModelScope.launch {
            val orderId = createdOrder!!.id!!
            when (val confirmOrderResult = googlePayClient.confirmOrder(orderId, result)) {
                is ApproveGooglePayPaymentResult.Success -> {
                    googlePayState = ActionState.Success(confirmOrderResult)
                }

                is ApproveGooglePayPaymentResult.Failure -> {
                    googlePayState = ActionState.Failure(confirmOrderResult.error)
                }
            }
        }
    }

    fun completeOrder() {
        val orderId = createdOrder?.id
        if (orderId == null) {
            completeOrderState = ActionState.Failure(Exception("Create an order to continue."))
        } else {
            viewModelScope.launch {
                completeOrderState = ActionState.Loading
                completeOrderState =
                    completeOrderUseCase(orderId, intentOption, "").mapToActionState()
            }
        }
    }
}