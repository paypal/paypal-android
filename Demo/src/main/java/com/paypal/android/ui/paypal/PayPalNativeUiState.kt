package com.paypal.android.ui.paypal

import com.paypal.android.api.model.Order
import com.paypal.android.cardpayments.OrderIntent
import com.paypal.android.corepayments.PayPalSDKError
import com.paypal.android.paypalnativepayments.PayPalNativeCheckoutResult

data class PayPalNativeUiState(
    val intentOption: OrderIntent = OrderIntent.AUTHORIZE,
    val isCreateOrderLoading: Boolean = false,
    val isCompleteOrderLoading: Boolean = false,
    val createdOrder: Order? = null,
    val completedOrder: Order? = null,
    val shippingPreference: ShippingPreferenceType = ShippingPreferenceType.GET_FROM_FILE,
    val isStartCheckoutLoading: Boolean = false,
    val isCheckoutCanceled: Boolean = false,
    val payPalNativeCheckoutResult: PayPalNativeCheckoutResult? = null,
    val payPalNativeCheckoutError: PayPalSDKError? = null
)
