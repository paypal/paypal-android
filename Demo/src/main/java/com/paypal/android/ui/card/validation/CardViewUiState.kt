package com.paypal.android.ui.card.validation

import androidx.compose.runtime.Immutable
import com.paypal.android.ui.card.CardOption

@Immutable
data class CardViewUiState(
    val focusedOption: CardOption? = null,
    val scaOption: String = "",
    val customerId: String = "",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
) {
    val scaOptionExpanded: Boolean = focusedOption?.let { it == CardOption.SCA } ?: false
}
