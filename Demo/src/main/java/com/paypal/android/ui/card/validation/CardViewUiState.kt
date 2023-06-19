package com.paypal.android.ui.card.validation

import androidx.compose.runtime.Immutable
import com.paypal.android.ui.card.CardOption

@Immutable
data class CardViewUiState(
    val focusedOption: CardOption? = null,
    val scaOption: String = "",
    val intentOption: String = "",
    val shouldVaultOption: String = "",
    val customerId: String = "",
    val statusText: String = "",
    val cardNumber: String = "",
    val expirationDate: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
) {
    val scaOptionExpanded: Boolean = focusedOption?.let { it == CardOption.SCA } ?: false
    val intentOptionExpanded: Boolean = focusedOption?.let { it == CardOption.INTENT } ?: false
    val shouldVaultOptionExpanded: Boolean =
        focusedOption?.let { it == CardOption.SHOULD_VAULT } ?: false
}
