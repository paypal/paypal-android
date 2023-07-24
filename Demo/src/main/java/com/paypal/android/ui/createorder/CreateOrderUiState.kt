package com.paypal.android.ui.createorder

data class CreateOrderUiState(
    val intentOption: String = "AUTHORIZE",
    val isLoading: Boolean = false,
)
