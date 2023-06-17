package com.paypal.android.ui.card

import androidx.lifecycle.ViewModel
import com.paypal.android.cardpayments.Card
import com.paypal.android.ui.card.validation.CardFormatter
import com.paypal.android.ui.card.validation.CardViewUiState
import com.paypal.android.ui.card.validation.DateFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class CardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CardViewUiState())
    val uiState = _uiState.asStateFlow()

    private val _cardNumber = MutableStateFlow("")
    val cardNumber = _cardNumber.asStateFlow()

    private val _expirationDate = MutableStateFlow("")
    val expirationDate = _expirationDate.asStateFlow()

    val card = uiState.map { it.card }
    val statusText = uiState.map { it.statusText }.distinctUntilChanged()

    val scaOptionExpanded =
        uiState.map { it.focusedOption == CardOption.SCA }.distinctUntilChanged()
    val intentOptionExpanded =
        uiState.map { it.focusedOption == CardOption.INTENT }.distinctUntilChanged()
    val shouldVaultOptionExpanded =
        uiState.map { it.focusedOption == CardOption.SHOULD_VAULT }.distinctUntilChanged()

    val scaOption = uiState.map { it.scaOption }.distinctUntilChanged()
    var intentOption = uiState.map { it.intentOption }.distinctUntilChanged()
    var shouldVaultOption = uiState.map { it.shouldVaultOption }.distinctUntilChanged()
    var customerId = uiState.map { it.customerId }.distinctUntilChanged()

    fun onOptionChange(option: CardOption, value: String) {
        when (option) {
            CardOption.SCA -> _uiState.update { currentState -> currentState.copy(scaOption = value) }
            CardOption.INTENT -> _uiState.update { currentState -> currentState.copy(intentOption = value) }
            CardOption.SHOULD_VAULT -> _uiState.update { currentState ->
                currentState.copy(
                    shouldVaultOption = value
                )
            }
            CardOption.CUSTOMER_VAULT_ID -> _uiState.update { currentState ->
                currentState.copy(
                    customerId = value
                )
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

    fun updateCard(card: Card) {
        _uiState.update { currentState ->
            currentState.copy(card = card)
        }
    }

    fun onCardNumberChanged(newValue: String) {
        _cardNumber.value = newValue
    }

    fun onExpirationDateChanged(newValue: String) {
        _expirationDate.value = DateFormatter.formatExpirationDate(newValue, _expirationDate.value)
    }
}