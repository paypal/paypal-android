package com.paypal.android.ui.vault

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VaultViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState = _uiState.asStateFlow()

    var setupToken: String
        get() = _uiState.value.setupToken
        set(value) {
            _uiState.update { it.copy(setupToken = value) }
        }

    var isSetupTokenLoading: Boolean
        get() = _uiState.value.isSetupTokenLoading
        set(value) {
            _uiState.update { it.copy(isSetupTokenLoading = value) }
        }
    var customerId: String
        get() = _uiState.value.customerId
        set(value) {
            _uiState.update { it.copy(customerId = value) }
        }
}