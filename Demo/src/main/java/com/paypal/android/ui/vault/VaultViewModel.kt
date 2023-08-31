package com.paypal.android.ui.vault

import androidx.lifecycle.ViewModel
import com.paypal.android.api.model.PaymentToken
import com.paypal.android.api.model.SetupToken
import com.paypal.android.cardpayments.VaultResult
import com.paypal.android.models.TestCard
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

    fun prefillCard(testCard: TestCard) {
        val card = testCard.card
        _uiState.update { currentState ->
            currentState.copy(
                cardNumber = card.number,
                cardExpirationDate = card.run { "$expirationMonth$expirationYear" },
                cardSecurityCode = card.securityCode
            )
        }
    }
}
