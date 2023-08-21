package com.paypal.android.ui.card.validation

import androidx.compose.runtime.Immutable
import com.paypal.android.api.model.Order
import com.paypal.android.cardpayments.OrderIntent

@Immutable
data class CardViewUiState(
    val order: Order? = null,
    val scaOption: String = "ALWAYS",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val isCreateOrderLoading: Boolean = false,
    val shouldVault: Boolean = false,
    val customerId: String = "",
    val isApproveOrderLoading: Boolean = false
)
