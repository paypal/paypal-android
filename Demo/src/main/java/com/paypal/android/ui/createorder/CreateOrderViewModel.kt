package com.paypal.android.ui.createorder

import androidx.lifecycle.ViewModel
import com.paypal.android.cardpayments.OrderIntent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CreateOrderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    var shouldVault: Boolean
        get() = _uiState.value.shouldVault
        set(value) {
            _uiState.update { it.copy(shouldVault = value) }
        }

    var customerId: String
        get() = _uiState.value.customerId
        set(value) {
            _uiState.update { it.copy(customerId = value) }
        }

    var isLoading: Boolean
        get() = _uiState.value.isLoading
        set(value) {
            _uiState.update { it.copy(isLoading = value) }
        }
}
