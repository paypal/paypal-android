package com.paypal.android.ui.paypalweb

import com.paypal.android.api.model.Order
import com.paypal.android.cardpayments.OrderIntent

data class PayPalWebUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val isLoading: Boolean = false,
    val shouldVault: Boolean = false,
    val customerId: String = "",
    val createdOrder: Order? = null,
)
