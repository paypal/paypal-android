package com.paypal.android.ui.card

import androidx.lifecycle.ViewModel
import com.paypal.android.cardpayments.Card
import com.paypal.android.ui.card.validation.CardViewUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class CardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CardViewUiState())
    val uiState = _uiState.asStateFlow()

    fun onValueChange(option: CardOption, value: String) {
        _uiState.update { currentState ->
            when (option) {
                CardOption.SCA -> currentState.copy(scaOption = value)
                CardOption.INTENT -> currentState.copy(intentOption = value)
                CardOption.SHOULD_VAULT -> currentState.copy(shouldVaultOption = value)
                CardOption.VAULT_CUSTOMER_ID -> currentState.copy(customerId = value)
                CardOption.CARD_NUMBER -> currentState.copy(cardNumber = value)
                CardOption.CARD_EXPIRATION_DATE -> currentState.copy(cardExpirationDate = value)
                CardOption.CARD_SECURITY_CODE -> currentState.copy(cardSecurityCode = value)
            }
        }
    }

    fun onOptionFocus(option: CardOption) {
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

    fun updateOrderDetailsText(orderDetails: String) {
        _uiState.update { currentState ->
            currentState.copy(orderDetails = orderDetails)
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
