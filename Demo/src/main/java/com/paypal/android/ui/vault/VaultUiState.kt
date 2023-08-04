package com.paypal.android.ui.vault

data class VaultUiState(
    val setupToken: String = "",
    val isSetupTokenLoading: Boolean = false,
    val customerId: String = "",
)