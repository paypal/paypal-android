package com.paypal.android.ui.card

import androidx.lifecycle.ViewModel
import com.paypal.android.api.model.Order
import com.paypal.android.api.model.OrderIntent
import com.paypal.android.cardpayments.model.CardResult
import com.paypal.android.models.TestCard
import com.paypal.android.ui.card.validation.CardViewUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CardViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(CardViewUiState())
    val uiState = _uiState.asStateFlow()

    var createdOrder: Order?
        get() = _uiState.value.createdOrder
        set(value) {
            _uiState.update { it.copy(createdOrder = value) }
        }

    var approveOrderResult: CardResult?
        get() = _uiState.value.approveOrderResult
        set(value) {
            _uiState.update { it.copy(approveOrderResult = value) }
        }

    var approveOrderErrorMessage: String?
        get() = _uiState.value.approveOrderErrorMessage
        set(value) {
            _uiState.update { it.copy(approveOrderErrorMessage = value) }
        }

    var completedOrder: Order?
        get() = _uiState.value.completedOrder
        set(value) {
            _uiState.update { it.copy(completedOrder = value) }
        }
    var scaOption: String
        get() = _uiState.value.scaOption
        set(value) {
            _uiState.update { it.copy(scaOption = value) }
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

    var intentOption: OrderIntent
        get() = _uiState.value.intentOption
        set(value) {
            _uiState.update { it.copy(intentOption = value) }
        }

    var shouldVault: Boolean
        get() = _uiState.value.shouldVault
        set(value) {
            _uiState.update { it.copy(shouldVault = value) }
        }

    var customerId: String
        get() = _uiState.value.customerId
        set(value) {
            _uiState.update { it.copy(customerId = value) }
        }

    var isCreateOrderLoading: Boolean
        get() = _uiState.value.isCreateOrderLoading
        set(value) {
            _uiState.update { it.copy(isCreateOrderLoading = value) }
        }
    var isApproveOrderLoading: Boolean
        get() = _uiState.value.isApproveOrderLoading
        set(value) {
            _uiState.update { it.copy(isApproveOrderLoading = value) }
        }

    var isCompleteOrderLoading: Boolean
        get() = _uiState.value.isCompleteOrderLoading
        set(value) {
            _uiState.update { it.copy(isCompleteOrderLoading = value) }
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
