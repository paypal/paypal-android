package com.paypal.android.ui.paypalweb

import androidx.lifecycle.ViewModel
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutFundingSource
import com.paypal.android.paypalwebpayments.PayPalWebCheckoutResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PayPalWebViewModel : ViewModel() {
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
}
