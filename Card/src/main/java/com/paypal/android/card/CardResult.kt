package com.paypal.android.card

import com.paypal.android.core.OrderStatus
import com.paypal.android.core.PayPalSDKError

sealed class CardResult {

    class Success(
        val orderID: String,
        val status: OrderStatus
    ) : CardResult()

    class Error(
        val payPalSDKError: PayPalSDKError,
        val correlationID: String?
    ) : CardResult()
}
