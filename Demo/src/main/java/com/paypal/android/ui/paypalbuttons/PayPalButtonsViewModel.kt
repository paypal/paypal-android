package com.paypal.android.ui.paypalbuttons

import androidx.lifecycle.ViewModel
import com.paypal.android.paymentbuttons.PayPalButtonColor
import com.paypal.android.paymentbuttons.PayPalButtonLabel
import com.paypal.android.paymentbuttons.PayPalCreditButtonColor
import com.paypal.android.paymentbuttons.PaymentButtonShape
import com.paypal.android.paymentbuttons.PaymentButtonSize
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

    var payPalButtonColor: PayPalButtonColor
        get() = _uiState.value.payPalButtonColor
        set(value) {
            _uiState.update { it.copy(payPalButtonColor = value) }
        }

    var payPalCreditButtonColor: PayPalCreditButtonColor
        get() = _uiState.value.payPalCreditButtonColor
        set(value) {
            _uiState.update { it.copy(payPalCreditButtonColor = value) }
        }

    var payPalButtonLabel: PayPalButtonLabel
        get() = _uiState.value.payPalButtonLabel
        set(value) {
            _uiState.update { it.copy(payPalButtonLabel = value) }
        }

    var customCornerRadius: Int?
        get() = _uiState.value.customCornerRadius
        set(value) {
            _uiState.update { it.copy(customCornerRadius = value) }
        }
    var paymentButtonShape: PaymentButtonShape
        get() = _uiState.value.paymentButtonShape
        set(value) {
            _uiState.update {
                it.copy(
                    paymentButtonShape = value,
                    customCornerRadius = null
                )
            }
        }

    var paymentButtonSize: PaymentButtonSize
        get() = _uiState.value.paymentButtonSize
        set(value) {
            _uiState.update { it.copy(paymentButtonSize = value) }
        }
}
