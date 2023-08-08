package com.paypal.android.ui.card.validation

import androidx.compose.runtime.Immutable

@Immutable
data class CardViewUiState(
    val scaOption: String = "ALWAYS",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
)
