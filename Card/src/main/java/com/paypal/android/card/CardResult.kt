package com.paypal.android.card

import com.paypal.android.core.OrderError
import com.paypal.android.core.OrderStatus

sealed class CardResult {

    class Success(
        val orderID: String,
        val status: OrderStatus
    ) : CardResult()

    class Error(
        val orderError: OrderError,
        val correlationID: String?
    ) : CardResult()
}
