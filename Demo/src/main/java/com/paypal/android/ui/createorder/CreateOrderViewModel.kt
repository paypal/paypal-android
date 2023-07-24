package com.paypal.android.ui.createorder

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CreateOrderViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CreateOrderUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption: String
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    fun updateStatusText(statusText: String) {
        _uiState.update { currentState ->
            currentState.copy(statusText = statusText)
        }
    }
}
