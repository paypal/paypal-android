package com.paypal.android.ui.paypal

import com.paypal.android.cardpayments.OrderIntent

data class PayPalNativeUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val isCreateOrderLoading: Boolean = false,
)