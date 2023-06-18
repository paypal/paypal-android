package com.paypal.android.ui.card.validation

import com.paypal.android.cardpayments.Card
import com.paypal.android.ui.card.CardOption

data class CardViewUiState(
    val focusedOption: CardOption? = null,
    val scaOption: String = "",
    val scaOptionExpanded: Boolean = false,
    val intentOption: String = "",
    val intentOptionExpanded: Boolean = false,
    val shouldVaultOption: String = "",
    val shouldVaultOptionExpanded: Boolean = false,
    val customerId: String = "",
    val statusText: String = "",
    val cardNumber: String = "",
    val expirationDate: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
) {
}
