package com.paypal.android.ui.card.validation

import com.paypal.android.cardpayments.Card
import com.paypal.android.ui.card.CardOption

data class CardViewUiState(
    val focusedOption: CardOption? = null,
    val scaOption: String = "",
    val intentOption: String = "",
    val shouldVaultOption: String = "",
    val customerId: String = "",
    val statusText: String = "",
    val card: Card = Card("", "", "", ""),
)
