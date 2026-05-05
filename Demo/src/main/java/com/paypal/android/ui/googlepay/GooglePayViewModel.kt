package com.paypal.android.ui.googlepay

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.api.services.MerchantIntegration
import com.paypal.android.api.services.SDKSampleServerAPI
import com.paypal.android.corepayments.CoreConfig
import com.paypal.android.googlepay.GooglePayCheckoutRequest
import com.paypal.android.googlepay.GooglePayClient
import com.paypal.android.googlepay.GooglePayFinishStartResult
import com.paypal.android.googlepay.GooglePayLaunchResult
import com.paypal.android.googlepay.GooglePayStartResult
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

    private val config = CoreConfig(SDKSampleServerAPI.clientId)
    private val googlePayClient = GooglePayClient(applicationContext, config)

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

    private var googlePayStartState
        get() = _uiState.value.googlePayStartState
        set(value) {
            _uiState.update { it.copy(googlePayStartState = value) }
        }

    private var googlePayFinishStartState
        get() = _uiState.value.googlePayFinishStartState
        set(value) {
            _uiState.update { it.copy(googlePayFinishStartState = value) }
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
                OrderRequest(
                    intent = intentOption,
                    shouldVaultOnSuccess = false,
                    appSwitchWhenEligible = false
                )
            }
            createOrderState = createOrderUseCase(orderRequest).mapToActionState()
        }
    }

    fun finishStart(result: GooglePayLaunchResult) {
        googlePayFinishStartState = ActionState.Loading
        val orderId = createdOrder?.id
        if (orderId != null) {
            viewModelScope.launch {
                val finishStartResult = googlePayClient.finishStart(result, orderId)
                googlePayFinishStartState = when (finishStartResult) {
                    is GooglePayFinishStartResult.Success -> ActionState.Success(finishStartResult)
                    is GooglePayFinishStartResult.Failure -> ActionState.Failure(finishStartResult.error)
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

    fun requestGooglePayLaunch() {
        googlePayStartState = ActionState.Loading
        viewModelScope.launch {
            val request =
                GooglePayCheckoutRequest(merchantId = MerchantIntegration.DEFAULT.merchantId)
            val result = googlePayClient.start(request)
            googlePayStartState = when (result) {
                is GooglePayStartResult.Success -> ActionState.Success(result)
                is GooglePayStartResult.Failure -> ActionState.Failure(result.error)
            }
        }
    }
}
