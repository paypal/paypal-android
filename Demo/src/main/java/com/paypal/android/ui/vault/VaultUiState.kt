package com.paypal.android.ui.vault

data class VaultUiState(
    val setupToken: String = "",
    val isCreateSetupTokenLoading: Boolean = false,
    val isUpdateSetupTokenLoading: Boolean = false,
    val customerId: String = "",
    val cardNumber: String = "",
    val cardExpirationDate: String = "",
    val cardSecurityCode: String = "",
)