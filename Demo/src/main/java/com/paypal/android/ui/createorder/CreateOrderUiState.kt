package com.paypal.android.ui.createorder

import com.paypal.android.api.model.OrderIntent

data class CreateOrderUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val isLoading: Boolean = false,
    val shouldVault: Boolean = false,
    val customerId: String = "",
)
