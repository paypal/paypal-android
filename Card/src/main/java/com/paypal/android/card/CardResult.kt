package com.paypal.android.card

import com.paypal.android.core.OrderStatus
import com.paypal.android.core.CoreSDKError

sealed class CardResult {

    class Success(
        val orderID: String,
        val status: OrderStatus
    ) : CardResult()

    class Error(
        val coreSDKError: CoreSDKError,
        val correlationID: String?
    ) : CardResult()
}
