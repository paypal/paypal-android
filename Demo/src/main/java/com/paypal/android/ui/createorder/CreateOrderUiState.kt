package com.paypal.android.ui.createorder

data class CreateOrderUiState(
    val intentOption: String = "AUTHORIZE",
    val isLoading: Boolean = false,
    val shouldVault: Boolean = false,
    val customerId: String = "",
)
