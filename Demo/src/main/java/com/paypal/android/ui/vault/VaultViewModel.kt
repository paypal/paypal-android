package com.paypal.android.ui.vault

import androidx.lifecycle.ViewModel
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.cardpayments.Card
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.cardpayments.VaultResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VaultViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState = _uiState.asStateFlow()

    var setupToken: SetupToken?
        get() = _uiState.value.setupToken
        set(value) {
            _uiState.update { it.copy(setupToken = value) }
        }

    var paymentToken: PaymentToken?
        get() = _uiState.value.paymentToken
        set(value) {
            _uiState.update { it.copy(paymentToken = value) }
        }

    var isCreateSetupTokenLoading: Boolean
        get() = _uiState.value.isCreateSetupTokenLoading
        set(value) {
            _uiState.update { it.copy(isCreateSetupTokenLoading = value) }
        }

    var isUpdateSetupTokenLoading: Boolean
        get() = _uiState.value.isUpdateSetupTokenLoading
        set(value) {
            _uiState.update { it.copy(isUpdateSetupTokenLoading = value) }
        }

    var isCreatePaymentTokenLoading: Boolean
        get() = _uiState.value.isCreatePaymentTokenLoading
        set(value) {
            _uiState.update { it.copy(isCreatePaymentTokenLoading = value) }
        }

    var isCreateOrderLoading: Boolean
        get() = _uiState.value.isCreateOrderLoading
        set(value) {
            _uiState.update { it.copy(isCreateOrderLoading = value) }
        }

    var customerId: String
        get() = _uiState.value.customerId
        set(value) {
            _uiState.update { it.copy(customerId = value) }
        }

    var cardNumber: String
        get() = _uiState.value.cardNumber
        set(value) {
            _uiState.update { it.copy(cardNumber = value) }
        }

    var cardExpirationDate: String
        get() = _uiState.value.cardExpirationDate
        set(value) {
            _uiState.update { it.copy(cardExpirationDate = value) }
        }

    var cardSecurityCode: String
        get() = _uiState.value.cardSecurityCode
        set(value) {
            _uiState.update { it.copy(cardSecurityCode = value) }
        }

    var vaultResult: VaultResult?
        get() = _uiState.value.vaultResult
        set(value) {
            _uiState.update { it.copy(vaultResult = value) }
        }

    var orderIntent: OrderIntent
        get() = _uiState.value.orderIntent
        set(value) {
            _uiState.update { it.copy(orderIntent = value) }
        }

    var createdOrder: Order?
        get() = _uiState.value.createdOrder
        set(value) {
            _uiState.update { it.copy(createdOrder = value) }
        }

    fun prefillCard(card: Card) {
        _uiState.update { currentState ->
            currentState.copy(
                cardNumber = card.number,
                cardExpirationDate = card.run { "$expirationMonth$expirationYear" },
                cardSecurityCode = card.securityCode
            )
        }
    }
}
