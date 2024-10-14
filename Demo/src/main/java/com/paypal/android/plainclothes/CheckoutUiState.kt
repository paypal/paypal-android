package com.paypal.android.plainclothes

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val isCardFormModalVisible: Boolean = false,
    val checkoutError: Throwable? = null
)