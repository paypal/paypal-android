package com.paypal.android.ui.paypalbuttons

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PayPalButtonsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PayPalButtonsUiState())
    val uiState = _uiState.asStateFlow()

    var selectedFundingType: ButtonFundingType
        get() = _uiState.value.fundingType
        set(value) {
            _uiState.update { it.copy(fundingType = value) }
        }
}