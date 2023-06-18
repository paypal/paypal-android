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

    fun onOptionChange(option: CardOption, value: String) {
        when (option) {
            CardOption.SCA -> _uiState.update { currentState -> currentState.copy(scaOption = value) }
            CardOption.INTENT -> _uiState.update { currentState -> currentState.copy(intentOption = value) }
            CardOption.SHOULD_VAULT -> _uiState.update { currentState ->
                currentState.copy(shouldVaultOption = value)
            }

            CardOption.CUSTOMER_VAULT_ID -> _uiState.update { currentState ->
                currentState.copy(customerId = value)
            }

            CardOption.CARD_NUMBER -> _uiState.update { currentState ->
                currentState.copy(cardNumber = value)
            }

            CardOption.CARD_EXPIRATION_DATE -> _uiState.update { currentState ->
                currentState.copy(cardExpirationDate = value)
            }

            CardOption.CARD_SECURITY_CODE -> _uiState.update { currentState ->
                currentState.copy(cardSecurityCode = value)
            }
        }
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

    fun applyCardToCardFields(card: Card) {
        _uiState.update { currentState ->
            currentState.copy(
                cardNumber = card.number,
                cardExpirationDate = card.run { "$expirationMonth/$expirationYear" },
                cardSecurityCode = card.securityCode
            )
        }
    }
}