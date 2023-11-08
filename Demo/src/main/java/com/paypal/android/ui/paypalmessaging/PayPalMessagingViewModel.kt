package com.paypal.android.ui.paypalmessaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paypal.android.api.services.SDKSampleServerAPI
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PayPalMessagingViewModel @Inject constructor(
    val sdkSampleServerAPI: SDKSampleServerAPI
) : ViewModel() {

    private val _clientId = MutableStateFlow("")
    val clientId = _clientId.asStateFlow()

    fun fetchClientId() {
        viewModelScope.launch {
            _clientId.update { sdkSampleServerAPI.fetchClientId() }
        }
    }
}
