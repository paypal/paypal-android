package com.paypal.android.ui.paypal

import com.paypal.android.api.model.Order
import com.paypal.android.cardpayments.OrderIntent

data class PayPalNativeUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val isCreateOrderLoading: Boolean = false,
    val createdOrder: Order? = null,
    val shippingPreference: ShippingPreferenceType = ShippingPreferenceType.GET_FROM_FILE,
    val isStartCheckoutLoading: Boolean = false,
)