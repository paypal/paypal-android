package com.paypal.android.ui.card

import androidx.lifecycle.ViewModel
import com.paypal.android.cardpayments.Card
import com.paypal.android.ui.card.validation.CardViewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CardViewUiState())
    val uiState = _uiState.asStateFlow()

    fun updateCardNumber(value: String) {
        _uiState.update { currentState -> currentState.copy(cardNumber = value) }
    }

    fun updateCardExpirationDate(value: String) {
        _uiState.update { currentState -> currentState.copy(cardExpirationDate = value) }
    }

    fun updateCardSecurityCode(value: String) {
        _uiState.update { currentState -> currentState.copy(cardSecurityCode = value) }
    }

    fun updateSCA(value: String) {
        _uiState.update { currentState -> currentState.copy(scaOption = value) }
    }

    fun updateIntent(value: String) {
        _uiState.update { currentState -> currentState.copy(intentOption = value) }
    }

    fun updateShouldVault(value: String) {
        _uiState.update { currentState -> currentState.copy(shouldVaultOption = value) }
    }

    fun updateVaultCustomerId(value: String) {
        _uiState.update { currentState -> currentState.copy(customerId = value) }
    }

    fun onFocusChange(option: CardOption) {
        _uiState.update { currentState ->
            currentState.copy(focusedOption = option)
        }
    }

    fun clearFocus() {
        _uiState.update { currentState ->
            currentState.copy(focusedOption = null)
        }
    }

    fun updateStatusText(statusText: String) {
        _uiState.update { currentState ->
            currentState.copy(statusText = statusText)
        }
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