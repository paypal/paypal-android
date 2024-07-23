package com.paypal.android.ui.venmocheckout

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class VenmoCheckoutViewModel @Inject constructor() : ViewModel() {

    companion object {
        const val TAG = "VenmoViewModel"
    }

    private val _uiState = MutableStateFlow(VenmoCheckoutUiState())
    val uiState = _uiState.asStateFlow()

    var intentOption
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }
}
